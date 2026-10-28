package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Fmt;
import br.melby.melbybet.core.Ranking;
import br.melby.melbybet.core.StatCat;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /perfil [nick]: cartão com tempo de jogo, stats, apostas e duelos. */
public final class Perfil {
    private Perfil() {}

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("perfil")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                mostrar(p, p.getGameProfile().name());
                return 1;
            })
            .then(argument("nick", word()).suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Jogo.casa.conhecidos.values(), b))
                .executes(c -> {
                    mostrar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nick"));
                    return 1;
                })));
    }

    private static Component linha(String rotulo, String valor) {
        return Component.literal(rotulo + ": ").withStyle(ChatFormatting.GOLD).append(Component.literal(valor).withStyle(ChatFormatting.WHITE));
    }

    private static String posicao(StatCat cat, String nome, Map<String, JsonObject> stats, JsonObject meu) {
        List<Ranking.Linha> r = Ranking.montar(cat, stats);
        for (int i = 0; i < r.size(); i++) {
            if (r.get(i).nome.equalsIgnoreCase(nome)) return cat.formatar(cat.valor(meu)) + " (" + (i + 1) + "º)";
        }
        return cat.formatar(cat.valor(meu));
    }

    public static void mostrar(ServerPlayer quem, String nickPedido) {
        Casa casa = Jogo.casa;
        String uuid = null;
        String nome = null;
        for (var e : casa.conhecidos.entrySet()) {
            if (e.getValue().equalsIgnoreCase(nickPedido)) {
                uuid = e.getKey();
                nome = e.getValue();
            }
        }
        if (uuid == null) {
            quem.sendSystemMessage(Jogo.prefixo("Não conheço ninguém chamado " + nickPedido + "."));
            return;
        }
        Casa.CorNick cor = casa.config.cores.get(nome.toLowerCase());
        MutableComponent titulo = Component.literal("== Perfil de ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        titulo.append(cor != null ? Prefixos.degrade(nome, cor.cor1, cor.cor2) : Component.literal(nome).withStyle(ChatFormatting.WHITE));
        titulo.append(Component.literal(" ==").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        quem.sendSystemMessage(titulo);

        // título (manual do OP ou o de sátira do ranking de apostas)
        Casa.Titulo manual = casa.config.titulos.get(nome.toLowerCase());
        List<Casa.Jogador> rank = casa.rankingApostadores();
        Casa.Jogador j = casa.jogadores.get(uuid);
        String tit = manual != null ? manual.texto : null;
        if (tit == null && j != null) {
            for (int i = 0; i < rank.size(); i++) if (rank.get(i) == j) tit = Casa.titulo(i + 1, j, rank.size());
        }
        if (tit != null) quem.sendSystemMessage(linha("Título", tit));

        long agora = System.currentTimeMillis();
        Casa.Visto v = casa.vistos.get(nome.toLowerCase());
        if (v != null) {
            if (v.online) quem.sendSystemMessage(linha("Status", "online há " + Fmt.duracao(agora - v.entrou)));
            else if (v.saiu > 0) quem.sendSystemMessage(linha("Status", "offline, visto há " + Fmt.duracao(agora - v.saiu)));
            if (v.primeira > 0) quem.sendSystemMessage(linha("Na Gangue há", Fmt.duracao(agora - v.primeira)));
        }

        Map<String, JsonObject> stats = Jogo.statsTodos();
        JsonObject meu = stats.get(nome);
        if (meu != null) {
            quem.sendSystemMessage(linha("Tempo online", posicao(StatCat.TEMPO, nome, stats, meu)));
            quem.sendSystemMessage(linha("Distância", posicao(StatCat.DISTANCIA, nome, stats, meu)));
            quem.sendSystemMessage(linha("Blocos quebrados", posicao(StatCat.QUEBROU, nome, stats, meu)));
            quem.sendSystemMessage(linha("Mobs mortos", posicao(StatCat.MATOU, nome, stats, meu)));
            quem.sendSystemMessage(linha("Mortes", posicao(StatCat.MORREU, nome, stats, meu)));
        }

        if (j != null && j.apostas > 0) {
            long lucro = j.ganho - j.gasto;
            int pct = j.apostas == 0 ? 0 : (int) Math.round(100.0 * j.acertos / j.apostas);
            quem.sendSystemMessage(linha("Apostas", j.apostas + " (" + j.acertos + " acertos, " + pct + "%), saldo " + (lucro >= 0 ? "+" : "") + lucro + " diamantes"));
        }
        if (j != null && (j.duelosGanhos + j.duelosPerdidos) > 0) {
            quem.sendSystemMessage(linha("Duelos", j.duelosGanhos + " vitória(s), " + j.duelosPerdidos + " derrota(s) (veja /duelo placar " + nome + ")"));
        }
        if (meu == null && j == null) quem.sendSystemMessage(Component.literal("Ainda sem dados.").withStyle(ChatFormatting.GRAY));
    }
}
