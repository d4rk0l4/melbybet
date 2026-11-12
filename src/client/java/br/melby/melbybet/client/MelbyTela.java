package br.melby.melbybet.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menu central /melby. Os botões da moderação e da administração só funcionam se o servidor liberar para você. */
public class MelbyTela extends Screen {
    private record Item(String nome, String dica, Runnable acao) {}

    public MelbyTela() {
        super(Component.literal("Melby"));
    }

    @Override
    protected void init() {
        Item[] itens = {
            new Item("Apostas", "Bolões e apostas", () -> TelasCliente.comando("bet")),
            new Item("Ranking", "Quem andou, minerou e morreu mais", () -> TelasCliente.comando("top")),
            new Item("Estatísticas", "Suas estatísticas e o ranking por categoria (F7)", EstatTela::abrir),
            new Item("Mortes", "Corpos ativos e histórico (M)", MortesTela::abrir),
            new Item("Meu perfil", "Seu perfil e o de outros jogadores", () -> TelasCliente.comando("perfil")),
            new Item("Cor do nick", "Degradê do seu nick no chat", () -> TelasCliente.comando("cor")),
            new Item("Gestos", "Girar, acenar, sentar...", () -> TelasCliente.comando("gestos")),
            new Item("Duelo", "Desafiar um jogador", () -> TelasCliente.comando("duelo")),
            new Item("Ajuda", "Livro de boas-vindas", () -> TelasCliente.comando("bet ajuda")),
            new Item("Moderação", "Só para a moderação", () -> TelasCliente.comando("melby mod")),
            new Item("Duelos (staff)", "Log, rastro do prêmio e anulação", () -> TelasCliente.comando("melby duelos")),
            new Item("Administração", "Só para admins", () -> TelasCliente.comando("melby adm")),
        };
        int colunas = 3, lar = 110, alt = 20, espaco = 6;
        int x0 = this.width / 2 - (colunas * lar + (colunas - 1) * espaco) / 2;
        for (int i = 0; i < itens.length; i++) {
            Item it = itens[i];
            int cx = x0 + (i % colunas) * (lar + espaco), cy = 36 + (i / colunas) * (alt + espaco);
            this.addRenderableWidget(Button.builder(Component.literal(it.nome()), b -> {
                this.onClose();
                it.acao().run();
            }).bounds(cx, cy, lar, alt).tooltip(Tooltip.create(Component.literal(it.dica()))).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Menu clássico (baú)"), b -> {
            this.onClose();
            TelasCliente.comando("melby classico");
        }).bounds(this.width / 2 - 70, 36 + 4 * (alt + espaco) + 4, 140, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        super.extractRenderState(g, mx, my, d);
        g.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFC928);
    }
}
