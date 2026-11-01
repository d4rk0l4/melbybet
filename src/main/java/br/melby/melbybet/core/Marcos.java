package br.melby.melbybet.core;

/** Marcos de horas online que o servidor comemora. */
public final class Marcos {
    private Marcos() {}

    public static final int[] HORAS = {5, 10, 25, 50, 100, 150, 200, 250, 300, 400, 500, 600, 750, 1000, 1500, 2000};

    /** O maior marco já alcançado com essas horas, ou 0. */
    public static int maiorAte(double horas) {
        int r = 0;
        for (int h : HORAS) if (horas >= h) r = h;
        return r;
    }
}
