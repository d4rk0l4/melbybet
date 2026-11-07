package br.melby.melbybet.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

/** Desenha a grade 9x3 de uma shulker box no tooltip, sem precisar abrir. */
public class ShulkerTooltipCliente implements ClientTooltipComponent {
    private static final int CEL = 18;
    private final ShulkerTooltip dados;

    public ShulkerTooltipCliente(ShulkerTooltip dados) {
        this.dados = dados;
    }

    @Override
    public int getHeight(Font font) {
        return 3 * CEL + 4;
    }

    @Override
    public int getWidth(Font font) {
        return 9 * CEL;
    }

    @Override
    public void extractImage(Font font, int x, int y, int largura, int altura, GuiGraphicsExtractor g) {
        for (int i = 0; i < 27; i++) {
            int cx = x + (i % 9) * CEL, cy = y + (i / 9) * CEL;
            g.fill(cx, cy, cx + CEL - 1, cy + CEL - 1, 0x55000000);
            if (i >= dados.itens().size()) continue;
            ItemStack st = dados.itens().get(i);
            if (st.isEmpty()) continue;
            g.item(st, cx + 1, cy + 1);
            g.itemDecorations(font, st, cx + 1, cy + 1);
        }
    }
}
