package br.melby.melbybet.client;

import br.melby.melbybet.ConstrucaoRede;
import br.melby.melbybet.core.Formas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Ferramentas de construção: guia de forma (círculo, esfera, elipse, cúpula), régua, espelho, lista de materiais e grade de mapa. */
public class GuiasTela extends Screen {
    private EditBox caixaA, caixaB, caixaC;

    public GuiasTela() {
        super(Component.literal("Ferramentas de construção"));
    }

    private Button botao(String texto, int x, int y, int w, Runnable acao) {
        return this.addRenderableWidget(Button.builder(Component.literal(texto), b -> acao.run()).bounds(x, y, w, 20).build());
    }

    private void atualizar() {
        Minecraft.getInstance().setScreenAndShow(new GuiasTela());
    }

    private EditBox numero(int x, int y, int w, int valor, String dica, Consumer<Integer> aoMudar) {
        EditBox c = new EditBox(this.font, x, y, w, 18, Component.literal(dica));
        c.setMaxLength(3);
        c.setValue(String.valueOf(valor));
        c.setHint(Component.literal(dica));
        c.setResponder(s -> {
            try {
                int v = Integer.parseInt(s.trim());
                if (v >= 1 && v <= Formas.MAX_RAIO) aoMudar.accept(v);
            } catch (NumberFormatException e) {
                // espera o jogador terminar de digitar
            }
        });
        return this.addRenderableWidget(c);
    }

    @Override
    protected void init() {
        int col = Math.min(150, (this.width - 24) / 2);
        int xe = this.width / 2 - col - 4, xd = this.width / 2 + 4;
        int y = 30;
        // ----- coluna da esquerda: guia de forma -----
        botao("Forma: " + ConstrucaoCliente.tipo.rotulo, xe, y, col, () -> {
            Formas.Tipo[] t = Formas.Tipo.values();
            ConstrucaoCliente.tipo = t[(ConstrucaoCliente.tipo.ordinal() + 1) % t.length];
            atualizar();
        });
        y += 22;
        var t = ConstrucaoCliente.tipo;
        if (t == Formas.Tipo.CIRCULO || t == Formas.Tipo.ELIPSE) {
            botao("Plano: " + ConstrucaoCliente.plano.rotulo, xe, y, col, () -> {
                Formas.Plano[] p = Formas.Plano.values();
                ConstrucaoCliente.plano = p[(ConstrucaoCliente.plano.ordinal() + 1) % p.length];
                atualizar();
            });
            y += 22;
        }
        int w3 = (col - 8) / 3;
        if (t == Formas.Tipo.CIRCULO) {
            caixaA = numero(xe, y, col, ConstrucaoCliente.rx, "Raio", v -> ConstrucaoCliente.rx = v);
        } else if (t == Formas.Tipo.ELIPSE) {
            caixaA = numero(xe, y, col / 2 - 2, ConstrucaoCliente.rx, "Raio 1", v -> ConstrucaoCliente.rx = v);
            caixaB = numero(xe + col / 2 + 2, y, col / 2 - 2, ConstrucaoCliente.rz, "Raio 2", v -> ConstrucaoCliente.rz = v);
        } else {
            caixaA = numero(xe, y, w3, ConstrucaoCliente.rx, "Raio X", v -> ConstrucaoCliente.rx = v);
            caixaB = numero(xe + w3 + 4, y, w3, ConstrucaoCliente.ry, "Raio Y", v -> ConstrucaoCliente.ry = v);
            caixaC = numero(xe + 2 * (w3 + 4), y, w3, ConstrucaoCliente.rz, "Raio Z", v -> ConstrucaoCliente.rz = v);
        }
        y += 22;
        botao("Centro: bloco que olho", xe, y, col, () -> {
            int[] b = ConstrucaoCliente.blocoOlhado();
            if (b == null) {
                WaypointStore.aviso("Olhe para um bloco.");
                return;
            }
            ConstrucaoCliente.centro = b;
            ConstrucaoCliente.formaAtiva = true;
            atualizar();
        });
        y += 22;
        botao("Centro: minha posição", xe, y, col, () -> {
            ConstrucaoCliente.centro = ConstrucaoCliente.posicaoDoJogador();
            ConstrucaoCliente.formaAtiva = true;
            atualizar();
        });
        y += 22;
        botao("Guia: " + (ConstrucaoCliente.formaAtiva ? "Ligada" : "Desligada"), xe, y, col, () -> {
            if (ConstrucaoCliente.centro == null) ConstrucaoCliente.centro = ConstrucaoCliente.posicaoDoJogador();
            ConstrucaoCliente.formaAtiva = !ConstrucaoCliente.formaAtiva;
            atualizar();
        });
        y += 22;
        botao("Mostrar: " + (ConstrucaoCliente.camadaDosPes ? "só a camada dos pés" : "tudo"), xe, y, col, () -> {
            ConstrucaoCliente.camadaDosPes = !ConstrucaoCliente.camadaDosPes;
            atualizar();
        });

        // ----- coluna da direita: régua, espelho, materiais, grade -----
        y = 30;
        botao("Régua: marcar ponto (R)", xd, y, col, () -> marcarRegua());
        y += 22;
        botao("Régua: limpar", xd, y, col, () -> {
            ConstrucaoCliente.reguaA = ConstrucaoCliente.reguaB = null;
            atualizar();
        });
        y += 26;
        botao("Espelho: " + (ConstrucaoCliente.espelhoAtivo ? "Ligado" : "Desligado"), xd, y, col, () -> {
            ConstrucaoCliente.espelhoAtivo = !ConstrucaoCliente.espelhoAtivo;
            atualizar();
        });
        y += 22;
        botao("Eixo: " + (ConstrucaoCliente.espelhoEixoX ? "X (esquerda/direita)" : "Z (frente/trás)"), xd, y, col, () -> {
            ConstrucaoCliente.espelhoEixoX = !ConstrucaoCliente.espelhoEixoX;
            atualizar();
        });
        y += 22;
        botao("Plano: " + (ConstrucaoCliente.espelhoEntreBlocos ? "entre blocos" : "no meio do bloco"), xd, y, col, () -> {
            ConstrucaoCliente.espelhoEntreBlocos = !ConstrucaoCliente.espelhoEntreBlocos;
            atualizar();
        });
        y += 22;
        botao("Espelho: no bloco que olho (Y)", xd, y, col, () -> definirEspelho());
        y += 26;
        botao("Materiais: " + (ConstrucaoCliente.materiaisVisivel ? "mostrando" : "escondidos"), xd, y, col, () -> {
            ConstrucaoCliente.materiaisVisivel = !ConstrucaoCliente.materiaisVisivel;
            atualizar();
        });
        y += 22;
        botao("Esconder grade do mapa-arte", xd, y, col, () -> {
            ConstrucaoCliente.grade = null;
            atualizar();
        });
        botao("Fechar", this.width / 2 - 50, this.height - 28, 100, this::onClose);
    }

