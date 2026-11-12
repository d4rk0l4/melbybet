package br.melby.melbybet.client;

import br.melby.melbybet.TelasRede;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Casa de apostas: escolha o bolão e a opção, digite o valor e aposte. */
public class BetTela extends Screen {
    private static TelasRede.BoloesPayload dados;
    private int bolao;
    private EditBox valor;
    private int ultimaVersao;

    public BetTela() {
        super(Component.literal("Casa de apostas"));
    }

    public static void receber(TelasRede.BoloesPayload p) {
        dados = p;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.gui.screen() instanceof BetTela t) t.rebuildWidgets();
    }

    @Override
    protected void init() {
        ClientPlayNetworking.send(new TelasRede.PedirPayload("boloes"));
        if (dados == null || dados.boloes().isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> this.onClose()).bounds(this.width / 2 - 50, 120, 100, 20).build());
            return;
        }
        if (bolao >= dados.boloes().size()) bolao = 0;
        int x = this.width / 2 - 150;
        TelasRede.BolaoInfo b = dados.boloes().get(bolao);
        int ybase = 60 + b.opcoes().size() * 24 + 8;
        if (dados.boloes().size() > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), bt -> {
                bolao = (bolao + dados.boloes().size() - 1) % dados.boloes().size();
                this.rebuildWidgets();
            }).bounds(x + 88, ybase, 24, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), bt -> {
                bolao = (bolao + 1) % dados.boloes().size();
                this.rebuildWidgets();
            }).bounds(x + 148, ybase, 24, 20).build());
        }
        String antigo = valor == null ? "1" : valor.getValue();
        valor = new EditBox(this.font, x + 230, 60 + b.opcoes().size() * 24 + 8, 70, 20, Component.literal("Valor"));
        valor.setValue(antigo);
        this.addRenderableWidget(valor);
        for (int i = 0; i < b.opcoes().size(); i++) {
            String op = b.opcoes().get(i);
            int pote = b.potes().get(i);
            this.addRenderableWidget(Button.builder(Component.literal(op + "  (" + pote + " 💎)"), bt -> {
                String v = valor.getValue().trim();
                if (!v.matches("\\d{1,5}")) return;
                ClientPlayNetworking.send(new TelasRede.ApostarPayload(b.id(), op, Integer.parseInt(v)));
            }).bounds(x, 60 + i * 24, 300, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), bt -> this.onClose()).bounds(x, 60 + b.opcoes().size() * 24 + 8, 80, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        super.extractRenderState(g, mx, my, d);
        g.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        if (dados == null) {
            g.centeredText(this.font, Component.literal("Carregando..."), this.width / 2, 60, 0xFFAAAAAA);
            return;
        }
        if (dados.boloes().isEmpty()) {
            g.centeredText(this.font, Component.literal("Nenhum bolão ativo"), this.width / 2, 80, 0xFFAAAAAA);
            return;
        }
        TelasRede.BolaoInfo b = dados.boloes().get(bolao);
        g.centeredText(this.font, Component.literal("#" + b.id() + " " + b.titulo()), this.width / 2, 27, 0xFFFFC928);
        String info = "Você tem " + dados.diamantes() + " 💎 | apostado aqui: " + b.meu()
            + (b.maxAposta() > 0 ? " | máx " + b.maxAposta() : "")
            + (b.restanteMs() >= 0 ? " | fecha em " + (b.restanteMs() / 60000) + " min" : "");
        g.centeredText(this.font, Component.literal(info), this.width / 2, 46, 0xFFFFFFFF);
        g.text(this.font, "Valor:", this.width / 2 + 80 - this.font.width("Valor:") - 4, 60 + b.opcoes().size() * 24 + 14, 0xFFAAAAAA);
        if (dados.boloes().size() > 1) g.centeredText(this.font, Component.literal((bolao + 1) + "/" + dados.boloes().size()), this.width / 2 - 150 + 130, 60 + b.opcoes().size() * 24 + 14, 0xFFFFFFFF);
        g.centeredText(this.font, Component.literal("Cofre " + dados.cofre() + " | Jackpot " + dados.jackpot() + " | clique numa opção para apostar"), this.width / 2, 60 + b.opcoes().size() * 24 + 36, 0xFF888888);
    }
}
