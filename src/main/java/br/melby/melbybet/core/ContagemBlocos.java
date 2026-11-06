package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Quantos blocos de cada tipo há numa área (para o /catalogo), mais os projetos salvos. Lógica pura. */
public class ContagemBlocos {
    public static final int MAX_PROJETOS = 20;
    public static final int POR_PAGINA = 12;

    /** Contagem de um projeto: id do bloco (sem "minecraft:") -> quantidade. */
    public static class Contagem {
        public Map<String, Long> blocos = new LinkedHashMap<>();
        public long total;
        /** Posições que não foram lidas porque o chunk não estava carregado. */
        public long puladas;

        public void somar(String id, long n) {
            if (n <= 0) return;
            blocos.merge(id, n, Long::sum);
            total += n;
        }

        /** Do mais numeroso para o menos; empate em ordem alfabética. */
        public List<Map.Entry<String, Long>> ordenado() {
            List<Map.Entry<String, Long>> l = new ArrayList<>(blocos.entrySet());
            l.sort((a, b) -> {
                int c = Long.compare(b.getValue(), a.getValue());
                return c != 0 ? c : a.getKey().compareTo(b.getKey());
            });
            return l;
        }

        public int paginas() {
            return Math.max(1, (blocos.size() + POR_PAGINA - 1) / POR_PAGINA);
        }

        public List<Map.Entry<String, Long>> pagina(int n) {
            List<Map.Entry<String, Long>> l = ordenado();
            int de = (n - 1) * POR_PAGINA;
            if (n < 1 || de >= l.size()) return List.of();
            return l.subList(de, Math.min(l.size(), de + POR_PAGINA));
        }
    }

    public static class Projeto {
        public String nome = "";
        public String dim = "";
        public String area = "";
        public int larg, alt, comp;
        public long criadoEm;
        public Contagem contagem = new Contagem();
    }

    /** uuid -> nome em minúsculas -> projeto. */
    public Map<String, Map<String, Projeto>> projetos = new LinkedHashMap<>();

    public static String percentual(long n, long total) {
        if (total <= 0) return "0%";
        return Fmt.decimal(100.0 * n / total, 1) + "%";
    }

    /** "3 pilhas + 10", "1 shulker + 2 pilhas", ou só "10" quando cabe em menos de uma pilha. */
    public static String pilhas(long n, int tam) {
        if (tam <= 1 || n < tam) return Fmt.inteiro(n);
        long shulker = 27L * tam;
        long sh = n / shulker, resto = n % shulker;
        long pil = resto / tam, sobra = resto % tam;
        List<String> p = new ArrayList<>();
        if (sh > 0) p.add(sh + (sh == 1 ? " shulker" : " shulkers"));
        if (pil > 0) p.add(pil + (pil == 1 ? " pilha" : " pilhas"));
        if (sobra > 0) p.add(Long.toString(sobra));
        return String.join(" + ", p);
    }

    public String salvar(String uuid, String nome, Projeto p, long agora) {
        String erro = MarcadorDados.validarNome(nome);
        if (erro != null) return erro;
        Map<String, Projeto> m = projetos.computeIfAbsent(uuid, k -> new LinkedHashMap<>());
        String k = MarcadorDados.chave(nome);
        if (!m.containsKey(k) && m.size() >= MAX_PROJETOS) return "Você já tem " + MAX_PROJETOS + " projetos salvos. Apague algum.";
        p.nome = nome;
        p.criadoEm = agora;
        m.put(k, p);
        return null;
    }

    public Projeto obter(String uuid, String nome) {
        Map<String, Projeto> m = projetos.get(uuid);
        return m == null ? null : m.get(MarcadorDados.chave(nome));
    }

    public boolean apagar(String uuid, String nome) {
        Map<String, Projeto> m = projetos.get(uuid);
        return m != null && m.remove(MarcadorDados.chave(nome)) != null;
    }

    public List<Projeto> lista(String uuid) {
        Map<String, Projeto> m = projetos.get(uuid);
        return m == null ? List.of() : new ArrayList<>(m.values());
    }

    public static ContagemBlocos ler(Path arquivo) {
        ContagemBlocos d = Json.ler(arquivo, ContagemBlocos.class);
        if (d == null) return new ContagemBlocos();
        if (d.projetos == null) d.projetos = new LinkedHashMap<>();
        for (Map<String, Projeto> m : d.projetos.values()) {
            for (Projeto p : m.values()) {
                if (p.contagem == null) p.contagem = new Contagem();
                if (p.contagem.blocos == null) p.contagem.blocos = new LinkedHashMap<>();
            }
        }
        return d;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
