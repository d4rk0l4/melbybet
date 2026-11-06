package br.melby.melbybet.core;

/** As cores dos mapas do jogo e a conversão de uma imagem para elas (com ou sem difusão de erro). Lógica pura. */
public final class PaletaMapa {
    private PaletaMapa() {}

    /** Cor RGB de cada cor-base do mapa (índice 0 é transparente). */
    public static final int[] BASE_RGB = {
        0x000000, 0x7FB238, 0xF7E9A3, 0xC7C7C7, 0xFF0000, 0xA0A0FF, 0xA7A7A7, 0x007C00, 0xFFFFFF, 0xA4A8B8,
        0x976D4D, 0x707070, 0x4040FF, 0x8F7748, 0xFFFCF5, 0xD87F33, 0xB24CD8, 0x6699D8, 0xE5E533, 0x7FCC19,
        0xF27FA5, 0x4C4C4C, 0x999999, 0x4C7F99, 0x7F3FB2, 0x334CB2, 0x664C33, 0x667F33, 0x993333, 0x191919,
        0xFAEE4D, 0x5CDBD5, 0x4A80FF, 0x00D93A, 0x815631, 0x700200, 0xD1B1A1, 0x9F5224, 0x95576C, 0x706C8A,
        0xBA8524, 0x677535, 0xA04D4E, 0x392923, 0x876B62, 0x575C5C, 0x7A4958, 0x4C3E5C, 0x4C3223, 0x4C522A,
        0x8E3C2E, 0x251610, 0xBD3031, 0x943F61, 0x5C191D, 0x167E86, 0x3A8E8C, 0x562C3E, 0x14B485, 0x646464,
        0xD8AF93, 0x7FA796};

    /** Quanto cada tom (0 a 3) escurece ou clareia a cor-base, em 255 avos. */
    public static final int[] TOM = {180, 220, 255, 135};

    public static final int PRIMEIRA = 4;
    public static final int ULTIMA = BASE_RGB.length * 4 - 1;

    /** RGB da cor de mapa {@code id} (byte sem sinal, de 4 a 247). */
    public static int rgb(int id, int[] base) {
        int b = base[id >> 2];
        int m = TOM[id & 3];
        return ((b >> 16 & 0xFF) * m / 255) << 16 | ((b >> 8 & 0xFF) * m / 255) << 8 | (b & 0xFF) * m / 255;
    }

    public static int[] paleta(int[] base) {
        int[] p = new int[(base.length) * 4];
        for (int i = 0; i < p.length; i++) p[i] = i < PRIMEIRA ? -1 : rgb(i, base);
        return p;
    }

    private static final int[] PALETA = paleta(BASE_RGB);
    private static final byte[] TABELA = new byte[32768];
    private static final boolean[] PRONTA = new boolean[32768];

    /** A cor do mapa mais próxima (distância "redmean", boa para olhos humanos). */
    public static int maisProxima(int r, int g, int b) {
        int melhor = PRIMEIRA;
        long melhorD = Long.MAX_VALUE;
        for (int i = PRIMEIRA; i < PALETA.length; i++) {
            int c = PALETA[i];
            int dr = r - (c >> 16 & 0xFF), dg = g - (c >> 8 & 0xFF), db = b - (c & 0xFF);
            long rm = (r + (c >> 16 & 0xFF)) / 2;
            long d = ((512 + rm) * dr * dr >> 8) + 4L * dg * dg + ((767 - rm) * db * db >> 8);
            if (d < melhorD) {
                melhorD = d;
                melhor = i;
            }
        }
        return melhor;
    }

    private static int busca(int r, int g, int b) {
        int k = (r >> 3) << 10 | (g >> 3) << 5 | (b >> 3);
        if (!PRONTA[k]) {
            // o centro da caixa de 8x8x8 que esse índice representa
            TABELA[k] = (byte) maisProxima((r & 0xF8) | 4, (g & 0xF8) | 4, (b & 0xF8) | 4);
            PRONTA[k] = true;
        }
        return TABELA[k] & 0xFF;
    }

    private static int limitar(float v) {
        return v < 0 ? 0 : Math.min(255, Math.round(v));
    }

    /**
     * Converte pixels ARGB para ids de cor de mapa. Pixel com transparência (alfa abaixo de 128) vira 0 (sem cor).
     * Com {@code difusao}, espalha o erro de cada pixel nos vizinhos (Floyd-Steinberg).
     */
    public static byte[] converter(int[] argb, int w, int h, boolean difusao) {
        byte[] r = new byte[w * h];
        float[][] erro = new float[2][w * 3 + 6];
        int atual = 0;
        for (int y = 0; y < h; y++) {
            float[] e = erro[atual], prox = erro[1 - atual];
            java.util.Arrays.fill(prox, 0f);
            for (int x = 0; x < w; x++) {
                int p = argb[y * w + x];
                if ((p >>> 24) < 128) {
                    r[y * w + x] = 0;
                    continue;
                }
                int rr = p >> 16 & 0xFF, gg = p >> 8 & 0xFF, bb = p & 0xFF;
                if (difusao) {
                    rr = limitar(rr + e[(x + 1) * 3]);
                    gg = limitar(gg + e[(x + 1) * 3 + 1]);
                    bb = limitar(bb + e[(x + 1) * 3 + 2]);
                }
                int id = busca(rr, gg, bb);
                r[y * w + x] = (byte) id;
                if (difusao) {
                    int c = PALETA[id];
                    float[] d = {rr - (c >> 16 & 0xFF), gg - (c >> 8 & 0xFF), bb - (c & 0xFF)};
                    for (int k = 0; k < 3; k++) {
                        e[(x + 2) * 3 + k] += d[k] * 7f / 16f;
                        prox[x * 3 + k] += d[k] * 3f / 16f;
                        prox[(x + 1) * 3 + k] += d[k] * 5f / 16f;
                        prox[(x + 2) * 3 + k] += d[k] * 1f / 16f;
                    }
                }
            }
            atual = 1 - atual;
        }
        return r;
    }
}
