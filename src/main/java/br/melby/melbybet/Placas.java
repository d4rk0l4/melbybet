package br.melby.melbybet;

import br.melby.melbybet.core.TextoPlaca;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Placas bonitas: cores, negrito, degradê, tanto ao escrever na placa quanto pelo /placa. */
public final class Placas {
    private Placas() {}

    /** Converte o texto com códigos em um texto colorido. */
    public static Component componente(String bruto) {
        MutableComponent r = Component.empty();
        for (TextoPlaca.Seg s : TextoPlaca.ler(bruto)) {
            r.append(Component.literal(s.texto).withStyle(st -> {
                if (s.cor >= 0) st = st.withColor(s.cor);
                if (s.negrito) st = st.withBold(true);
                if (s.italico) st = st.withItalic(true);
                if (s.sublinhado) st = st.withUnderlined(true);
                if (s.riscado) st = st.withStrikethrough(true);
                if (s.embaralhado) st = st.withObfuscated(true);
                return st;
            }));
        }
        return r;
    }

    /** Chamado depois que um jogador escreve na placa: converte as linhas que têm códigos. */
    public static void converter(SignBlockEntity placa, SignTextSlot frente) {
        placa.updateText(st -> {
            for (int i = 0; i < 4; i++) {
                String raw = st.getMessages(false).get(i).getString();
                if (TextoPlaca.temCodigo(raw)) st = st.asMutable().setLine(i, componente(raw)).asImmutable();
            }
            return st;
        }, frente);
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("placa")
            .then(literal("copiar").executes(c -> copiar(c.getSource().getPlayerOrException())))
            .then(literal("historico").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                SignBlockEntity s = placaOlhada(p);
                if (s == null) return avisoErro(p, "Olhe para uma placa (a até 5 blocos).");
                return HistoricoPlacas.historico(p, s);
            }))
            .then(literal("restaurar").then(argument("id", IntegerArgumentType.integer(1)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                SignBlockEntity s = placaOlhada(p);
                if (s == null) return avisoErro(p, "Olhe para a placa (a até 5 blocos).");
                return HistoricoPlacas.restaurar(p, s, IntegerArgumentType.getInteger(c, "id"));
            })))
            .then(literal("colar").executes(c -> colar(c.getSource().getPlayerOrException())))
            .then(literal("limpar").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                SignBlockEntity s = placaOlhada(p);
                if (s == null) return erro(c, "Olhe para uma placa (a até 5 blocos).");
                if (s.isWaxed()) return erro(c, "Essa placa está encerada, não dá para mudar.");
                if (!p.level().mayInteract(p, s.getBlockPos())) return erro(c, "Você não pode mexer nessa placa.");
                var lado = s.getSlotPlayerIsFacing(p);
                HistoricoPlacas.antes(s, lado);
                for (int i = 0; i < 4; i++) editar(s, p, i, "");
                HistoricoPlacas.depois(p, s, lado);
                return ok(c, "Placa limpa.");
            }))
            .then(argument("linha", IntegerArgumentType.integer(1, 4))
                .then(argument("texto", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    SignBlockEntity s = placaOlhada(p);
                    if (s == null) return erro(c, "Olhe para uma placa (a até 5 blocos).");
                    if (s.isWaxed()) return erro(c, "Essa placa está encerada, não dá para mudar.");
                    if (!p.level().mayInteract(p, s.getBlockPos())) return erro(c, "Você não pode mexer nessa placa.");
                    String texto = StringArgumentType.getString(c, "texto");
                    if (TextoPlaca.semCodigos(texto).length() > 40) return erro(c, "Linha grande demais.");
                    var lado = s.getSlotPlayerIsFacing(p);
                    HistoricoPlacas.antes(s, lado);
                    editar(s, p, IntegerArgumentType.getInteger(c, "linha") - 1, texto);
                    HistoricoPlacas.depois(p, s, lado);
                    return ok(c, "Linha " + IntegerArgumentType.getInteger(c, "linha") + " atualizada.");
                }))));
    }

    /** Linhas copiadas por jogador (com códigos), para colar em outra placa. */
    private static final java.util.Map<java.util.UUID, String[]> COPIADAS = new java.util.HashMap<>();

    static String[] lerLinhas(SignBlockEntity s, SignTextSlot lado) {
        String[] r = new String[4];
        s.updateText(st -> {
            for (int i = 0; i < 4; i++) {
                List<TextoPlaca.Seg> segs = new ArrayList<>();
                st.getMessages(false).get(i).visit((estilo, texto) -> {
                    TextoPlaca.Seg g = new TextoPlaca.Seg();
                    g.texto = texto;
                    g.cor = estilo.getColor() == null ? -1 : estilo.getColor().getValue();
                    g.negrito = estilo.isBold();
                    g.italico = estilo.isItalic();
                    g.sublinhado = estilo.isUnderlined();
                    g.riscado = estilo.isStrikethrough();
                    g.embaralhado = estilo.isObfuscated();
                    if (!texto.isEmpty()) segs.add(g);
                    return Optional.empty();
                }, Style.EMPTY);
                r[i] = TextoPlaca.paraCodigos(segs);
            }
            return st;
        }, lado);
        return r;
    }

    private static int copiar(ServerPlayer p) {
        SignBlockEntity s = placaOlhada(p);
        if (s == null) return avisoErro(p, "Olhe para uma placa (a até 5 blocos).");
        String[] l = lerLinhas(s, s.getSlotPlayerIsFacing(p));
        COPIADAS.put(p.getUUID(), l);
        boolean vazia = true;
        for (String x : l) if (!x.isEmpty()) vazia = false;
        if (vazia) return avisoErro(p, "Essa placa está sem texto.");
        p.sendSystemMessage(Component.literal("Texto da placa (clique numa linha para copiar o comando dela):").withStyle(ChatFormatting.GOLD), false);
        for (int i = 0; i < 4; i++) {
            final String cmd = "/placa " + (i + 1) + " " + l[i];
            final String cru = l[i];
            MutableComponent previa = componente(cru).copy().withStyle(st -> st.withClickEvent(new ClickEvent.CopyToClipboard(cmd))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Clique para copiar: " + cmd))));
            MutableComponent codigo = Component.literal("  " + (cru.isEmpty() ? "(vazia)" : cru)).withStyle(st -> st.withColor(ChatFormatting.DARK_GRAY)
                .withClickEvent(new ClickEvent.CopyToClipboard(cru)).withHoverEvent(new HoverEvent.ShowText(Component.literal("Clique para copiar só o texto"))));
            p.sendSystemMessage(Component.literal("[" + (i + 1) + "] ").withStyle(ChatFormatting.AQUA).append(previa).append(codigo), false);
        }
        p.sendSystemMessage(Component.literal("Guardei as 4 linhas. Olhe para outra placa e use /placa colar.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int colar(ServerPlayer p) {
        String[] l = COPIADAS.get(p.getUUID());
        if (l == null) return avisoErro(p, "Copie uma placa primeiro com /placa copiar.");
        SignBlockEntity s = placaOlhada(p);
        if (s == null) return avisoErro(p, "Olhe para a placa de destino (a até 5 blocos).");
        if (s.isWaxed()) return avisoErro(p, "Essa placa está encerada, não dá para mudar.");
        if (!p.level().mayInteract(p, s.getBlockPos())) return avisoErro(p, "Você não pode mexer nessa placa.");
        var lado = s.getSlotPlayerIsFacing(p);
        HistoricoPlacas.antes(s, lado);
        for (int i = 0; i < 4; i++) editar(s, p, i, l[i]);
        HistoricoPlacas.depois(p, s, lado);
        p.sendSystemMessage(Component.literal("Texto colado na placa.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    static int avisoErro(ServerPlayer p, String m) {
        p.sendSystemMessage(Component.literal(m).withStyle(ChatFormatting.RED), true);
        return 0;
    }

    public static void aoSair(ServerPlayer p) {
        COPIADAS.remove(p.getUUID());
    }

    static void editar(SignBlockEntity s, ServerPlayer p, int linha, String texto) {
        SignTextSlot frente = s.getSlotPlayerIsFacing(p);
        s.updateText(st -> st.asMutable().setLine(linha, componente(texto)).asImmutable(), frente);
    }

    static SignBlockEntity placaOlhada(ServerPlayer p) {
        HitResult h = p.pick(5.0, 1.0F, false);
        if (!(h instanceof BlockHitResult bh)) return null;
        return p.level().getBlockEntity(bh.getBlockPos()) instanceof SignBlockEntity s ? s : null;
    }

    private static int ok(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static int erro(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendFailure(Component.literal(s));
        return 0;
    }
}
