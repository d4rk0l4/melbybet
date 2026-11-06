package br.melby.melbybet.client;

import br.melby.melbybet.core.Waypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Lista de waypoints: ligar/desligar cada um, editar, apagar, criar um novo e opções gerais. */
public class WaypointTela extends Screen {
    private static final int POR_PAGINA = 8;
    private static final int LINHA = 24;
    private final int pagina;
    private List<Waypoints.Ponto> lista = List.of();
    private int x0, y0, largura;

    public WaypointTela(int pagina) {
        super(Component.literal("Waypoints"));
        this.pagina = pagina;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        lista = WaypointStore.dados.ordenados(WaypointStore.dimAtual(mc), mc.player.getX(), mc.player.getY(), mc.player.getZ());
        int paginas = Math.max(1, (lista.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pg = Math.max(0, Math.min(pagina, paginas - 1));
        largura = Math.min(this.width - 16, 360);
        x0 = (this.width - largura) / 2;
        y0 = 34;
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pg * POR_PAGINA + i;
            if (idx >= lista.size()) break;
            Waypoints.Ponto p = lista.get(idx);
            int y = y0 + i * LINHA;
            this.addRenderableWidget(Button.builder(Component.literal(p.visivel ? "On" : "Off"), b -> {
                p.visivel = !p.visivel;
                WaypointStore.salvar();
                mc.setScreenAndShow(new WaypointTela(pg));
            }).bounds(x0, y, 28, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Editar"), b -> mc.setScreenAndShow(new WaypointEditarTela(p, false, pg)))
                .bounds(x0 + largura - 72, y, 48, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                WaypointStore.dados.remover(p.id);
                WaypointStore.salvar();
                mc.setScreenAndShow(new WaypointTela(pg));
            }).bounds(x0 + largura - 20, y, 20, 20).build());
        }
        int yb = y0 + POR_PAGINA * LINHA + 6;
        int w3 = (largura - 8) / 3;
        this.addRenderableWidget(Button.builder(Component.literal("Novo aqui"), b -> {
            Waypoints.Ponto n = new Waypoints.Ponto();
            n.nome = WaypointStore.dados.nomeNovo();
            n.dim = WaypointStore.dimAtual(mc);
            n.x = mc.player.blockPosition().getX();
            n.y = mc.player.blockPosition().getY();
            n.z = mc.player.blockPosition().getZ();
            n.cor = Waypoints.corDoIndice(WaypointStore.dados.pontos.size());
            n.criadoEm = System.currentTimeMillis();
            mc.setScreenAndShow(new WaypointEditarTela(n, true, pg));
        }).bounds(x0, yb, w3, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("< Anterior"), b -> mc.setScreenAndShow(new WaypointTela(pg - 1)))
            .bounds(x0 + w3 + 4, yb, w3 / 2 - 2, 20).build()).active = pg > 0;
        this.addRenderableWidget(Button.builder(Component.literal("Próxima >"), b -> mc.setScreenAndShow(new WaypointTela(pg + 1)))
            .bounds(x0 + w3 + 4 + w3 / 2 + 2, yb, w3 / 2 - 2, 20).build()).active = pg < paginas - 1;
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> this.onClose()).bounds(x0 + 2 * (w3 + 4), yb, w3, 20).build());
        int yc = yb + 24;
        this.addRenderableWidget(Button.builder(Component.literal("Todos: " + (WaypointStore.dados.tudoVisivel ? "On" : "Off")), b -> {
            WaypointStore.dados.tudoVisivel = !WaypointStore.dados.tudoVisivel;
            WaypointStore.salvar();
            mc.setScreenAndShow(new WaypointTela(pg));
        }).bounds(x0, yc, w3, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Bússola: " + (WaypointStore.dados.bussola ? "On" : "Off")), b -> {
            WaypointStore.dados.bussola = !WaypointStore.dados.bussola;
            WaypointStore.salvar();
            mc.setScreenAndShow(new WaypointTela(pg));
        }).bounds(x0 + w3 + 4, yc, w3, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Feixe: " + (WaypointStore.dados.feixe ? "On" : "Off")), b -> {
            WaypointStore.dados.feixe = !WaypointStore.dados.feixe;
            WaypointStore.salvar();
            mc.setScreenAndShow(new WaypointTela(pg));
        }).bounds(x0 + 2 * (w3 + 4), yc, w3, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        g.centeredText(this.font, "Waypoints (" + lista.size() + ")", this.width / 2, 14, 0xFFFFFFFF);
        int paginas = Math.max(1, (lista.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pg = Math.max(0, Math.min(pagina, paginas - 1));
        String dim = WaypointStore.dimAtual(mc);
        if (lista.isEmpty()) g.centeredText(this.font, "Nenhum waypoint ainda. Aperte N no jogo ou use \"Novo aqui\".", this.width / 2, y0 + 20, 0xFFAAAAAA);
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pg * POR_PAGINA + i;
            if (idx >= lista.size()) break;
            Waypoints.Ponto p = lista.get(idx);
            int y = y0 + i * LINHA;
            int cor = 0xFF000000 | p.cor;
            int alfa = p.visivel ? 0xFF : 0x66;
            g.fill(x0 + 32, y + 1, x0 + 50, y + 19, (alfa << 24) | p.cor);
            g.text(this.font, p.letra(), x0 + 38, y + 6, 0xFF000000);
            String extra = p.automatico() ? " (corpo)" : Waypoints.TEMP.equals(p.tipo) ? " (temporário)" : Waypoints.COMPARTILHADO.equals(p.tipo) ? " (compartilhado)" : "";
            g.text(this.font, p.nome + extra, x0 + 56, y + 2, p.visivel ? 0xFFFFFFFF : 0xFF777777);
            String onde = p.x + " " + p.y + " " + p.z;
            String dist = p.dim.equals(dim) ? Waypoints.formatarDistancia(Waypoints.distancia(p, mc.player.getX(), mc.player.getY(), mc.player.getZ())) : p.dim;
            g.text(this.font, onde + "  -  " + dist, x0 + 56, y + 12, 0xFFAAAAAA);
        }
        g.centeredText(this.font, "Página " + (pg + 1) + "/" + paginas, this.width / 2, y0 + POR_PAGINA * LINHA + 30 + 28, 0xFF888888);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
