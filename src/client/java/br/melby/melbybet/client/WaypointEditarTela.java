package br.melby.melbybet.client;

import br.melby.melbybet.WaypointRede;
import br.melby.melbybet.core.Waypoints;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Cria ou edita um waypoint: nome, inicial, cor, coordenadas, temporário e compartilhar. */
public class WaypointEditarTela extends Screen {
    private final Waypoints.Ponto ponto;
    private final boolean novo;
    private final int voltarPagina;
    private EditBox nome, inicial, ex, ey, ez, raio, minutos;
    private Button botaoCor, botaoTemp;
    private int cor;
    private boolean temp;
    private String erro = "";

    public WaypointEditarTela(Waypoints.Ponto ponto, boolean novo, int voltarPagina) {
        super(Component.literal(novo ? "Novo waypoint" : "Editar waypoint"));
        this.ponto = ponto;
        this.novo = novo;
        this.voltarPagina = voltarPagina;
        this.cor = ponto.cor;
        this.temp = Waypoints.TEMP.equals(ponto.tipo);
    }

    private EditBox caixa(int x, int y, int w, String valor, int max, String dica) {
        EditBox c = new EditBox(this.font, x, y, w, 18, Component.literal(dica));
        c.setMaxLength(max);
        c.setValue(valor);
        c.setHint(Component.literal(dica));
        return this.addRenderableWidget(c);
    }

    @Override
    protected void init() {
        int w = Math.min(this.width - 20, 300);
        int x0 = (this.width - w) / 2;
        int y = 34;
        nome = caixa(x0, y, w - 60, ponto.nome, Waypoints.MAX_NOME, "Nome");
        inicial = caixa(x0 + w - 54, y, 24, ponto.inicial == null ? "" : ponto.inicial, 1, "Ab");
        botaoCor = this.addRenderableWidget(Button.builder(Component.literal("Cor"), b -> cor = Waypoints.proximaCor(cor)).bounds(x0 + w - 26, y - 1, 26, 20).build());
        y += 26;
        int tw = (w - 8) / 3;
        ex = caixa(x0, y, tw, String.valueOf(ponto.x), 9, "X");
        ey = caixa(x0 + tw + 4, y, tw, String.valueOf(ponto.y), 5, "Y");
        ez = caixa(x0 + 2 * (tw + 4), y, tw, String.valueOf(ponto.z), 9, "Z");
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Usar minha posição"), b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            ex.setValue(String.valueOf(mc.player.blockPosition().getX()));
            ey.setValue(String.valueOf(mc.player.blockPosition().getY()));
            ez.setValue(String.valueOf(mc.player.blockPosition().getZ()));
        }).bounds(x0, y, w, 20).build());
        y += 26;
        botaoTemp = this.addRenderableWidget(Button.builder(Component.literal(temp ? "Temporário: Sim" : "Temporário: Não"), b -> {
            temp = !temp;
            b.setMessage(Component.literal(temp ? "Temporário: Sim" : "Temporário: Não"));
        }).bounds(x0, y, w, 20).build());
        y += 24;
        raio = caixa(x0, y, w / 2 - 2, String.valueOf(ponto.raio > 0 ? ponto.raio : WaypointStore.dados.raioPadrao), 4, "Some ao chegar (blocos)");
        long min = ponto.expiraEm > 0 ? Math.max(1, (ponto.expiraEm - System.currentTimeMillis()) / 60000) : WaypointStore.dados.minutosTemp;
        minutos = caixa(x0 + w / 2 + 2, y, w / 2 - 2, String.valueOf(min), 4, "Minutos (0 = sem prazo)");
        y += 30;
        int bw = (w - 8) / 3;
        this.addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> salvar()).bounds(x0, y, bw, 20).build());
        Button comp = this.addRenderableWidget(Button.builder(Component.literal("Compartilhar"), b -> compartilhar()).bounds(x0 + bw + 4, y, bw, 20).build());
        comp.active = ClientPlayNetworking.canSend(WaypointRede.CompartilharPayload.TYPE);
        this.addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> this.onClose()).bounds(x0 + 2 * (bw + 4), y, bw, 20).build());
    }

    private static int inteiro(EditBox c, int padrao) {
        try {
            return Integer.parseInt(c.getValue().trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    private boolean numerosValidos() {
        for (EditBox c : new EditBox[] {ex, ey, ez}) {
            try {
                Integer.parseInt(c.getValue().trim());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private void salvar() {
        Minecraft mc = Minecraft.getInstance();
        String n = nome.getValue().trim();
        String motivo = Waypoints.validarNome(n);
        if (motivo != null) {
            erro = motivo;
            return;
        }
        if (!numerosValidos()) {
            erro = "As coordenadas precisam ser números inteiros.";
            return;
        }
        ponto.nome = n;
        ponto.inicial = inicial.getValue().trim();
        ponto.cor = cor;
        ponto.x = inteiro(ex, 0);
        ponto.y = inteiro(ey, 0);
        ponto.z = inteiro(ez, 0);
        if (temp) {
            if (!ponto.automatico()) ponto.tipo = Waypoints.TEMP;
            ponto.raio = Math.max(0, inteiro(raio, 0));
            int m = Math.max(0, inteiro(minutos, 0));
            ponto.expiraEm = m > 0 ? System.currentTimeMillis() + m * 60_000L : 0;
        } else if (Waypoints.TEMP.equals(ponto.tipo)) {
            ponto.tipo = Waypoints.NORMAL;
            ponto.raio = 0;
            ponto.expiraEm = 0;
        }
        if (novo) {
            String e = WaypointStore.dados.adicionar(ponto);
            if (e != null) {
                erro = e;
                return;
            }
        }
        WaypointStore.salvar();
        mc.setScreenAndShow(new WaypointTela(voltarPagina));
    }

    private void compartilhar() {
        String n = nome.getValue().trim();
        if (Waypoints.validarNome(n) != null || !numerosValidos()) {
            erro = "Preencha nome e coordenadas antes de compartilhar.";
            return;
        }
        ClientPlayNetworking.send(new WaypointRede.CompartilharPayload(n, ponto.dim, inteiro(ex, 0), inteiro(ey, 0), inteiro(ez, 0)));
        erro = "";
        WaypointStore.aviso("Waypoint compartilhado no chat.");
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(new WaypointTela(voltarPagina));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
        int w = Math.min(this.width - 20, 300);
        int x0 = (this.width - w) / 2;
        g.fill(botaoCor.getX() + 4, botaoCor.getY() + 4, botaoCor.getX() + botaoCor.getWidth() - 4, botaoCor.getY() + botaoCor.getHeight() - 4, 0xFF000000 | cor);
        g.text(this.font, "Dimensão: " + ponto.dim, x0, 34 + 26 + 22 + 26 + 24 + 24 + 30 + 26, 0xFF888888);
        if (!erro.isEmpty()) g.centeredText(this.font, erro, this.width / 2, 34 + 26 + 22 + 26 + 24 + 24 + 30 + 26 + 12, 0xFFFF5555);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
