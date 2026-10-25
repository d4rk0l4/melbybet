package br.melby.melbybet.core;

/** Cores em degradê para títulos e mensagens. */
public final class Gradiente {
    private Gradiente() {}

    /** n cores indo de a até b (RGB 0xRRGGBB). */
    public static int[] cores(int n, int a, int b) {
        int[] r = new int[Math.max(n, 0)];
        for (int i = 0; i < r.length; i++) {
            double t = r.length == 1 ? 0 : (double) i / (r.length - 1);
            int rr = (int) Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
            int gg = (int) Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
            int bb = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
            r[i] = (rr << 16) | (gg << 8) | bb;
        }
        return r;
    }

    /** "#ff8a24" -> 0xFF8A24, ou -1 se não for uma cor válida. */
    public static int hex(String s) {
        if (s == null || !s.matches("#[0-9a-fA-F]{6}")) return -1;
        return Integer.parseInt(s.substring(1), 16);
    }
}
