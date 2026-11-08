package br.melby.melbybet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Contador de farm: soma o que entra no seu inventário (sem tela aberta) e mostra itens por minuto. */
public final class FarmHud {
    private FarmHud() {}

    public static boolean ativo;
    private static long inicio;
    private static Map<String, Integer> anterior = new HashMap<>();
    private static final Map<net.minecraft.world.item.Item, Long> ganhos = new HashMap<>();
    private static final Map<String, net.minecraft.world.item.Item> itens = new HashMap<>();
    private static final Map<String, Long> total = new HashMap<>();

    /** Liga (zerando) ou desliga. */
    public static void alternar(Minecraft mc) {
        ativo = !ativo;
        total.clear();
        itens.clear();
        ganhos.clear();
        if (ativo) {
            inicio = System.currentTimeMillis();
            anterior = ler(mc);
        }
    }

    private static Map<String, Integer> ler(Minecraft mc) {
        Map<String, Integer> m = new HashMap<>();
        var inv = mc.player.getInventory();
        for (int i = 0; i < 41; i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
            m.merge(id, st.getCount(), Integer::sum);
            itens.putIfAbsent(id, st.getItem());
        }
        return m;
    }

    public static void aCadaTick(Minecraft mc) {
        if (!ativo || mc.player == null) return;
        // com baú ou inventário aberto, só atualiza a referência (mover itens não é ganho)
        Map<String, Integer> agora = ler(mc);
        if (mc.gui.screen() == null) {
            for (var e : agora.entrySet()) {
                int antes = anterior.getOrDefault(e.getKey(), 0);
                if (e.getValue() > antes) total.merge(e.getKey(), (long) (e.getValue() - antes), Long::sum);
            }
        }
        anterior = agora;
    }

    public static void desenhar(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (!ativo || mc.player == null) return;
        double min = Math.max(1.0 / 60, (System.currentTimeMillis() - inicio) / 60000.0);
        int y = 60;
        int mm = (int) min, ss = (int) ((min - mm) * 60);
        g.text(mc.font, "Farm " + mm + ":" + (ss < 10 ? "0" : "") + ss, 6, y, 0xFFFFC928);
        y += 11;
        List<Map.Entry<String, Long>> l = total.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(6).toList();
        if (l.isEmpty()) g.text(mc.font, "Nada novo ainda", 6, y, 0xFFAAAAAA);
        for (var e : l) {
            var item = itens.get(e.getKey());
            Component nome = item != null ? item.getDefaultInstance().getHoverName() : Component.literal(e.getKey());
            String taxa = String.format(java.util.Locale.ROOT, "%.1f/min", e.getValue() / min);
            g.text(mc.font, Component.literal(e.getValue() + " ").append(nome).append(" (" + taxa + ")"), 6, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
