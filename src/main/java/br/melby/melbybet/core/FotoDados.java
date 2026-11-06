package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Murais de fotos (várias molduras com mapas) e as regras e a geometria deles. Lógica pura, em config/melbybet/fotomolduras.json. */
public class FotoDados {
    public static final int MAX_LADO = 6;
    public static final int MAX_POR_JOGADOR = 5;
    public static final int MAX_MAPAS_TOTAL = 1500;

    public static class Grupo {
        public int id;
        public String dono = "", donoNome = "", dim = "", site = "";
        /** Direção para onde a parede olha: NORTH, SOUTH, EAST ou WEST. */
        public String direcao = "NORTH";
        /** Célula do canto inferior esquerdo (visto de frente). */
        public int x, y, z, larg, alt;
        /** UUIDs das molduras, linha a linha de cima para baixo. */
        public List<String> molduras = new ArrayList<>();
        public List<Integer> mapas = new ArrayList<>();
        public long criadoEm;

        public int quantidade() {
            return larg * alt;
        }
    }

    public int proximoId = 1;
    public List<Grupo> grupos = new ArrayList<>();

    /** "3x2" (ou "3X2") vira {3, 2}; null se o texto não for um tamanho válido de 1 a 6. */
    public static int[] tamanho(String s) {
        if (s == null) return null;
        String[] p = s.trim().toLowerCase().split("x");
        if (p.length != 2) return null;
        try {
            int l = Integer.parseInt(p[0].trim()), a = Integer.parseInt(p[1].trim());
            if (l < 1 || a < 1 || l > MAX_LADO || a > MAX_LADO) return null;
            return new int[] {l, a};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** O vetor "para a direita" de quem olha de frente para a parede, dado o vetor que sai da parede (nx, nz). */
    public static int[] direita(int nx, int nz) {
        return new int[] {nz, -nx};
    }

    /** Posição da moldura na coluna {@code col} e linha {@code lin} (0 = a de cima), a partir da célula inferior esquerda. */
    public static int[] celula(int x, int y, int z, int nx, int nz, int alt, int col, int lin) {
        int[] d = direita(nx, nz);
        return new int[] {x + d[0] * col, y + (alt - 1 - lin), z + d[1] * col};
    }

    /** Devolve o motivo se o jogador não pode criar mais um mural, ou null. */
    public String podeCriar(String uuid, boolean op, int mapasNovos) {
        if (!op && doJogador(uuid).size() >= MAX_POR_JOGADOR) return "Você já tem " + MAX_POR_JOGADOR + " murais. Apague algum (/fotomoldura apagar).";
        if (totalMapas() + mapasNovos > MAX_MAPAS_TOTAL) return "O servidor chegou no limite de mapas de fotos. Fale com a staff.";
        return null;
    }

    public int totalMapas() {
        int n = 0;
        for (Grupo g : grupos) n += g.mapas.size();
        return n;
    }

    public Grupo adicionar(Grupo g, long agora) {
        g.id = proximoId++;
        g.criadoEm = agora;
        grupos.add(g);
        return g;
    }

    public List<Grupo> doJogador(String uuid) {
        List<Grupo> r = new ArrayList<>();
        for (Grupo g : grupos) if (g.dono.equals(uuid)) r.add(g);
        return r;
    }

    public Grupo achar(int id) {
        for (Grupo g : grupos) if (g.id == id) return g;
        return null;
    }

    public Grupo acharPorMoldura(String uuidMoldura) {
        for (Grupo g : grupos) if (g.molduras.contains(uuidMoldura)) return g;
        return null;
    }

    public boolean remover(int id) {
        return grupos.removeIf(g -> g.id == id);
    }

    /** Devolve o motivo se o jogador não pode mexer no mural, ou null. Dono ou OP. */
    public static String pode(Grupo g, String uuid, boolean op) {
        if (op || g.dono.equals(uuid)) return null;
        return "Esse mural é de " + g.donoNome + ".";
    }

    public static FotoDados ler(Path arquivo) {
        FotoDados d = Json.ler(arquivo, FotoDados.class);
        if (d == null) return new FotoDados();
        if (d.grupos == null) d.grupos = new ArrayList<>();
        d.grupos.removeIf(g -> g == null);
        for (Grupo g : d.grupos) {
            if (g.molduras == null) g.molduras = new ArrayList<>();
            if (g.mapas == null) g.mapas = new ArrayList<>();
            if (g.dono == null) g.dono = "";
            if (g.donoNome == null) g.donoNome = "";
            if (g.dim == null) g.dim = "";
            if (g.site == null) g.site = "";
            if (g.direcao == null) g.direcao = "NORTH";
        }
        return d;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
