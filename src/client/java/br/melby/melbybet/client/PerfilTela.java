package br.melby.melbybet.client;

import br.melby.melbybet.core.Organizar;
import br.melby.melbybet.core.Organizar.Categoria;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Onde você gosta de cada tipo de item. Escolha o tipo em cima e clique nos slots onde ele deve ficar (mochila em cima,
 * hotbar embaixo). O botão "Organizar" do inventário e dos baús usa este perfil.
 */
public class PerfilTela extends Screen {
    private static final int CEL = 20;
    private Categoria selecionada = Categoria.ESPADA;
    private int gx, gy;
    private Button rotulo;

    public PerfilTela() {
        super(Component.literal("Perfil do inventário"));
    }

    private Organizar.Perfil perfil() {
        return InventarioCliente.perfil;
    }

    @Override
    protected void init() {
        gx = (this.width - 9 * CEL) / 2;
        gy = 62;
        int w = 9 * CEL;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> trocar(-1)).bounds(gx, 34, 20, 20).build());
        rotulo = this.addRenderableWidget(Button.builder(Component.literal(selecionada.rotulo), b -> trocar(1)).bounds(gx + 22, 34, w - 44, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> trocar(1)).bounds(gx + w - 20, 34, 20, 20).build());
        int y = gy + 4 * CEL + 18;
        int meio = (w - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Aprender da hotbar"), b -> aprender(0, 9)).bounds(gx, y, meio, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Aprender de tudo"), b -> aprender(0, 36)).bounds(gx + meio + 4, y, meio, 20).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Limpar"), b -> {
            perfil().limpar();
            InventarioCliente.salvar();
        }).bounds(gx, y, meio, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Padrão"), b -> {
            InventarioCliente.perfil = Organizar.Perfil.padrao();
            InventarioCliente.salvar();
        }).bounds(gx + meio + 4, y, meio, 20).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Organizar mexe na hotbar: " + (perfil().organizarHotbar ? "Sim" : "Não")), b -> {
            perfil().organizarHotbar = !perfil().organizarHotbar;
            b.setMessage(Component.literal("Organizar mexe na hotbar: " + (perfil().organizarHotbar ? "Sim" : "Não")));
            InventarioCliente.salvar();
        }).bounds(gx, y, w, 20).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Guardar iguais ignora a hotbar: " + (perfil().guardarIgnoraHotbar ? "Sim" : "Não")), b -> {
            perfil().guardarIgnoraHotbar = !perfil().guardarIgnoraHotbar;
            b.setMessage(Component.literal("Guardar iguais ignora a hotbar: " + (perfil().guardarIgnoraHotbar ? "Sim" : "Não")));
            InventarioCliente.salvar();
        }).bounds(gx, y, w, 20).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Repor bloco ao acabar: " + (perfil().reporBloco ? "Sim" : "Não")), b -> {
            perfil().reporBloco = !perfil().reporBloco;
            b.setMessage(Component.literal("Repor bloco ao acabar: " + (perfil().reporBloco ? "Sim" : "Não")));
            InventarioCliente.salvar();
        }).bounds(gx, y, w, 20).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> this.onClose()).bounds(gx, y, w, 20).build());
    }

    private void trocar(int d) {
        Categoria[] c = Categoria.values();
        int n = 0;
        for (int i = 0; i < c.length; i++) if (c[i] == selecionada) n = i;
        // "Outro" não tem posição fixa: pula
        do {
            n = Math.floorMod(n + d, c.length);
        } while (c[n] == Categoria.OUTRO);
        selecionada = c[n];
        if (rotulo != null) rotulo.setMessage(Component.literal(selecionada.rotulo));
    }

    /** Copia do inventário atual: cada slot com item passa a ser o lugar do tipo daquele item. */
    private void aprender(int de, int ate) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var inv = mc.player.getInventory();
        for (int s = de; s < ate; s++) {
            ItemStack st = inv.getItem(s);
            if (st.isEmpty()) continue;
            Categoria c = InventarioMixinAjuda.categoriaDe(st);
            if (c == Categoria.OUTRO) continue;
            if (perfil().dono(s) != c) perfil().alternar(c, s);
        }
        InventarioCliente.salvar();
    }

    private int slotEm(double mx, double my) {
        // mochila: linhas 0 a 2 (slots 9 a 35); hotbar: linha 3 mais abaixo (slots 0 a 8)
        for (int col = 0; col < 9; col++) {
            for (int lin = 0; lin < 3; lin++) {
                int x = gx + col * CEL, y = gy + lin * CEL;
                if (mx >= x && mx < x + CEL && my >= y && my < y + CEL) return 9 + lin * 9 + col;
            }
            int x = gx + col * CEL, y = gy + 3 * CEL + 6;
            if (mx >= x && mx < x + CEL && my >= y && my < y + CEL) return col;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean dupla) {
        if (super.mouseClicked(event, dupla)) return true;
        int s = slotEm(event.x(), event.y());
        if (s < 0) return false;
        perfil().alternar(selecionada, s);
        InventarioCliente.salvar();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        g.centeredText(this.font, "Escolha o tipo e clique nos slots onde ele deve ficar", this.width / 2, 24, 0xFFAAAAAA);
        for (int s = 0; s < Organizar.SLOTS; s++) {
            int col = s < 9 ? s : (s - 9) % 9;
            int x = gx + col * CEL;
            int y = s < 9 ? gy + 3 * CEL + 6 : gy + ((s - 9) / 9) * CEL;
            Categoria dono = perfil().dono(s);
            g.fill(x, y, x + CEL - 1, y + CEL - 1, dono == null ? 0x66000000 : (dono == selecionada ? 0xAA335533 : 0xAA333355));
            if (dono == selecionada) g.outline(x, y, CEL - 1, CEL - 1, 0xFFFFC928);
            if (dono != null) g.fakeItem(new ItemStack(InventarioCliente.icone(dono)), x + 2, y + 2);
        }
        g.text(this.font, "Mochila", gx, gy - 10, 0xFF888888);
        g.text(this.font, "Hotbar", gx, gy + 3 * CEL - 4 + 0, 0xFF888888);
        int s = slotEm(mouseX, mouseY);
        if (s >= 0) {
            Categoria d = perfil().dono(s);
            g.setTooltipForNextFrame(this.font, Component.literal(d == null ? "Livre (clique para " + selecionada.rotulo + ")" : d.rotulo + " (clique para " + (d == selecionada ? "tirar" : "trocar") + ")"), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
