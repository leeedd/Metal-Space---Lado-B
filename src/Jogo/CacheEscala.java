package Jogo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pedacos de imagem ja redimensionados, para nao reescalar a mesma arte a cada quadro.
 * O resultado e o mesmo de desenhar com escala (mesma interpolacao); so o custo muda: escala uma vez,
 * depois e copia direta (e o Java2D ainda guarda a copia na placa de video).
 */
final class CacheEscala {
    private static final int LIMITE = 400; // pedacos guardados; os menos usados saem primeiro
    private static final Map<Chave, BufferedImage> CACHE = new LinkedHashMap<Chave, BufferedImage>(64, .75f, true) {
        private static final long serialVersionUID = 1L;
        @Override protected boolean removeEldestEntry(Map.Entry<Chave, BufferedImage> e) { return size() > LIMITE; }
    };

    private CacheEscala() { }

    /**
     * Mesmo que g.drawImage(img, dx, dy, dx + w, dy + h, sx1, sy1, sx2, sy2, null) com a interpolacao dada
     * (sx1 > sx2 espelha na horizontal, como no drawImage).
     */
    static void desenhar(Graphics2D g, Image img, int dx, int dy, int w, int h, int sx1, int sy1, int sx2, int sy2, Object interpolacao) {
        if (img == null || w <= 0 || h <= 0) return;
        g.drawImage(pedaco(img, w, h, sx1, sy1, sx2, sy2, interpolacao), dx, dy, null);
    }

    /** Igual, usando a interpolacao que ja esta valendo em g (sem nenhuma, o Java2D usa vizinho mais proximo). */
    static void desenhar(Graphics2D g, Image img, int dx, int dy, int w, int h, int sx1, int sy1, int sx2, int sy2) {
        Object i = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        desenhar(g, img, dx, dy, w, h, sx1, sy1, sx2, sy2, i == null ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : i);
    }

    static synchronized BufferedImage pedaco(Image img, int w, int h, int sx1, int sy1, int sx2, int sy2, Object interpolacao) {
        Chave k = new Chave(img, w, h, sx1, sy1, sx2, sy2, interpolacao);
        BufferedImage b = CACHE.get(k);
        if (b == null) {
            b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D e = b.createGraphics();
            e.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolacao);
            e.drawImage(img, 0, 0, w, h, sx1, sy1, sx2, sy2, null);
            e.dispose();
            if (opaca(b)) { // sem transparencia (ex.: cenario): guarda opaca, que e copia direta, sem mistura de cores
                BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D og = o.createGraphics();
                og.drawImage(b, 0, 0, null);
                og.dispose();
                b = o;
            }
            CACHE.put(k, b);
        }
        return b;
    }

    private static boolean opaca(BufferedImage b) {
        int[] linha = new int[b.getWidth()];
        for (int y = 0; y < b.getHeight(); y++) {
            b.getRGB(0, y, linha.length, 1, linha, 0, linha.length);
            for (int p : linha) if ((p >>> 24) != 255) return false;
        }
        return true;
    }

    private static final class Chave {
        final Image img; final int w, h, sx1, sy1, sx2, sy2; final Object interp;
        Chave(Image img, int w, int h, int sx1, int sy1, int sx2, int sy2, Object interp) {
            this.img = img; this.w = w; this.h = h; this.sx1 = sx1; this.sy1 = sy1; this.sx2 = sx2; this.sy2 = sy2; this.interp = interp;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Chave)) return false;
            Chave c = (Chave) o;
            return c.img == img && c.w == w && c.h == h && c.sx1 == sx1 && c.sy1 == sy1 && c.sx2 == sx2 && c.sy2 == sy2 && c.interp == interp;
        }
        @Override public int hashCode() {
            int r = System.identityHashCode(img);
            r = r * 31 + w; r = r * 31 + h; r = r * 31 + sx1; r = r * 31 + sy1; r = r * 31 + sx2; r = r * 31 + sy2;
            return r * 31 + System.identityHashCode(interp);
        }
    }
}
