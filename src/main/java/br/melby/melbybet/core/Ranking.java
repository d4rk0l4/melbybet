package br.melby.melbybet.core;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Monta o ranking de uma categoria a partir das estatísticas de cada jogador. */
public final class Ranking {
    private Ranking() {}

    public static class Linha {
        public final String nome;
        public final double valor;

        public Linha(String nome, double valor) {
            this.nome = nome;
            this.valor = valor;
        }
    }

    /** @param porJogador nome -> objeto "stats" do jogador. Zerados ficam de fora. */
    public static List<Linha> montar(StatCat cat, Map<String, JsonObject> porJogador) {
        List<Linha> l = new ArrayList<>();
        for (var e : porJogador.entrySet()) {
            double v = cat.valor(e.getValue());
            if (v > 0) l.add(new Linha(e.getKey(), v));
        }
        l.sort((a, b) -> {
            int c = Double.compare(b.valor, a.valor);
            return c != 0 ? c : a.nome.compareToIgnoreCase(b.nome);
        });
        return l;
    }
}
