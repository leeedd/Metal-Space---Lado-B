/*
 * Decompiled with CFR 0.152.
 */
package Jogo;


import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

public final class FundoEspacial {
    private static final double[] VELOCIDADES = new double[]{31.0, 76.0, 150.0};
    private static final int[] QUANTIDADES = new int[]{62, 39, 21};
    private final Image cenarioAbismo = Assets.imagem("abismo_cenario_091.png");
    private final Image atlasUniversos = Assets.imagem("universos_096.png");
    private double chegada;
    private final Estrela[][] estrelas = new Estrela[3][];
    private final double[][] posicoesIniciais = new double[3][];
    private final Random sorte = new Random(2019L);
    private double rolagemTextura;
    private double tempo;
    private double progresso;
    private double destinoProgresso;
    private int setor;
    private final Planeta[] planetas = new Planeta[]{new Planeta(1140.0, 182, 63, new Color(42, 111, 191), new Color(22, 48, 101), false), new Planeta(1760.0, 435, 95, new Color(180, 93, 53), new Color(69, 30, 52), true), new Planeta(2410.0, 265, 55, new Color(79, 171, 146), new Color(19, 76, 109), false)};

    /** Planeta de anel em que a nave esta lutando (aparece ao fundo com os aneis), ou nenhum. */
    private int anelSetor = -1, anelPlaneta;
    private double anelVisivel;
    void definirPlaneta(int setor, int planeta, boolean anel) {
        if (anel) { anelSetor = setor; anelPlaneta = planeta; }
        anelVisivel = Math.max(0, Math.min(1, anelVisivel + (anel ? .02 : -.02)));
        if (anelVisivel == 0) anelSetor = -1;
    }
    /** Saturno de perto: o planeta grande no canto e os aneis de pedra cruzando o ceu, girando devagar. */
    private void desenharAneis(Graphics2D base) {
        if (anelSetor < 0 || anelVisivel <= 0) return;
        Graphics2D g = (Graphics2D) base.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float a = (float) (anelVisivel * .8);
        int cx = 790, cy = 470; double rx = 760, ry = 150, giro = -.26;
        Color cr = Turne.CORES_RISCO[Turne.risco(anelSetor, anelPlaneta)];
        // metade de tras do anel
        aneis(g, cx, cy, rx, ry, giro, cr, a, false);
        java.awt.image.BufferedImage pl = MapaEstelar.planeta(anelSetor, anelPlaneta);
        if (pl != null) {
            int s = 330; Graphics2D q = (Graphics2D) g.create();
            q.setComposite(AlphaComposite.SrcOver.derive(a));
            q.rotate(tempo * .02, cx, cy);
            q.drawImage(pl, cx - s / 2, cy - s / 2, s, s, null);
            // sombra do lado escuro, para ficar longe e nao brigar com o jogo
            q.setComposite(AlphaComposite.SrcOver.derive(a * .45f)); q.setColor(new Color(4, 4, 14)); q.fillOval(cx - s / 2 + s / 5, cy - s / 2 + s / 6, s, s);
            q.dispose();
        }
        // metade da frente, por cima do planeta
        aneis(g, cx, cy, rx, ry, giro, cr, a, true);
        g.dispose();
    }
    private void aneis(Graphics2D base, int cx, int cy, double rx, double ry, double giro, Color cr, float a, boolean frente) {
        Graphics2D g = (Graphics2D) base.create(); g.rotate(giro, cx, cy);
        // faixas: varias elipses finas, como as divisoes dos aneis de Saturno
        double[] raio = {.42, .5, .56, .66, .72, .84, .9};
        int[] alfa = {50, 80, 40, 95, 60, 70, 35};
        for (int k = 0; k < raio.length; k++) {
            int c = 120 + k * 12;
            g.setComposite(AlphaComposite.SrcOver.derive(a * alfa[k] / 255f * 1.6f));
            g.setColor(k % 3 == 1 ? cr : new Color(c, c - 12, c - 30));
            g.setStroke(new BasicStroke(k % 2 == 0 ? 7 : 4));
            int w = (int) (rx * raio[k] * 2), h = (int) (ry * raio[k] * 2);
            g.drawArc(cx - w / 2, cy - h / 2, w, h, frente ? 180 : 0, 180);
        }
        // pedras soltas no anel, girando
        Random r = new Random(anelSetor * 13 + anelPlaneta);
        g.setComposite(AlphaComposite.SrcOver.derive(a));
        for (int k = 0; k < 220; k++) {
            double f = .4 + r.nextDouble() * .52, ang = r.nextDouble() * Math.PI * 2 + tempo * (.05 / f);
            if ((Math.sin(ang) > 0) != frente) continue;
            int x = (int) (cx + Math.cos(ang) * rx * f), y = (int) (cy + Math.sin(ang) * ry * f), c = 100 + r.nextInt(110), t = 2 + r.nextInt(3);
            g.setColor(r.nextInt(5) == 0 ? cr : new Color(c, c - 10, c - 28)); g.fillRect(x, y, t, t);
        }
        g.dispose();
    }
    public void definirProgresso(double d, int n) {
        this.destinoProgresso = Math.max(0.0, Math.min(1.0, d));
        int novo = Math.max(0, Math.min(7, n));
        if (novo != this.setor) { this.setor = novo; this.progresso = 0; this.tempo = 0; this.chegada = .8; }
    }

