package br.melby.melbybet.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Guias de forma para construção: quais blocos formam a casca de um círculo, esfera, elipse ou cúpula de certo tamanho.
 * Só geometria (nada é colocado no mundo). Posições relativas ao centro.
 */
public final class Formas {
    private Formas() {}

    public static final int MAX_RAIO = 64;

    public enum Tipo {
        CIRCULO("Círculo"), ESFERA("Esfera"), ELIPSE("Elipse"), CUPULA("Cúpula");

        public final String rotulo;

        Tipo(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    /** Plano de um círculo ou elipse. */
    public enum Plano {
        HORIZONTAL("Horizontal (XZ)"), VERTICAL_X("Vertical (XY)"), VERTICAL_Z("Vertical (ZY)");

        public final String rotulo;

        Plano(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    private static boolean dentro(int x, int y, int z, double rx, double ry, double rz) {
        double a = rx + 0.5, b = ry + 0.5, c = rz + 0.5;
        double v = (x * x) / (a * a) + (y * y) / (b * b) + (z * z) / (c * c);
        return v <= 1.0;
    }

    /** Casca (só a superfície) da forma, ou o contorno no caso de círculo e elipse. Cada item é {x, y, z} relativo ao centro. */
    public static List<int[]> casca(Tipo tipo, Plano plano, int rx, int ry, int rz) {
        rx = Math.max(1, Math.min(MAX_RAIO, rx));
        ry = Math.max(1, Math.min(MAX_RAIO, ry));
        rz = Math.max(1, Math.min(MAX_RAIO, rz));
        List<int[]> r = new ArrayList<>();
        switch (tipo) {
            case CIRCULO -> contorno2d(r, plano, rx, rx);
            case ELIPSE -> contorno2d(r, plano, rx, rz);
            case ESFERA, CUPULA -> {
                boolean cupula = tipo == Tipo.CUPULA;
                int yMin = cupula ? 0 : -ry;
                for (int x = -rx; x <= rx; x++) {
                    for (int y = yMin; y <= ry; y++) {
                        for (int z = -rz; z <= rz; z++) {
                            if (!dentro(x, y, z, rx, ry, rz)) continue;
                            boolean borda = !dentro(x + 1, y, z, rx, ry, rz) || !dentro(x - 1, y, z, rx, ry, rz)
                                || !dentro(x, y + 1, z, rx, ry, rz) || (!cupula || y > 0) && !dentro(x, y - 1, z, rx, ry, rz)
                                || !dentro(x, y, z + 1, rx, ry, rz) || !dentro(x, y, z - 1, rx, ry, rz);
                            if (borda) r.add(new int[] {x, y, z});
                        }
                    }
                }
            }
        }
        return r;
    }

    /** Contorno 2D de uma elipse de semieixos a (primeiro eixo do plano) e b (segundo). */
    private static void contorno2d(List<int[]> r, Plano plano, int a, int b) {
        for (int u = -a; u <= a; u++) {
            for (int v = -b; v <= b; v++) {
                if (!dentro(u, v, 0, a, b, 0)) continue;
                boolean borda = !dentro(u + 1, v, 0, a, b, 0) || !dentro(u - 1, v, 0, a, b, 0) || !dentro(u, v + 1, 0, a, b, 0) || !dentro(u, v - 1, 0, a, b, 0);
                if (!borda) continue;
                switch (plano) {
                    case HORIZONTAL -> r.add(new int[] {u, 0, v});
                    case VERTICAL_X -> r.add(new int[] {u, v, 0});
                    case VERTICAL_Z -> r.add(new int[] {0, v, u});
                }
            }
        }
    }

    /** Quantos blocos tem a casca (para estimar material). */
    public static int quantos(Tipo tipo, Plano plano, int rx, int ry, int rz) {
        return casca(tipo, plano, rx, ry, rz).size();
    }

    // ---------- régua e espelho ----------

    /** Distâncias entre dois blocos: dx, dy, dz (diferença em blocos, contando os dois cantos) e a distância reta. */
    public static double[] medir(int[] a, int[] b) {
        double dx = Math.abs(a[0] - b[0]) + 1, dy = Math.abs(a[1] - b[1]) + 1, dz = Math.abs(a[2] - b[2]) + 1;
        double reta = Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
        return new double[] {dx, dy, dz, reta};
    }

    /** Bloco espelhado em relação a um plano. eixoX = o plano é perpendicular a X (espelha x). entreBlocos = plano na borda do bloco. */
    public static int[] espelhar(int[] p, boolean eixoX, int plano, boolean entreBlocos) {
        int soma = 2 * plano + (entreBlocos ? 1 : 0);
        return eixoX ? new int[] {soma - p[0], p[1], p[2]} : new int[] {p[0], p[1], soma - p[2]};
    }
}
