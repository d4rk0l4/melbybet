package br.melby.melbybet.client;

import br.melby.melbybet.core.ContagemBlocos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Lista na tela do que ainda falta de um projeto do /catalogo, descontando o que você tem no inventário. */
public final class MateriaisHud {
    private MateriaisHud() {}

    private static final int LINHAS = 9;

    public static void desenhar(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ConstrucaoCliente.materiaisVisivel || ConstrucaoCliente.projeto.isEmpty() || ConstrucaoCliente.blocos.isEmpty()) return;
        Map<String, Long> tenho = new HashMap<>();
        var inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty()) tenho.merge(BuiltInRegistries.ITEM.getKey(st.getItem()).getPath(), (long) st.getCount(), Long::sum);
        }
        long totalPrecisa = 0, totalTenho = 0;
        int faltando = 0;
        int y = 28;
        int x = 6;
        // soma tudo primeiro, depois desenha os que mais faltam
        long[] falta = new long[ConstrucaoCliente.blocos.size()];
        for (int i = 0; i < falta.length; i++) {
            long precisa = ConstrucaoCliente.qtds.get(i);
            long tem = tenho.getOrDefault(ConstrucaoCliente.itens.get(i), 0L);
            totalPrecisa += precisa;
            totalTenho += Math.min(tem, precisa);
            falta[i] = Math.max(0, precisa - tem);
            if (falta[i] > 0) faltando++;
        }
        String cab = ConstrucaoCliente.projeto + "  " + ContagemBlocos.percentual(totalTenho, totalPrecisa);
        g.text(mc.font, cab, x, y, 0xFFFFC928);
        y += 11;
        if (faltando == 0) {
            g.text(mc.font, "Tudo pronto!", x, y, 0xFF55FF55);
            return;
        }
        int mostrados = 0;
        for (int i = 0; i < falta.length && mostrados < LINHAS; i++) {
            if (falta[i] == 0) continue;
            long precisa = ConstrucaoCliente.qtds.get(i);
            Component nome = Component.translatable("block.minecraft." + ConstrucaoCliente.blocos.get(i));
            g.text(mc.font, Component.empty().append(nome).append(Component.literal("  " + (precisa - falta[i]) + "/" + precisa + "  faltam " + falta[i])), x, y, 0xFFFFFFFF);
            y += 10;
            mostrados++;
        }
        if (faltando > mostrados) g.text(mc.font, "... e mais " + (faltando - mostrados) + " tipos", x, y, 0xFFAAAAAA);
    }
}
