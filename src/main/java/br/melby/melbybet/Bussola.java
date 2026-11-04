package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;

import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /bussola <x> <z> (ou <x> <y> <z>): com uma bússola na mão, faz a agulha apontar para aquelas coordenadas.
 * Usa o rastreador de pedra-ímã do próprio jogo, então não precisa de nada no cliente. O Y é ignorado.
 */
public final class Bussola {
    private Bussola() {}

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("bussola")
            .then(literal("reset").executes(c -> limpar(c.getSource().getPlayerOrException())))
            .then(argument("x", IntegerArgumentType.integer())
                .then(argument("z", IntegerArgumentType.integer()).executes(c -> apontar(c.getSource().getPlayerOrException(),
                    IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"))))
                .then(argument("y", IntegerArgumentType.integer())
                    .then(argument("z", IntegerArgumentType.integer()).executes(c -> apontar(c.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"))))))
            .executes(c -> {
                aviso(c.getSource().getPlayerOrException(), "Use /bussola <x> <z> segurando uma bússola. /bussola reset volta ao normal.", ChatFormatting.YELLOW);
                return 0;
            }));
    }

    private static ItemStack bussolaNaMao(ServerPlayer p) {
        ItemStack a = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (a.is(Items.COMPASS)) return a;
        ItemStack b = p.getItemInHand(InteractionHand.OFF_HAND);
        return b.is(Items.COMPASS) ? b : null;
    }

    private static int apontar(ServerPlayer p, int x, int z) {
        ItemStack st = bussolaNaMao(p);
        if (st == null) return erro(p, "Segure uma bússola na mão.");
        BlockPos alvo = new BlockPos(x, p.blockPosition().getY(), z);
        st.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(p.level().dimension(), alvo)), false));
        p.containerMenu.broadcastChanges();
        double dx = x + 0.5 - p.getX(), dz = z + 0.5 - p.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        aviso(p, "Bússola apontando para " + x + ", " + z + " (a " + dist + " blocos). Só funciona nesta dimensão.", ChatFormatting.GREEN);
        Jogo.som(p, "minecraft:item.lodestone_compass.lock", 1.0f);
        return 1;
    }

    private static int limpar(ServerPlayer p) {
        ItemStack st = bussolaNaMao(p);
        if (st == null) return erro(p, "Segure uma bússola na mão.");
        st.remove(DataComponents.LODESTONE_TRACKER);
        p.containerMenu.broadcastChanges();
        aviso(p, "Bússola de volta ao normal.", ChatFormatting.GREEN);
        return 1;
    }

    private static void aviso(ServerPlayer p, String s, ChatFormatting cor) {
        p.sendSystemMessage(Component.literal(s).withStyle(cor), true);
    }

    private static int erro(ServerPlayer p, String s) {
        aviso(p, s, ChatFormatting.RED);
        return 0;
    }
}
