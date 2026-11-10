package br.melby.melbybet.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menu de gestos. */
public class GestosTela extends Screen {
    private static final String[][] G = {
        {"Girar", "spin"}, {"Acenar", "wave"}, {"Apontar", "point"}, {"Engatinhar", "crawl"}, {"Sentar", "sit"}
    };

    public GestosTela() {
        super(Component.literal("Gestos"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        for (int i = 0; i < G.length; i++) {
            String cmd = G[i][1];
            this.addRenderableWidget(Button.builder(Component.literal(G[i][0]), b -> {
                this.onClose();
                TelasCliente.comando(cmd);
            }).bounds(x, 40 + i * 24, 200, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> this.onClose()).bounds(x, 40 + G.length * 24 + 6, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        super.extractRenderState(g, mx, my, d);
        g.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }
}
