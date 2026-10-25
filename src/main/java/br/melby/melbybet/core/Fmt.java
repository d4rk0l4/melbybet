package br.melby.melbybet.core;

/** Números no formato brasileiro, como no ranking do Discord: 2.440 / 41,42 / 8,90. */
public final class Fmt {
    private Fmt() {}

    public static String inteiro(long n) {
        String s = Long.toString(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) sb.append('.');
            sb.append(s.charAt(i));
        }
        return (n < 0 ? "-" : "") + sb;
    }

    public static String decimal(double v, int casas) {
        long fator = 1;
        for (int i = 0; i < casas; i++) fator *= 10;
        long x = Math.round(Math.abs(v) * fator);
        String frac = Long.toString(x % fator);
        while (frac.length() < casas) frac = "0" + frac;
        return (v < 0 ? "-" : "") + inteiro(x / fator) + (casas > 0 ? "," + frac : "");
    }
}
