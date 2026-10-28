package br.melby.melbybet.core;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

/** Contas do aviso de reinício. Sem nada do Minecraft aqui. */
public final class Reinicio {
    private Reinicio() {}

    /** Quando faltar isso (em segundos) o servidor avisa. */
    public static final long[] AVISOS_S = {1800, 900, 600, 300, 180, 120, 60, 30, 10, 5, 4, 3, 2, 1};

    private static final Pattern HORA = Pattern.compile("^([01]?\\d|2[0-3]):([0-5]\\d)$");

    public static boolean valido(String hora) {
        return hora != null && HORA.matcher(hora.trim()).matches();
    }

    /** "6:5" não passa; "6:05" vira "06:05". null se for inválido. */
    public static String normalizar(String hora) {
        if (!valido(hora)) return null;
        String[] p = hora.trim().split(":");
        return (p[0].length() == 1 ? "0" + p[0] : p[0]) + ":" + p[1];
    }

    /** Milissegundos até o próximo horário da lista, ou -1 se a lista estiver vazia. */
    public static long msAteProximo(List<String> horarios, ZoneId zona, Instant agora) {
        long melhor = -1;
        ZonedDateTime now = agora.atZone(zona);
        for (String h : horarios) {
            if (!valido(h)) continue;
            String[] p = h.trim().split(":");
            ZonedDateTime alvo = now.with(LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1])));
            if (!alvo.isAfter(now)) alvo = alvo.plusDays(1);
            long d = alvo.toInstant().toEpochMilli() - agora.toEpochMilli();
            if (melhor < 0 || d < melhor) melhor = d;
        }
        return melhor;
    }

    /** O aviso (em segundos) que acabou de ser cruzado entre as duas medidas, ou -1. */
    public static long avisoCruzado(long antesMs, long agoraMs) {
        for (long s : AVISOS_S) {
            long t = s * 1000;
            if (antesMs > t && agoraMs <= t) return s;
        }
        return -1;
    }

    public static String texto(long segundos) {
        if (segundos >= 60) {
            long m = segundos / 60;
            return m + (m == 1 ? " minuto" : " minutos");
        }
        return segundos + (segundos == 1 ? " segundo" : " segundos");
    }
}
