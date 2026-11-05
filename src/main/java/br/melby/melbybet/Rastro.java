package br.melby.melbybet;

import br.melby.melbybet.core.DueloLog;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rastro do prêmio: por um tempo depois do duelo, registra no log o que o vencedor faz com o que ganhou (diamantes
 * ou os itens da aposta): que baú abriu e quanto guardou ou pegou, quanto sumiu do inventário e onde, ender chest.
 * É feito por comparação de contagens, então tem limites (itens dentro de shulker, troca direta etc.).
 */
public final class Rastro {
    private Rastro() {}

    private static final class Seguimento {
        int duelo;
        UUID uuid;
        String nome;
        /** Vazio = diamantes; senão, os itens ganhos (um modelo por tipo). */
        List<ItemStack> alvos = new ArrayList<>();
        int ultInv = -1, ultEc = -1;
        long ate;
    }

    private record Uso(BlockPos pos, String bloco, String dim, long quando) {}

    private static final Map<UUID, Seguimento> SEGUE = new HashMap<>();
    private static final Map<UUID, Uso> USOS = new HashMap<>();
    private static long tick;

    public static void registrar() {
        UseBlockCallback.EVENT.register((p, n, mao, hit) -> {
            if (n.isClientSide() || SEGUE.isEmpty() || !SEGUE.containsKey(p.getUUID())) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            USOS.put(p.getUUID(), new Uso(pos, n.getBlockState(pos).getBlock().getName().getString(), Corpos.dimDe((net.minecraft.server.level.ServerLevel) n), System.currentTimeMillis()));
            return InteractionResult.PASS;
        });
    }

    /** O duelo terminou com vencedor: começa a acompanhar (se o vencedor estiver online, já com a contagem de agora). */
    static void iniciar(DueloLog.Reg r) {
        int min = Jogo.casa.config.rastroMin;
        if (min <= 0 || r.vencedorUuid.isEmpty()) return;
        r.rastroAte = System.currentTimeMillis() + min * 60_000L;
        r.rastro(System.currentTimeMillis(), "Rastro iniciado (" + min + " min). Prêmio: " + (r.itens ? r.resultado : r.premio + " diamante(s)"));
        ServerPlayer p = Jogo.server.getPlayerList().getPlayer(UUID.fromString(r.vencedorUuid));
        if (p != null) comecar(r, p);
    }

    private static void comecar(DueloLog.Reg r, ServerPlayer p) {
        Seguimento s = new Seguimento();
        s.duelo = r.id;
        s.uuid = p.getUUID();
        s.nome = p.getGameProfile().name();
        s.ate = r.rastroAte;
        if (r.itens) for (String j : r.transferidos) {
            ItemStack m = Corpos.deJson(j);
            if (!m.isEmpty()) s.alvos.add(m);
        }
        s.ultInv = contar(p.getInventory(), s);
        s.ultEc = contar(p.getEnderChestInventory(), s);
        SEGUE.put(p.getUUID(), s);
    }

    /** Quando o vencedor volta a entrar no servidor dentro da janela do rastro. */
    public static void aoEntrar(ServerPlayer p) {
        long agora = System.currentTimeMillis();
        for (DueloLog.Reg r : DueloLogs.log.regs) {
            if (r.rastroAte > agora && r.vencedorUuid.equals(p.getUUID().toString()) && r.estado.equals("RESOLVIDO")) {
                comecar(r, p);
                r.rastro(agora, "Voltou ao jogo (rastro continua)");
                DueloLogs.marcarSujo();
            }
        }
    }

    public static void aoSair(ServerPlayer p) {
        Seguimento s = SEGUE.remove(p.getUUID());
        if (s == null) return;
        DueloLog.Reg r = DueloLogs.log.achar(s.duelo);
        if (r != null) {
            r.rastro(System.currentTimeMillis(), "Saiu do jogo com " + contar(p.getInventory(), s) + " no inventário e " + contar(p.getEnderChestInventory(), s) + " no ender chest");
            DueloLogs.marcarSujo();
        }
    }

