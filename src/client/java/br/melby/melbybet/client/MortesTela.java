package br.melby.melbybet.client;

import br.melby.melbybet.MortesRede;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Histórico das suas últimas mortes: onde foi, quando, quantos itens ainda estão no corpo. */
public class MortesTela extends Screen {
    private static MortesRede.ListaPayload dados;
    private static long recebidoEm;
    private static int indice;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private Button anterior, proximo, itens;

    public MortesTela() {
        super(Component.literal("Histórico de mortes"));
    }

    public static void abrir() {
        ClientPlayNetworking.send(new MortesRede.PedirPayload(0));
        Minecraft.getInstance().setScreenAndShow(new MortesTela());
    }

    public static void receber(MortesRede.ListaPayload p) {
        dados = p;
        recebidoEm = System.currentTimeMillis();
        if (indice >= p.mortes().size()) indice = 0;
        if (Minecraft.getInstance().gui.screen() instanceof MortesTela t) t.atualizar();
    }

    private MortesRede.Morte atual() {
        return dados != null && indice < dados.mortes().size() ? dados.mortes().get(indice) : null;
    }

    private static String dimensao(String d) {
        return switch (d) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "The End";
            default -> d;
        };
    }

    private long protegidoRestanteMs() {
        var m = atual();
        return m == null ? 0 : Math.max(0, m.protegidoMs() - (System.currentTimeMillis() - recebidoEm));
    }

    @Override
    protected void init() {
        int w = Math.min(300, this.width - 20), x = this.width / 2 - w / 2, y = this.height / 2 + 62;
        int bw = (w - 8) / 3;
        anterior = this.addRenderableWidget(Button.builder(Component.literal("Anterior"), b -> {
            indice--;
            atualizar();
        }).bounds(x, y, bw, 20).build());
        itens = this.addRenderableWidget(Button.builder(Component.literal("Itens"), b -> {
            var m = atual();
            if (m != null && !m.recuperado()) {
                ClientPlayNetworking.send(new MortesRede.PedirPayload(m.corpoId()));
                this.onClose();
            }
        }).bounds(x + bw + 4, y, bw, 20).build());
        proximo = this.addRenderableWidget(Button.builder(Component.literal("Próximo"), b -> {
            indice++;
            atualizar();
        }).bounds(x + 2 * (bw + 4), y, bw, 20).build());
        atualizar();
    }

    private void atualizar() {
        int n = dados == null ? 0 : dados.mortes().size();
        if (anterior == null) return;
        anterior.active = indice > 0;
        proximo.active = indice < n - 1;
        var m = atual();
        itens.active = m != null && !m.recuperado();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int w = Math.min(300, this.width - 20), x = this.width / 2 - w / 2, top = this.height / 2 - 80;
        g.fill(x - 6, top - 8, x + w + 6, this.height / 2 + 90, 0xE0202020);
        g.centeredText(this.font, this.title, this.width / 2, top, 0xFFFFFFFF);
        var m = atual();
        if (dados == null) {
            g.centeredText(this.font, Component.literal("Carregando..."), this.width / 2, top + 40, 0xFFAAAAAA);
            return;
        }
        if (m == null) {
            g.centeredText(this.font, Component.literal("Você ainda não morreu."), this.width / 2, top + 40, 0xFFAAAAAA);
            return;
        }
        g.centeredText(this.font, Component.literal(DATA.format(Instant.ofEpochMilli(m.quando())) + "  (" + (indice + 1) + "/" + dados.mortes().size() + ")"), this.width / 2, top + 14, 0xFFFFC928);
        int y = top + 34, c1 = x, c2 = x + 70;
        linha(g, "Nome:", dados.nome(), c1, c2, y);
        linha(g, "Dimensão:", dimensao(m.dim()), c1, c2, y + 14);
        linha(g, "Local:", m.x() + " X", c1, c2, y + 28);
        linha(g, "", m.y() + " Y", c1, c2, y + 40);
        linha(g, "", m.z() + " Z", c1, c2, y + 52);
        Minecraft mc = Minecraft.getInstance();
        String dist = "";
        if (mc.player != null && m.dim().equals(WaypointStore.dimAtual(mc))) {
            double dx = m.x() + 0.5 - mc.player.getX(), dy = m.y() - mc.player.getY(), dz = m.z() + 0.5 - mc.player.getZ();
            dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + " blocos";
        } else {
            dist = "outra dimensão";
        }
        linha(g, "Distância:", dist, c1, c2, y + 70);
        String estado;
        int cor = 0xFFFFFFFF;
        if (m.recuperado()) {
            estado = "Itens recuperados";
            cor = 0xFF88FF88;
        } else {
            long prot = protegidoRestanteMs();
            estado = m.itens() + " item(ns) no corpo" + (prot > 0 ? " (protegido " + ((prot + 999) / 1000) + "s)" : "");
            cor = prot > 0 ? 0xFFFFAA55 : 0xFFFFFFFF;
        }
        g.text(this.font, estado, c1, y + 86, cor);
        if (mc.player != null) {
            int bx = x + w - 90;
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, bx, y, bx + 80, y + 100, 40, 0.0625F, mouseX, mouseY, mc.player);
        }
    }

    private void linha(GuiGraphicsExtractor g, String rotulo, String valor, int c1, int c2, int y) {
        g.text(this.font, rotulo, c1, y, 0xFFCCCCCC);
        g.text(this.font, valor, c2, y, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
