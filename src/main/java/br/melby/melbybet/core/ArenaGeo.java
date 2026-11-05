package br.melby.melbybet.core;

import java.util.ArrayList;
import java.util.List;

/** Geometria da arena de duelo (caixa de vidro) e escolha de lugar. Lógica pura, sem Minecraft. */
public final class ArenaGeo {
    private ArenaGeo() {}

    /** Caixa alinhada aos eixos; x0,y0,z0 é o canto mínimo e larg/comp/alt são as medidas EXTERNAS, em blocos. */
    public record Caixa(int x0, int y0, int z0, int larg, int comp, int alt) {
        public int x1() { return x0 + larg - 1; }
        public int y1() { return y0 + alt - 1; }
        public int z1() { return z0 + comp - 1; }

        public boolean contem(int x, int y, int z) {
            return x >= x0 && x <= x1() && y >= y0 && y <= y1() && z >= z0 && z <= z1();
        }

        /** Dentro da caixa, sem contar chão, teto e paredes. */
        public boolean interior(int x, int y, int z) {
            return x > x0 && x < x1() && y > y0 && y < y1() && z > z0 && z < z1();
        }

        /** Chão, teto e paredes. */
        public boolean casca(int x, int y, int z) {
            return contem(x, y, z) && !interior(x, y, z);
        }

        /** A mesma caixa aumentada em m blocos para todos os lados. */
        public Caixa expandida(int m) {
            return new Caixa(x0 - m, y0 - m, z0 - m, larg + 2 * m, comp + 2 * m, alt + 2 * m);
        }

        /** As duas caixas (cada uma com folga extra) se tocam? */
        public boolean cruza(Caixa o, int folga) {
            Caixa a = expandida(folga);
            return a.x0 <= o.x1() && a.x1() >= o.x0 && a.y0 <= o.y1() && a.y1() >= o.y0 && a.z0 <= o.z1() && a.z1() >= o.z0;
        }

        public int total() { return larg * comp * alt; }

        /** Centro horizontal. */
        public double centroX() { return x0 + larg / 2.0; }
        public double centroZ() { return z0 + comp / 2.0; }

        /** Onde o jogador A e o B nascem: lados opostos (z), em cima do chão, no meio da largura. */
        public double[] posA() { return new double[] {centroX(), y0 + 1, z0 + 2.5, 0}; }
        public double[] posB() { return new double[] {centroX(), y0 + 1, z1() - 1.5, 180}; }

        /** O ponto (x,y,z) está dentro da parte livre da caixa (os blocos do interior ocupam [x0+1, x1) e assim por diante)? */
        public boolean dentroLivre(double x, double y, double z) {
            return x >= x0 + 1 && x < x1() && y >= y0 + 0.5 && y < y1() && z >= z0 + 1 && z < z1();
        }
    }

    /**
     * Y do chão da arena: acima do terreno e abaixo do teto do mundo, ou -1 se não couber.
     * @param terrenoMax altura do terreno mais alto sob a arena
     * @param minY menor Y do mundo; @param maxY maior Y do mundo (inclusive)
     */
    public static int alturaBase(int terrenoMax, int minY, int maxY, int alt, int acima) {
        int teto = maxY - alt - 1; // deixa 1 bloco de margem acima do teto da arena
        int y = Math.min(terrenoMax + acima, teto);
        if (y < terrenoMax + 8 || y - 1 < minY) return -1;
        return y;
    }

    /** Pontos (x,z) para tentar, do centro para fora, em anéis quadrados de distância passo*k. */
    public static List<int[]> candidatos(int cx, int cz, int passo, int aneis) {
        List<int[]> r = new ArrayList<>();
        r.add(new int[] {cx, cz});
        for (int k = 1; k <= aneis; k++) {
            for (int dx = -k; dx <= k; dx++) {
                for (int dz = -k; dz <= k; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == k) r.add(new int[] {cx + dx * passo, cz + dz * passo});
                }
            }
        }
        return r;
    }

    /** Caixa com a metade do tamanho centrada em (cx,cz). */
    public static Caixa centrada(int cx, int cz, int y0, int larg, int comp, int alt) {
        return new Caixa(cx - larg / 2, y0, cz - comp / 2, larg, comp, alt);
    }
}
