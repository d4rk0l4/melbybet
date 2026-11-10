package br.melby.melbybet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Escolha um jogador online e desafie para um duelo (diamantes ou itens). */
public class DueloTela extends Screen {
    private final List<String> nomes = new ArrayList<>();
    private String escolhido = "";
    private EditBox valor;
    private int pagina;
    private static final int POR = 8;

    public DueloTela() {
        super(Component.literal("Duelo"));
    }

    @Override
    protected void init() {
        nomes.clear();
        Minecraft mc = Minecraft.getInstance();
        String eu = mc.player.getGameProfile().name();
        if (mc.getConnection() != null)
            for (var i : mc.getConnection().getOnlinePlayers()) {
                String n = i.getProfile().name();
                if (!n.equalsIgnoreCase(eu)) nomes.add(n);
            }
        nomes.sort(String.CASE_INSENSITIVE_ORDER);
        int x = this.width / 2 - 100;
        for (int i = 0; i < POR; i++) {
            int idx = pagina * POR + i;
            if (idx >= nomes.size()) break;
            String n = nomes.get(idx);
            this.addRenderableWidget(Button.builder(Component.literal((n.equals(escolhido) ? "> " : "") + n), b -> {
                escolhido = n;
                this.rebuildWidgets();
            }).bounds(x, 30 + i * 22, 200, 20).build());
        }
        int y = 30 + POR * 22 + 4;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (pagina > 0) { pagina--; this.rebuildWidgets(); }
        }).bounds(x, y, 30, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if ((pagina + 1) * POR < nomes.size()) { pagina++; this.rebuildWidgets(); }
        }).bounds(x + 170, y, 30, 20).build());
        valor = new EditBox(this.font, x + 36, y, 128, 20, Component.literal("Diamantes"));
        valor.setValue("1");
        this.addRenderableWidget(valor);
        y += 26;
        this.addRenderableWidget(Button.builder(Component.literal("Desafiar (diamantes)"), b -> {
            if (escolhido.isEmpty()) return;
            String v = valor.getValue().trim();
            if (!v.matches("\\d{1,4}")) return;
            this.onClose();
            TelasCliente.comando("duelo " + escolhido + " " + v);
        }).bounds(x, y, 98, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Desafiar (itens)"), b -> {
            if (escolhido.isEmpty()) return;
            this.onClose();
            TelasCliente.comando("duelo " + escolhido + " inv");
        }).bounds(x + 102, y, 98, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        super.extractRenderState(g, mx, my, d);
        g.centeredText(this.font, Component.literal(nomes.isEmpty() ? "Ninguém online para desafiar" : "Escolha quem desafiar"), this.width / 2, 14, 0xFFFFFFFF);
    }
}
