package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;

/** Gestos: /spin (giro em pé até andar), /sit (senta), /wave (acena) e /crawl (rasteja). */
public final class Gestos {
    private Gestos() {}

    /** Quem está girando: posição de partida (para saber se andou) e o ângulo atual. */
    private static final class Giro {
        final Vec3 inicio;
        float angulo;

        Giro(Vec3 inicio, float angulo) {
            this.inicio = inicio;
            this.angulo = angulo;
        }
    }

    private static final Map<UUID, Giro> GIRANDO = new HashMap<>();
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
        d.register(literal("gestos").executes(c -> {
            abrirMenu(c.getSource().getPlayerOrException());
            return 1;
        }));
        d.register(literal("spin").executes(c -> girar(c.getSource().getPlayerOrException())));
        d.register(literal("point").executes(c -> {
            apontar(c.getSource().getPlayerOrException());
            return 1;
        }));
        d.register(literal("wave").executes(c -> acenar(c.getSource().getPlayerOrException())));
        d.register(literal("crawl").executes(c -> rastejar(c.getSource().getPlayerOrException())));
        d.register(literal("sit").executes(c -> sentar(c.getSource().getPlayerOrException())));
    }

    // ---------- as ações (usadas pelos comandos e pelo menu /gestos) ----------

    static int girar(ServerPlayer p) {
        if (GIRANDO.containsKey(p.getUUID())) {
            pararDeGirar(p);
            p.sendSystemMessage(Jogo.prefixo("Parou de girar."));
        } else {
            GIRANDO.put(p.getUUID(), new Giro(p.position(), p.getYRot()));
            p.sendSystemMessage(Jogo.prefixo("Girando! Ande para parar."));
        }
        return 1;
    }

    static int acenar(ServerPlayer p) {
        p.swing(InteractionHand.MAIN_HAND, true);
        return 1;
    }

    static int rastejar(ServerPlayer p) {
        if (RASTEJANDO.contains(p.getUUID())) {
            pararDeRastejar(p);
            p.sendSystemMessage(Jogo.prefixo("Você levantou."));
            return 1;
        }
        if (p.getVehicle() != null) {
            p.sendSystemMessage(Jogo.prefixo("Desça primeiro."));
            return 0;
        }
        pararDeGirar(p);
        RASTEJANDO.add(p.getUUID());
        p.sendSystemMessage(Jogo.prefixo("Rastejando. Use /crawl de novo para levantar."));
        return 1;
    }

    static int sentar(ServerPlayer p) {
        if (BANCOS.containsKey(p.getUUID()) || p.getVehicle() != null) {
            levantar(p);
            return 1;
        }
        if (!p.onGround()) {
            p.sendSystemMessage(Jogo.prefixo("Só dá para sentar no chão."));
            return 0;
        }
        pararDeGirar(p);
        pararDeRastejar(p);
        ArmorStand banco = new ArmorStand(p.level(), p.getX(), p.getY() - Jogo.casa.config.bancoAltura, p.getZ());
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
    }

    // ---------- menu /gestos ----------

    private static GuiElementBuilder item(net.minecraft.world.item.Item icone, String nome, ChatFormatting cor, String... lore) {
        GuiElementBuilder e = new GuiElementBuilder(icone).setName(Component.literal(nome).withStyle(cor, ChatFormatting.BOLD)).hideDefaultTooltip();
        for (String l : lore) e.addLoreLine(Component.literal(l).withStyle(ChatFormatting.GRAY));
        return e;
    }

    public static void abrirMenu(ServerPlayer p) {
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        g.setTitle(Component.literal("Gestos").withStyle(ChatFormatting.GOLD));
        g.setSlot(10, item(Items.ENDER_PEARL, "Girar (/spin)", ChatFormatting.LIGHT_PURPLE, "Gira em pé até você andar.")
            .setCallback((i, t, a, gg) -> {
                gg.close();
                girar(p);
            }).build());
        g.setSlot(11, item(Items.SADDLE, "Sentar (/sit)", ChatFormatting.GREEN, "Senta no chão; shift levanta.")
            .setCallback((i, t, a, gg) -> {
                gg.close();
                sentar(p);
            }).build());
        g.setSlot(12, item(Items.TOTEM_OF_UNDYING, "Acenar (/wave)", ChatFormatting.YELLOW, "Acena com o braço.")
            .setCallback((i, t, a, gg) -> {
                gg.close();
                acenar(p);
            }).build());
        g.setSlot(13, item(Items.SPYGLASS, "Apontar (/point)", ChatFormatting.AQUA, "Marca onde você está olhando e", "manda as coordenadas a quem está perto.")
            .setCallback((i, t, a, gg) -> {
                gg.close();
                apontar(p);
            }).build());
        g.setSlot(14, item(Items.LEATHER_BOOTS, "Rastejar (/crawl)", ChatFormatting.GOLD, "Rasteja em qualquer lugar;", "use de novo para levantar.")
            .setCallback((i, t, a, gg) -> {
                gg.close();
                rastejar(p);
            }).build());
        g.setSlot(22, item(Items.BARRIER, "Fechar", ChatFormatting.RED).setCallback((i, t, a, gg) -> gg.close()).build());
        g.open();
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

    private static void levantar(ServerPlayer p) {
        ArmorStand banco = BANCOS.remove(p.getUUID());
        if (p.getVehicle() != null) p.stopRiding();
        if (banco != null) banco.discard();
    }

    // ---------- /point ----------

    private static final class Marca {
        final ServerLevel nivel;
        final Vec3 pos;
        final long fim;

        Marca(ServerLevel nivel, Vec3 pos, long fim) {
            this.nivel = nivel;
            this.pos = pos;
            this.fim = fim;
        }
    }

    private static final List<Marca> MARCAS = new ArrayList<>();
    private static final Map<UUID, Long> ULTIMO_APONTAR = new HashMap<>();

    private static void apontar(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel nivel)) return;
        long agora = System.currentTimeMillis();
        Long ult = ULTIMO_APONTAR.get(p.getUUID());
        if (ult != null && agora - ult < 3000) {
            p.sendSystemMessage(Jogo.prefixo("Calma, espere uns segundos para apontar de novo."));
            return;
        }
        Vec3 olho = p.getEyePosition();
        Vec3 fim = olho.add(p.getLookAngle().scale(120));
        BlockHitResult r = nivel.clip(new ClipContext(olho, fim, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        if (r.getType() == HitResult.Type.MISS) {
            p.sendSystemMessage(Jogo.prefixo("Não há nada para apontar daqui (muito longe ou céu)."));
            return;
        }
        ULTIMO_APONTAR.put(p.getUUID(), agora);
        Vec3 alvo = r.getLocation();
        MARCAS.add(new Marca(nivel, alvo, agora + 6000));
        p.swing(InteractionHand.MAIN_HAND, true);
        int bx = (int) Math.floor(alvo.x);
        int by = (int) Math.floor(alvo.y);
        int bz = (int) Math.floor(alvo.z);
        String xyz = bx + " " + by + " " + bz;
        long dist = Math.round(olho.distanceTo(alvo));
        Component msg = Component.literal(p.getGameProfile().name() + " apontou para " + xyz + " (" + dist + " blocos) ")
            .withStyle(net.minecraft.ChatFormatting.AQUA)
            .append(Component.literal("[Copiar]").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(xyz))));
        for (ServerPlayer o : Jogo.server.getPlayerList().getPlayers()) {
            if (o.level() != p.level() || o.distanceToSqr(p) > 160.0 * 160.0) continue;
            o.sendSystemMessage(msg);
            Jogo.som(o, "minecraft:block.amethyst_block.chime", 1.3f);
        }
    }

    private static void desenharMarcas() {
        long agora = System.currentTimeMillis();
        MARCAS.removeIf(m -> agora > m.fim);
        for (Marca m : MARCAS) {
            for (int k = 0; k < 7; k++) {
                m.nivel.sendParticles(ParticleTypes.END_ROD, m.pos.x, m.pos.y + k * 0.3, m.pos.z, 1, 0.05, 0.0, 0.05, 0.0);
            }
        }
    }

    private static final float PASSO_GIRO = 30.0f;

    /** Manda a rotação do corpo e da cabeça para quem está perto, sem mexer na câmera de ninguém. */
    private static void mostrarRotacao(ServerPlayer p, float graus) {
        byte y = Mth.packDegrees(graus);
        byte x = Mth.packDegrees(p.getXRot());
        var corpo = new ClientboundMoveEntityPacket.Rot(p.getId(), y, x, p.onGround());
        var cabeca = new ClientboundRotateHeadPacket(p, y);
        for (ServerPlayer o : Jogo.server.getPlayerList().getPlayers()) {
            if (o.level() != p.level() || o.distanceToSqr(p) > 96.0 * 96.0) continue;
            o.connection.send(corpo);
            o.connection.send(cabeca);
        }
    }

    private static void pararDeGirar(ServerPlayer p) {
        if (GIRANDO.remove(p.getUUID()) != null) mostrarRotacao(p, p.getYRot());
    }

    /** Chamado a cada tick do servidor. */
    public static void aCadaTick() {
        if (Jogo.server == null) return;
        ciclo++;
        if (ciclo % 4 == 0 && !MARCAS.isEmpty()) desenharMarcas();
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
                Giro giro = e.getValue();
                double dx = p.getX() - giro.inicio.x;
                double dz = p.getZ() - giro.inicio.z;
                if (dx * dx + dz * dz > 0.0016) {
                    it.remove();
                    mostrarRotacao(p, p.getYRot());
                    continue;
                }
                giro.angulo += PASSO_GIRO;
                mostrarRotacao(p, giro.angulo);
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
