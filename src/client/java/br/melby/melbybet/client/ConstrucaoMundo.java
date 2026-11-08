package br.melby.melbybet.client;

import br.melby.melbybet.core.Formas;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Desenho no mundo das ferramentas de construção: guia de forma, régua, espelho e grade do mapa-arte. */
public final class ConstrucaoMundo {
    private ConstrucaoMundo() {}

    private static final int COR_FORMA = 0xFFFFC928, COR_REGUA = 0xFF55FFFF, COR_ESPELHO = 0xFFFF55FF, COR_GRADE = 0xFF55FF55;
    private static final int MAX_BLOCOS = 1500;
    private static final double ALCANCE = 48;

    public static void desenhar(String dim) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        forma(mc);
        regua(mc);
        espelho(mc);
        grade(mc, dim);
    }

    private static void forma(Minecraft mc) {
        if (!ConstrucaoCliente.formaAtiva || ConstrucaoCliente.centro == null) return;
        int[] c = ConstrucaoCliente.centro;
        int pesY = (int) Math.floor(mc.player.getY());
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        List<int[]> lista = new ArrayList<>();
        for (int[] r : ConstrucaoCliente.casca()) {
            int x = c[0] + r[0], y = c[1] + r[1], z = c[2] + r[2];
            if (ConstrucaoCliente.camadaDosPes && y != pesY) continue;
            double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
            if (dx * dx + dy * dy + dz * dz > ALCANCE * ALCANCE) continue;
            lista.add(new int[] {x, y, z});
        }
        if (lista.size() > MAX_BLOCOS) lista = lista.subList(0, MAX_BLOCOS);
        for (int[] b : lista) Gizmos.cuboid(new BlockPos(b[0], b[1], b[2]), GizmoStyle.stroke(COR_FORMA, 1.5f)).setAlwaysOnTop();
        Gizmos.cuboid(new BlockPos(c[0], c[1], c[2]), GizmoStyle.stroke(0xFFFFFFFF, 3.0f)).setAlwaysOnTop();
    }

    private static void regua(Minecraft mc) {
        int[] a = ConstrucaoCliente.reguaA, b = ConstrucaoCliente.reguaB;
        if (a != null) Gizmos.cuboid(new BlockPos(a[0], a[1], a[2]), GizmoStyle.stroke(COR_REGUA, 3.0f)).setAlwaysOnTop();
        if (b != null) Gizmos.cuboid(new BlockPos(b[0], b[1], b[2]), GizmoStyle.stroke(COR_REGUA, 3.0f)).setAlwaysOnTop();
        if (a == null || b == null) return;
        Gizmos.line(new Vec3(a[0] + 0.5, a[1] + 0.5, a[2] + 0.5), new Vec3(b[0] + 0.5, b[1] + 0.5, b[2] + 0.5), COR_REGUA, 3.0f).setAlwaysOnTop();
        double[] m = Formas.medir(a, b);
        BlockPos meio = new BlockPos((a[0] + b[0]) / 2, Math.max(a[1], b[1]) + 1, (a[2] + b[2]) / 2);
        Gizmos.billboardTextOverBlock((int) m[0] + " x " + (int) m[1] + " x " + (int) m[2] + " blocos", meio, 1, 0xFFFFFFFF, 1.0f).setAlwaysOnTop();
        Gizmos.billboardTextOverBlock(String.format(java.util.Locale.ROOT, "reta %.1f", m[3]), meio, 0, COR_REGUA, 1.0f).setAlwaysOnTop();
    }

    private static void espelho(Minecraft mc) {
        if (!ConstrucaoCliente.espelhoAtivo) return;
        double pl = ConstrucaoCliente.espelhoPlano + (ConstrucaoCliente.espelhoEntreBlocos ? 1.0 : 0.5);
        double cx = mc.player.getX(), cz = mc.player.getZ(), y0 = mc.player.getY() - 6, y1 = mc.player.getY() + 10;
        int n = 8;
        for (int i = -n; i <= n; i++) {
            double t = (ConstrucaoCliente.espelhoEixoX ? Math.floor(cz) : Math.floor(cx)) + i * 2;
            if (ConstrucaoCliente.espelhoEixoX) {
                Gizmos.line(new Vec3(pl, y0, t), new Vec3(pl, y1, t), COR_ESPELHO, 1.5f);
            } else {
                Gizmos.line(new Vec3(t, y0, pl), new Vec3(t, y1, pl), COR_ESPELHO, 1.5f);
            }
        }
        for (double y = Math.floor(y0); y <= y1; y += 2) {
            if (ConstrucaoCliente.espelhoEixoX) {
                Gizmos.line(new Vec3(pl, y, Math.floor(cz) - 2 * n), new Vec3(pl, y, Math.floor(cz) + 2 * n), COR_ESPELHO, 1.5f);
            } else {
                Gizmos.line(new Vec3(Math.floor(cx) - 2 * n, y, pl), new Vec3(Math.floor(cx) + 2 * n, y, pl), COR_ESPELHO, 1.5f);
            }
        }
        int[] olhado = ConstrucaoCliente.blocoOlhado();
        if (olhado != null) {
            int[] e = Formas.espelhar(olhado, ConstrucaoCliente.espelhoEixoX, ConstrucaoCliente.espelhoPlano, ConstrucaoCliente.espelhoEntreBlocos);
            Gizmos.cuboid(new BlockPos(e[0], e[1], e[2]), GizmoStyle.stroke(COR_ESPELHO, 3.0f)).setAlwaysOnTop();
        }
    }

    private static void grade(Minecraft mc, String dim) {
        var g = ConstrucaoCliente.grade;
        if (g == null || !g.ativo() || !g.dim().equals(dim)) return;
        int lado = 128 << g.escala();
        double y = g.y() + 0.05;
        for (int c = 0; c < g.cols(); c++) {
            for (int l = 0; l < g.linhas(); l++) {
                double x0 = g.x0() + (double) c * lado, z0 = g.z0() + (double) l * lado, x1 = x0 + lado, z1 = z0 + lado;
                Gizmos.line(new Vec3(x0, y, z0), new Vec3(x1, y, z0), COR_GRADE, 3.0f).setAlwaysOnTop();
                Gizmos.line(new Vec3(x1, y, z0), new Vec3(x1, y, z1), COR_GRADE, 3.0f).setAlwaysOnTop();
                Gizmos.line(new Vec3(x1, y, z1), new Vec3(x0, y, z1), COR_GRADE, 3.0f).setAlwaysOnTop();
                Gizmos.line(new Vec3(x0, y, z1), new Vec3(x0, y, z0), COR_GRADE, 3.0f).setAlwaysOnTop();
                if (g.cols() * g.linhas() <= 100) {
                    BlockPos centro = new BlockPos((int) (x0 + lado / 2.0), g.y() + 1, (int) (z0 + lado / 2.0));
                    Gizmos.billboardTextOverBlock("Mapa " + (l * g.cols() + c + 1), centro, 0, 0xFFFFFFFF, 3.0f).setAlwaysOnTop();
                }
            }
        }
    }
}
