package br.melby.melbybet;

import br.melby.melbybet.core.TextoPlaca;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
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
    public static void converter(SignBlockEntity placa, boolean frente) {
        placa.updateText(st -> {
            for (int i = 0; i < 4; i++) {
                String raw = st.getMessage(i, false).getString();
                if (TextoPlaca.temCodigo(raw)) st = st.setMessage(i, componente(raw));
            }
            return st;
        }, frente);
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("placa")
            .then(literal("limpar").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                SignBlockEntity s = placaOlhada(p);
                if (s == null) return erro(c, "Olhe para uma placa (a até 5 blocos).");
                for (int i = 0; i < 4; i++) editar(s, p, i, "");
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
                    editar(s, p, IntegerArgumentType.getInteger(c, "linha") - 1, texto);
                    return ok(c, "Linha " + IntegerArgumentType.getInteger(c, "linha") + " atualizada.");
                }))));
    }

    private static void editar(SignBlockEntity s, ServerPlayer p, int linha, String texto) {
        boolean frente = s.isFacingFrontText(p);
        s.updateText(st -> st.setMessage(linha, componente(texto)), frente);
    }

    private static SignBlockEntity placaOlhada(ServerPlayer p) {
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
