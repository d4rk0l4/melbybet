package br.melby.melbybet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Mostra onde monstros nascem: vermelho = luz de bloco 0 e sem céu (nasce a qualquer hora); amarelo = sem luz de bloco mas com céu (nasce à noite). */
public final class LuzCliente {
    private LuzCliente() {}

    public static boolean ativo;
    private static final int RAIO = 12, MAX = 1400;
    private record Q(int x, int y, int z, boolean vermelho) {}
    private static List<Q> cache = List.of();
    private static int tick;

    public static void aCadaTick(Minecraft mc) {
        if (!ativo || mc.player == null || mc.level == null) {
            cache = List.of();
            return;
        }
        if (tick++ % 10 != 0) return;
        List<Q> novo = new ArrayList<>();
        BlockPos c = mc.player.blockPosition();
        var nivel = mc.level;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -RAIO; dx <= RAIO && novo.size() < MAX; dx++) {
            for (int dz = -RAIO; dz <= RAIO && novo.size() < MAX; dz++) {
                for (int dy = -4; dy <= 3; dy++) {
                    pos.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockPos baixo = pos.below();
                    if (!nivel.getBlockState(baixo).isFaceSturdy(nivel, baixo, Direction.UP)) continue;
                    var estado = nivel.getBlockState(pos);
                    if (!estado.getCollisionShape(nivel, pos).isEmpty() || !estado.getFluidState().isEmpty()) continue;
                    if (nivel.getBrightness(LightLayer.BLOCK, pos) > 0) continue;
                    boolean ceu = nivel.getBrightness(LightLayer.SKY, pos) > 0;
                    novo.add(new Q(pos.getX(), pos.getY(), pos.getZ(), !ceu));
                    if (novo.size() >= MAX) break;
                }
            }
        }
        cache = novo;
    }

    public static void desenhar() {
        if (!ativo) return;
        for (Q q : cache) {
            int cor = q.vermelho ? 0xFF3030 : 0xFFD020;
            Gizmos.cuboid(new AABB(q.x + 0.1, q.y + 0.01, q.z + 0.1, q.x + 0.9, q.y + 0.04, q.z + 0.9),
                GizmoStyle.strokeAndFill(0xFF000000 | cor, 1.0f, 0x99000000 | cor));
        }
    }
}
