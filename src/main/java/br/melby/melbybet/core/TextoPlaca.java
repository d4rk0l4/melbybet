package br.melby.melbybet.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Texto de placa com códigos: &c (cores), &l negrito, &o itálico, &n sublinhado, &m riscado, &k embaralhado,
 * &r limpa, &#ff8a24 (cor exata) e degradê <#ff0000-#0000ff>texto</>. Sem nada do Minecraft aqui.
 */
public final class TextoPlaca {
    private TextoPlaca() {}

    public static final class Seg {
        public String texto;
        public int cor = -1;
        public boolean negrito, italico, sublinhado, riscado, embaralhado;
    }

    private static final int[] PALETA = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final Pattern GRADIENTE = Pattern.compile("^<(#[0-9a-fA-F]{6})-(#[0-9a-fA-F]{6})>");

    /** Tem algum código que precisa ser convertido? */
    public static boolean temCodigo(String s) {
        return s.indexOf('&') >= 0 || s.contains("<#");
    }

    private static final class Estado {
        int cor = -1;
        boolean n, i, u, m, k;
        void zerar() { cor = -1; n = i = u = m = k = false; }
        Seg para(String t) {
            Seg s = new Seg();
            s.texto = t; s.cor = cor; s.negrito = n; s.italico = i; s.sublinhado = u; s.riscado = m; s.embaralhado = k;
            return s;
        }
    }

    public static List<Seg> ler(String bruto) {
        List<Seg> r = new ArrayList<>();
        Estado e = new Estado();
        StringBuilder atual = new StringBuilder();
        int g1 = -1, g2 = -1;
        List<Seg> grad = new ArrayList<>(); // um caractere por item, com o estilo que tinha
        int i = 0;
        while (i < bruto.length()) {
            char ch = bruto.charAt(i);
            if (ch == '<' && g1 < 0) {
                Matcher mt = GRADIENTE.matcher(bruto.substring(i));
                if (mt.find()) {
                    sobra(r, e, atual);
                    g1 = Gradiente.hex(mt.group(1));
                    g2 = Gradiente.hex(mt.group(2));
                    i += mt.end();
                    continue;
                }
            }
            if (ch == '<' && g1 >= 0 && bruto.startsWith("</>", i)) {
                fechar(r, grad, g1, g2);
                g1 = -1;
                i += 3;
                continue;
            }
            if (ch == '&' && i + 1 < bruto.length()) {
                char n = Character.toLowerCase(bruto.charAt(i + 1));
                if (n == '#' && i + 7 < bruto.length() + 0 && Gradiente.hex(bruto.substring(i + 1, i + 8)) >= 0) {
                    if (g1 < 0) {
                        sobra(r, e, atual);
                        e.zerar();
                        e.cor = Gradiente.hex(bruto.substring(i + 1, i + 8));
                    }
                    i += 8;
                    continue;
                }
                int d = Character.digit(n, 16);
                if (d >= 0) {
                    if (g1 < 0) {
                        sobra(r, e, atual);
                        e.zerar();
                        e.cor = PALETA[d];
                    }
                    i += 2;
                    continue;
                }
                if ("lonmkr".indexOf(n) >= 0) {
                    if (g1 < 0) sobra(r, e, atual);
                    switch (n) {
                        case 'l': e.n = true; break;
                        case 'o': e.i = true; break;
                        case 'n': e.u = true; break;
                        case 'm': e.m = true; break;
                        case 'k': e.k = true; break;
                        default: e.zerar();
                    }
                    i += 2;
                    continue;
                }
            }
            if (g1 >= 0) grad.add(e.para(String.valueOf(ch)));
            else atual.append(ch);
            i++;
        }
        if (g1 >= 0) fechar(r, grad, g1, g2);
        sobra(r, e, atual);
        return r;
    }

    private static void sobra(List<Seg> r, Estado e, StringBuilder atual) {
        if (atual.length() == 0) return;
        r.add(e.para(atual.toString()));
        atual.setLength(0);
    }

    private static void fechar(List<Seg> r, List<Seg> grad, int a, int b) {
        int[] cores = Gradiente.cores(grad.size(), a, b);
        for (int k = 0; k < grad.size(); k++) {
            grad.get(k).cor = cores[k];
            r.add(grad.get(k));
        }
        grad.clear();
    }

    /** O texto sem códigos (o que aparece na placa). */
    public static String semCodigos(String bruto) {
        StringBuilder sb = new StringBuilder();
        for (Seg s : ler(bruto)) sb.append(s.texto);
        return sb.toString();
    }
}
