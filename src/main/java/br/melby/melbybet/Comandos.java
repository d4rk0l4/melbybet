package br.melby.melbybet;

import br.melby.melbybet.core.Bolao;
import br.melby.melbybet.core.Casa;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class Comandos {
    private Comandos() {}

    private static final PermissionNode<Boolean> ADMIN = PermissionNode.of("melbybet", "admin");

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("bet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p == null) return erro(c, "Só jogadores podem abrir o menu.");
            MenuBet.abrir(p);
            return 1;
        }));
        d.register(literal("top").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p == null) return erro(c, "Só jogadores podem abrir o menu.");
            MenuTop.abrir(p);
            return 1;
        }));

        LiteralArgumentBuilder<CommandSourceStack> adm = literal("melbybet")
            .requires(PermissionPredicates.require(ADMIN, PermissionLevel.ADMINS));

        adm.then(literal("criar")
            .then(argument("gatilho", word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(Bolao.Gatilho.values()).map(Enum::name).toList(), b))
                .then(argument("titulo", greedyString()).executes(Comandos::criar))));

        adm.then(literal("abrir").then(argument("id", integer(1)).executes(c -> mudar(c, "abrir"))));
        adm.then(literal("fechar").then(argument("id", integer(1)).executes(c -> mudar(c, "fechar"))));
        adm.then(literal("cancelar").then(argument("id", integer(1)).executes(c -> mudar(c, "cancelar"))));
        adm.then(literal("resolver").then(argument("id", integer(1))
            .then(argument("vencedor", greedyString()).executes(Comandos::resolver))));
        adm.then(literal("prazo").then(argument("id", integer(1))
            .then(argument("minutos", integer(1, 1440)).executes(Comandos::prazo))));
        adm.then(literal("lista").executes(Comandos::lista));
        adm.then(literal("cofre").executes(c -> {
            Casa casa = Jogo.casa;
            return ok(c, "Cofre da Casa: " + casa.cofre + " | Jackpot: " + casa.jackpot + " | Temporada " + casa.temporada);
        }));
        adm.then(literal("roster").then(argument("nicks", greedyString()).executes(c -> {
            List<String> n = new ArrayList<>();
            for (String s : StringArgumentType.getString(c, "nicks").split("[;,\\s]+")) if (!s.isBlank()) n.add(s.trim());
            if (n.isEmpty()) return erro(c, "Informe os nicks separados por espaço, vírgula ou ponto e vírgula.");
            Jogo.casa.config.roster = n;
            Jogo.salvar();
            return ok(c, "Roster: " + String.join(", ", n));
        })));
        adm.then(literal("temporada").then(literal("nova").executes(c -> {
            Jogo.casa.novaTemporada(System.currentTimeMillis());
            Jogo.salvar();
            Jogo.avisar("Começou a temporada " + Jogo.casa.temporada + "! O ranking de apostadores foi zerado.");
            return 1;
        })));
        d.register(adm);
    }

    // ---------- admin ----------

    /** /melbybet criar VOID Quem cai no Void primeiro?   |   para MANUAL: titulo | opcao1;opcao2;opcao3 */
    private static int criar(CommandContext<CommandSourceStack> c) {
        Bolao.Gatilho g = Bolao.Gatilho.de(StringArgumentType.getString(c, "gatilho"));
        if (g == null) return erro(c, "Gatilho inválido. Use: " + String.join(", ", Arrays.stream(Bolao.Gatilho.values()).map(Enum::name).toList()));
        String bruto = StringArgumentType.getString(c, "titulo");
        String titulo = bruto;
        List<String> opcoes = new ArrayList<>(Jogo.casa.config.roster);
        int barra = bruto.indexOf('|');
        if (barra >= 0) {
            titulo = bruto.substring(0, barra).trim();
            opcoes.clear();
            for (String o : bruto.substring(barra + 1).split(";")) if (!o.isBlank()) opcoes.add(o.trim());
        } else if (g == Bolao.Gatilho.MANUAL) {
            return erro(c, "Bolão manual precisa das opções: /melbybet criar MANUAL Título | opção1;opção2");
        }
        if (titulo.isBlank()) return erro(c, "Falta o título.");
        if (opcoes.size() < 2) return erro(c, "Precisa de pelo menos 2 opções.");
        Bolao b = Jogo.casa.criar(titulo, g, opcoes, System.currentTimeMillis());
        Jogo.salvar();
        return ok(c, "Bolão #" + b.id + " criado (fechado). Abra com /melbybet abrir " + b.id);
    }

    private static int mudar(CommandContext<CommandSourceStack> c, String acao) {
        Bolao b = Jogo.casa.achar(IntegerArgumentType.getInteger(c, "id"));
        if (b == null) return erro(c, "Bolão não encontrado.");
        String erro = null;
        switch (acao) {
            case "abrir" -> {
                erro = Jogo.casa.abrir(b);
                if (erro == null) Jogo.avisar("Novo bolão aberto: \"" + b.titulo + "\". Use /bet para apostar!");
            }
            case "fechar" -> {
                erro = Jogo.casa.fechar(b);
                if (erro == null) Jogo.avisar("Bolão \"" + b.titulo + "\" fechado para apostas.");
            }
            default -> {
                if (!b.emAndamento()) erro = "Esse bolão já terminou.";
                else {
                    Jogo.casa.cancelar(b, System.currentTimeMillis());
                    Jogo.entregarTodos();
                    Jogo.avisar("Bolão \"" + b.titulo + "\" cancelado. Apostas devolvidas.");
                }
            }
        }
        if (erro != null) return erro(c, erro);
        Jogo.salvar();
        return 1;
    }

    private static int resolver(CommandContext<CommandSourceStack> c) {
        Bolao b = Jogo.casa.achar(IntegerArgumentType.getInteger(c, "id"));
        if (b == null) return erro(c, "Bolão não encontrado.");
        if (!b.emAndamento()) return erro(c, "Esse bolão já terminou.");
        String venc = b.opcaoExata(StringArgumentType.getString(c, "vencedor"));
        if (venc == null) return erro(c, "Opção inexistente. Opções: " + String.join(", ", b.opcoes));
        Jogo.resolver(b, venc);
        return 1;
    }

    private static int prazo(CommandContext<CommandSourceStack> c) {
        Bolao b = Jogo.casa.achar(IntegerArgumentType.getInteger(c, "id"));
        if (b == null) return erro(c, "Bolão não encontrado.");
        int min = IntegerArgumentType.getInteger(c, "minutos");
        b.expiraEm = System.currentTimeMillis() + min * 60_000L;
        Jogo.salvar();
        return ok(c, "O bolão #" + b.id + " fecha sozinho em " + min + " min (só vale se estiver aberto).");
    }

    private static int lista(CommandContext<CommandSourceStack> c) {
        List<Bolao> l = Jogo.casa.bolaos;
        if (l.isEmpty()) return ok(c, "Nenhum bolão criado.");
        for (Bolao b : l) {
            ok(c, "#" + b.id + " [" + b.status + "] " + b.titulo + " (" + b.gatilho + ", pote " + b.pote() + ")"
                + (b.status == Bolao.Status.AGUARDANDO ? " candidatos: " + String.join(", ", b.candidatos) : ""));
        }
        return l.size();
    }

    // ---------- respostas ----------

    private static int ok(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static int erro(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendFailure(Component.literal(s));
        return 0;
    }
}
