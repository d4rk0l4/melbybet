package br.melby.melbybet.core;

/** Direção e distância entre dois pontos do mundo. No Minecraft, +x é leste e +z é sul. */
public final class Direcao {
    private Direcao() {}

    private static final String[] NOMES = {"norte", "nordeste", "leste", "sudeste", "sul", "sudoeste", "oeste", "noroeste"};
    private static final String[] SETAS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    /** Ângulo em graus, 0 = norte e crescendo no sentido horário. */
    public static double angulo(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        return a < 0 ? a + 360 : a;
    }

    private static int indice(double graus) {
        return (int) Math.round(((graus % 360) + 360) % 360 / 45.0) % 8;
    }

    public static String cardinal(double dx, double dz) {
        return NOMES[indice(angulo(dx, dz))];
    }

    /**
     * Seta relativa a para onde o jogador olha. yaw é o do Minecraft (0 = sul, 90 = oeste, -90 = leste, 180 = norte).
     * ↑ é à frente, → à direita.
     */
    public static String seta(double dx, double dz, double yaw) {
        double olhando = ((yaw + 180) % 360 + 360) % 360; // 0 = norte, crescendo no sentido horário
        return SETAS[indice(angulo(dx, dz) - olhando)];
    }
}
