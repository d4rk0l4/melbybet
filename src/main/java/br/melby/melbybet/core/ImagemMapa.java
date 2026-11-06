package br.melby.melbybet.core;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

/** Lê uma imagem, ajusta ao tamanho do mural e a quebra em mapas de 128x128 já com as cores do jogo. Lógica pura. */
public final class ImagemMapa {
    private ImagemMapa() {}

    public static final long MAX_PIXELS = 25_000_000L;
    public static final int LADO = 128;

    public static final String CORTAR = "cortar", AJUSTAR = "ajustar", ESTICAR = "esticar";

    /** Decodifica PNG, JPG, GIF (primeiro quadro) ou BMP. Recusa imagens gigantes antes de abri-las. */
    public static BufferedImage ler(byte[] dados) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(dados))) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(in);
            if (!it.hasNext()) throw new IllegalArgumentException("Formato de imagem não suportado (use PNG, JPG, GIF ou BMP; WebP não funciona).");
            ImageReader rd = it.next();
            try {
                rd.setInput(in, true, true);
                long w = rd.getWidth(0), h = rd.getHeight(0);
                if (w <= 0 || h <= 0) throw new IllegalArgumentException("Imagem vazia.");
                if (w * h > MAX_PIXELS) throw new IllegalArgumentException("Imagem grande demais (" + w + "x" + h + "). O máximo é 25 milhões de pixels.");
                BufferedImage img = rd.read(0);
                if (img == null) throw new IllegalArgumentException("Não consegui ler a imagem.");
                return img;
            } finally {
                rd.dispose();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não consegui ler a imagem.");
        }
    }

    private static BufferedImage metade(BufferedImage s, int w, int h) {
        BufferedImage d = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = d.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(s, 0, 0, d.getWidth(), d.getHeight(), null);
        g.dispose();
        return d;
    }

    /** Encolhe aos poucos (pela metade a cada passo), que fica bem mais nítido que de uma vez. */
    private static BufferedImage reduzir(BufferedImage s, int alvoW, int alvoH) {
        BufferedImage atual = s;
        while (atual.getWidth() / 2 >= alvoW && atual.getHeight() / 2 >= alvoH) atual = metade(atual, atual.getWidth() / 2, atual.getHeight() / 2);
        return atual;
    }

    /** O mural final (larg x alt mapas) como pixels ARGB. */
    public static BufferedImage compor(BufferedImage src, int larg, int alt, String modo) {
        int W = larg * LADO, H = alt * LADO;
        BufferedImage dst = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double sw = src.getWidth(), sh = src.getHeight();
        if (ESTICAR.equals(modo)) {
            g.drawImage(reduzir(src, W, H), 0, 0, W, H, null);
        } else {
            boolean cobrir = !AJUSTAR.equals(modo);
            double esc = cobrir ? Math.max(W / sw, H / sh) : Math.min(W / sw, H / sh);
            int dw = Math.max(1, (int) Math.round(sw * esc)), dh = Math.max(1, (int) Math.round(sh * esc));
            if (!cobrir) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, W, H);
            }
            g.drawImage(reduzir(src, dw, dh), (W - dw) / 2, (H - dh) / 2, dw, dh, null);
        }
        g.dispose();
        return dst;
    }

    /** Devolve um byte[16384] por mapa, de cima para baixo e da esquerda para a direita (índice = linha * larg + coluna). */
    public static byte[][] preparar(BufferedImage src, int larg, int alt, String modo, boolean difusao) {
        BufferedImage m = compor(src, larg, alt, modo);
        int W = larg * LADO, H = alt * LADO;
        int[] px = m.getRGB(0, 0, W, H, null, 0, W);
        byte[] cores = PaletaMapa.converter(px, W, H, difusao);
        byte[][] r = new byte[larg * alt][LADO * LADO];
        for (int lin = 0; lin < alt; lin++) {
            for (int col = 0; col < larg; col++) {
                byte[] mapa = r[lin * larg + col];
                for (int y = 0; y < LADO; y++) System.arraycopy(cores, (lin * LADO + y) * W + col * LADO, mapa, y * LADO, LADO);
            }
        }
        return r;
    }
}
