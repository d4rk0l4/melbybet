package br.melby.melbybet;

import br.melby.melbybet.core.Fmt;
import br.melby.melbybet.core.Loteria;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Random;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /loteria: bilhetes em diamantes, tempo configurado pelo OP ao abrir. */
public final class Sorteio {
    private Sorteio() {}

    private static final Random SORTE = new Random();
    private static final List<Long> AVISOS = List.of(600L, 300L, 60L, 10L);
    private static long ultimoMs = -1;

    private static boolean ehAdmin(CommandSourceStack s) {
        return s.getPlayer() == null || Comandos.admin(s.getPlayer());
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("loteria")
            .executes(c -> {
                status(c.getSource().getPlayerOrException());
                return 1;
            })
            .then(literal("comprar")
                .executes(c -> comprar(c, 1))
                .then(argument("qtd", IntegerArgumentType.integer(1, 100)).executes(c -> comprar(c, IntegerArgumentType.getInteger(c, "qtd")))))
            .then(literal("criar").requires(Sorteio::ehAdmin)
                .then(argument("minutos", IntegerArgumentType.integer(1, 10080))
                    .then(argument("preco", IntegerArgumentType.integer(1, 64))
                        .executes(c -> criar(c, IntegerArgumentType.getInteger(c, "minutos"), IntegerArgumentType.getInteger(c, "preco"), 10))
                        .then(argument("limite", IntegerArgumentType.integer(1, 100))
                            .executes(c -> criar(c, IntegerArgumentType.getInteger(c, "minutos"), IntegerArgumentType.getInteger(c, "preco"), IntegerArgumentType.getInteger(c, "limite")))))))
            .then(literal("sortear").requires(Sorteio::ehAdmin).executes(c -> {
                if (!Jogo.casa.loteria.ativa) return erro(c, "Não tem loteria aberta.");
                sortearAgora();
                return 1;
            }))
            .then(literal("cancelar").requires(Sorteio::ehAdmin).executes(c -> {
                if (!Jogo.casa.loteria.cancelar(Jogo.casa, System.currentTimeMillis())) return erro(c, "Não tem loteria aberta.");
                Jogo.avisar("A loteria foi cancelada. Os bilhetes foram devolvidos.");
                Jogo.entregarTodos();
                Jogo.salvar();
                ultimoMs = -1;
                return 1;
            })));
    }

    private static MutableComponent botao(String rotulo, String comando) {
        return Component.literal("[" + rotulo + "]").withStyle(st -> st.withColor(ChatFormatting.GREEN).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand(comando))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Clique para comprar"))));
    }

    private static Component linhaCompra() {
        return Component.empty().append(botao("Comprar 1 bilhete", "/loteria comprar 1")).append(Component.literal(" "))
            .append(botao("Comprar 5", "/loteria comprar 5"));
    }

    private static void status(ServerPlayer p) {
        Loteria l = Jogo.casa.loteria;
        if (!l.ativa) {
            p.sendSystemMessage(Jogo.prefixo("Não tem loteria aberta agora."));
            return;
        }
        p.sendSystemMessage(Component.literal("== Loteria da Gangue ==").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal("Bilhete: " + l.preco + " diamante(s). Pote: " + l.pote() + ". Seus bilhetes: "
            + l.bilhetesDe(p.getUUID().toString()) + "/" + l.maxPorPessoa + ".").withStyle(ChatFormatting.YELLOW));
        p.sendSystemMessage(Component.literal("Sorteio em " + Fmt.duracao(l.restanteMs(System.currentTimeMillis())) + ". Quanto mais bilhetes, mais chance.").withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(linhaCompra());
    }

    private static int comprar(CommandContext<CommandSourceStack> c, int qtd) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Loteria l = Jogo.casa.loteria;
        long agora = System.currentTimeMillis();
        String id = p.getUUID().toString();
        String erro = l.validarCompra(id, qtd, agora);
        if (erro != null) return erro(c, erro);
        int custo = qtd * l.preco;
        if (!Jogo.tirar(p, custo)) return erro(c, "Você precisa de " + custo + " diamante(s) (tem " + Jogo.contar(p) + ").");
        l.comprar(id, p.getGameProfile().name(), qtd);
        Jogo.salvar();
        p.sendSystemMessage(Jogo.prefixo("Você comprou " + qtd + " bilhete(s) da loteria. Agora tem " + l.bilhetesDe(id) + "/" + l.maxPorPessoa + ". Pote: " + l.pote() + "."));
        Jogo.avisar(p.getGameProfile().name() + " entrou na loteria! Pote em " + l.pote() + " diamante(s).");
        return 1;
    }

    private static int criar(CommandContext<CommandSourceStack> c, int minutos, int preco, int limite) {
        String erro = Jogo.casa.loteria.abrir(preco, minutos, limite, System.currentTimeMillis());
        if (erro != null) return erro(c, erro);
        Jogo.salvar();
        ultimoMs = -1;
        anunciarAbertura();
        return 1;
    }

    private static void anunciarAbertura() {
        Loteria l = Jogo.casa.loteria;
        Component msg = Component.literal("[Loteria] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal("Aberta! Bilhete de " + l.preco + " diamante(s), até " + l.maxPorPessoa + " por pessoa. Sorteio em " + Fmt.duracao(l.minutos * 60_000L) + ". ").withStyle(ChatFormatting.WHITE))
            .append(linhaCompra());
        Jogo.server.getPlayerList().broadcastSystemMessage(msg, false);
        Jogo.webhook("🎟️ **Loteria da Gangue aberta!** Bilhete de **" + l.preco + "** 💎, sorteio em " + Fmt.duracao(l.minutos * 60_000L) + ".");
    }

    public static void sortearAgora() {
        Loteria.Resultado r = Jogo.casa.loteria.sortear(Jogo.casa, SORTE, System.currentTimeMillis());
        ultimoMs = -1;
        if (r == null) return;
        if (r.devolvido) {
            Jogo.avisar("Loteria encerrada: entrou menos de 2 pessoas, então os bilhetes foram devolvidos.");
            Jogo.webhook("🎟️ Loteria encerrada sem participantes suficientes. Bilhetes devolvidos.");
        } else {
            Jogo.avisar("Loteria sorteada! " + r.nome + " levou " + r.premio + " diamante(s)! (" + r.totalBilhetes + " bilhetes, " + r.participantes + " pessoas)");
            Jogo.webhook("🎟️ **Loteria sorteada!** " + r.nome + " levou **" + r.premio + "** 💎 (" + r.totalBilhetes + " bilhetes, " + r.participantes + " pessoas).");
        }
        Jogo.entregarTodos();
        Jogo.salvar();
    }

    /** Chamado a cada segundo: avisos de tempo e o sorteio na hora marcada. */
    public static void aCadaSegundo() {
        if (Jogo.server == null) return;
        Loteria l = Jogo.casa.loteria;
        if (!l.ativa) {
            ultimoMs = -1;
            return;
        }
        long resta = l.fimMs - System.currentTimeMillis();
        if (resta <= 0) {
            sortearAgora();
            return;
        }
        if (ultimoMs >= 0) {
            long aviso = br.melby.melbybet.core.Limpeza.cruzado(AVISOS, ultimoMs, resta);
            if (aviso > 0) {
                Component msg = Component.literal("[Loteria] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("Sorteio em " + Fmt.duracao(aviso * 1000) + "! Pote: " + l.pote() + " diamante(s). ").withStyle(ChatFormatting.WHITE))
                    .append(linhaCompra());
                Jogo.server.getPlayerList().broadcastSystemMessage(msg, false);
            }
        }
        ultimoMs = resta;
    }

    private static int erro(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendFailure(Component.literal(s));
        return 0;
    }
}
