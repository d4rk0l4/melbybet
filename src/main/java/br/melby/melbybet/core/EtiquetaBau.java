package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Etiquetas de baús (nome, descrição, categoria), em config/melbybet/baus.json. O primeiro a etiquetar vira o dono. Lógica pura. */
public class EtiquetaBau {
    public static final int MAX_NOME = 32;
    public static final int MAX_DESCRICAO = 100;
    public static final int MAX_CATEGORIA = 20;
    public static final int MAX_POR_JOGADOR = 200;

    public static class Etiqueta {
        public String nome = "", descricao = "", categoria = "";
        public String dono = "", donoNome = "";
        public long quando;
        public String dim = "";
        public int x, y, z;

        public boolean vazia() {
            return nome.isEmpty() && descricao.isEmpty() && categoria.isEmpty();
        }
    }

    /** chave "dim|x|y|z" -> etiqueta. */
    public Map<String, Etiqueta> baus = new LinkedHashMap<>();

    public static String chave(String dim, int x, int y, int z) {
        return dim + "|" + x + "|" + y + "|" + z;
    }

    /** Qual das duas metades de um baú duplo guarda a etiqueta: a de menor (x, y, z). */
    public static int[] canonico(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (x1 != x2) return x1 < x2 ? new int[] {x1, y1, z1} : new int[] {x2, y2, z2};
        if (y1 != y2) return y1 < y2 ? new int[] {x1, y1, z1} : new int[] {x2, y2, z2};
        return z1 <= z2 ? new int[] {x1, y1, z1} : new int[] {x2, y2, z2};
    }

    public enum Campo { NOME, DESCRICAO, CATEGORIA }

    public static String limpar(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    /** Devolve o erro, ou null. Texto vazio apaga aquele campo. */
    public String definir(String chave, String dim, int x, int y, int z, Campo campo, String valor, String uuid, String nomeJogador, boolean op, long agora) {
        valor = limpar(valor);
        int max = campo == Campo.NOME ? MAX_NOME : campo == Campo.DESCRICAO ? MAX_DESCRICAO : MAX_CATEGORIA;
        if (valor.length() > max) return "Texto grande demais (máximo " + max + " letras).";
        if (valor.indexOf('§') >= 0) return "Sem o símbolo §.";
        Etiqueta e = baus.get(chave);
        if (e != null && !e.dono.equals(uuid) && !op) return "Esse baú foi etiquetado por " + e.donoNome + ". Só ele (ou um OP) muda.";
        if (e == null) {
            if (valor.isEmpty()) return "Esse baú não tem etiqueta.";
            if (!op && contar(uuid) >= MAX_POR_JOGADOR) return "Você já etiquetou " + MAX_POR_JOGADOR + " baús.";
            e = new Etiqueta();
            e.dono = uuid;
            e.donoNome = nomeJogador;
            e.dim = dim;
            e.x = x;
            e.y = y;
            e.z = z;
            baus.put(chave, e);
        }
        switch (campo) {
            case NOME -> e.nome = valor;
            case DESCRICAO -> e.descricao = valor;
            default -> e.categoria = valor;
        }
        e.quando = agora;
        if (e.vazia()) baus.remove(chave);
        return null;
    }

    /** Remove tudo. Devolve o erro, ou null. */
    public String remover(String chave, String uuid, boolean op) {
        Etiqueta e = baus.get(chave);
        if (e == null) return "Esse baú não tem etiqueta.";
        if (!e.dono.equals(uuid) && !op) return "Esse baú foi etiquetado por " + e.donoNome + ". Só ele (ou um OP) muda.";
        baus.remove(chave);
        return null;
    }

    public int contar(String uuid) {
        int n = 0;
        for (Etiqueta e : baus.values()) if (e.dono.equals(uuid)) n++;
        return n;
    }

    /** Move a etiqueta quando o baú duplo perde a metade que a guardava. */
    public void mover(String de, String para, String dim, int x, int y, int z) {
        Etiqueta e = baus.remove(de);
        if (e == null || baus.containsKey(para)) return;
        e.dim = dim;
        e.x = x;
        e.y = y;
        e.z = z;
        baus.put(para, e);
    }

    private static String norm(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    /** Etiquetas cujo nome, descrição ou categoria contém o texto (sem acento nem maiúscula). */
    public List<Etiqueta> buscar(String texto) {
        String q = norm(limpar(texto));
        List<Etiqueta> r = new ArrayList<>();
        for (Etiqueta e : baus.values()) {
            if (norm(e.nome + " " + e.descricao + " " + e.categoria).contains(q)) r.add(e);
        }
        return r;
    }

    public List<Etiqueta> doJogador(String uuid, String categoria) {
        List<Etiqueta> r = new ArrayList<>();
        for (Etiqueta e : baus.values()) {
            if (!e.dono.equals(uuid)) continue;
            if (categoria != null && !categoria.isEmpty() && !norm(e.categoria).equals(norm(categoria))) continue;
            r.add(e);
        }
        r.sort((a, b) -> {
            int c = norm(a.categoria).compareTo(norm(b.categoria));
            return c != 0 ? c : norm(a.nome).compareTo(norm(b.nome));
        });
        return r;
    }

    public static EtiquetaBau ler(Path arquivo) {
        EtiquetaBau d = Json.ler(arquivo, EtiquetaBau.class);
        if (d == null) return new EtiquetaBau();
        if (d.baus == null) d.baus = new LinkedHashMap<>();
        for (Etiqueta e : d.baus.values()) {
            if (e.nome == null) e.nome = "";
            if (e.descricao == null) e.descricao = "";
            if (e.categoria == null) e.categoria = "";
            if (e.dono == null) e.dono = "";
            if (e.donoNome == null) e.donoNome = "";
            if (e.dim == null) e.dim = "";
        }
        return d;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
