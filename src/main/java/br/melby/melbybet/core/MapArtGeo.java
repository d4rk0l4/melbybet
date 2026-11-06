package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Contas de mapa-arte: grade dos mapas do jogo, tamanho em blocos, quantidade de mapas e blocos, e projetos salvos. Lógica pura. */
public class MapArtGeo {
    public static final int MAX_ESCALA = 4;
    public static final int MAX_LADO = 16;
    public static final int MAX_MAPAS = 64;
    public static final int MAX_PROJETOS = 10;
    public static final int PIXELS = 128;

    /** Lado de um mapa em blocos: 128, 256, 512, 1024 ou 2048. */
    public static int lado(int escala) {
        return PIXELS << escala;
    }

    /** Índice da célula da grade que contém o ponto (a grade do jogo é deslocada em 64 blocos). */
    public static int celula(double coord, int escala) {
        return (int) Math.floor((coord + 64.0) / lado(escala));
    }

    public static int inicio(int indice, int escala) {
        return indice * lado(escala) - 64;
    }

    public static int fim(int indice, int escala) {
        return inicio(indice, escala) + lado(escala) - 1;
    }

    /** Centro do mapa daquela célula (o que o mapa guarda como centerX / centerZ). */
    public static int centro(int indice, int escala) {
        return indice * lado(escala) + lado(escala) / 2 - 64;
    }

    /** Devolve o erro, ou null. */
    public static String validar(int largM, int altM, int escala) {
        if (escala < 0 || escala > MAX_ESCALA) return "A escala vai de 0 a " + MAX_ESCALA + ".";
        if (largM < 1 || altM < 1) return "O tamanho precisa ser de pelo menos 1 mapa.";
        if (largM > MAX_LADO || altM > MAX_LADO) return "No máximo " + MAX_LADO + " mapas de lado.";
        if (largM * altM > MAX_MAPAS) return "No máximo " + MAX_MAPAS + " mapas no total.";
        return null;
    }

    /** Um resumo do que um mapa-arte de largM x altM mapas pede. */
    public static class Conta {
        public int mapas, escala, largBlocos, altBlocos;
        public long pixels, areaBlocos, blocosPlano, blocosRelevo;
    }

    public static Conta calcular(int largM, int altM, int escala) {
        Conta c = new Conta();
        c.escala = escala;
        c.mapas = largM * altM;
        c.largBlocos = largM * lado(escala);
        c.altBlocos = altM * lado(escala);
        c.pixels = (long) c.mapas * PIXELS * PIXELS;
        c.areaBlocos = (long) c.largBlocos * c.altBlocos;
        c.blocosPlano = c.pixels;
        // com relevo (cores de três tons), cada coluna de 128 precisa de uma linha extra ao norte para dar o tom
        c.blocosRelevo = (long) largM * PIXELS * ((long) altM * PIXELS + 1);
        return c;
    }

    public static class Projeto {
        public String nome = "";
        public String dim = "";
        public int escala, gx, gz, largM, altM;
        public long criadoEm;

        public int x0() { return inicio(gx, escala); }
        public int z0() { return inicio(gz, escala); }
        public int x1() { return fim(gx + largM - 1, escala); }
        public int z1() { return fim(gz + altM - 1, escala); }
    }

    /** uuid -> nome em minúsculas -> projeto. */
    public Map<String, Map<String, Projeto>> projetos = new LinkedHashMap<>();

    public String salvar(String uuid, Projeto p, long agora) {
        String erro = MarcadorDados.validarNome(p.nome);
        if (erro != null) return erro;
        erro = validar(p.largM, p.altM, p.escala);
        if (erro != null) return erro;
        Map<String, Projeto> m = projetos.computeIfAbsent(uuid, k -> new LinkedHashMap<>());
        String k = MarcadorDados.chave(p.nome);
        if (!m.containsKey(k) && m.size() >= MAX_PROJETOS) return "Você já tem " + MAX_PROJETOS + " projetos de mapa-arte. Apague algum.";
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

    public static MapArtGeo ler(Path arquivo) {
        MapArtGeo d = Json.ler(arquivo, MapArtGeo.class);
        if (d == null) return new MapArtGeo();
        if (d.projetos == null) d.projetos = new LinkedHashMap<>();
        return d;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
