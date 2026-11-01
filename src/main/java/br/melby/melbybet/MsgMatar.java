package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /msgmatar: frases que aparecem no chat quando você mata outro jogador (várias, e até uma por vítima). */
public final class MsgMatar {
    private MsgMatar() {}

    private static final Random SORTE = new Random();

    /** Mensagem já escolhida para quando a morte aparecer no chat. */
    private record Pendente(String matador, String texto, long ate) {}

    private static final Map<String, Pendente> PENDENTES = new HashMap<>();

    private static int adicionar(ServerPlayer p, String alvo, String t) {
        t = t.trim();
        if (!t.contains("{vitima}")) {
            p.sendSystemMessage(Jogo.prefixo("Use {vitima} onde vai o nome de quem morreu. Ex.: {matador} mandou {vitima} pro espaço"));
            return 0;
        }
        if (t.length() > 100) {
            p.sendSystemMessage(Jogo.prefixo("Até 100 caracteres."));
            return 0;
        }
        List<String> l = Jogo.casa.msgsMatar(p.getGameProfile().name(), alvo);
        if (l.size() >= 10) {
            p.sendSystemMessage(Jogo.prefixo("Máximo de 10 mensagens " + (alvo == null ? "padrão" : "contra " + alvo) + ". Tire uma com /msgmatar remover <número>."));
            return 0;
        }
        l.add(t);
        Jogo.salvar();
        p.sendSystemMessage(Jogo.prefixo("Adicionada. Exemplo: ").copy().append(montar(t, p.getGameProfile().name(), alvo == null ? "Fulano" : alvo)));
        return 1;
    }

    private static void listar(ServerPlayer p) {
        List<String[]> todas = Jogo.casa.todasMsgsMatar(p.getGameProfile().name());
        if (todas.isEmpty()) {
            p.sendSystemMessage(Jogo.prefixo("Você não tem mensagens de kill. Use /msgmatar add <texto> (com {vitima}). Ex.: /msgmatar add &c{matador} &7mandou {vitima} pro espaço"));
            return;
        }
        p.sendSystemMessage(Component.literal("Suas mensagens de kill:").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        int n = 1;
        for (String[] m : todas) {
            int i = m[0].indexOf('>');
            String quando = i < 0 ? "qualquer um" : "contra " + m[0].substring(i + 1);
            p.sendSystemMessage(Component.literal(n++ + ". (" + quando + ") " + m[1]).withStyle(ChatFormatting.GRAY));
        }
        p.sendSystemMessage(Component.literal("Remover: /msgmatar remover <número>. Uma por vítima: /msgmatar contra <jogador> <texto>.").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("msgmatar")
            .executes(c -> {
                listar(c.getSource().getPlayerOrException());
                return 1;
            })
            .then(literal("add").then(argument("texto", StringArgumentType.greedyString()).executes(c ->
                adicionar(c.getSource().getPlayerOrException(), null, StringArgumentType.getString(c, "texto")))))
            .then(literal("contra")
                .then(argument("jogador", StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(Jogo.casa.conhecidos.values(), b))
                    .then(argument("texto", StringArgumentType.greedyString()).executes(c ->
                        adicionar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "jogador"), StringArgumentType.getString(c, "texto"))))))
            .then(literal("remover").then(argument("n", IntegerArgumentType.integer(1)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                if (!Jogo.casa.removerMsgMatar(p.getGameProfile().name(), IntegerArgumentType.getInteger(c, "n"))) {
                    c.getSource().sendFailure(Component.literal("Não existe essa mensagem. Veja os números com /msgmatar."));
                    return 0;
                }
                Jogo.salvar();
                p.sendSystemMessage(Jogo.prefixo("Mensagem removida."));
                return 1;
            })))
            .then(literal("off").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String m = p.getGameProfile().name().toLowerCase();
                Jogo.casa.migrarMsgMatar();
                Jogo.casa.config.msgMatarLista.keySet().removeIf(k -> k.equals(m) || k.startsWith(m + ">"));
                Jogo.salvar();
                p.sendSystemMessage(Jogo.prefixo("Todas as suas mensagens de kill foram removidas."));
                return 1;
            }))
            .then(argument("texto", StringArgumentType.greedyString()).executes(c ->
                adicionar(c.getSource().getPlayerOrException(), null, StringArgumentType.getString(c, "texto")))));
    }

    private static Component montar(String texto, String matador, String vitima) {
        String t = texto.replace("{matador}", matador).replace("{vitima}", vitima);
        return Component.literal("☠ ").withStyle(ChatFormatting.RED).append(Placas.componente(t));
    }

    /** Antes da morte: escolhe a mensagem do matador para trocar a padrão. */
    public static void antes(ServerPlayer morto, DamageSource fonte) {
        if (!(fonte.getEntity() instanceof ServerPlayer matador) || matador == morto) return;
        String texto = Jogo.casa.escolherMsgMatar(matador.getGameProfile().name(), morto.getGameProfile().name(), SORTE);
        if (texto == null) return;
        long agora = System.currentTimeMillis();
        PENDENTES.values().removeIf(x -> x.ate < agora);
        PENDENTES.put(morto.getGameProfile().name().toLowerCase(), new Pendente(matador.getGameProfile().name(), texto, agora + 5000));
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
        Jogo.server.getPlayerList().broadcastSystemMessage(montar(p.texto, p.matador, vitima), false);
        return false;
    }
}
