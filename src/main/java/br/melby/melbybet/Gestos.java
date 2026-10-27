package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;

/** Gestos: /spin (giro de correnteza do tridente até andar), /sit (senta), /wave (acena) e /crawl (rasteja). */
public final class Gestos {
    private Gestos() {}

    private static final Map<UUID, Vec3> GIRANDO = new HashMap<>();
    private static final Map<UUID, ArmorStand> BANCOS = new HashMap<>();
    private static final Set<UUID> RASTEJANDO = ConcurrentHashMap.newKeySet();
    /** Blocos falsos (barreiras que só o próprio jogador vê) acima da cabeça de quem rasteja. */
    private static final Map<UUID, Set<BlockPos>> FALSOS = new HashMap<>();
    private static int ciclo;

    /** Usado pelo mixin: o servidor marca este jogador como rastejando para todos verem. */
    public static boolean rastejando(ServerPlayer p) {
        return RASTEJANDO.contains(p.getUUID());
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("spin").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (GIRANDO.remove(p.getUUID()) != null) {
                p.sendSystemMessage(Jogo.prefixo("Parou de girar."));
            } else {
                GIRANDO.put(p.getUUID(), p.position());
                girar(p);
                p.sendSystemMessage(Jogo.prefixo("Girando! Ande para parar."));
            }
            return 1;
        }));
        d.register(literal("wave").executes(c -> {
            c.getSource().getPlayerOrException().swing(InteractionHand.MAIN_HAND, true);
            return 1;
        }));
        d.register(literal("crawl").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (RASTEJANDO.contains(p.getUUID())) {
                pararDeRastejar(p);
                p.sendSystemMessage(Jogo.prefixo("Você levantou."));
                return 1;
            }
            if (p.getVehicle() != null) {
                p.sendSystemMessage(Jogo.prefixo("Desça primeiro."));
                return 0;
            }
            GIRANDO.remove(p.getUUID());
            RASTEJANDO.add(p.getUUID());
            p.sendSystemMessage(Jogo.prefixo("Rastejando. Use /crawl de novo para levantar."));
            return 1;
        }));
        d.register(literal("sit").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (BANCOS.containsKey(p.getUUID()) || p.getVehicle() != null) {
                levantar(p);
                return 1;
            }
            if (!p.onGround()) {
                p.sendSystemMessage(Jogo.prefixo("Só dá para sentar no chão."));
                return 0;
            }
            GIRANDO.remove(p.getUUID());
            pararDeRastejar(p);
            ArmorStand banco = new ArmorStand(p.level(), p.getX(), p.getY() - ALTURA_BANCO, p.getZ());
            banco.setYRot(p.getYRot());
            banco.setInvisible(true);
            banco.setNoGravity(true);
            banco.setInvulnerable(true);
            banco.setSilent(true);
            p.level().addFreshEntity(banco);
            p.startRiding(banco, true, false);
            BANCOS.put(p.getUUID(), banco);
            p.sendSystemMessage(Jogo.prefixo("Sentado. Aperte shift para levantar."));
            return 1;
        }));
    }

    private static void enviarBloco(ServerPlayer p, BlockPos pos, boolean falso) {
        var estado = falso ? Blocks.BARRIER.defaultBlockState() : p.level().getBlockState(pos);
        p.connection.send(new ClientboundBlockUpdatePacket(pos, estado));
    }

    /** Remove os blocos falsos (devolvendo o bloco real na tela do jogador) e deixa de marcar como rastejando. */
    private static void pararDeRastejar(ServerPlayer p) {
        RASTEJANDO.remove(p.getUUID());
        Set<BlockPos> falsos = FALSOS.remove(p.getUUID());
        if (falsos != null && p != null) for (BlockPos pos : falsos) enviarBloco(p, pos, false);
    }

    private static void manterRastejo(ServerPlayer p) {
        BlockPos centro = p.blockPosition().above();
        Set<BlockPos> novos = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = centro.offset(dx, 0, dz);
                if (p.level().getBlockState(pos).isAir()) novos.add(pos);
            }
        }
        Set<BlockPos> antigos = FALSOS.getOrDefault(p.getUUID(), new HashSet<>());
        for (BlockPos pos : antigos) if (!novos.contains(pos)) enviarBloco(p, pos, false);
        boolean reenviar = ciclo % 10 == 0;
        for (BlockPos pos : novos) if (reenviar || !antigos.contains(pos)) enviarBloco(p, pos, true);
        FALSOS.put(p.getUUID(), novos);
    }

    /** Quanto o banco invisível fica abaixo dos pés, para o jogador sentar rente ao chão. */
    private static final double ALTURA_BANCO = 1.1;

    private static void levantar(ServerPlayer p) {
        ArmorStand banco = BANCOS.remove(p.getUUID());
        if (p.getVehicle() != null) p.stopRiding();
        if (banco != null) banco.discard();
    }

    /** Liga a animação de correnteza. Com 2 ticks e renovada a cada tick, ela acaba logo que paramos de renovar. */
    private static void girar(ServerPlayer p) {
        p.startAutoSpinAttack(2, 0.0f, new ItemStack(Items.TRIDENT));
    }

    /** Chamado a cada tick do servidor. */
    public static void aCadaTick() {
        if (Jogo.server == null) return;
        ciclo++;
        if (!RASTEJANDO.isEmpty()) {
            for (UUID id : new HashSet<>(RASTEJANDO)) {
                ServerPlayer p = Jogo.server.getPlayerList().getPlayer(id);
                if (p == null) {
                    RASTEJANDO.remove(id);
                    FALSOS.remove(id);
                } else if (!p.isAlive() || p.getVehicle() != null) {
                    pararDeRastejar(p);
                } else {
                    manterRastejo(p);
                }
            }
        }
        if (!GIRANDO.isEmpty()) {
            var it = GIRANDO.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                ServerPlayer p = Jogo.server.getPlayerList().getPlayer(e.getKey());
                if (p == null || !p.isAlive()) {
                    it.remove();
                    continue;
                }
                Vec3 antes = e.getValue();
                double dx = p.getX() - antes.x;
                double dz = p.getZ() - antes.z;
                if (dx * dx + dz * dz > 0.0016) {
                    it.remove();
                    continue;
                }
                girar(p);
            }
        }
        if (!BANCOS.isEmpty()) {
            var it = BANCOS.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                ServerPlayer p = Jogo.server.getPlayerList().getPlayer(e.getKey());
                ArmorStand banco = e.getValue();
                if (p == null || !p.isAlive() || p.getVehicle() != banco) {
                    if (p != null && p.getVehicle() == banco) p.stopRiding();
                    banco.discard();
                    it.remove();
                }
            }
        }
    }
}
