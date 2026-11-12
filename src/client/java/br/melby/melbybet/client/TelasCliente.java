package br.melby.melbybet.client;

import br.melby.melbybet.TelasRede;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Recebe do servidor: telas para abrir, avisos no canto e o painel do HUD. */
public final class TelasCliente {
    private TelasCliente() {}

    public static List<String> painel = new ArrayList<>();
    private record Aviso(String titulo, String texto, long ate) {}
    private static final List<Aviso> avisos = new ArrayList<>();

    public static void registrar() {
        ClientPlayNetworking.registerGlobalReceiver(TelasRede.AbrirPayload.TYPE, (p, ctx) -> {
            // as telas próprias foram removidas; o servidor não abre mais nenhuma
        });
        ClientPlayNetworking.registerGlobalReceiver(TelasRede.AvisoPayload.TYPE, (p, ctx) -> {
            synchronized (avisos) {
                avisos.add(new Aviso(p.titulo(), p.texto(), System.currentTimeMillis() + 6000));
                while (avisos.size() > 4) avisos.remove(0);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(TelasRede.PainelPayload.TYPE, (p, ctx) -> painel = new ArrayList<>(p.linhas()));
    }

    public static void comando(String c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(c);
    }

    /** Painel (canto superior direito) e avisos (canto inferior direito). */
    public static void desenhar(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        if (!painel.isEmpty() && mc.gui.screen() == null) {
            int largura = 0;
            for (String s : painel) largura = Math.max(largura, mc.font.width(s));
            int x = w - largura - 10, y = 6;
            g.fill(x - 4, y - 3, w - 3, y + painel.size() * 11 + 2, 0x88000000);
            for (String s : painel) {
                g.text(mc.font, s, x, y, 0xFFFFFFFF);
                y += 11;
            }
        }
        long agora = System.currentTimeMillis();
        synchronized (avisos) {
            avisos.removeIf(a -> a.ate < agora);
            int y = h - 50;
            for (int i = avisos.size() - 1; i >= 0; i--) {
                Aviso a = avisos.get(i);
                int lar = Math.max(mc.font.width(a.titulo), mc.font.width(a.texto)) + 12;
                int x = w - lar - 6;
                g.fill(x, y, x + lar, y + 28, 0xCC202020);
                g.fill(x, y, x + 2, y + 28, 0xFFFFC928);
                g.text(mc.font, a.titulo, x + 6, y + 4, 0xFFFFC928);
                g.text(mc.font, a.texto, x + 6, y + 16, 0xFFFFFFFF);
                y -= 32;
            }
        }
    }
}
