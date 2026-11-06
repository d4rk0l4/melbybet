package br.melby.melbybet;

import br.melby.melbybet.core.EtiquetaBau;
import br.melby.melbybet.core.EtiquetaBau.Campo;
import br.melby.melbybet.core.EtiquetaBau.Etiqueta;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.file.Path;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /bau: nome, descrição e categoria para baús, barris e shulkers. Ao abrir um baú etiquetado, a etiqueta aparece na barra
 * de ação. O primeiro a etiquetar vira o dono; OP muda qualquer um. Funciona sem nada no cliente.
 */
public final class Baus {
    private Baus() {}

    private static EtiquetaBau dados = new EtiquetaBau();
    private static Path arq;
    private static boolean sujo;

    public static void carregar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("baus.json");
        dados = EtiquetaBau.ler(arq);
        sujo = false;
    }

    public static void salvarAgora() {
        if (arq == null || !sujo) return;
        try {
            dados.gravar(arq);
            sujo = false;
        } catch (Exception e) {
            Melbybet.LOGGER.error("Falha ao salvar {}", arq, e);
        }
    }

    public static void aCadaSegundo() {
        if (sujo) salvarAgora();
    }

    /** Posição que guarda a etiqueta: nos baús duplos, a metade de menor coordenada. */
    private static BlockPos principal(Level n, BlockPos pos) {
        BlockState st = n.getBlockState(pos);
        return principal(st, pos);
    }

    private static BlockPos principal(BlockState st, BlockPos pos) {
        if (st.getBlock() instanceof ChestBlock && st.hasProperty(ChestBlock.TYPE) && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction d = ChestBlock.getConnectedDirection(st);
            BlockPos o = pos.relative(d);
            int[] c = EtiquetaBau.canonico(pos.getX(), pos.getY(), pos.getZ(), o.getX(), o.getY(), o.getZ());
            return new BlockPos(c[0], c[1], c[2]);
        }
        return pos;
    }

    private static String chave(ServerLevel n, BlockPos p) {
        return EtiquetaBau.chave(Corpos.dimDe(n), p.getX(), p.getY(), p.getZ());
    }

    public static void registrarEventos() {
        // ao abrir um baú etiquetado, mostra a etiqueta
        UseBlockCallback.EVENT.register((p, n, mao, hit) -> {
            if (n.isClientSide() || dados.baus.isEmpty() || !(p instanceof ServerPlayer sp) || !(n instanceof ServerLevel sl)) return InteractionResult.PASS;
            if (!(n.getBlockEntity(hit.getBlockPos()) instanceof BaseContainerBlockEntity)) return InteractionResult.PASS;
            Etiqueta e = dados.baus.get(chave(sl, principal(n, hit.getBlockPos())));
            if (e != null) sp.sendSystemMessage(linha(e), true);
            return InteractionResult.PASS;
        });
        // quebrou o baú: a etiqueta some; se era um duplo, vai para a metade que sobrou
        PlayerBlockBreakEvents.AFTER.register((n, p, pos, st, be) -> {
            if (dados.baus.isEmpty() || !(n instanceof ServerLevel sl) || !(be instanceof BaseContainerBlockEntity)) return;
            BlockPos prin = principal(st, pos);
            String k = chave(sl, prin);
            if (!dados.baus.containsKey(k)) return;
            if (st.getBlock() instanceof ChestBlock && st.hasProperty(ChestBlock.TYPE) && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos outra = pos.relative(ChestBlock.getConnectedDirection(st));
                dados.mover(k, chave(sl, outra), Corpos.dimDe(sl), outra.getX(), outra.getY(), outra.getZ());
            } else {
                dados.baus.remove(k);
            }
            sujo = true;
        });
    }

    private static Component linha(Etiqueta e) {
        var c = Component.literal(e.nome.isEmpty() ? "(sem nome)" : e.nome).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if (!e.categoria.isEmpty()) c.append(Component.literal("  [" + e.categoria + "]").withStyle(ChatFormatting.AQUA));
        if (!e.descricao.isEmpty()) c.append(Component.literal("  " + e.descricao).withStyle(ChatFormatting.GRAY));
        return c;
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("bau")
            .then(campo("nome", Campo.NOME))
            .then(campo("descricao", Campo.DESCRICAO))
            .then(campo("categoria", Campo.CATEGORIA))
            .then(literal("info").executes(c -> info(c.getSource().getPlayerOrException())))
            .then(literal("limpar").executes(c -> limpar(c.getSource().getPlayerOrException())))
            .then(literal("lista")
                .then(argument("categoria", StringArgumentType.greedyString()).executes(c -> lista(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "categoria"))))
                .executes(c -> lista(c.getSource().getPlayerOrException(), "")))
            .then(literal("buscar").then(argument("texto", StringArgumentType.greedyString())
                .executes(c -> buscar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "texto")))))
            .executes(c -> {
                ajuda(c.getSource().getPlayerOrException());
                return 1;
            }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> campo(String nome, Campo campo) {
        return literal(nome).then(argument("texto", StringArgumentType.greedyString())
            .executes(c -> definir(c.getSource().getPlayerOrException(), campo, StringArgumentType.getString(c, "texto"))));
    }

    private static void ajuda(ServerPlayer p) {
        String[] l = {"/bau nome <texto>, /bau descricao <texto>, /bau categoria <texto>: etiqueta o baú para o qual você olha",
            "/bau info: mostra a etiqueta | /bau limpar: apaga a etiqueta",
            "/bau lista [categoria]: seus baús etiquetados | /bau buscar <texto>: acha baús de qualquer jogador"};
        p.sendSystemMessage(Component.literal("Etiquetas de baú").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (String s : l) p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.GRAY), false);
    }

    private static BlockPos baOlhado(ServerPlayer p) {
        HitResult h = p.pick(6.0, 1.0F, false);
        if (!(h instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK) return null;
        if (!(p.level().getBlockEntity(bh.getBlockPos()) instanceof BaseContainerBlockEntity)) return null;
        return principal(p.level(), bh.getBlockPos());
    }

    private static int definir(ServerPlayer p, Campo campo, String valor) {
        BlockPos pos = baOlhado(p);
        if (pos == null) return erro(p, "Olhe para um baú, barril ou shulker (a até 6 blocos).");
        ServerLevel n = (ServerLevel) p.level();
        String err = dados.definir(chave(n, pos), Corpos.dimDe(n), pos.getX(), pos.getY(), pos.getZ(), campo, valor, p.getUUID().toString(),
            p.getGameProfile().name(), Comandos.admin(p), System.currentTimeMillis());
        if (err != null) return erro(p, err);
        sujo = true;
        Etiqueta e = dados.baus.get(chave(n, pos));
        p.sendSystemMessage(e == null ? Component.literal("Etiqueta apagada.").withStyle(ChatFormatting.YELLOW) : linha(e), true);
        return 1;
    }

    private static int info(ServerPlayer p) {
        BlockPos pos = baOlhado(p);
        if (pos == null) return erro(p, "Olhe para um baú, barril ou shulker (a até 6 blocos).");
        Etiqueta e = dados.baus.get(chave((ServerLevel) p.level(), pos));
        if (e == null) return erro(p, "Esse baú não tem etiqueta. Use /bau nome <texto>.");
        p.sendSystemMessage(linha(e), false);
        p.sendSystemMessage(Component.literal("Etiquetado por " + e.donoNome).withStyle(ChatFormatting.DARK_GRAY), false);
        return 1;
    }

    private static int limpar(ServerPlayer p) {
        BlockPos pos = baOlhado(p);
        if (pos == null) return erro(p, "Olhe para um baú, barril ou shulker (a até 6 blocos).");
        String err = dados.remover(chave((ServerLevel) p.level(), pos), p.getUUID().toString(), Comandos.admin(p));
        if (err != null) return erro(p, err);
        sujo = true;
        p.sendSystemMessage(Component.literal("Etiqueta apagada.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static Component local(Etiqueta e, ServerPlayer p) {
        String coords = e.x + " " + e.y + " " + e.z;
        String dim = e.dim.equals(Corpos.dimDe((ServerLevel) p.level())) ? "" : " (" + e.dim + ")";
        return Component.literal(coords + dim).withStyle(st -> st.withColor(ChatFormatting.DARK_AQUA)
            .withClickEvent(new ClickEvent.CopyToClipboard(coords)).withHoverEvent(new HoverEvent.ShowText(Component.literal("Clique para copiar as coordenadas"))));
    }

    private static void imprimir(ServerPlayer p, List<Etiqueta> l, boolean comDono) {
        int n = 0;
        for (Etiqueta e : l) {
            if (++n > 25) {
                p.sendSystemMessage(Component.literal("... e mais " + (l.size() - 25) + ". Refine a busca.").withStyle(ChatFormatting.GRAY), false);
                break;
            }
            var c = linha(e).copy().append(Component.literal("  "));
            c.append(local(e, p));
            if (comDono) c.append(Component.literal("  de " + e.donoNome).withStyle(ChatFormatting.DARK_GRAY));
            p.sendSystemMessage(c, false);
        }
    }

    private static int lista(ServerPlayer p, String categoria) {
        List<Etiqueta> l = dados.doJogador(p.getUUID().toString(), categoria);
        if (l.isEmpty()) return erro(p, categoria.isEmpty() ? "Você não etiquetou nenhum baú." : "Nenhum baú seu na categoria \"" + categoria + "\".");
        p.sendSystemMessage(Component.literal("Seus baús (" + l.size() + ")").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        imprimir(p, l, false);
        return l.size();
    }

    private static int buscar(ServerPlayer p, String texto) {
        List<Etiqueta> l = dados.buscar(texto);
        if (l.isEmpty()) return erro(p, "Nenhum baú etiquetado combina com \"" + texto + "\".");
        p.sendSystemMessage(Component.literal("Baús que combinam com \"" + texto + "\" (" + l.size() + ")").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        imprimir(p, l, true);
        return l.size();
    }

    private static int erro(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED), true);
        return 0;
    }
}
