package Jogo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Random;

/**
 * Movimento na tela inicial sem mexer na arte: estrelas e riscos de velocidade passando, asteroides em pixel art
 * (os do jogo) cruzando o espaco e o fogo dos motores tremulando. Estrelas e asteroides so aparecem no espaco
 * escuro alcancavel pelas bordas (mascara): por isso passam ATRAS da nave, do logo, do planeta e dos botoes.
 * Tudo em blocos de 2 px (pixel art), sem suavizacao.
 */
final class FundoInicioAnimado {
    static final int W = 960, H = 600, BLOCO = 2;
    /** Bocais dos motores na arte (escala 960x600, medidos no brilho dos jatos) e direcao do jato. */
    static final int[][] BOCAIS = {{150, 399}, {170, 466}};
    private static final double JATO_DX = -1, JATO_DY = .6;
    /** A nave aponta para cima/direita: o espaco "passa" para baixo/esquerda. */
    private static final double FUGA_DY = .42;

    private final BufferedImage mascara;           // alfa 255 = espaco livre
    private final BufferedImage camada = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    private final double[] ex, ey, ev; private final int[] et, ec;
    private final double[] mx, my, mv; private final int[] ms, mt;
    /** Particulas de exaustao correndo pelos jatos: distancia do bocal e velocidade. */
    private final double[] pd = new double[28], pv = new double[28];
    private final Image[] meteoros;
    private final Random r = new Random(19);
    private double tempo;

    FundoInicioAnimado(Image arte, Rectangle... fora) {
        mascara = mascara(arte, fora);
        int n = 110;
        ex = new double[n]; ey = new double[n]; ev = new double[n]; et = new int[n]; ec = new int[n];
        for (int i = 0; i < n; i++) novaEstrela(i, true);
        meteoros = new Image[]{Assets.imagem("meteoro_16bits_0.png"), Assets.imagem("meteoro_16bits_1.png"),
                Assets.imagem("meteoro_16bits_2.png"), Assets.imagem("meteoro_16bits_3.png")};
        int m = 5;
        mx = new double[m]; my = new double[m]; mv = new double[m]; ms = new int[m]; mt = new int[m];
        for (int i = 0; i < m; i++) novoMeteoro(i, true);
        for (int i = 0; i < pd.length; i++) { pd[i] = r.nextInt(160); pv[i] = 260 + r.nextInt(220); }
    }

    private void novaEstrela(int i, boolean qualquerX) {
        int camadaVel = r.nextInt(3);
        ex[i] = qualquerX ? r.nextInt(W) : W + r.nextInt(80);
        ey[i] = 30 + r.nextInt(H - 60);
        ev[i] = camadaVel == 0 ? 40 + r.nextInt(30) : camadaVel == 1 ? 110 + r.nextInt(60) : 260 + r.nextInt(160);
        et[i] = camadaVel;                                  // 2 = risco de velocidade
        int[] cores = {0xFF9FB6E8, 0xFFFFFFFF, 0xFFB8E6FF, 0xFFFFD9A8};
        ec[i] = cores[r.nextInt(cores.length)];
    }

    private void novoMeteoro(int i, boolean qualquerX) {
        mx[i] = qualquerX ? r.nextInt(W) : W + 40 + r.nextInt(300);
        my[i] = 60 + r.nextInt(H - 140);
        mv[i] = 25 + r.nextInt(45);
        ms[i] = r.nextInt(4);
        mt[i] = 22 + r.nextInt(10) * 2;                     // 22 a 40 px, como os asteroides do jogo
    }

    void atualizar(double dt) {
        tempo += dt;
        for (int i = 0; i < ex.length; i++) {
            ex[i] -= ev[i] * dt; ey[i] += ev[i] * FUGA_DY * dt;
            if (ex[i] < -30 || ey[i] > H + 10) { novaEstrela(i, false); if (r.nextBoolean()) { ex[i] = r.nextInt(W); ey[i] = -10; } }
        }
        for (int i = 0; i < mx.length; i++) {
            mx[i] -= mv[i] * dt; my[i] += mv[i] * FUGA_DY * dt;
            if (mx[i] < -60 || my[i] > H + 20) { novoMeteoro(i, false); if (r.nextBoolean()) { mx[i] = 200 + r.nextInt(W); my[i] = -50; } }
        }
        for (int i = 0; i < pd.length; i++) { pd[i] += pv[i] * dt; if (pd[i] > 190) { pd[i] = r.nextInt(8); pv[i] = 260 + r.nextInt(220); } }
    }

