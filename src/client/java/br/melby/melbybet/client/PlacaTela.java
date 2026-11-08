package br.melby.melbybet.client;

import br.melby.melbybet.ConstrucaoRede;
import br.melby.melbybet.Placas;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Editor de placa: 4 linhas com os códigos de cor, prévia ao vivo e histórico de versões. */
public class PlacaTela extends Screen {
    private final ConstrucaoRede.PlacaDadosPayload dados;
    private final EditBox[] caixas = new EditBox[4];

    public PlacaTela(ConstrucaoRede.PlacaDadosPayload dados) {
        super(Component.literal("Editar placa"));
        this.dados = dados;
    }

    /** Pede ao servidor os dados da placa que o jogador está olhando. */
    public static void pedir() {
        int[] b = ConstrucaoCliente.blocoOlhado();
        if (b == null) {
            WaypointStore.aviso("Olhe para uma placa (a até 8 blocos).");
            return;
        }
        ClientPlayNetworking.send(new ConstrucaoRede.PlacaPedirPayload(b[0], b[1], b[2]));
    }

    /** Chegou a resposta do servidor: abre (ou atualiza) a tela. */
    public static void receber(ConstrucaoRede.PlacaDadosPayload d) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreenAndShow(new PlacaTela(d));
    }

    @Override
    protected void init() {
        int w = Math.min(260, this.width - 20);
        int x = this.width / 2 - w / 2;
        int y = 28;
        for (int i = 0; i < 4; i++) {
            EditBox c = new EditBox(this.font, x, y + i * 22, w, 18, Component.literal("Linha " + (i + 1)));
            c.setMaxLength(120);
            c.setValue(i < dados.linhas().size() ? dados.linhas().get(i) : "");
            c.setHint(Component.literal("Linha " + (i + 1)));
            caixas[i] = this.addRenderableWidget(c);
        }
        int by = y + 4 * 22 + 62;
        this.addRenderableWidget(Button.builder(Component.literal("Aplicar"), b -> {
            List<String> l = new ArrayList<>();
            for (EditBox c : caixas) l.add(c.getValue());
            ClientPlayNetworking.send(new ConstrucaoRede.PlacaAplicarPayload(dados.x(), dados.y(), dados.z(), l));
            this.onClose();
        }).bounds(x, by, w / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> this.onClose()).bounds(x + w / 2 + 2, by, w / 2 - 2, 20).build());
        // histórico
        int hy = by + 28;
        int n = Math.min(dados.versoes().size(), Math.max(0, (this.height - hy - 8) / 22));
        for (int i = 0; i < n; i++) {
            var v = dados.versoes().get(i);
            this.addRenderableWidget(Button.builder(Component.literal("Restaurar: " + v.rotulo()), b -> {
                ClientPlayNetworking.send(new ConstrucaoRede.PlacaRestaurarPayload(dados.x(), dados.y(), dados.z(), v.id()));
                this.onClose();
            }).bounds(x, hy + i * 22, w, 20).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        int w = Math.min(260, this.width - 20);
        int x = this.width / 2 - w / 2;
        int py = 28 + 4 * 22 + 2;
        g.fill(x, py, x + w, py + 52, 0xFF3B2A14);
        for (int i = 0; i < 4; i++) {
            g.centeredText(this.font, Placas.componente(caixas[i].getValue()), this.width / 2, py + 3 + i * 12, 0xFFFFFFFF);
        }
        g.text(this.font, "Códigos: &c cor, &l negrito, &#ff8800, <#ff0000-#0000ff>texto</>", x, py + 56, 0xFF888888);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