    public static void marcarRegua() {
        int[] b = ConstrucaoCliente.blocoOlhado();
        if (b == null) {
            WaypointStore.aviso("Olhe para um bloco para marcar.");
            return;
        }
        if (ConstrucaoCliente.reguaA == null || ConstrucaoCliente.reguaB != null) {
            ConstrucaoCliente.reguaA = b;
            ConstrucaoCliente.reguaB = null;
            WaypointStore.aviso("Régua: ponto A marcado. Marque o B.");
        } else {
            ConstrucaoCliente.reguaB = b;
            double[] m = Formas.medir(ConstrucaoCliente.reguaA, b);
            WaypointStore.aviso("Régua: " + (int) m[0] + " x " + (int) m[1] + " x " + (int) m[2] + " blocos (reta " + String.format(java.util.Locale.ROOT, "%.1f", m[3]) + ")");
        }
    }

    public static void definirEspelho() {
        int[] b = ConstrucaoCliente.blocoOlhado();
        if (b == null) {
            WaypointStore.aviso("Olhe para um bloco para definir o plano do espelho.");
            return;
        }
        ConstrucaoCliente.espelhoPlano = ConstrucaoCliente.espelhoEixoX ? b[0] : b[2];
        ConstrucaoCliente.espelhoAtivo = true;
        WaypointStore.aviso("Espelho no " + (ConstrucaoCliente.espelhoEixoX ? "x " : "z ") + ConstrucaoCliente.espelhoPlano + ". O contorno rosa mostra o bloco espelhado.");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        int n = ConstrucaoCliente.casca().size();
        int col = Math.min(150, (this.width - 24) / 2);
        int xe = this.width / 2 - col - 4;
        g.text(this.font, "~" + n + " blocos (" + (n / 64) + " pilhas e " + (n % 64) + ")", xe, this.height - 52, 0xFFAAAAAA);
        if (ConstrucaoCliente.centro != null) g.text(this.font, "Centro: " + ConstrucaoCliente.centro[0] + " " + ConstrucaoCliente.centro[1] + " " + ConstrucaoCliente.centro[2], xe, this.height - 42, 0xFF888888);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
