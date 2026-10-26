package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Fmt;
import br.melby.melbybet.core.Lembrete;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /capsula: lembrete pessoal com hora marcada; OPs também podem mandar um aviso para todos. */
public final class Capsulas {
    private Capsulas() {}

    private static final int MAX_POR_JOGADOR = 20;
    private static final int MAX_TEXTO = 200;

    private static int criar(CommandSourceStack src, String entrada, boolean todos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Lembrete.Resultado r = Lembrete.ler(entrada);
        if (r == null) {
            p.sendSystemMessage(Jogo.prefixo("Não entendi. Exemplo: /capsula \"Arrumar baús\" 2 horas  (aceita s, min, h e dias)."));
            return 0;
        }
        if (r.ms < Lembrete.MIN_MS) {
            p.sendSystemMessage(Jogo.prefixo("O tempo mínimo é 10 segundos."));
            return 0;
        }
        if (r.ms > Lembrete.MAX_MS) {
            p.sendSystemMessage(Jogo.prefixo("O tempo máximo é 365 dias."));
            return 0;
        }
        if (r.texto.length() > MAX_TEXTO) {
            p.sendSystemMessage(Jogo.prefixo("O texto pode ter até " + MAX_TEXTO + " letras."));
            return 0;
        }
        String dono = todos ? "*" : p.getUUID().toString();
        if (Jogo.casa.capsulasPendentesDe(dono) >= MAX_POR_JOGADOR) {
            p.sendSystemMessage(Jogo.prefixo("Você já tem " + MAX_POR_JOGADOR + " lembretes pendentes."));
            return 0;
        }
        long agora = System.currentTimeMillis();
        Casa.Capsula c = Jogo.casa.novaCapsula(dono, p.getGameProfile().name(), r.texto, agora + r.ms, agora);
        Jogo.salvar();
        p.sendSystemMessage(Jogo.prefixo((todos ? "Aviso para todos" : "Lembrete") + " #" + c.id + " marcado para daqui a " + Fmt.duracao(r.ms) + ": " + r.texto));
        return 1;
    }

    private static Component botaoCancelar(int id) {
        return Component.literal("[Cancelar]").withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand("/capsula cancelar " + id))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Apaga este lembrete"))));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("capsula")
            .executes(c -> {
                c.getSource().getPlayerOrException().sendSystemMessage(Jogo.prefixo(
                    "Use /capsula \"texto\" 2 horas. /capsula lista mostra os seus; /capsula cancelar <id> apaga."));
                return 1;
            })
            .then(literal("lista").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                boolean admin = Comandos.admin(p);
                long agora = System.currentTimeMillis();
                int n = 0;
                for (Casa.Capsula x : Jogo.casa.capsulas) {
                    boolean meu = x.uuid.equals(p.getUUID().toString());
                    boolean global = x.uuid.equals("*");
                    if (!meu && !(global && admin)) continue;
                    if (n++ == 0) p.sendSystemMessage(Component.literal("Seus lembretes:").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                    p.sendSystemMessage(Component.literal("#" + x.id + (global ? " [todos]" : "") + " em " + Fmt.duracao(Math.max(0, x.quando - agora))
                        + ": " + x.texto + " ").withStyle(ChatFormatting.WHITE).append(botaoCancelar(x.id)));
                }
                if (n == 0) p.sendSystemMessage(Jogo.prefixo("Nenhum lembrete pendente."));
                return 1;
            }))
            .then(literal("cancelar").then(argument("id", IntegerArgumentType.integer(1)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                int id = IntegerArgumentType.getInteger(c, "id");
                for (Casa.Capsula x : Jogo.casa.capsulas) {
                    if (x.id != id) continue;
                    boolean meu = x.uuid.equals(p.getUUID().toString());
                    if (!meu && !Comandos.admin(p)) break;
                    Jogo.casa.removerCapsula(id);
                    Jogo.salvar();
                    p.sendSystemMessage(Jogo.prefixo("Lembrete #" + id + " apagado."));
                    return 1;
                }
                p.sendSystemMessage(Jogo.prefixo("Não achei esse lembrete."));
                return 0;
            })))
            .then(literal("todos").requires(s -> s.getPlayer() != null && Comandos.admin(s.getPlayer()))
                .then(argument("texto", greedyString()).executes(c -> criar(c.getSource(), StringArgumentType.getString(c, "texto"), true))))
            .then(argument("texto", greedyString()).executes(c -> criar(c.getSource(), StringArgumentType.getString(c, "texto"), false))));
    }

    private static void som(ServerPlayer p, String som) {
        Jogo.server.getCommands().performPrefixedCommand(Jogo.server.createCommandSourceStack().withSuppressedOutput(),
            "playsound " + som + " player " + p.getGameProfile().name() + " ~ ~ ~ 1 1");
    }

    /** Chamado uma vez por segundo: entrega o que venceu. Lembrete de quem está offline espera até ele entrar. */
    public static void aCadaSegundo() {
        if (Jogo.server == null || Jogo.casa.capsulas.isEmpty()) return;
        long agora = System.currentTimeMillis();
        List<Casa.Capsula> feitas = new ArrayList<>();
        for (Casa.Capsula c : Jogo.casa.capsulas) {
            if (agora < c.quando) continue;
            if (c.uuid.equals("*")) {
                Component msg = Component.literal("[Aviso] ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal(c.texto + " (de " + c.nome + ")").withStyle(ChatFormatting.WHITE));
                for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(msg);
                    som(p, "minecraft:block.bell.use");
                }
                feitas.add(c);
                continue;
            }
            ServerPlayer p = Jogo.server.getPlayerList().getPlayer(java.util.UUID.fromString(c.uuid));
            if (p == null) continue;
            p.sendSystemMessage(Component.literal("[Lembrete] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(c.texto).withStyle(ChatFormatting.WHITE)));
            p.sendSystemMessage(Component.literal("Lembrete: " + c.texto).withStyle(ChatFormatting.GOLD), true);
            som(p, "minecraft:block.note_block.bell");
            feitas.add(c);
        }
        if (!feitas.isEmpty()) {
            Jogo.casa.capsulas.removeAll(feitas);
            Jogo.salvar();
        }
    }
}