    public FundoEspacial() {
        this(Assets.imagem("fundo.png"));
    }

    FundoEspacial(Image image) {
        for (int i = 0; i < this.estrelas.length; ++i) {
            this.estrelas[i] = new Estrela[QUANTIDADES[i]];
            this.posicoesIniciais[i] = new double[QUANTIDADES[i]];
            for (int j = 0; j < this.estrelas[i].length; ++j) {
                Estrela estrela = new Estrela();
                estrela.x = this.sorte.nextDouble() * 960.0;
                estrela.y = 72.0 + this.sorte.nextDouble() * 528.0;
                estrela.tamanho = i == 0 ? 1 : (i == 1 ? 2 : 2 + this.sorte.nextInt(2));
                estrela.faseBrilho = this.sorte.nextDouble() * Math.PI * 2.0;
                estrela.variacao = 0.65 + this.sorte.nextDouble() * 0.65;
                estrela.cor = this.sorte.nextInt(6) == 0 ? new Color(255, 224, 170) : (this.sorte.nextInt(4) == 0 ? new Color(136, 216, 255) : new Color(227, 240, 255));
                this.estrelas[i][j] = estrela;
                this.posicoesIniciais[i][j] = estrela.x;
            }
        }
    }

    public void atualizar(double d) {
        if (d <= 0.0) {
            return;
        }
        d = Math.min(0.05, d);
        this.tempo += d;
        this.chegada = Math.max(0, this.chegada - d);
        this.progresso += (this.destinoProgresso - this.progresso) * Math.min(1.0, d * 0.65);
        this.rolagemTextura += 19.0 * d;
        for (Planeta planeta : this.planetas) {
            planeta.x -= (7.0 + (double)planeta.raio * 0.06) * d;
            if (!(planeta.x < (double)(-planeta.raio * 3))) continue;
            planeta.x += 2800.0;
        }
        for (int i = 0; i < this.estrelas.length; ++i) {
            for (Estrela estrela : this.estrelas[i]) {
                estrela.x -= VELOCIDADES[i] * estrela.variacao * d;
                if (!(estrela.x < -26.0)) continue;
                estrela.x += 1012.0;
            }
        }
    }

    /** Degrade do fim do Abismo: 16 faixas de 10 px com transparencia min(1, n/6), como era desenhado quadro a quadro. */
    private BufferedImage degradeAbismo;

