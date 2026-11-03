package br.melby.melbybet.core;

import java.text.Normalizer;

/** Busca de item por nome (sem acento, sem maiúscula, aceitando o começo das palavras). Sem nada do Minecraft aqui. */
public final class Busca {
    private Busca() {}

    public static String normalizar(String s) {
        String t = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase();
        return t.replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Quanto o nome combina com o que foi digitado: 0 = nada, 100 = igual. */
    public static int pontuar(String consulta, String nome) {
        String q = normalizar(consulta), n = normalizar(nome);
        if (q.isEmpty() || n.isEmpty()) return 0;
        if (q.equals(n)) return 100;
        if (n.startsWith(q)) return 80;
        String[] qs = q.split(" "), ns = n.split(" ");
        boolean todas = true;
        for (String w : qs) {
            boolean achou = false;
            for (String x : ns) if (x.startsWith(w)) { achou = true; break; }
            if (!achou) { todas = false; break; }
        }
        if (todas) return 60;
        if (n.contains(q)) return 40;
        return 0;
    }

    public static int melhor(String consulta, String... nomes) {
        int m = 0;
        for (String n : nomes) m = Math.max(m, pontuar(consulta, n));
        return m;
    }
}
