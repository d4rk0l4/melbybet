package br.melby.melbybet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Cor do nick no chat: duas cores (#RRGGBB), prévia ao vivo do degradê. */
public class CorTela extends Screen {
    private EditBox c1, c2;
    private static final String[] PRE = {"#FF5555", "#FFAA00", "#FFFF55", "#55FF55", "#55FFFF", "#5555FF", "#FF55FF", "#FFFFFF"};

    public CorTela() {
        super(Component.literal("Cor do nick"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        c1 = new EditBox(this.font, x, 60, 98, 20, Component.literal("Cor 1"));
        c1.setValue("#FF5555");
        c2 = new EditBox(this.font, x + 102, 60, 98, 20, Component.literal("Cor 2"));
        c2.setValue("#5555FF");
        this.addRenderableWidget(c1);
        this.addRenderableWidget(c2);
        for (int i = 0; i < PRE.length; i++) {
            String h = PRE[i];
            this.addRenderableWidget(Button.builder(Component.literal(" "), b -> c1.setValue(h)).bounds(x + i * 25, 86, 23, 12).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Aplicar"), b -> {
            if (cor(c1.getValue()) < 0 || cor(c2.getValue()) < 0) return;
            this.onClose();
            TelasCliente.comando("cor " + c1.getValue().trim() + " " + c2.getValue().trim());
        }).bounds(x, 150, 98, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Remover cor"), b -> {
            this.onClose();
            TelasCliente.comando("cor off");
        }).bounds(x + 102, 150, 98, 20).build());
    }

    private static int cor(String s) {
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (!s.matches("[0-9a-fA-F]{6}")) return -1;
        return Integer.parseInt(s, 16);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        super.extractRenderState(g, mx, my, d);
        g.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        g.text(this.font, "Cor 1 (clique nos quadrados) / Cor 2", this.width / 2 - 100, 48, 0xFFAAAAAA);
        int a = cor(c1.getValue()), b = cor(c2.getValue());
        String nome = Minecraft.getInstance().player.getGameProfile().name();
        if (a < 0 || b < 0) {
            g.centeredText(this.font, Component.literal("Use o formato #RRGGBB"), this.width / 2, 120, 0xFFFF5555);
            return;
        }
        int total = this.font.width(nome);
        int x = this.width / 2 - total / 2;
        int n = nome.length();
        for (int i = 0; i < n; i++) {
            float t = n <= 1 ? 0 : i / (float) (n - 1);
            int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
            int gr = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
            int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
            String ch = String.valueOf(nome.charAt(i));
            g.text(this.font, ch, x, 120, 0xFF000000 | (r << 16) | (gr << 8) | bl);
            x += this.font.width(ch);
        }
    }
}
