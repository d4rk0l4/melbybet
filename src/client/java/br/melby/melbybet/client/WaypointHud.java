package br.melby.melbybet.client;

import br.melby.melbybet.core.Waypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Bússola no topo da tela: cada waypoint vira um quadradinho colorido na direção dele; os que estão atrás ficam nas pontas. */
public final class WaypointHud {
    private WaypointHud() {}

    public static void desenhar(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !WaypointStore.ativo()) return;
        Waypoints w = WaypointStore.dados;
        if (!w.bussola || !w.tudoVisivel || w.pontos.isEmpty()) return;
        String dim = WaypointStore.dimAtual(mc);
        int largTela = g.guiWidth();
        int larg = Math.min(240, largTela - 40);
        int x0 = (largTela - larg) / 2;
        int y = 4;
        g.fill(x0, y, x0 + larg, y + 12, 0x66000000);
        g.fill(largTela / 2, y, largTela / 2 + 1, y + 12, 0xAAFFFFFF);
        double yaw = mc.player.getYRot();
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        Waypoints.Ponto central = null;
        double menorDif = 15;
        for (Waypoints.Ponto p : w.pontos) {
            if (!p.visivel || !p.dim.equals(dim)) continue;
            double dif = Waypoints.diferenca(Waypoints.rumo(p, px, pz), yaw);
            double f = Waypoints.fracaoBussola(dif, 90);
            int alfa = 0xFF;
            if (Double.isNaN(f)) {
                f = dif > 0 ? -1 : 1;
                alfa = 0x88;
            } else if (Math.abs(dif) < menorDif) {
                menorDif = Math.abs(dif);
                central = p;
            }
            int cx = largTela / 2 + (int) Math.round(f * (larg / 2.0 - 5));
            g.fill(cx - 4, y + 1, cx + 5, y + 11, (alfa << 24) | p.cor);
            g.text(mc.font, p.letra(), cx - 2, y + 2, 0xFF000000);
        }
        if (central != null) {
            String t = central.nome + "  " + Waypoints.formatarDistancia(Waypoints.distancia(central, px, py, pz));
            g.centeredText(mc.font, t, largTela / 2, y + 16, 0xFF000000 | central.cor);
        }
    }
}