    private BufferedImage montarDegradeAbismo(int n6, int n7, int altura) {
        BufferedImage b = new BufferedImage(160, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int n = 0; n < 16; ++n) {
            Graphics2D faixa = (Graphics2D) g.create();
            faixa.clipRect(n * 10, 0, 10, altura);
            faixa.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, Math.min(1.0f, n / 6.0f)));
            faixa.drawImage(this.cenarioAbismo, 0, 0, 160, altura, n6 * 87 / 100, 0, n6, n7, null);
            faixa.dispose();
        }
        g.dispose();
        return b;
    }

    public void desenhar(Graphics2D graphics2D) {
        int n;
        int n2;
        Graphics2D graphics2D2 = (Graphics2D)graphics2D.create();
        graphics2D2.clipRect(0, 72, 960, 528);
        graphics2D2.setColor(Color.BLACK);
        graphics2D2.fillRect(0, 72, 960, 528);
        int n3 = setor == 0 ? 1440 : 960;
        int n4 = 528;
        int n5 = (int)(this.tempo * 38.0) % (n3 * 2);
        graphics2D2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Image cenario = this.setor == 0 ? this.cenarioAbismo : this.atlasUniversos;
        int n6 = cenario.getWidth(null), n7 = cenario.getHeight(null);
        int sx = setor == 0 ? 0 : (setor % 4) * n6 / 4;
        int sy = setor == 0 ? 0 : (setor / 4) * n7 / 2;
        int sw = setor == 0 ? n6 * 3 / 4 : n6 / 4;
        int sh = setor == 0 ? n7 : sw * 528 / 960;
        if (setor != 0) sy += (n7 / 2 - sh) / 2;
        // blocos do cenario ja escalados (cache): so os que aparecem na tela; os impares sao espelhados
        for (int i = -1; i <= 2; ++i) {
            n2 = i * n3 - n5;
            if (n2 >= 960 || n2 + n3 <= 0) continue;
            if (Math.floorMod(i, 2) == 0) CacheEscala.desenhar(graphics2D2, cenario, n2, 72, n3, 528, sx, sy, sx + sw, sy + sh, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            else CacheEscala.desenhar(graphics2D2, cenario, n2, 72, n3, 528, sx + sw, sy, sx, sy + sh, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        }
        float f = (float)Math.max(0.0, Math.min(1.0, (this.progresso - 0.38) / 0.62));
        graphics2D2.setComposite(AlphaComposite.getInstance(3, f * 0.48f));
        graphics2D2.setColor(new Color(43 + this.setor % 3 * 15, 0, 48));
        graphics2D2.fillRect(0, 72, 960, n4);
        graphics2D2.setComposite(AlphaComposite.getInstance(3, f));
        n2 = (int)(1080.0f - f * 250.0f);
        // fim do Abismo: as 16 faixas com transparencia crescente ja montadas numa imagem so (antes eram 16 desenhos por quadro)
        if (setor == 0 && f > 0) {
            if (this.degradeAbismo == null) this.degradeAbismo = montarDegradeAbismo(n6, n7, n4);
            graphics2D2.drawImage(this.degradeAbismo, n2, 72, null);
        }
        graphics2D2.setComposite(AlphaComposite.SrcOver);
        desenharAneis(graphics2D2);
        graphics2D2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (n = 0; n < this.estrelas.length; ++n) {
            for (Estrela estrela : this.estrelas[n]) {
                double d = 0.73 + 0.27 * Math.sin(this.tempo * (1.0 + (double)n * 0.6) + estrela.faseBrilho);
                float f2 = (float)Math.max(0.2, Math.min(0.95, d * (0.5 + (double)n * 0.17)));
                graphics2D2.setComposite(AlphaComposite.getInstance(3, f2));
                graphics2D2.setColor(estrela.cor);
                int n8 = (int)Math.round(estrela.x);
                int n9 = (int)Math.round(estrela.y);
                if (n == 2) {
                    graphics2D2.setStroke(new BasicStroke(estrela.tamanho >= 3 ? 2.0f : 1.0f));
                    int n10 = (int)(7.0 + VELOCIDADES[n] / 18.0);
                    graphics2D2.drawLine(n8, n9, n8 + n10, n9);
                }
                graphics2D2.fillOval(n8, n9, estrela.tamanho, estrela.tamanho);
            }
        }
        if (chegada > 0) {
            graphics2D2.setComposite(AlphaComposite.getInstance(3, (float)(chegada / .8)));
            graphics2D2.setColor(new Color(190,234,255));graphics2D2.fillRect(0,72,960,528);
        }
        graphics2D2.dispose();
    }


    public void reiniciar() {
        this.setor = 0; this.chegada = 0;
        this.rolagemTextura = 0.0;
        this.planetas[0].x = 1140.0;
        this.planetas[1].x = 1760.0;
        this.planetas[2].x = 2410.0;
        this.tempo = 0.0;
        this.progresso = 0.0;
        this.destinoProgresso = 0.0;
        for (int i = 0; i < this.estrelas.length; ++i) {
            for (int j = 0; j < this.estrelas[i].length; ++j) {
                this.estrelas[i][j].x = this.posicoesIniciais[i][j];
            }
        }
    }

    double deslocamentoTeste() {
        return this.rolagemTextura;
    }

    double estrelaTeste(int n, int n2) {
        return this.estrelas[n][n2].x;
    }

    private static final class Estrela {
        double x;
        double y;
        double faseBrilho;
        double variacao;
        int tamanho;
        Color cor;

        private Estrela() {
        }
    }

    private static final class Planeta {
        double x;
        final int raio;

        Planeta(double d, int n, int n2, Color color, Color color2, boolean bl) {
            this.x = d;
            this.raio = n2;
        }

    }
}

