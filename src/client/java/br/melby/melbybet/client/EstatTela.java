package br.melby.melbybet.client;

import br.melby.melbybet.EstatRede;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Suas estatísticas e o ranking da Gangue, categoria por categoria. */
public class EstatTela extends Screen {
    private static EstatRede.DadosPayload dados;
    private static int selecionada, topo;
    private static final int VISIVEIS = 11, LINHA = 16;
    private Button sobe, desce;

    public EstatTela() {
        super(Component.literal("Estatísticas"));
    }

    public static void abrir() {
        ClientPlayNetworking.send(new EstatRede.PedirPayload());
        Minecraft.getInstance().setScreenAndShow(new EstatTela());
    }

    public static void receber(EstatRede.DadosPayload p) {
        dados = p;
        if (selecionada >= p.cats().size()) selecionada = 0;
    }

    private int esq() {
        return Math.max(6, this.width / 2 - 170);
    }

    @Override
    protected void init() {
        int x = esq(), y = 40 + VISIVEIS * LINHA + 4;
        sobe = this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
            topo = Math.max(0, topo - VISIVEIS);
        }).bounds(x, y, 50, 20).build());
        desce = this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
            int n = dados == null ? 0 : dados.cats().size();
            if (topo + VISIVEIS < n) topo += VISIVEIS;
        }).bounds(x + 54, y, 50, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> this.onClose()).bounds(this.width / 2 + 120, y, 50, 20).build());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean duplo) {
        if (dados != null && e.button() == 0) {
            int x = esq(), y = 40;
            for (int i = 0; i < VISIVEIS; i++) {
                int idx = topo + i;
                if (idx >= dados.cats().size()) break;
                if (e.x() >= x && e.x() < x + 150 && e.y() >= y + i * LINHA && e.y() < y + (i + 1) * LINHA) {
                    selecionada = idx;
                    return true;
                }
            }
        }
        return super.mouseClicked(e, duplo);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        if (dados == null) {
            g.centeredText(this.font, Component.literal("Carregando..."), this.width / 2, 60, 0xFFAAAAAA);
            return;
        }
        String cab = dados.nome() + (dados.naGangueHa().isEmpty() ? "" : "  |  na Gangue há " + dados.naGangueHa()) + (dados.tempoOnline().isEmpty() ? "" : "  |  online " + dados.tempoOnline());
        g.centeredText(this.font, Component.literal(cab), this.width / 2, 22, 0xFFFFC928);
        int x = esq(), y = 40;
        for (int i = 0; i < VISIVEIS; i++) {
            int idx = topo + i;
            if (idx >= dados.cats().size()) break;
            var c = dados.cats().get(idx);
            int ly = y + i * LINHA;
            if (idx == selecionada) g.fill(x - 2, ly - 1, x + 150, ly + LINHA - 1, 0x66FFC928);
            var item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(c.icone()));
            g.item(new ItemStack(item), x, ly - 1);
            g.text(this.font, c.rotulo().replace("Mais ", ""), x + 20, ly + 2, 0xFFFFFFFF);
        }
        var c = dados.cats().get(selecionada);
        int rx = x + 160;
        g.text(this.font, c.rotulo(), rx, 40, 0xFFFFC928);
        g.text(this.font, "Você: " + c.meu() + (c.pos() > 0 ? "  (" + c.pos() + "º de " + c.total() + ")" : "  (sem posição)"), rx, 54, 0xFFFFFFFF);
        for (int i = 0; i < c.nomes().size(); i++) {
            int cor = i == 0 ? 0xFFFFD700 : i == 1 ? 0xFFC0C0C0 : i == 2 ? 0xFFCD7F32 : 0xFFAAAAAA;
            boolean eu = c.nomes().get(i).equalsIgnoreCase(dados.nome());
            g.text(this.font, (i + 1) + "º " + c.nomes().get(i), rx, 74 + i * 12, eu ? 0xFF55FF55 : cor);
            g.text(this.font, c.valores().get(i), rx + 120, 74 + i * 12, eu ? 0xFF55FF55 : cor);
        }
        if (c.nomes().isEmpty()) g.text(this.font, "Ninguém pontuou ainda.", rx, 74, 0xFF888888);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
