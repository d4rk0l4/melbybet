package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;

/** Gestos: /spin (giro de correnteza do tridente até andar), /sit (senta) e /wave (acena). */
public final class Gestos {
    private Gestos() {}

    private static final Map<UUID, Vec3> GIRANDO = new HashMap<>();
    private static final Map<UUID, ArmorStand> BANCOS = new HashMap<>();

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
            ArmorStand banco = new ArmorStand(EntityType.ARMOR_STAND, p.level());
            banco.setPos(p.getX(), p.getY() - ALTURA_BANCO, p.getZ());
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
