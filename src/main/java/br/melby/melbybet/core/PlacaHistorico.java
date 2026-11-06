package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Histórico de edições de placas (uma lista de versões por lado de cada placa). Lógica pura, em config/melbybet/placas-historico.json. */
public class PlacaHistorico {
    public static final int MAX_VERSOES = 20;
    public static final int MAX_PLACAS = 3000;

    public static class Versao {
        public int id;
        public long quando;
        /** Quem escreveu (UUID); vazio na versão original, de antes do mod começar a registrar. */
        public String uuid = "";
        public String nome = "";
        /** As 4 linhas, já com códigos (&c, &#hex, degradê). */
        public String[] linhas = {"", "", "", ""};
    }

    public int proximoId = 1;
    /** chave "dim|x|y|z|LADO" -> versões da mais antiga para a mais nova. */
    public Map<String, List<Versao>> placas = new LinkedHashMap<>();

    public static String chave(String dim, int x, int y, int z, String lado) {
        return dim + "|" + x + "|" + y + "|" + z + "|" + lado;
    }

    public static boolean iguais(String[] a, String[] b) {
        return Arrays.equals(a, b);
    }

    private static boolean vazia(String[] l) {
        for (String s : l) if (s != null && !s.isEmpty()) return false;
        return true;
    }

    /**
     * Registra uma edição. Na primeira vez numa placa, guarda também o texto de antes (se tinha algum) como versão original.
     * Devolve a versão criada, ou null se o texto não mudou.
     */
    public Versao registrar(String chave, String[] antes, String[] depois, String uuid, String nome, long agora) {
        if (iguais(antes, depois)) return null;
        List<Versao> l = placas.get(chave);
        if (l == null) {
            l = new ArrayList<>();
            placas.put(chave, l);
            if (!vazia(antes)) {
                Versao o = new Versao();
                o.id = proximoId++;
                o.quando = agora;
                o.linhas = antes.clone();
                o.nome = "(original)";
                l.add(o);
            }
            while (placas.size() > MAX_PLACAS) placas.remove(placas.keySet().iterator().next());
        }
        Versao v = new Versao();
        v.id = proximoId++;
        v.quando = agora;
        v.uuid = uuid;
        v.nome = nome;
        v.linhas = depois.clone();
        l.add(v);
        while (l.size() > MAX_VERSOES) l.remove(0);
        return v;
    }

    /** Da mais nova para a mais antiga. */
    public List<Versao> lista(String chave) {
        List<Versao> l = placas.get(chave);
        List<Versao> r = new ArrayList<>();
        if (l != null) for (int i = l.size() - 1; i >= 0; i--) r.add(l.get(i));
        return r;
    }

    /** A chave da placa a que a versão pertence, ou null. */
    public String chaveDe(int id) {
        for (Map.Entry<String, List<Versao>> e : placas.entrySet()) for (Versao v : e.getValue()) if (v.id == id) return e.getKey();
        return null;
    }

    public Versao achar(int id) {
        for (List<Versao> l : placas.values()) for (Versao v : l) if (v.id == id) return v;
        return null;
    }

    /** Esquece a placa (os dois lados) quando ela é quebrada. */
    public void remover(String dim, int x, int y, int z) {
        placas.remove(chave(dim, x, y, z, "FRONT"));
        placas.remove(chave(dim, x, y, z, "BACK"));
    }

    /** Devolve o motivo se o jogador não pode restaurar essa versão, ou null. OP restaura qualquer uma; os outros, só as que eles mesmos escreveram. */
    public static String podeRestaurar(Versao v, String uuid, boolean op) {
        if (op) return null;
        if (v.uuid == null || v.uuid.isEmpty()) return "Só um OP restaura a versão original da placa.";
        if (!v.uuid.equals(uuid)) return "Você só pode restaurar versões que você mesmo escreveu.";
        return null;
    }

    public static PlacaHistorico ler(Path arquivo) {
        PlacaHistorico d = Json.ler(arquivo, PlacaHistorico.class);
        if (d == null) return new PlacaHistorico();
        if (d.placas == null) d.placas = new LinkedHashMap<>();
        for (List<Versao> l : d.placas.values()) {
            for (Versao v : l) {
                if (v.linhas == null || v.linhas.length != 4) v.linhas = new String[] {"", "", "", ""};
                for (int i = 0; i < 4; i++) if (v.linhas[i] == null) v.linhas[i] = "";
                if (v.uuid == null) v.uuid = "";
                if (v.nome == null) v.nome = "";
            }
        }
        return d;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
