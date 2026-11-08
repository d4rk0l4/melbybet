package br.melby.melbybet.client;

import br.melby.melbybet.ConstrucaoRede;
import br.melby.melbybet.core.ImagemMapa;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Foto na parede a partir de um arquivo do seu computador: o jogo reduz a imagem aqui e manda ao servidor em pedaços. */
public class FotoTela extends Screen {
    private static String caminho = "";
    private static int larg = 3, alt = 2, modo;
    private static final String[] MODOS = {ImagemMapa.CORTAR, ImagemMapa.AJUSTAR, ImagemMapa.ESTICAR};
    private static int sessao = (int) (System.nanoTime() & 0x7FFFFFFF);
    private EditBox caixa;
    private String status = "";

    public FotoTela() {
        super(Component.literal("Foto na parede"));
    }

    @Override
    protected void init() {
        int w = Math.min(280, this.width - 20);
        int x = this.width / 2 - w / 2;
        int y = 40;
        caixa = new EditBox(this.font, x, y, w, 18, Component.literal("Arquivo"));
        caixa.setMaxLength(400);
        caixa.setValue(caminho);
        caixa.setHint(Component.literal("Cole o caminho da imagem ou arraste o arquivo aqui"));
        caixa.setResponder(s -> caminho = s);
        this.addRenderableWidget(caixa);
        y += 26;
        this.addRenderableWidget(Button.builder(Component.literal("Largura: " + larg), b -> {
            larg = larg % 6 + 1;
            b.setMessage(Component.literal("Largura: " + larg));
        }).bounds(x, y, w / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Altura: " + alt), b -> {
            alt = alt % 6 + 1;
            b.setMessage(Component.literal("Altura: " + alt));
        }).bounds(x + w / 2 + 2, y, w / 2 - 2, 20).build());
        y += 24;
        this.addRenderableWidget(Button.builder(Component.literal("Modo: " + MODOS[modo]), b -> {
            modo = (modo + 1) % MODOS.length;
            b.setMessage(Component.literal("Modo: " + MODOS[modo]));
        }).bounds(x, y, w, 20).build());
        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("Enviar (olhe para o canto inferior esquerdo da parede)"), b -> enviar()).bounds(x, y, w, 20).build());
        y += 24;
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> this.onClose()).bounds(x, y, w, 20).build());
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths.isEmpty()) return;
        caminho = paths.get(0).toString();
        caixa.setValue(caminho);
    }

    private void enviar() {
        try {
            String c = caminho.trim();
            if (c.length() >= 2 && c.startsWith("\"") && c.endsWith("\"")) c = c.substring(1, c.length() - 1);
            Path p = Path.of(c);
            if (!Files.isRegularFile(p)) {
                status = "Arquivo não encontrado.";
                return;
            }
            if (Files.size(p) > 40L * 1024 * 1024) {
                status = "Arquivo grande demais (máximo 40 MB).";
                return;
            }
            BufferedImage img = ImageIO.read(p.toFile());
            if (img == null) {
                status = "Formato não suportado. Use PNG, JPG, GIF ou BMP.";
                return;
            }
            byte[] dados = reduzir(img, larg, alt);
            int total = (dados.length + ConstrucaoRede.FotoPartePayload.MAX_PARTE - 1) / ConstrucaoRede.FotoPartePayload.MAX_PARTE;
            if (total > 70) {
                status = "A imagem ficou grande demais depois de reduzir.";
                return;
            }
            sessao++;
            for (int i = 0; i < total; i++) {
                int de = i * ConstrucaoRede.FotoPartePayload.MAX_PARTE;
                int ate = Math.min(dados.length, de + ConstrucaoRede.FotoPartePayload.MAX_PARTE);
                byte[] parte = java.util.Arrays.copyOfRange(dados, de, ate);
                ClientPlayNetworking.send(new ConstrucaoRede.FotoPartePayload(sessao, i, total, larg, alt, MODOS[modo], parte));
            }
            Minecraft.getInstance().setScreenAndShow(null);
            WaypointStore.aviso("Imagem enviada (" + (dados.length / 1024) + " KB). Veja o chat.");
        } catch (Exception e) {
            status = "Não consegui ler: " + e.getClass().getSimpleName();
        }
    }

    /** Reduz para no máximo 2 mapas por lado do mural (a qualidade final é de 128 px por mapa) e codifica em PNG. */
    static byte[] reduzir(BufferedImage src, int larg, int alt) throws Exception {
        int maxW = larg * 256, maxH = alt * 256;
        double f = Math.min(1.0, Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * f)), h = Math.max(1, (int) Math.round(src.getHeight() * f));
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(dst, "png", out);
        return out.toByteArray();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
        g.centeredText(this.font, Component.literal("Tamanho em molduras. Cada uma vira um mapa."), this.width / 2, 26, 0xFFAAAAAA);
        if (!status.isEmpty()) g.centeredText(this.font, Component.literal(status), this.width / 2, this.height - 24, 0xFFFF5555);
    }
}
