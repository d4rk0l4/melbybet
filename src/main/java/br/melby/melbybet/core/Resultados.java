package br.melby.melbybet.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Descobre quem ganhou um bolão por conquista, lendo os arquivos de advancements. */
public final class Resultados {
    private Resultados() {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    /** Instante (ms) em que o jogador concluiu a conquista, ou -1 se não concluiu. */
    public static long quando(String json, String conquista) {
        try {
            JsonObject raiz = JsonParser.parseString(json).getAsJsonObject();
            JsonElement e = raiz.get(conquista);
            if (e == null || !e.isJsonObject()) return -1;
            JsonObject o = e.getAsJsonObject();
            JsonElement done = o.get("done");
            if (done == null || !done.getAsBoolean()) return -1;
            JsonElement crit = o.get("criteria");
            if (crit == null || !crit.isJsonObject()) return -1;
            long menor = Long.MAX_VALUE;
            for (var c : crit.getAsJsonObject().entrySet()) {
                long t = OffsetDateTime.parse(c.getValue().getAsString(), FMT).toInstant().toEpochMilli();
                menor = Math.min(menor, t);
            }
            return menor == Long.MAX_VALUE ? -1 : menor;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public static class Decisao {
        public final List<String> vencedores;

        Decisao(List<String> v) {
            this.vencedores = v;
        }

        /** Exatamente um vencedor. */
        public boolean claro() { return vencedores.size() == 1; }
    }

    /**
     * @param nomePorUuid uuid -> nome dos jogadores conhecidos
     * @param ler uuid -> texto do arquivo de advancements (ou null se não existir)
     * @param desde só vale conquista a partir deste instante (ms), para não pagar quem já tinha antes
     * @return null se ninguém concluiu ainda; vários nomes se houve empate no mesmo segundo
     */
    public static Decisao porConquista(String conquista, Map<String, String> nomePorUuid,
                                       Function<String, String> ler, long desde) {
        long melhor = Long.MAX_VALUE;
        List<String> nomes = new ArrayList<>();
        for (var e : nomePorUuid.entrySet()) {
            String txt = ler.apply(e.getKey());
            if (txt == null) continue;
            long t = quando(txt, conquista);
            if (t < 0 || t < desde) continue;
            long seg = t / 1000;
            if (seg < melhor) {
                melhor = seg;
                nomes.clear();
                nomes.add(e.getValue());
            } else if (seg == melhor) {
                nomes.add(e.getValue());
            }
        }
        return nomes.isEmpty() ? null : new Decisao(nomes);
    }
}
