package br.melby.melbybet.core;

import java.util.ArrayList;
import java.util.List;

/** Contas do aviso da limpeza de itens do chão. Sem nada do Minecraft aqui. */
public final class Limpeza {
    private Limpeza() {}

    private static final long[] FINAIS = {10, 5, 3, 2, 1};

    /** Em quais segundos restantes avisar: o primeiro escolhido e a contagem final. Do maior para o menor. */
    public static List<Long> limiares(int primeiroSeg) {
        List<Long> r = new ArrayList<>();
        if (primeiroSeg > 0) r.add((long) primeiroSeg);
        for (long f : FINAIS) if (f < primeiroSeg) r.add(f);
        return r;
    }

    /** O aviso (em segundos) que acabou de ser cruzado entre as duas medidas, ou -1. */
    public static long cruzado(List<Long> limiares, long antesMs, long agoraMs) {
        for (long s : limiares) {
            long t = s * 1000;
            if (antesMs > t && agoraMs <= t) return s;
        }
        return -1;
    }
}
