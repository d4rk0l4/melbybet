package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /coords: mostra onde você está com [Copiar] e [Enviar] clicáveis; também guarda uma lista de locais (só anotação, sem teleporte). */
public final class Coords {
    private Coords() {}

    private static String dimensao(ServerPlayer p) {
        String s = p.level().dimension().toString();
        int a = s.lastIndexOf(':');
        int b = s.lastIndexOf(']');
        return a >= 0 && b > a ? s.substring(a + 1, b) : "overworld";
    }

    private static String nomeDim(String dim) {
        return switch (dim) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> dim;
        };
    }

    private static String texto(Casa.Local l) {
        return l.x + " " + l.y + " " + l.z + " (" + nomeDim(l.dim) + ")";
    }

    private static MutableComponent botao(String rotulo, ChatFormatting cor, ClickEvent clique, String dica) {
        return Component.literal("[" + rotulo + "]").withStyle(s -> s.withColor(cor).withBold(true)
            .withClickEvent(clique).withHoverEvent(new HoverEvent.ShowText(Component.literal(dica))));
    }

    private static Casa.Local aqui(ServerPlayer p) {
        Casa.Local l = new Casa.Local();
        l.dim = dimensao(p);
        l.x = p.blockPosition().getX();
        l.y = p.blockPosition().getY();
        l.z = p.blockPosition().getZ();
        return l;
    }

    private static MutableComponent linha(String titulo, Casa.Local l, String apagar) {
        String xyz = l.x + " " + l.y + " " + l.z;
        MutableComponent m = Component.literal(titulo + ": ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(texto(l) + " ").withStyle(ChatFormatting.WHITE))
            .append(botao("Copiar", ChatFormatting.AQUA, new ClickEvent.CopyToClipboard(xyz), "Copia " + xyz))
            .append(Component.literal(" "))
            .append(botao("Enviar", ChatFormatting.GREEN, new ClickEvent.RunCommand("/coords enviar " + l.dim + " " + xyz), "Manda no chat para todos"));
        if (apagar != null) {
            m.append(Component.literal(" ")).append(botao("Apagar", ChatFormatting.RED, new ClickEvent.RunCommand("/coords apagar " + apagar), "Remove da lista"));
        }
        return m;
    }

    private static Map<String, Casa.Local> minhas(ServerPlayer p) {
        return Jogo.casa.locais.computeIfAbsent(p.getUUID().toString(), k -> new LinkedHashMap<>());
    }

    /** Ao morrer: mostra só para quem morreu onde foi, com [Copiar] e [Enviar]. */
    public static void avisarMorte(ServerPlayer p) {
        p.sendSystemMessage(linha("Você morreu em", aqui(p), null));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("coords")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                p.sendSystemMessage(linha("Você está em", aqui(p), null));
                return 1;
            })
            .then(literal("salvar").then(argument("nome", word()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String nome = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nome");
                if (nome.length() > 20) {
                    p.sendSystemMessage(Jogo.prefixo("O nome pode ter até 20 letras."));
                    return 0;
                }
                Map<String, Casa.Local> m = minhas(p);
                if (!m.containsKey(nome) && m.size() >= 30) {
                    p.sendSystemMessage(Jogo.prefixo("Sua lista está cheia (30 locais). Apague algum antes."));
                    return 0;
                }
                m.put(nome, aqui(p));
                Jogo.salvar();
                p.sendSystemMessage(Jogo.prefixo("Local \"" + nome + "\" salvo. Veja com /coords lista."));
                return 1;
            })))
            .then(literal("lista").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Map<String, Casa.Local> m = minhas(p);
                if (m.isEmpty()) {
                    p.sendSystemMessage(Jogo.prefixo("Nenhum local salvo. Use /coords salvar <nome>."));
                    return 1;
                }
                p.sendSystemMessage(Component.literal("Seus locais:").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                m.forEach((nome, l) -> p.sendSystemMessage(linha(nome, l, nome)));
                return 1;
            }))
            .then(literal("apagar").then(argument("nome", word()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String nome = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nome");
                if (minhas(p).remove(nome) != null) {
                    Jogo.salvar();
                    p.sendSystemMessage(Jogo.prefixo("Local \"" + nome + "\" apagado."));
                } else {
                    p.sendSystemMessage(Jogo.prefixo("Não achei esse local."));
                }
                return 1;
            })))
            // clique em [Enviar]: /coords enviar <dimensao> <x> <y> <z>
            .then(literal("enviar").then(argument("dados", greedyString()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String[] t = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "dados").trim().split("\\s+");
                Casa.Local l = new Casa.Local();
                try {
                    if (t.length != 4) throw new NumberFormatException();
                    l.dim = t[0].length() > 30 ? "overworld" : t[0];
                    l.x = Integer.parseInt(t[1]);
                    l.y = Integer.parseInt(t[2]);
                    l.z = Integer.parseInt(t[3]);
                } catch (NumberFormatException e) {
                    p.sendSystemMessage(Jogo.prefixo("Use os botões de /coords para enviar."));
                    return 0;
                }
                String xyz = l.x + " " + l.y + " " + l.z;
                Component msg = Component.literal("Minhas coordenadas: " + texto(l) + " ")
                    .append(botao("Copiar", ChatFormatting.AQUA, new ClickEvent.CopyToClipboard(xyz), "Copia " + xyz));
                Chat.globalComponente(p, msg);
                return 1;
            }))));
    }
}
