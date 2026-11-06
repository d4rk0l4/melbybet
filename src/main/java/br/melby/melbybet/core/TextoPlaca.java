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

    private static boolean mesmoEstilo(Seg a, Seg b) {
        return a.negrito == b.negrito && a.italico == b.italico && a.sublinhado == b.sublinhado && a.riscado == b.riscado && a.embaralhado == b.embaralhado;
    }

    private static String codigoCor(int cor) {
        for (int k = 0; k < PALETA.length; k++) if (PALETA[k] == cor) return "&" + Integer.toHexString(k);
        return "&" + Gradiente.paraHex(cor);
    }

    private static String codigoFlags(Seg s) {
        return (s.negrito ? "&l" : "") + (s.italico ? "&o" : "") + (s.sublinhado ? "&n" : "") + (s.riscado ? "&m" : "") + (s.embaralhado ? "&k" : "");
    }

    /**
     * O contrário de {@link #ler}: devolve o texto com códigos (&c, &l, &#rrggbb e degradês) que, escrito de novo,
     * dá a mesma aparência. Trechos de cores que formam um degradê viram <#a-#b>texto</>.
     */
    public static String paraCodigos(List<Seg> segs) {
        List<Seg> cs = new ArrayList<>(); // um caractere por item
        for (Seg s : segs) {
            for (int k = 0; k < s.texto.length(); k++) {
                Seg c = new Seg();
                c.texto = String.valueOf(s.texto.charAt(k));
                c.cor = s.cor;
                c.negrito = s.negrito;
                c.italico = s.italico;
                c.sublinhado = s.sublinhado;
                c.riscado = s.riscado;
                c.embaralhado = s.embaralhado;
                cs.add(c);
            }
        }
        StringBuilder sb = new StringBuilder();
        Seg atual = new Seg(); // estado que o leitor teria agora: sem cor e sem estilo
        int i = 0;
        while (i < cs.size()) {
            Seg c = cs.get(i);
            // degradê: o maior trecho com o mesmo estilo cujas cores são exatamente as de um degradê entre as pontas
            int melhor = 0;
            if (c.cor >= 0) {
                int fim = i;
                while (fim + 1 < cs.size() && cs.get(fim + 1).cor >= 0 && mesmoEstilo(c, cs.get(fim + 1))) fim++;
                for (int f = fim; f >= i + 2; f--) {
                    if (cs.get(f).cor == c.cor) continue;
                    int n = f - i + 1;
                    int[] esperado = Gradiente.cores(n, c.cor, cs.get(f).cor);
                    boolean ok = true;
                    for (int k = 0; k < n && ok; k++) ok = cs.get(i + k).cor == esperado[k];
                    if (ok) {
                        melhor = n;
                        break;
                    }
                }
            }
            if (melhor > 0) {
                if (atual.cor != -1 || !mesmoEstilo(atual, c)) {
                    if (atual.cor != -1 || atual.negrito || atual.italico || atual.sublinhado || atual.riscado || atual.embaralhado) sb.append("&r");
                    sb.append(codigoFlags(c));
                    atual = new Seg();
                    atual.negrito = c.negrito;
                    atual.italico = c.italico;
                    atual.sublinhado = c.sublinhado;
                    atual.riscado = c.riscado;
                    atual.embaralhado = c.embaralhado;
                }
                sb.append('<').append(Gradiente.paraHex(c.cor)).append('-').append(Gradiente.paraHex(cs.get(i + melhor - 1).cor)).append('>');
                for (int k = 0; k < melhor; k++) sb.append(cs.get(i + k).texto);
                sb.append("</>");
                i += melhor;
                continue;
            }
            if (c.cor != atual.cor || !mesmoEstilo(atual, c)) {
                boolean tinha = atual.cor != -1 || atual.negrito || atual.italico || atual.sublinhado || atual.riscado || atual.embaralhado;
                if (c.cor >= 0) sb.append(codigoCor(c.cor));
                else if (tinha) sb.append("&r");
                sb.append(codigoFlags(c));
                atual = new Seg();
                atual.cor = c.cor;
                atual.negrito = c.negrito;
                atual.italico = c.italico;
                atual.sublinhado = c.sublinhado;
                atual.riscado = c.riscado;
                atual.embaralhado = c.embaralhado;
            }
            sb.append(c.texto);
            i++;
        }
        return sb.toString();
    }

    /** O texto sem códigos (o que aparece na placa). */
    public static String semCodigos(String bruto) {
        StringBuilder sb = new StringBuilder();
        for (Seg s : ler(bruto)) sb.append(s.texto);
        return sb.toString();
    }
}
