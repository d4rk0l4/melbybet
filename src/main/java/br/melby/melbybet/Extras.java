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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

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

/** Freeze e os atalhos de chat: @inv, @ec e @item. */
public final class Extras {
    private Extras() {}

    // ---------- freeze ----------

    private static final Set<UUID> CONGELADOS = new HashSet<>();

    public static boolean alternarFreeze(ServerPlayer alvo) {
        UUID id = alvo.getUUID();
        if (CONGELADOS.remove(id)) {
            alvo.removeEffect(MobEffects.SLOWNESS);
            alvo.removeEffect(MobEffects.JUMP_BOOST);
            alvo.sendSystemMessage(Jogo.prefixo("Você foi descongelado."));
            return false;
        }
        CONGELADOS.add(id);
        manterCongelado(alvo);
        alvo.sendSystemMessage(Jogo.prefixo("Um admin congelou você. Não saia do jogo."));
        return true;
    }

    private static void manterCongelado(ServerPlayer p) {
        // Efeitos curtos e renovados: se o servidor reiniciar, o congelamento acaba sozinho.
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 10, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 128, false, false));
        p.sendSystemMessage(Component.literal("Você está congelado por um admin.").withStyle(ChatFormatting.AQUA), true);
    }

    // ---------- afk ----------

    private static final String TIME_AFK = "melbyafk";

    private static final class EstadoAfk {
        double x, y, z;
        float giroY, giroX;
        long parouDesde;
        boolean afk;
        String timeAnterior;
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

    private static void marcarAfk(ServerPlayer p, EstadoAfk e) {
        Scoreboard sb = Jogo.server.getScoreboard();
        PlayerTeam time = sb.getPlayerTeam(TIME_AFK);
        if (time == null) time = sb.addPlayerTeam(TIME_AFK);
        time.setPlayerPrefix(Component.literal("[AFK] ").withStyle(ChatFormatting.GRAY));
        time.setColor(ChatFormatting.GRAY);
        String nome = p.getScoreboardName();
        PlayerTeam atual = sb.getPlayersTeam(nome);
        e.timeAnterior = (atual != null && atual != time) ? atual.getName() : null;
        sb.addPlayerToTeam(nome, time);
        e.afk = true;
        Jogo.avisar(p.getGameProfile().name() + " ficou AFK.");
    }

    private static void desmarcarAfk(ServerPlayer p, EstadoAfk e) {
        Scoreboard sb = Jogo.server.getScoreboard();
        PlayerTeam time = sb.getPlayerTeam(TIME_AFK);
        String nome = p.getScoreboardName();
        if (time != null) sb.removePlayerFromTeam(nome, time);
        if (e.timeAnterior != null) {
            PlayerTeam antes = sb.getPlayerTeam(e.timeAnterior);
            if (antes != null) sb.addPlayerToTeam(nome, antes);
        }
        e.afk = false;
        e.timeAnterior = null;
        e.parouDesde = System.currentTimeMillis();
        guardarPosicao(p, e);
        Jogo.avisar(p.getGameProfile().name() + " voltou.");
    }

    /** Ao ligar o servidor: tira o time AFK que sobrou salvo no mundo. */
    public static void limparAfk() {
        Scoreboard sb = Jogo.server.getScoreboard();
        PlayerTeam time = sb.getPlayerTeam(TIME_AFK);
        if (time != null) sb.removePlayerTeam(time);
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
        // quem saiu do jogo: tira do time para o prefixo não ficar preso
        Scoreboard sb = Jogo.server.getScoreboard();
        PlayerTeam time = sb.getPlayerTeam(TIME_AFK);
        AFK.entrySet().removeIf(en -> {
            if (online.contains(en.getKey())) return false;
            return true;
        });
        if (time != null) {
            for (String nome : new ArrayList<>(time.getPlayers())) {
                boolean on = false;
                for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) if (p.getScoreboardName().equals(nome)) on = true;
                if (!on) sb.removePlayerFromTeam(nome, time);
            }
        }
    }

    /** Chamado a cada segundo. */
    public static void aCadaSegundo() {
        conferirAfk();
        if (CONGELADOS.isEmpty()) return;
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            if (CONGELADOS.contains(p.getUUID())) manterCongelado(p);
        }
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
