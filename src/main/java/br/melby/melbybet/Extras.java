package br.melby.melbybet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** AFK e os atalhos de chat: @inv, @ec e @item. */
public final class Extras {
    private Extras() {}

    // ---------- afk ----------

    private static final class EstadoAfk {
        double x, y, z;
        float giroY, giroX;
        long parouDesde;
        boolean afk;
    }

    private static final Map<UUID, EstadoAfk> AFK = new HashMap<>();

    private static void guardarPosicao(ServerPlayer p, EstadoAfk e) {
        e.x = p.getX();
        e.y = p.getY();
        e.z = p.getZ();
        e.giroY = p.getYRot();
        e.giroX = p.getXRot();
    }

    private static boolean mexeu(ServerPlayer p, EstadoAfk e) {
        double dx = p.getX() - e.x, dy = p.getY() - e.y, dz = p.getZ() - e.z;
        return dx * dx + dy * dy + dz * dz > 0.01
            || Math.abs(p.getYRot() - e.giroY) > 0.5f
            || Math.abs(p.getXRot() - e.giroX) > 0.5f;
    }

    /** /afk: liga ou desliga. Sai sozinho quando a pessoa se mexe. */
    public static boolean alternarAfk(ServerPlayer p) {
        EstadoAfk e = AFK.computeIfAbsent(p.getUUID(), k -> new EstadoAfk());
        if (e.afk) {
            desmarcarAfk(p, e);
            return false;
        }
        guardarPosicao(p, e);
        marcarAfk(p, e);
        return true;
    }

    public static boolean estaAfk(UUID id) {
        EstadoAfk e = AFK.get(id);
        return e != null && e.afk;
    }

    private static void marcarAfk(ServerPlayer p, EstadoAfk e) {
        e.afk = true;
        Prefixos.atualizar(p, Jogo.casa.rankingApostadores());
        Jogo.avisar(p.getGameProfile().name() + " ficou AFK.");
    }

    private static void desmarcarAfk(ServerPlayer p, EstadoAfk e) {
        e.afk = false;
        e.parouDesde = System.currentTimeMillis();
        guardarPosicao(p, e);
        Prefixos.atualizar(p, Jogo.casa.rankingApostadores());
        Jogo.avisar(p.getGameProfile().name() + " voltou.");
    }

    private static void conferirAfk() {
        long agora = System.currentTimeMillis();
        long limite = Jogo.casa.config.afkAutoMinutos * 60_000L;
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            online.add(p.getUUID());
            EstadoAfk e = AFK.get(p.getUUID());
            if (e == null) {
                e = new EstadoAfk();
                e.parouDesde = agora;
                guardarPosicao(p, e);
                AFK.put(p.getUUID(), e);
                continue;
            }
            if (mexeu(p, e)) {
                if (e.afk) desmarcarAfk(p, e);
                e.parouDesde = agora;
                guardarPosicao(p, e);
            } else if (!e.afk && limite > 0 && agora - e.parouDesde >= limite) {
                marcarAfk(p, e);
            }
        }
        AFK.keySet().removeIf(id -> !online.contains(id));
    }

    /** Chamado a cada segundo. */
    public static void aCadaSegundo() {
        conferirAfk();
        Prefixos.atualizarTodos();
        Presenca.aCadaSegundo();
    }

    // ---------- fotos compartilhadas no chat ----------

    private record Foto(String dono, boolean ender, List<ItemStack> itens) {}

    private static final Map<Integer, Foto> FOTOS = new LinkedHashMap<>();
    private static int proximaFoto = 1;
    private static final Pattern ATALHO = Pattern.compile("@(inv|ec|item)\\b", Pattern.CASE_INSENSITIVE);

    /** Troca @inv, @ec e @item no texto do chat por componentes clicáveis. */
    public static Component decorar(ServerPlayer p, Component mensagem) {
        if (p == null) return mensagem;
        String s = mensagem.getString();
        if (s.indexOf('@') < 0) return mensagem;
        Matcher m = ATALHO.matcher(s);
        MutableComponent saida = Component.empty();
        int ultimo = 0;
        boolean mudou = false;
        while (m.find()) {
            Component troca = trocar(p, m.group(1).toLowerCase());
            if (troca == null) continue;
            saida.append(Component.literal(s.substring(ultimo, m.start())));
            saida.append(troca);
            ultimo = m.end();
            mudou = true;
        }
        if (!mudou) return mensagem;
        saida.append(Component.literal(s.substring(ultimo)));
        return saida;
    }

    private static Component trocar(ServerPlayer p, String atalho) {
        String nome = p.getGameProfile().name();
        if (atalho.equals("item")) {
            ItemStack st = p.getMainHandItem();
            if (st.isEmpty()) return null;
            return st.getDisplayName(); // já traz o balão com encantamentos e descrição
        }
        boolean ender = atalho.equals("ec");
        List<ItemStack> itens = new ArrayList<>();
        if (ender) {
            for (int i = 0; i < p.getEnderChestInventory().getContainerSize(); i++) itens.add(p.getEnderChestInventory().getItem(i).copy());
        } else {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) itens.add(p.getInventory().getItem(i).copy());
        }
        int id = proximaFoto++;
        FOTOS.put(id, new Foto(nome, ender, itens));
        while (FOTOS.size() > 60) FOTOS.remove(FOTOS.keySet().iterator().next());
        String rotulo = ender ? "[Ender Chest de " + nome + "]" : "[Inventário de " + nome + "]";
        return Component.literal(rotulo).withStyle(estilo -> estilo
            .withColor(ChatFormatting.AQUA)
            .withClickEvent(new ClickEvent.RunCommand("/mbver " + id))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Clique para ver"))));
    }

    public static boolean verFoto(ServerPlayer quem, int id) {
        Foto f = FOTOS.get(id);
        if (f == null) return false;
        Vista.foto(quem, f.dono(), f.ender(), f.itens());
        return true;
    }
}
