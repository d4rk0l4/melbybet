package br.melby.melbybet.core;

/** Lê um apelido com códigos de cor (&c, &#ff8a24) e tira o degradê dele. Sem nada do Minecraft aqui. */
public final class Apelido {
    private Apelido() {}

    public static final class Resultado {
        public String texto = "";
        /** Primeira e última cor encontradas; -1 se o apelido não tinha código de cor. */
        public int cor1 = -1, cor2 = -1;
    }

    private static final int[] PALETA = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    public static Resultado ler(String bruto) {
        Resultado r = new Resultado();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < bruto.length()) {
            char ch = bruto.charAt(i);
            if (ch == '&' && i + 1 < bruto.length()) {
                char n = bruto.charAt(i + 1);
                if (n == '#' && i + 7 < bruto.length() + 0 && Gradiente.hex(bruto.substring(i + 1, i + 8)) >= 0) {
                    cor(r, Gradiente.hex(bruto.substring(i + 1, i + 8)));
                    i += 8;
                    continue;
                }
                int d = Character.digit(n, 16);
                if (d >= 0) {
                    cor(r, PALETA[d]);
                    i += 2;
                    continue;
                }
            }
            sb.append(ch);
            i++;
        }
        r.texto = sb.toString().trim();
        return r;
    }

    private static void cor(Resultado r, int c) {
        if (r.cor1 < 0) r.cor1 = c;
        r.cor2 = c;
    }

    /** Motivo de o apelido não servir, ou null se estiver bom. */
    public static String validar(String texto) {
        if (texto.length() < 2 || texto.length() > 16) return "O apelido precisa ter de 2 a 16 letras (sem contar os códigos de cor).";
        if (!texto.matches("[A-Za-z0-9_\\-À-ÿ ]+")) return "Use só letras, números, espaço, _ e -.";
        return null;
    }
}
