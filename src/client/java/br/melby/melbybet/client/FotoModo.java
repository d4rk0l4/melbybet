package br.melby.melbybet.client;

import net.minecraft.client.Minecraft;

/** Modo foto: zoom segurando uma tecla (o F1 do jogo já esconde o HUD). */
public final class FotoModo {
    private FotoModo() {}

    private static boolean zoomAtivo;
    private static int fovOriginal = -1;

    public static void zoom(Minecraft mc, boolean segurando) {
        if (segurando && !zoomAtivo) {
            fovOriginal = mc.options.fov().get();
            mc.options.fov().set(30);
            zoomAtivo = true;
        } else if (!segurando && zoomAtivo) {
            mc.options.fov().set(fovOriginal > 0 ? fovOriginal : 70);
            zoomAtivo = false;
        }
    }
}
