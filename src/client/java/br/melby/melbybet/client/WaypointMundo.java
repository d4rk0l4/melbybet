package br.melby.melbybet.client;

import br.melby.melbybet.WaypointRede;
import br.melby.melbybet.core.Waypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Desenho no mundo (por "gizmos" do jogo, visíveis através das paredes): marcadores de waypoint e o contorno do /marcador. */
public final class WaypointMundo {
    private WaypointMundo() {}

    private static final int COR_AREA = 0xFF55FFFF, COR_P1 = 0xFF55FF55, COR_P2 = 0xFFFF5555;
    /** Escala do texto flutuante (nome e distância). */
    private static final float ESCALA_TEXTO = 1.0f;

    public static void desenhar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        String dim = WaypointStore.dimAtual(mc);
        desenharArea(dim);
        if (!WaypointStore.ativo() || !WaypointStore.dados.tudoVisivel) return;
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        boolean feixe = WaypointStore.dados.feixe;
        for (Waypoints.Ponto p : WaypointStore.dados.pontos) {
            if (!p.visivel || !p.dim.equals(dim)) continue;
            int rgb = p.cor & 0xFFFFFF;
            double cx = p.x + 0.5, cz = p.z + 0.5;
            // losango (cubo pequeno) no ponto
            Gizmos.cuboid(new AABB(cx - 0.25, p.y + 0.25, cz - 0.25, cx + 0.25, p.y + 0.75, cz + 0.25),
                GizmoStyle.strokeAndFill(0xFF000000 | rgb, 2.0f, 0x88000000 | rgb)).setAlwaysOnTop();
            if (feixe) {
                Gizmos.line(new Vec3(cx, p.y, cz), new Vec3(cx, p.y + 320, cz), 0xCC000000 | rgb, 4.0f);
            }
            double d = Waypoints.distancia(p, px, py, pz);
            BlockPos acima = new BlockPos(p.x, p.y + 1, p.z);
            Gizmos.billboardTextOverBlock(p.nome, acima, 1, 0xFF000000 | rgb, ESCALA_TEXTO).setAlwaysOnTop();
            Gizmos.billboardTextOverBlock(Waypoints.formatarDistancia(d), acima, 0, 0xFFFFFFFF, ESCALA_TEXTO).setAlwaysOnTop();
        }
    }

    private static void desenharArea(String dim) {
        WaypointRede.AreaPayload a = WaypointStore.area;
        if (a == null || !a.dim().equals(dim) || (!a.tem1() && !a.tem2())) return;
        if (a.tem1() && a.tem2()) {
            AABB caixa = new AABB(Math.min(a.ax(), a.bx()), Math.min(a.ay(), a.by()), Math.min(a.az(), a.bz()),
                Math.max(a.ax(), a.bx()) + 1, Math.max(a.ay(), a.by()) + 1, Math.max(a.az(), a.bz()) + 1);
            Gizmos.cuboid(caixa, GizmoStyle.stroke(COR_AREA, 2.0f)).setAlwaysOnTop();
        }
        if (a.tem1()) Gizmos.cuboid(new BlockPos(a.ax(), a.ay(), a.az()), GizmoStyle.stroke(COR_P1, 3.0f)).setAlwaysOnTop();
        if (a.tem2()) Gizmos.cuboid(new BlockPos(a.bx(), a.by(), a.bz()), GizmoStyle.stroke(COR_P2, 3.0f)).setAlwaysOnTop();
    }
}
