package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /msgmatar: a frase que aparece no chat quando você mata outro jogador. */
public final class MsgMatar {
    private MsgMatar() {}

    private record Pendente(String matador, long ate) {}

    /** Vítima (nick em minúsculas) -> quem matou, até o jogo mandar a mensagem de morte. */
    private static final Map<String, Pendente> PENDENTES = new HashMap<>();

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("msgmatar")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String atual = Jogo.casa.config.msgMatar.get(p.getGameProfile().name().toLowerCase());
                p.sendSystemMessage(Jogo.prefixo(atual == null
                    ? "Você não tem mensagem de kill. Use /msgmatar <texto> com {vitima} (e {matador}). Ex.: /msgmatar &c{matador} &7mandou {vitima} pro espaço"
                    : "Sua mensagem de kill: " + atual + "  (/msgmatar off para tirar)"));
                return 1;
            })
            .then(literal("off").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Jogo.casa.config.msgMatar.remove(p.getGameProfile().name().toLowerCase());
                Jogo.salvar();
                p.sendSystemMessage(Jogo.prefixo("Mensagem de kill removida."));
                return 1;
            }))
            .then(argument("texto", StringArgumentType.greedyString()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String t = StringArgumentType.getString(c, "texto").trim();
                if (!t.contains("{vitima}")) {
                    c.getSource().sendFailure(Component.literal("Use {vitima} onde vai o nome de quem morreu. Ex.: {matador} mandou {vitima} pro espaço"));
                    return 0;
                }
                if (t.length() > 100) {
                    c.getSource().sendFailure(Component.literal("Até 100 caracteres."));
                    return 0;
                }
                Jogo.casa.config.msgMatar.put(p.getGameProfile().name().toLowerCase(), t);
                Jogo.salvar();
                p.sendSystemMessage(Jogo.prefixo("Pronto! Quando você matar alguém, vai aparecer: ").copy()
                    .append(montar(t, p.getGameProfile().name(), "Fulano")));
                return 1;
            })));
    }

    private static Component montar(String texto, String matador, String vitima) {
        String t = texto.replace("{matador}", matador).replace("{vitima}", vitima);
        return Component.literal("☠ ").withStyle(ChatFormatting.RED).append(Placas.componente(t));
    }

    /** Antes da morte: guarda quem matou, para trocar a mensagem padrão. */
    public static void antes(ServerPlayer morto, DamageSource fonte) {
        if (!(fonte.getEntity() instanceof ServerPlayer matador) || matador == morto) return;
        if (!Jogo.casa.config.msgMatar.containsKey(matador.getGameProfile().name().toLowerCase())) return;
        long agora = System.currentTimeMillis();
        PENDENTES.values().removeIf(x -> x.ate < agora);
        PENDENTES.put(morto.getGameProfile().name().toLowerCase(), new Pendente(matador.getGameProfile().name(), agora + 5000));
    }

    /** true = deixa passar a mensagem; false = ela foi trocada pela do matador. */
    public static boolean permitir(Component mensagem) {
        if (Jogo.server == null || PENDENTES.isEmpty()) return true;
        if (!(mensagem.getContents() instanceof TranslatableContents tc)) return true;
        if (!tc.getKey().startsWith("death.") || tc.getArgs().length == 0) return true;
        Object a = tc.getArgs()[0];
        String vitima = (a instanceof Component c ? c.getString() : String.valueOf(a)).trim();
        while (vitima.startsWith("[")) {
            int fim = vitima.indexOf(']');
            if (fim < 0) break;
            vitima = vitima.substring(fim + 1).trim();
        }
        Pendente p = PENDENTES.remove(vitima.toLowerCase());
        if (p == null) return true;
        String texto = Jogo.casa.config.msgMatar.get(p.matador.toLowerCase());
        if (texto == null) return true;
        Jogo.server.getPlayerList().broadcastSystemMessage(montar(texto, p.matador, vitima), false);
        return false;
    }
}