    /** Desenha por cima da arte de fundo (chamar logo depois de desenhar a arte). */
    void desenhar(Graphics2D g) {
        Graphics2D c = camada.createGraphics();
        c.setComposite(AlphaComposite.Clear);
        c.fillRect(0, 0, W, H);
        c.setComposite(AlphaComposite.SrcOver);
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < ex.length; i++) {
            int x = bloco(ex[i]), y = bloco(ey[i]);
            if (et[i] == 2) {                               // risco de velocidade: blocos na diagonal da fuga
                int comp = 4 + (int) ((ev[i] - 260) / 40);
                for (int k = 0; k < comp; k++) {
                    int a = Math.max(40, 255 - k * 200 / comp);
                    c.setColor(new Color((ec[i] & 0xFFFFFF) | (a << 24), true));
                    c.fillRect(x + k * BLOCO * 2, bloco(ey[i] - k * BLOCO * 2 * FUGA_DY), BLOCO * 2, BLOCO);
                }
            } else if (et[i] == 1) { c.setColor(new Color(ec[i], true)); c.fillRect(x, y, BLOCO * 2, BLOCO * 2); }
            else { c.setColor(new Color((ec[i] & 0xFFFFFF) | 0xBB000000, true)); c.fillRect(x, y, BLOCO, BLOCO); }
        }
        for (int i = 0; i < mx.length; i++) {
            Image m = meteoros[ms[i]];
            if (m == null) continue;
            int w = mt[i], h = Math.max(BLOCO, mt[i] * m.getHeight(null) / Math.max(1, m.getWidth(null))); // como no jogo
            if (m.getWidth(null) <= 0) continue;
            c.drawImage(m, bloco(mx[i]), bloco(my[i]), w, h, null);
        }
        c.setComposite(AlphaComposite.DstIn);               // so fica o que caiu no espaco livre
        c.drawImage(mascara, 0, 0, null);
        c.dispose();
        g.drawImage(camada, 0, 0, null);
        desenharJatos(g);
    }

    /** Exaustao dos motores: particulas brancas/ciano correndo pelos jatos da arte e um brilho que pisca no bocal. */
    private void desenharJatos(Graphics2D g) {
        Graphics2D j = (Graphics2D) g.create();
        for (int i = 0; i < pd.length; i++) {
            int[] b = BOCAIS[i % BOCAIS.length];
            double d = pd[i], f = d / 190.0;
            int x = bloco(b[0] + JATO_DX * d), y = bloco(b[1] + JATO_DY * d + ((i * 7) % 5 - 2) * 2);
            int a = (int) (255 * (1 - f));
            j.setColor(new Color(40, 150, 255, Math.max(0, a * 2 / 3)));
            j.fillRect(x - BLOCO, y - BLOCO, BLOCO * 4, BLOCO * 3);
            j.setColor(new Color(255, 255, 255, Math.max(0, a)));
            j.fillRect(x, y, BLOCO * 2, BLOCO);
        }
        for (int b = 0; b < BOCAIS.length; b++) {
            double pulso = .5 + .5 * Math.sin(tempo * 21 + b * 1.9) * Math.sin(tempo * 6.1 + b);
            int x = bloco(BOCAIS[b][0]), y = bloco(BOCAIS[b][1]), tam = BLOCO * (3 + (int) (pulso * 3));
            j.setColor(new Color(120, 210, 255, (int) (90 + 120 * pulso)));
            j.fillRect(x - tam / 2, y - tam / 2, tam, tam);
            j.setColor(new Color(255, 255, 255, (int) (140 + 115 * pulso)));
            j.fillRect(x - BLOCO, y - BLOCO, BLOCO * 2, BLOCO * 2);
        }
        j.dispose();
    }

    private static int bloco(double v) { return ((int) Math.floor(v)) / BLOCO * BLOCO; }

    /** Espaco livre: pixels escuros ligados as bordas (os escuros dentro da nave/logo ficam de fora). */
    static BufferedImage mascara(Image arte, Rectangle... fora) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(arte, 0, 0, W, H, null);
        g.dispose();
        boolean[] claro = new boolean[W * H];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int p = img.getRGB(x, y), rr = (p >> 16) & 255, gg = (p >> 8) & 255, bb = p & 255;
            double lum = .3 * rr + .59 * gg + .11 * bb;
            claro[y * W + x] = lum > 72 || rr > 150;          // nave, logo, planeta, botoes, chamas
        }
        // engrossa a frente 2 px para fechar frestas do contorno
        boolean[] frente = new boolean[W * H];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            if (!claro[y * W + x]) continue;
            for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
                int a = x + dx, b = y + dy;
                if (a >= 0 && a < W && b >= 0 && b < H) frente[b * W + a] = true;
            }
        }
        for (Rectangle f : fora) for (int y = Math.max(0, f.y); y < Math.min(H, f.y + f.height); y++)
            for (int x = Math.max(0, f.x); x < Math.min(W, f.x + f.width); x++) frente[y * W + x] = true;
        boolean[] livre = new boolean[W * H];
        ArrayDeque<Integer> fila = new ArrayDeque<>();
        for (int x = 0; x < W; x++) { semente(x, 0, frente, livre, fila); semente(x, H - 1, frente, livre, fila); }
        for (int y = 0; y < H; y++) { semente(0, y, frente, livre, fila); semente(W - 1, y, frente, livre, fila); }
        while (!fila.isEmpty()) {
            int i = fila.poll(), x = i % W, y = i / W;
            if (x > 0) semente(x - 1, y, frente, livre, fila);
            if (x < W - 1) semente(x + 1, y, frente, livre, fila);
            if (y > 0) semente(x, y - 1, frente, livre, fila);
            if (y < H - 1) semente(x, y + 1, frente, livre, fila);
        }
        BufferedImage m = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) if (livre[y * W + x]) m.setRGB(x, y, 0xFFFFFFFF);
        return m;
    }

    private static void semente(int x, int y, boolean[] frente, boolean[] livre, ArrayDeque<Integer> fila) {
        int i = y * W + x;
        if (frente[i] || livre[i]) return;
        livre[i] = true;
        fila.add(i);
    }

    BufferedImage mascaraTeste() { return mascara; }
}