    public static void aoMorrer(ServerPlayer p) {
        Seguimento s = SEGUE.get(p.getUUID());
        if (s == null) return;
        DueloLog.Reg r = DueloLogs.log.achar(s.duelo);
        if (r != null) {
            r.rastro(System.currentTimeMillis(), "Morreu em " + pos(p) + " com " + contar(p.getInventory(), s) + " do prêmio (vai para o corpo)");
            DueloLogs.marcarSujo();
        }
        s.ultInv = 0;
    }

    private static int contar(Container c, Seguimento s) {
        int t = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack st = c.getItem(i);
            if (st.isEmpty()) continue;
            if (s.alvos.isEmpty()) {
                if (st.is(Items.DIAMOND)) t += st.getCount();
            } else {
                for (ItemStack m : s.alvos) {
                    if (Duelos.mesmoItem(m, st)) {
                        t += st.getCount();
                        break;
                    }
                }
            }
        }
        return t;
    }

    private static String pos(ServerPlayer p) {
        return (int) p.getX() + " " + (int) p.getY() + " " + (int) p.getZ() + " (" + Corpos.dimDe((net.minecraft.server.level.ServerLevel) p.level()) + ")";
    }

    private static String oQue(Seguimento s) {
        return s.alvos.isEmpty() ? "diamante(s)" : "item(ns) do prêmio";
    }

    public static void aCadaTick() {
        tick++;
        if (SEGUE.isEmpty() || tick % 10 != 0 || Jogo.server == null) return;
        long agora = System.currentTimeMillis();
        for (Seguimento s : new ArrayList<>(SEGUE.values())) {
            DueloLog.Reg r = DueloLogs.log.achar(s.duelo);
            ServerPlayer p = Jogo.server.getPlayerList().getPlayer(s.uuid);
            if (r == null || p == null || r.estado.equals("ANULADO")) {
                SEGUE.remove(s.uuid);
                continue;
            }
            if (agora > s.ate) {
                r.rastro(agora, "Rastro encerrado. Agora: " + contar(p.getInventory(), s) + " no inventário, " + contar(p.getEnderChestInventory(), s) + " no ender chest");
                r.rastroAte = 0;
                SEGUE.remove(s.uuid);
                DueloLogs.marcarSujo();
                continue;
            }
            int inv = contar(p.getInventory(), s), ec = contar(p.getEnderChestInventory(), s);
            int dInv = inv - s.ultInv, dEc = ec - s.ultEc;
            if (dInv == 0 && dEc == 0) continue;
            String quem = s.nome;
            AbstractContainerMenu menu = p.containerMenu;
            boolean aberto = menu != p.inventoryMenu;
            Uso uso = USOS.get(s.uuid);
            boolean recente = uso != null && agora - uso.quando() < 20_000L;
            if (dEc != 0 && dInv == -dEc) {
                r.rastro(agora, quem + (dEc > 0 ? " guardou " : " tirou ") + Math.abs(dEc) + " " + oQue(s) + (dEc > 0 ? " no" : " do") + " ender chest");
            } else if (dEc != 0) {
                r.rastro(agora, quem + ": ender chest " + (dEc > 0 ? "+" : "") + dEc + " " + oQue(s));
            }
            if (dInv != 0 && !(dEc != 0 && dInv == -dEc)) {
                if (aberto && recente && menu.getSlot(0).container != p.getEnderChestInventory()) {
                    r.rastro(agora, quem + (dInv < 0 ? " guardou " : " pegou ") + Math.abs(dInv) + " " + oQue(s) + (dInv < 0 ? " em " : " de ")
                        + uso.bloco() + " em " + uso.pos().getX() + " " + uso.pos().getY() + " " + uso.pos().getZ() + " (" + uso.dim() + ")");
                } else if (dInv < 0) {
                    r.rastro(agora, quem + " perdeu " + (-dInv) + " " + oQue(s) + " do inventário (largou no chão, deu a alguém ou usou) perto de " + pos(p));
                } else {
                    r.rastro(agora, quem + " ganhou " + dInv + " " + oQue(s) + " no inventário (chão ou outro jogador) perto de " + pos(p));
                }
            }
            s.ultInv = inv;
            s.ultEc = ec;
            DueloLogs.marcarSujo();
        }
    }
}
