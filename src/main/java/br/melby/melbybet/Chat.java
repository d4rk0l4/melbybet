package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Chat local (padrão), global (/g) e privado travado (/tell nick), com espiões para admin. */
public final class Chat {
    private Chat() {}

    private enum Tipo { LOCAL, GLOBAL, PRIVADO }

    private static final class Modo {
        Tipo tipo = Tipo.LOCAL;
        String alvo;
    }

    private static final Map<UUID, Modo> MODOS = new HashMap<>();
    private static final Map<UUID, String> ULTIMO = new HashMap<>();
    private static final Set<UUID> ESPIOES = new HashSet<>();

    public static void registrar() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((mensagem, remetente, params) -> {
            if (Jogo.server == null || remetente == null) return true;
            tratar(remetente, mensagem.decoratedContent());
            return false; // a mensagem padrão não sai: quem manda somos nós
        });
    }

    private static Modo modo(ServerPlayer p) {
        return MODOS.computeIfAbsent(p.getUUID(), k -> new Modo());
    }

    private static void tratar(ServerPlayer p, Component conteudo) {
        Modo m = modo(p);
        switch (m.tipo) {
            case GLOBAL -> falar(p, conteudo, true);
            case PRIVADO -> {
                ServerPlayer alvo = Jogo.server.getPlayerList().getPlayerByName(m.alvo);
                if (alvo == null) {
                    m.tipo = Tipo.LOCAL;
                    p.sendSystemMessage(Jogo.prefixo(m.alvo + " saiu do jogo. Você voltou para o chat local."));
                    falar(p, conteudo, false);
                } else {
                    privado(p, alvo, conteudo.getString());
                }
            }
            default -> falar(p, conteudo, false);
        }
    }

    // ---------- chat local e global ----------

    private static void falar(ServerPlayer p, Component conteudo, boolean global) {
        MutableComponent linha = Component.empty();
        if (global) linha.append(Component.literal("[G] ").withStyle(ChatFormatting.GOLD));
        linha.append(Prefixos.prefixoDe(p));
        linha.append(Prefixos.nomeColorido(p));
        linha.append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
        linha.append(conteudo);

        int raio = Jogo.casa.config.chatLocalRaio;
        int ouviram = 0;
        int outros = 0;
        for (ServerPlayer o : Jogo.server.getPlayerList().getPlayers()) {
            if (o == p) continue;
            outros++;
            if (global || raio <= 0 || (o.level() == p.level() && o.distanceToSqr(p) <= (double) raio * raio)) {
                o.sendSystemMessage(linha);
                ouviram++;
            }
        }
        p.sendSystemMessage(linha);
        Melbybet.LOGGER.info("[Chat{}] {}: {}", global ? " global" : "", p.getGameProfile().name(), conteudo.getString());
        if (!global && outros > 0 && ouviram == 0) {
            p.sendSystemMessage(Component.literal("Ninguém por perto ouviu. Para falar com todos use /g <mensagem>.").withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }

    public static void global(ServerPlayer p, String texto) {
        falar(p, Extras.decorar(p, Component.literal(texto)), true);
    }

    public static void local(ServerPlayer p, String texto) {
        falar(p, Extras.decorar(p, Component.literal(texto)), false);
    }

    /** /g sozinho: trava (ou destrava) o chat no global. */
    public static boolean alternarGlobal(ServerPlayer p) {
        Modo m = modo(p);
        if (m.tipo == Tipo.GLOBAL) {
            m.tipo = Tipo.LOCAL;
            return false;
        }
        m.tipo = Tipo.GLOBAL;
        return true;
    }

    public static void travarLocal(ServerPlayer p) {
        modo(p).tipo = Tipo.LOCAL;
    }

    // ---------- privado ----------

    /** /tell nick sozinho: trava o chat naquela conversa. */
    public static void travarPrivado(ServerPlayer p, ServerPlayer alvo) {
        Modo m = modo(p);
        m.tipo = Tipo.PRIVADO;
        m.alvo = alvo.getGameProfile().name();
    }

    public static void privado(ServerPlayer de, ServerPlayer para, String texto) {
        String nd = de.getGameProfile().name();
        String np = para.getGameProfile().name();
        Component corpo = Component.literal(texto).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
        de.sendSystemMessage(Component.literal("[Para " + np + "] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(corpo));
        para.sendSystemMessage(Component.literal("[De " + nd + "] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(corpo));
        ULTIMO.put(de.getUUID(), np);
        ULTIMO.put(para.getUUID(), nd);

        Jogo.casa.registrarTell(nd, np, texto, System.currentTimeMillis());
        Melbybet.LOGGER.info("[Tell] {} -> {}: {}", nd, np, texto);
        Component espiao = Component.literal("[Spy] " + nd + " -> " + np + ": " + texto).withStyle(ChatFormatting.DARK_GRAY);
        for (ServerPlayer o : Jogo.server.getPlayerList().getPlayers()) {
            if (o != de && o != para && ESPIOES.contains(o.getUUID())) o.sendSystemMessage(espiao);
        }
    }

    public static String ultimoDe(ServerPlayer p) {
        return ULTIMO.get(p.getUUID());
    }

    // ---------- espiar ----------

    public static boolean alternarEspiao(ServerPlayer p) {
        if (ESPIOES.remove(p.getUUID())) return false;
        ESPIOES.add(p.getUUID());
        return true;
    }

    public static List<Casa.Tell> historico(String nick) {
        return Jogo.casa.tellsDe(nick, 15);
    }
}
