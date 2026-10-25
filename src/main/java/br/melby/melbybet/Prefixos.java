package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Gradiente;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tag antes do nick (tab, cabeça e chat): [AFK] e o título em degradê.
 * Usa um time do próprio Minecraft por jogador, chamado mb_xxxxxxxx.
 */
public final class Prefixos {
    private Prefixos() {}

    private static final Map<UUID, String> ASSINATURA = new HashMap<>();

    public static MutableComponent degrade(String texto, int a, int b) {
        MutableComponent r = Component.empty();
        int[] cores = Gradiente.cores(texto.length(), a, b);
        for (int i = 0; i < texto.length(); i++) {
            int cor = cores[i];
            r.append(Component.literal(String.valueOf(texto.charAt(i))).withStyle(st -> st.withColor(cor)));
        }
        return r;
    }

    private static int[] coresDoTitulo(String titulo) {
        switch (titulo) {
            case "Rei da Banca": return new int[] {0xFFAA00, 0xFFE066};
            case "Pé-Frio Oficial": return new int[] {0x55CCFF, 0x3355FF};
            case "Doador da Casa": return new int[] {0xFF5555, 0xAA0000};
            case "Profeta": return new int[] {0xB388FF, 0xFF80D5};
            default: return new int[] {0xAAAAAA, 0xFFFFFF};
        }
    }

    /** Título e cores do jogador: o que o OP deu, senão o da posição nas apostas. Null = nenhum. */
    private static Object[] tituloDe(ServerPlayer p, List<Casa.Jogador> ranking) {
        Casa casa = Jogo.casa;
        Casa.Titulo manual = casa.config.titulos.get(p.getGameProfile().name().toLowerCase());
        if (manual != null && !manual.texto.isBlank()) return new Object[] {manual.texto, manual.cor1, manual.cor2};
        String nome = p.getGameProfile().name();
        for (int i = 0; i < ranking.size(); i++) {
            Casa.Jogador j = ranking.get(i);
            if (!j.nome.equalsIgnoreCase(nome)) continue;
            String t = Casa.titulo(i + 1, j, ranking.size());
            if (t.equals("Apostador")) return null;
            int[] c = coresDoTitulo(t);
            return new Object[] {t, c[0], c[1]};
        }
        return null;
    }

    public static void atualizarTodos() {
        List<Casa.Jogador> ranking = Jogo.casa.rankingApostadores();
        Set<UUID> online = new java.util.HashSet<>();
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            online.add(p.getUUID());
            atualizar(p, ranking);
        }
        // quem saiu: apaga o time para não sobrar tag
        Scoreboard sb = Jogo.server.getScoreboard();
        for (UUID id : new ArrayList<>(ASSINATURA.keySet())) {
            if (online.contains(id)) continue;
            PlayerTeam t = sb.getPlayerTeam(nomeDoTime(id));
            if (t != null) sb.removePlayerTeam(t);
            ASSINATURA.remove(id);
        }
    }

    private static String nomeDoTime(UUID id) {
        return "mb_" + id.toString().substring(0, 8);
    }

    public static void atualizar(ServerPlayer p, List<Casa.Jogador> ranking) {
        boolean afk = Extras.estaAfk(p.getUUID());
        Object[] titulo = tituloDe(p, ranking);
        String assinatura = (afk ? "A" : "-") + (titulo == null ? "" : titulo[0] + "|" + titulo[1] + "|" + titulo[2]);
        if (assinatura.equals(ASSINATURA.get(p.getUUID()))) return;

        Scoreboard sb = Jogo.server.getScoreboard();
        String nome = p.getScoreboardName();
        PlayerTeam atual = sb.getPlayersTeam(nome);
        if (atual != null && !atual.getName().startsWith("mb_")) return; // time de outro mod: não mexe

        if (!afk && titulo == null) {
            if (atual != null) {
                sb.removePlayerFromTeam(nome, atual);
                sb.removePlayerTeam(atual);
            }
            ASSINATURA.put(p.getUUID(), assinatura);
            return;
        }
        MutableComponent prefixo = Component.empty();
        if (afk) prefixo.append(Component.literal("[AFK] ").withStyle(ChatFormatting.GRAY));
        if (titulo != null) {
            prefixo.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY));
            prefixo.append(degrade((String) titulo[0], (Integer) titulo[1], (Integer) titulo[2]));
            prefixo.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
        }
        PlayerTeam t = sb.getPlayerTeam(nomeDoTime(p.getUUID()));
        if (t == null) t = sb.addPlayerTeam(nomeDoTime(p.getUUID()));
        t.setPlayerPrefix(prefixo);
        sb.addPlayerToTeam(nome, t);
        ASSINATURA.put(p.getUUID(), assinatura);
    }

    /** Ao ligar o servidor: apaga times antigos que ficaram salvos no mundo. */
    public static void limparTudo() {
        Scoreboard sb = Jogo.server.getScoreboard();
        for (PlayerTeam t : new ArrayList<>(sb.getPlayerTeams())) {
            if (t.getName().startsWith("mb_") || t.getName().equals("melbyafk")) sb.removePlayerTeam(t);
        }
        ASSINATURA.clear();
    }

    /** Força recalcular (depois de mudar título, por exemplo). */
    public static void esquecer() {
        ASSINATURA.clear();
    }
}
