package br.melby.melbybet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Vanish e os atalhos de chat: @inv, @ec e @item. */
public final class Extras {
    private Extras() {}

    // ---------- vanish ----------

    private static final Set<UUID> SUMIDOS = new HashSet<>();

    public static boolean alternarVanish(ServerPlayer p) {
        UUID id = p.getUUID();
        if (SUMIDOS.remove(id)) {
            p.removeEffect(MobEffects.INVISIBILITY);
            mostrar(p);
            return false;
        }
        SUMIDOS.add(id);
        manterSumido(p);
        return true;
    }

    private static void manterSumido(ServerPlayer p) {
        // Dura pouco e é renovado a cada segundo: se o servidor reiniciar, o efeito some sozinho.
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
        var pacote = new ClientboundPlayerInfoRemovePacket(List.of(p.getUUID()));
        for (ServerPlayer o : Jogo.server.getPlayerList().getPlayers()) {
            if (o != p) o.connection.send(pacote);
        }
    }

    private static void mostrar(ServerPlayer p) {
        var pacote = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(p));
        for (ServerPlayer o : Jogo.server.getPlayerList().getPlayers()) {
            if (o != p) o.connection.send(pacote);
        }
    }

    /** Chamado a cada segundo. */
    public static void aCadaSegundo() {
        if (SUMIDOS.isEmpty()) return;
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            if (SUMIDOS.contains(p.getUUID())) manterSumido(p);
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
