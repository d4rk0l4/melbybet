package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dados do /marcador: a seleção atual de cada jogador (dois cantos) e as áreas que ele salvou com nome, que podem ser
 * compartilhadas com outros. Lógica pura, guardada em config/melbybet/marcadores.json.
 */
public class MarcadorDados {
    public static final int MAX_NOME = 24;
    public static final int MAX_AREAS = 30;

    /** Área já normalizada (x0 <= x1, y0 <= y1, z0 <= z1), com os dois cantos incluídos. */
    public static class Area {
        public String nome = "";
        public String dono = "";
        public String donoNome = "";
        public String dim = "minecraft:overworld";
        public int x0, y0, z0, x1, y1, z1;
        /** UUIDs que podem ver e carregar esta área. */
        public List<String> partilha = new ArrayList<>();
        public long criadaEm;

        public int larg() { return x1 - x0 + 1; }
        public int alt() { return y1 - y0 + 1; }
        public int comp() { return z1 - z0 + 1; }

        public long volume() { return (long) larg() * alt() * comp(); }

        public boolean contem(int x, int y, int z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }

        public Area copia() {
            Area a = new Area();
            a.nome = nome;
            a.dono = dono;
            a.donoNome = donoNome;
            a.dim = dim;
            a.x0 = x0;
            a.y0 = y0;
            a.z0 = z0;
            a.x1 = x1;
            a.y1 = y1;
            a.z1 = z1;
            a.partilha = new ArrayList<>(partilha);
            a.criadaEm = criadaEm;
            return a;
        }

        /** "x 10..20, y 60..70, z -5..4". */
        public String resumo() {
            return "x " + x0 + ".." + x1 + ", y " + y0 + ".." + y1 + ", z " + z0 + ".." + z1;
        }
    }

    /** Os dois cantos escolhidos agora; p1 e p2 são {x,y,z} ou null. */
    public static class Selecao {
        public String dim = "minecraft:overworld";
        public int[] p1, p2;

        public boolean completa() {
            return p1 != null && p2 != null;
        }

        /** Marca o canto 1 ou 2. Trocar de dimensão descarta o outro canto, já que um não vale na outra. */
        public void definir(int canto, String dimensao, int x, int y, int z) {
            if (!dim.equals(dimensao)) {
                p1 = null;
                p2 = null;
                dim = dimensao;
            }
            if (canto == 1) p1 = new int[] {x, y, z};
            else p2 = new int[] {x, y, z};
        }

        public void limpar() {
            p1 = null;
            p2 = null;
        }

        /** A área normalizada, ou null se faltar um canto. */
        public Area area() {
            if (!completa()) return null;
            Area a = new Area();
            a.dim = dim;
            a.x0 = Math.min(p1[0], p2[0]);
            a.x1 = Math.max(p1[0], p2[0]);
            a.y0 = Math.min(p1[1], p2[1]);
            a.y1 = Math.max(p1[1], p2[1]);
            a.z0 = Math.min(p1[2], p2[2]);
            a.z1 = Math.max(p1[2], p2[2]);
            return a;
        }

        public void carregar(Area a) {
            dim = a.dim;
            p1 = new int[] {a.x0, a.y0, a.z0};
            p2 = new int[] {a.x1, a.y1, a.z1};
        }

        /**
         * Cresce (n > 0) ou encolhe (n < 0) a seleção em todos os lados. O Y fica entre minY e maxY (inclusive).
         * Devolve false, sem mexer em nada, se encolher deixaria algum lado com menos de 1 bloco.
         */
        public boolean ajustar(int n, int minY, int maxY) {
            Area a = area();
            if (a == null) return false;
            if (n < 0 && (a.larg() + 2 * n < 1 || a.alt() + 2 * n < 1 || a.comp() + 2 * n < 1)) return false;
            p1 = new int[] {a.x0 - n, Math.max(minY, a.y0 - n), a.z0 - n};
            p2 = new int[] {a.x1 + n, Math.min(maxY, a.y1 + n), a.z1 + n};
            return true;
        }

        /** Estica a seleção do fundo ao topo do mundo. */
        public boolean altura(int minY, int maxY) {
            Area a = area();
            if (a == null) return false;
            p1 = new int[] {a.x0, minY, a.z0};
            p2 = new int[] {a.x1, maxY, a.z1};
            return true;
        }
    }

    public static class Jogador {
        public Selecao atual = new Selecao();
        /** Nome em minúsculas -> área. */
        public Map<String, Area> salvas = new LinkedHashMap<>();
    }

    public Map<String, Jogador> jogadores = new LinkedHashMap<>();

    public Jogador de(String uuid) {
        return jogadores.computeIfAbsent(uuid, k -> new Jogador());
    }

    public static String chave(String nome) {
        return nome.toLowerCase(Locale.ROOT);
    }

    /** Devolve o motivo se o nome não serve, ou null. Letras, números, _ e -, até 24 caracteres. */
    public static String validarNome(String nome) {
        if (nome == null || nome.isEmpty()) return "Dê um nome para a área.";
        if (nome.length() > MAX_NOME) return "Nome muito longo (máximo " + MAX_NOME + " letras).";
        if (!nome.matches("[\\p{L}0-9_-]+")) return "Use só letras, números, _ e - no nome.";
        return null;
    }

