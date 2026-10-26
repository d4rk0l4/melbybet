package br.melby.melbybet.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê "Arrumar baús 2 horas", "\"Arrumar baús\" 30min" etc. Sem nada do Minecraft. */
public final class Lembrete {
    private Lembrete() {}

    public static final long MIN_MS = 10_000L;
    public static final long MAX_MS = 365L * 24 * 3600 * 1000;

    public static final class Resultado {
        public final String texto;
        public final long ms;

        Resultado(String texto, long ms) {
            this.texto = texto;
            this.ms = ms;
        }
    }

    private static final Pattern JUNTO = Pattern.compile("^(\\d+)([a-zçãé]+)$");
    private static final Pattern NUM = Pattern.compile("^\\d+$");

    /** Multiplicador da unidade em ms, ou 0 se não for uma unidade. */
    static long unidade(String u) {
        return switch (u.toLowerCase(Locale.ROOT)) {
            case "s", "seg", "segundo", "segundos" -> 1000L;
            case "m", "min", "mins", "minuto", "minutos" -> 60_000L;
            case "h", "hr", "hrs", "hora", "horas" -> 3_600_000L;
            case "d", "dia", "dias" -> 86_400_000L;
            default -> 0L;
        };
    }

    private static String semAspas(String s) {
        s = s.trim();
        if (s.length() >= 2) {
            char a = s.charAt(0);
            char b = s.charAt(s.length() - 1);
            boolean par = (a == '"' && b == '"') || (a == '\u201c' && b == '\u201d');
            if (par) s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    /** Devolve o texto e o tempo, ou null se não achou um tempo no final. */
    public static Resultado ler(String entrada) {
        if (entrada == null) return null;
        String[] t = entrada.trim().split("\\s+");
        if (t.length < 2) return null;
        long n;
        long un;
        int usados;
        Matcher m = JUNTO.matcher(t[t.length - 1]);
        if (m.matches() && unidade(m.group(2)) > 0) {
            n = Long.parseLong(m.group(1));
            un = unidade(m.group(2));
            usados = 1;
        } else if (t.length >= 3 && unidade(t[t.length - 1]) > 0 && NUM.matcher(t[t.length - 2]).matches()) {
            n = Long.parseLong(t[t.length - 2]);
            un = unidade(t[t.length - 1]);
            usados = 2;
        } else {
            return null;
        }
        if (n <= 0 || n > 100_000L) return null;
        long ms = n * un;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length - usados; i++) {
            if (i > 0) sb.append(' ');
            sb.append(t[i]);
        }
        String texto = semAspas(sb.toString());
        if (texto.isEmpty()) return null;
        return new Resultado(texto, ms);
    }
}
