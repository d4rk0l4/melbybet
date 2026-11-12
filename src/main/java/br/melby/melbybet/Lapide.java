package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /lapide [texto]: uma lápide decorativa de pedra com texto flutuante. /lapide remover tira o texto da sua lápide mais próxima. */
public final class Lapide {
    private Lapide() {}

    private static final String TAG = "mblapide";

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("lapide")
            .executes(c -> construir(c.getSource().getPlayerOrException(), "Descanse em paz"))
            .then(literal("remover").executes(c -> remover(c.getSource().getPlayerOrException())))
            .then(argument("texto", StringArgumentType.greedyString())
                .executes(c -> construir(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "texto")))));
    }

    private static int construir(ServerPlayer p, String bruto) {
        String texto = bruto.replaceAll("[\"'\\\\§]", "").replaceAll("\\s+", " ").trim();
        if (texto.length() > 40) texto = texto.substring(0, 40);
        if (texto.isEmpty()) texto = "Descanse em paz";
        ServerLevel nivel = (ServerLevel) p.level();
        Direction dir = p.getDirection();
        BlockPos base = p.blockPosition().relative(dir, 2);
        BlockPos topo = base.above();
        boolean livre = nivel.getBlockState(base).isAir() && nivel.getBlockState(topo).isAir();
        boolean chao = nivel.getBlockState(base.below()).isFaceSturdy(nivel, base.below(), Direction.UP);
        if (!livre || !chao) {
            p.sendSystemMessage(Jogo.prefixo("Precisa de 2 blocos de ar livres, 2 passos à sua frente, com chão firme."));
            return 0;
        }
        nivel.setBlock(base, Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);
        nivel.setBlock(topo, Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);
        // texto no lado que olha para você
        double x = base.getX() + 0.5 - dir.getStepX() * 0.55;
        double z = base.getZ() + 0.5 - dir.getStepZ() * 0.55;
        double y = base.getY() + 1.15;
        float yaw = p.getYRot() + 180f;
        String nbt = "{Tags:[\"" + TAG + "\",\"" + TAG + "_" + p.getUUID() + "\"],text:{text:\"Aqui jaz\\n" + texto + "\",color:\"gray\"},line_width:110,Rotation:[" + yaw + "f,0f]}";
        String cmd = String.format(java.util.Locale.ROOT, "execute in %s run summon minecraft:text_display %.3f %.3f %.3f %s",
            "minecraft:" + Corpos.dimDe(nivel), x, y, z, nbt);
        Jogo.server.getCommands().performPrefixedCommand(Jogo.server.createCommandSourceStack().withSuppressedOutput(), cmd);
        p.sendSystemMessage(Jogo.prefixo("Lápide feita. Para tirar o texto: /lapide remover (as pedras você quebra normalmente)."));
        return 1;
    }

    private static int remover(ServerPlayer p) {
        String filtro = Comandos.staff(p) ? "tag=" + TAG : "tag=" + TAG + "_" + p.getUUID();
        ServerLevel nivel = (ServerLevel) p.level();
        String cmd = String.format(java.util.Locale.ROOT, "execute in %s positioned %.2f %.2f %.2f run kill @e[type=minecraft:text_display,%s,distance=..4]",
            "minecraft:" + Corpos.dimDe(nivel), p.getX(), p.getY(), p.getZ(), filtro);
        Jogo.server.getCommands().performPrefixedCommand(Jogo.server.createCommandSourceStack().withSuppressedOutput(), cmd);
        p.sendSystemMessage(Jogo.prefixo("Texto da lápide removido (num raio de 4 blocos, só as suas)."));
        return 1;
    }
}