    /** Salva a área com esse nome (substitui uma do mesmo dono com o mesmo nome). Devolve o erro, ou null. */
    public String salvar(String uuid, String donoNome, String nome, Area area, long agora) {
        String erro = validarNome(nome);
        if (erro != null) return erro;
        if (area == null) return "Marque os dois cantos primeiro.";
        Jogador j = de(uuid);
        Area antiga = j.salvas.get(chave(nome));
        if (antiga == null && j.salvas.size() >= MAX_AREAS) return "Você já tem " + MAX_AREAS + " áreas salvas. Apague alguma.";
        Area a = area.copia();
        a.nome = nome;
        a.dono = uuid;
        a.donoNome = donoNome;
        a.criadaEm = agora;
        if (antiga != null) a.partilha = new ArrayList<>(antiga.partilha);
        j.salvas.put(chave(nome), a);
        return null;
    }

    public boolean apagar(String uuid, String nome) {
        return de(uuid).salvas.remove(chave(nome)) != null;
    }

    /** Dá acesso a quem é de outro jogador. Devolve o erro, ou null. */
    public String compartilhar(String uuid, String nome, String outroUuid) {
        Area a = de(uuid).salvas.get(chave(nome));
        if (a == null) return "Você não tem uma área chamada \"" + nome + "\".";
        if (uuid.equals(outroUuid)) return "Essa área já é sua.";
        if (a.partilha.contains(outroUuid)) return "Já está compartilhada com essa pessoa.";
        a.partilha.add(outroUuid);
        return null;
    }

    public String descompartilhar(String uuid, String nome, String outroUuid) {
        Area a = de(uuid).salvas.get(chave(nome));
        if (a == null) return "Você não tem uma área chamada \"" + nome + "\".";
        if (!a.partilha.remove(outroUuid)) return "Essa área não estava compartilhada com essa pessoa.";
        return null;
    }

    /** As áreas do jogador, seguidas das que os outros compartilharam com ele. */
    public List<Area> visiveis(String uuid) {
        List<Area> r = new ArrayList<>(de(uuid).salvas.values());
        for (Map.Entry<String, Jogador> e : jogadores.entrySet()) {
            if (e.getKey().equals(uuid)) continue;
            for (Area a : e.getValue().salvas.values()) if (a.partilha.contains(uuid)) r.add(a);
        }
        return r;
    }

    /** "nome" (a sua, senão a única compartilhada com esse nome) ou "dono:nome". Null se não achar ou se for ambíguo. */
    public Area buscar(String uuid, String ref) {
        int i = ref.indexOf(':');
        if (i > 0) {
            String quem = ref.substring(0, i), nome = ref.substring(i + 1);
            for (Area a : visiveis(uuid)) if (a.donoNome.equalsIgnoreCase(quem) && a.nome.equalsIgnoreCase(nome)) return a;
            return null;
        }
        Area minha = de(uuid).salvas.get(chave(ref));
        if (minha != null) return minha;
        Area achada = null;
        for (Area a : visiveis(uuid)) {
            if (!a.nome.equalsIgnoreCase(ref)) continue;
            if (achada != null) return null;
            achada = a;
        }
        return achada;
    }

    /**
     * Pontos para desenhar as 12 arestas da caixa (os blocos inteiros, de x0 até x1+1), no máximo {@code maxPontos}.
     * Cada ponto é {x, y, z}. Caixas grandes ganham pontos mais espaçados.
     */
    public static List<double[]> arestas(Area a, int maxPontos) {
        double xa = a.x0, xb = a.x1 + 1.0, ya = a.y0, yb = a.y1 + 1.0, za = a.z0, zb = a.z1 + 1.0;
        double comprimento = 4.0 * ((xb - xa) + (yb - ya) + (zb - za));
        double passo = Math.max(1.0, Math.ceil(comprimento / Math.max(8, maxPontos)));
        List<double[]> r = new ArrayList<>();
        double[] xs = {xa, xb}, ys = {ya, yb}, zs = {za, zb};
        // pontos igualmente espaçados em cada aresta, sempre incluindo as duas pontas
        for (double y : ys) for (double z : zs) for (double x : ao_longo(xa, xb, passo)) r.add(new double[] {x, y, z});
        for (double x : xs) for (double z : zs) for (double y : ao_longo(ya, yb, passo)) r.add(new double[] {x, y, z});
        for (double x : xs) for (double y : ys) for (double z : ao_longo(za, zb, passo)) r.add(new double[] {x, y, z});
        // o último ponto de cada aresta pode cair em cima do canto: tira repetidos
        List<double[]> unicos = new ArrayList<>();
        java.util.Set<String> vistos = new java.util.HashSet<>();
        for (double[] p : r) if (vistos.add(p[0] + "," + p[1] + "," + p[2])) unicos.add(p);
        return unicos;
    }

    private static double[] ao_longo(double de, double ate, double passo) {
        int n = (int) Math.max(1, Math.ceil((ate - de) / passo));
        double[] r = new double[n + 1];
        for (int i = 0; i <= n; i++) r[i] = de + (ate - de) * i / n;
        return r;
    }

    public static MarcadorDados ler(Path arquivo) {
        MarcadorDados d = Json.ler(arquivo, MarcadorDados.class);
        if (d == null) return new MarcadorDados();
        if (d.jogadores == null) d.jogadores = new LinkedHashMap<>();
        for (Jogador j : d.jogadores.values()) {
            if (j.atual == null) j.atual = new Selecao();
            if (j.salvas == null) j.salvas = new LinkedHashMap<>();
            for (Area a : j.salvas.values()) if (a.partilha == null) a.partilha = new ArrayList<>();
        }
        return d;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
