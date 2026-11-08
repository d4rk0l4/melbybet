package br.melby.melbybet.client;

import br.melby.melbybet.ConstrucaoRede;
import br.melby.melbybet.core.Formas;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** Estado das ferramentas de construção do cliente: guia de forma, régua, espelho, lista de materiais e grade do mapa-arte. */
public final class ConstrucaoCliente {
    private ConstrucaoCliente() {}

    // guia de forma
    public static boolean formaAtiva;
    public static Formas.Tipo tipo = Formas.Tipo.CIRCULO;
    public static Formas.Plano plano = Formas.Plano.HORIZONTAL;
    public static int rx = 8, ry = 8, rz = 8;
    public static int[] centro;
    public static boolean camadaDosPes;
    private static String chaveCache = "";
    private static List<int[]> cache = List.of();

    // régua
    public static int[] reguaA, reguaB;

    // espelho
    public static boolean espelhoAtivo, espelhoEixoX = true, espelhoEntreBlocos;
    public static int espelhoPlano;

    // lista de materiais
    public static String projeto = "";
    public static List<String> blocos = List.of(), itens = List.of();
    public static List<Long> qtds = List.of();
    public static boolean materiaisVisivel = true;

    // grade do mapa-arte
    public static ConstrucaoRede.MapArtPayload grade;

    public static List<int[]> casca() {
        String k = tipo + "|" + plano + "|" + rx + "|" + ry + "|" + rz;
        if (!k.equals(chaveCache)) {
            cache = Formas.casca(tipo, plano, rx, ry, rz);
            chaveCache = k;
        }
        return cache;
    }

    public static void limparTudo() {
        formaAtiva = false;
        centro = null;
        reguaA = reguaB = null;
        espelhoAtivo = false;
        projeto = "";
        blocos = itens = List.of();
        qtds = List.of();
        grade = null;
    }

    /** O bloco para onde o jogador olha (a até 64 blocos), ou null. */
    public static int[] blocoOlhado() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        HitResult h = mc.player.pick(64.0, 1.0F, false);
        if (!(h instanceof BlockHitResult bh) || h.getType() != HitResult.Type.BLOCK) return null;
        return new int[] {bh.getBlockPos().getX(), bh.getBlockPos().getY(), bh.getBlockPos().getZ()};
    }

    public static int[] posicaoDoJogador() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        var p = mc.player.blockPosition();
        return new int[] {p.getX(), p.getY(), p.getZ()};
    }
}
