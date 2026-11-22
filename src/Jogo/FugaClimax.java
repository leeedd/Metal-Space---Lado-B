package Jogo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Climax da fuga: perto da saida uma gargula colossal pula da lava, agarra a nave e afunda com ela.
 * Dentro da cabine tudo falha (luzes piscando, controles mortos, sistemas apagando). Aparece o botao da dobra:
 * o jogador tem 5 s para socar o botao. Mesmo acertando, o painel apaga, a lava cobre o vidro e tudo fica preto
 * (o jogador acha que morreu). Corte: a nave explode para fora da lava, vai de frente para o portal, e de volta a
 * cabine entra em dobra; dai a camera nao sai mais da cabine ate a dobra estabilizar. Sem apertar: o casco rompe.
 * Cenas externas usam climax_1..4.png e braco_3_dobra.png quando chegarem; ate la, desenho com a arte que existe.
 */
final class FugaClimax {
    static final int SUBIDA = 0, PULO = 1, AFUNDA = 2, FALHAS = 3, BOTAO = 4, APAGA = 5, PRETO = 6, EXPLODE = 7, RUMO = 8, PORTAL = 9, TUNEL = 10, RUPTURA = 11;
    static final double[] DUR = {2.6, 1.8, 2.4, 3.0, 5.0, 3.2, 3.0, 2.6, 2.4, 1.8, 8.0, 2.6};
    private static final int W = 960, H = 600, LY = 522;
    private static final BufferedImage CL1 = ler("climax_1_subida.png"), CL2 = ler("climax_2_pulo.png"), CL2B = ler("climax_2b_agarra.png"), SOCO = ler("maos_soco.png"), CL3 = ler("climax_3_afunda.png"), CL4 = ler("climax_4_explosao.png"), DOBRA_EXT = ler("braco_3_dobra.png"),
        FUNDO = ler("inferno_fundo.png"), LAVA = ler("inferno_lava.png"), GARG = ler("demonio_gargula.png"), MAO = ler("gargula_mao.png"), TUNEL_ARTE = ler("hiperespaco_tunel.png"), PORTAL_ARTE = ler("portal_dobra.png");
    private final FugaFinal f; int estado = SUBIDA; double e, t; private double soco = -1; private final Random rnd = new Random(7);
    FugaClimax(FugaFinal f) { this.f = f; f.objsLimpar(); }

    boolean esperandoBotao() { return estado == BOTAO && soco < 0; }
    /** ESPACO: o soco no botao da dobra (so vale na janela de 5 s). */
    void ativar() { if (estado == BOTAO && soco < 0) { soco = 0; SomJogo.tocar(SomJogo.Efeito.IMPACTO); } }

    void atualizar(double dt) {
        e += dt; t += dt; double antes = e - dt;
        if (estado == SUBIDA && antes == 0) f.falar("A SAIDA ESTA LOGO ALI!");
        if (estado == PULO && antes == 0) { SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); SomJogo.tocar(SomJogo.Efeito.TROVAO); }
        if (estado == AFUNDA && (int) (e * 3) != (int) (antes * 3)) SomJogo.tocar(SomJogo.Efeito.DANO);
        if (estado == FALHAS || estado == BOTAO) { // tudo falhando: sistemas apagando, controles mortos, alarme
            f.alarme = 1; if ((int) (e * 2.5) != (int) (antes * 2.5)) { int s = rnd.nextInt(6); f.ferir(s, 12 + rnd.nextInt(14)); SomJogo.tocar(SomJogo.Efeito.ALERTA); }
            f.sist[FugaFinal.ESTRUTURA] = Math.max(4, f.sist[FugaFinal.ESTRUTURA] - dt * 6); f.manX = (rnd.nextDouble() - .5) * 1.6; f.manY = (rnd.nextDouble() - .5) * .8; f.potencia = Math.max(0, f.potencia - dt * .3); f.escudo = 0;
            if (estado == FALHAS && antes == 0) f.falar("FALHA GERAL! CONTROLES SEM RESPOSTA!");
            if (estado == BOTAO && antes == 0) { f.falar("DOBRA PRONTA. ATIVE AGORA!"); f.dobra = 100; SomJogo.tocar(SomJogo.Efeito.INTERDICAO); }
        }
        if (soco >= 0) { soco += dt; if (soco > .45 && estado == BOTAO) { trocar(APAGA); SomJogo.tocar(SomJogo.Efeito.ESCUDO_QUEBRA); } }
        if (estado == EXPLODE && antes == 0) { SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); SomJogo.tocar(SomJogo.Efeito.FUSAO); }
        if (estado == RUMO && antes == 0) SomJogo.tocar(SomJogo.Efeito.DOBRA);
        if (estado == PORTAL && antes == 0) f.falar("ENTRANDO EM DOBRA ESPACIAL!");
        if (estado == TUNEL) { // turbulencia que vai acalmando, e o computador de bordo
            f.alarme = e < 4 ? .5 : 0; f.vel = 48; f.potencia = f.potenciaMax(); f.dobra = 100;
            if (antes < .2 && e >= .2) f.falar("INSTABILIDADE DIMENSIONAL DETECTADA!");
            if (antes < 2.4 && e >= 2.4) f.falar("SAINDO DA INFLUENCIA GRAVITACIONAL DO BLACK HOLE!");
            if (antes < 5.0 && e >= 5.0) { f.falar("BLACK HOLE SUPERADO."); SomJogo.tocar(SomJogo.Efeito.SAIDA_DOBRA); }
            if (antes < 6.6 && e >= 6.6) f.falar("DOBRA ESPACIAL ESTABILIZADA.");
        }
        if (estado == RUPTURA && antes == 0) { SomJogo.tocar(SomJogo.Efeito.DERROTA); f.falar("CASCO ROMPIDO..."); }
        if (e >= DUR[estado]) {
            if (estado == BOTAO) trocar(RUPTURA); // nao apertou a tempo
            else if (estado == RUPTURA) f.falhouClimax();
            else if (estado == TUNEL) f.terminou = true;
            else trocar(estado == APAGA ? PRETO : estado + 1);
        }
    }
    private void trocar(int s) { estado = s; e = 0; }

    // ---------------- desenho ----------------
    void desenhar(Graphics2D g, Nave nave, FugaCockpit cabine) {
        Graphics2D v = (Graphics2D) g.create(); double tr = tremor(); v.translate((rnd.nextDouble() - .5) * tr, (rnd.nextDouble() - .5) * tr);
        switch (estado) {
            case SUBIDA: if (CL1 != null) imagem(v, CL1, 1 + e * .02); else FugaFinal.pixel(v, lg -> subida(lg, nave)); break;
            case PULO: if (CL2 != null) imagem(v, CL2B != null && e > 1.0 ? CL2B : CL2, 1.05 + e * .03); else FugaFinal.pixel(v, lg -> pulo(lg, nave)); if (CL2 != null && e > 1.0 && e < 1.2) flash(v, (1.2 - e) / .2 * .6); break; // estica a mao... e agarra
            case AFUNDA: if (CL3 != null) { double k = e / DUR[AFUNDA]; Graphics2D a = (Graphics2D) v.create(); a.translate(0, -k * k * 60); imagem(a, CL3, 1.05 + k * .06); a.dispose(); lavaEm(v, (int) (H + 40 - k * k * 330)); } else FugaFinal.pixel(v, lg -> afunda(lg, nave)); break; // a camera desce com a nave: a lava sobe por cima
            case FALHAS: case BOTAO: case APAGA: case RUPTURA: cabineAfundando(v, cabine); break;
            case PRETO: v.setColor(Color.BLACK); v.fillRect(-20, -20, W + 40, H + 40); break;
            case EXPLODE: if (CL4 != null) { imagem(v, CL4, 1.25 - Math.min(1, e / 1.2) * .25); } else FugaFinal.pixel(v, lg -> explode(lg, nave)); flash(v, 1 - e / .5); break;
            case RUMO: if (DOBRA_EXT != null) imagem(v, DOBRA_EXT, 1 + e * .25); else FugaFinal.pixel(v, lg -> rumo(lg)); break;
            case PORTAL: case TUNEL: cabineDobra(v, cabine); break;
        }
        v.dispose();
        if (estado == BOTAO && soco < 0) prompt(g);
        if (estado == PORTAL && e > DUR[PORTAL] - .5) flash(g, (e - DUR[PORTAL] + .5) / .5);
        if (estado == TUNEL) flash(g, 1 - e / .6);
    }
    private double tremor() {
        switch (estado) { case PULO: return 16; case AFUNDA: return 10 + e * 4; case FALHAS: return 9; case BOTAO: return 11 + e * 2; case APAGA: return Math.max(0, 14 - e * 6); case EXPLODE: return Math.max(4, 22 - e * 8); case RUMO: return 6;
            case PORTAL: return 10 + e * 8; case TUNEL: return Math.max(1, 20 * (1 - e / DUR[TUNEL]) * (1 - e / DUR[TUNEL]) + 1); case RUPTURA: return 18; default: return 2; }
    }
    // ---- cenas externas desenhadas (ate a arte chegar) ----
    private void cenario(Graphics2D g) {
        if (FUNDO != null) { int w = FUNDO.getWidth() * (LY + 40) / FUNDO.getHeight(), off = (int) (t * 60) % w; for (int x = -off; x < W; x += w) g.drawImage(FUNDO, x, 0, w, LY + 40, null); }
        else { g.setColor(new Color(60, 10, 10)); g.fillRect(0, 0, W, H); }
    }
    private void lava(Graphics2D g) {
        if (LAVA != null) { int fh = LAVA.getHeight() / 4, q = (int) (t * 6) % 4, y0 = LY - 50; g.drawImage(LAVA, 0, y0, W, Math.max(H, y0 + fh), 0, q * fh, LAVA.getWidth(), q * fh + fh, null); }
        else { g.setColor(new Color(255, 120, 20)); g.fillRect(0, LY, W, H - LY); }
    }
    private void bolhas(Graphics2D g, double x, double k) { for (int i = 0; i < 5; i++) { int r = (int) (10 + k * 30 * (1 + i % 3) / 2.0 + Math.sin(t * 9 + i) * 4); g.setColor(new Color(255, 180, 60, 220)); g.fillOval((int) x - 120 + i * 50 - r / 2, LY - r / 2 + 8, r, r); } }
    private void gargula(Graphics2D g, double cx, double cy, int w, int q) {
        if (GARG == null) { g.setColor(new Color(60, 55, 65)); g.fillOval((int) cx - w / 2, (int) cy - w / 2, w, w); return; }
        int fw = GARG.getWidth() / 4, fh = GARG.getHeight(), h = w * fh / fw; g.drawImage(GARG, (int) cx - w / 2, (int) cy - h / 2, (int) cx + w / 2, (int) cy + h / 2, q * fw, 0, q * fw + fw, fh, null);
    }
    // a nave sobe de bico para cima (o cenario desce); a gargula vem de TRAS e de baixo, pula da lava e agarra a cauda
    private static final double ANG = -.75; // bico para cima e para a frente
    private double subY() { return estado == SUBIDA ? e * 70 : DUR[SUBIDA] * 70 + (estado == PULO ? e * 30 : 0); } // quanto o cenario ja desceu
    private void cenarioSubindo(Graphics2D g, double desce) {
        if (FUNDO != null) { int h = (int) (H * 1.2), w = FUNDO.getWidth() * h / FUNDO.getHeight(), y0 = -h + H + (int) Math.min(80, desce * .35), off = (int) (t * 20) % w; // o paredao desce devagar: a nave sobe
            for (int x = -off; x < W; x += w) g.drawImage(FUNDO, x, y0, w, h, null); }
        else { g.setColor(new Color(60, 10, 10)); g.fillRect(0, 0, W, H); }
        g.setColor(new Color(255, 200, 120, 110)); Random r = new Random(4); for (int i = 0; i < 46; i++) { int x = r.nextInt(W), y = (int) ((r.nextInt(H) + t * 900) % H); g.fillRect(x, y, 2, 30); } // riscos de velocidade descendo
    }
    private void lavaEm(Graphics2D g, int y) { if (LAVA != null) { int fh = LAVA.getHeight() / 4, q = (int) (t * 6) % 4; g.drawImage(LAVA, 0, y - 50, W, Math.max(H + 300, y - 50 + fh), 0, q * fh, LAVA.getWidth(), q * fh + fh, null); } else { g.setColor(new Color(255, 120, 20)); g.fillRect(0, y, W, H + 300); } }
    private static final double NX = 600, NY = 230, NE = 2.2; // a nave subindo, no alto e a direita
    private void subida(Graphics2D g, Nave nave) {
        double d = subY(); cenarioSubindo(g, d); int ly = 545 + (int) Math.min(30, d * .15); lavaEm(g, ly); // a lava ficando para tras, embaixo
        naveEm(g, nave, NX + Math.sin(t * 2) * 10, NY + Math.sin(t * 3) * 8, NE, ANG, 0);
        if (e > 1.2) bolhas(g, 330, Math.min(1, (e - 1.2) / 1.2)); // atras e embaixo da nave a lava comeca a inchar
    }
    private void gargulaVirada(Graphics2D g, double cx, double cy, int w, int q, double ang) { // a folha olha para a esquerda: espelha para olhar a nave (direita)
        Graphics2D a = (Graphics2D) g.create(); a.translate(cx, cy); a.rotate(ang); a.scale(-1, 1); gargula(a, 0, 0, w, q); a.dispose();
    }
    private void pulo(Graphics2D g, Nave nave) {
        double d = subY(); cenarioSubindo(g, d); int ly = 575; double k = Math.min(1, e / .7), kk = 1 - (1 - k) * (1 - k);
        double puxa = k >= 1 ? (e - .7) / (DUR[PULO] - .7) : 0, sx = NX - puxa * 40, sy = NY + puxa * 50; // agarrada: comeca a ser puxada para tras e para baixo
        double gx = 250 + kk * 110, gy = ly + 280 - kk * 300; // de tras e de baixo, saltando da lava
        lavaEm(g, ly); gargulaVirada(g, gx, gy, 520, k < 1 ? 3 : 0, -.4 * (1 - k) - .15);
        double mx = gx + (sx - gx) * Math.min(1, e / .7), my = gy - 60 + (sy - gy + 60) * Math.min(1, e / .7); // a mao vem de tras ate a nave
        braco(g, gx + 110, gy - 50, mx, my, false); naveEm(g, nave, sx + Math.sin(t * 30) * (k >= 1 ? 5 : 0), sy, NE, ANG + puxa * .5, 0); if (k >= 1) braco(g, gx + 110, gy - 50, sx, sy, true); // fechada em volta do corpo da nave
        for (int i = 0; i < 26; i++) { double a = -Math.PI / 2 + (i - 13) * .1, dd = kk * (200 + i * 9 % 80); g.setColor(i % 3 == 0 ? new Color(255, 230, 120) : new Color(255, 120, 20)); int r = 10 + i % 4 * 5; g.fillOval((int) (gx + Math.cos(a) * dd), (int) (ly + Math.sin(a) * dd + e * e * 260), r, r); } // a lava explode atras da nave
    }
    /** Braco de pedra saindo da gargula; fechada = os dedos por cima do meio da nave. */
    private void braco(Graphics2D g, double ox, double oy, double mx, double my, boolean fechada) {
        Graphics2D a = (Graphics2D) g.create(); a.setStroke(new BasicStroke(1));
        if (!fechada) { a.setColor(new Color(45, 40, 52)); a.setStroke(new BasicStroke(46, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); a.drawLine((int) ox, (int) oy, (int) mx - 20, (int) my + 30);
            a.setColor(new Color(255, 120, 30)); a.setStroke(new BasicStroke(3)); a.drawLine((int) ox + 10, (int) oy - 8, (int) mx - 30, (int) my + 18); a.dispose(); return; }
        for (int i = 0; i < 4; i++) { int fx = (int) mx - 50 + i * 26, fy = (int) my - 30 + Math.abs(i - 1) * 6; a.setColor(new Color(45, 40, 52)); a.fillRoundRect(fx, fy, 30, 74, 18, 18); a.setColor(new Color(70, 64, 80)); a.fillRoundRect(fx + 4, fy + 4, 14, 30, 10, 10);
            a.setColor(new Color(255, 120, 30)); a.drawLine(fx + 8, fy + 40, fx + 20, fy + 60); a.setColor(new Color(220, 210, 190)); a.fillPolygon(new int[]{fx + 6, fx + 24, fx + 15}, new int[]{fy + 70, fy + 70, fy + 92}, 3); } // os dedos e garras por cima do corpo da nave
        a.dispose();
    }
    private void afunda(Graphics2D g, Nave nave) {
        double d = subY(); double k = e / DUR[AFUNDA], kk = k * k; cenarioSubindo(g, d); int ly = (int) (575 - kk * 300); // a camera acompanha para baixo: a lava sobe na tela
        double sx = NX - 40 - k * 120, sy = NY + 50 + kk * 260, gx = 360 - k * 100, gy = 595 + kk * 200;
        gargulaVirada(g, gx, gy, 520, 0, -.15);
        braco(g, gx + 110, gy - 50, sx, sy, false); naveEm(g, nave, sx, sy + Math.sin(t * 34) * 5, NE, ANG + .5 + Math.sin(t * 20) * .1, 0); braco(g, gx + 110, gy - 50, sx, sy, true);
        for (int i = 0; i < 8; i++) { g.setColor(new Color(255, 220, 120)); int x = (int) sx - 30 + rnd.nextInt(80), y = (int) sy - 10 + rnd.nextInt(30); g.drawLine(x, y, x + rnd.nextInt(20) - 10, y - rnd.nextInt(20)); } // faiscas
        lavaEm(g, ly); // a lava por cima: os dois afundam
    }
    private void explode(Graphics2D g, Nave nave) {
        cenario(g); lava(g); double k = Math.min(1, e / 1.0), cx = 480;
        for (int i = 0; i < 40; i++) { double a = -Math.PI / 2 + (i - 20) * .05, d = k * (260 + (i * 37) % 200); g.setColor(i % 4 == 0 ? new Color(255, 240, 160) : i % 2 == 0 ? new Color(255, 150, 30) : new Color(220, 70, 10)); int r = 14 + i % 5 * 7; g.fillOval((int) (cx + Math.cos(a) * d * 1.3) - r / 2, (int) (LY + Math.sin(a) * d + e * e * 120) - r / 2, r, r); } // coluna de lava
        for (int i = 0; i < 10; i++) { double a = -Math.PI / 2 + (i - 5) * .3, d = e * (300 + i * 20); g.setColor(new Color(50, 45, 55)); int r = 18 + i % 3 * 8; g.fillRect((int) (cx + Math.cos(a) * d) - r / 2, (int) (LY + Math.sin(a) * d + e * e * 200) - r / 2, r, r); } // pedacos da mao
        double p = Math.min(1, e / 1.6), sx = cx + p * 260, sy = LY - 20 - (1 - (1 - p) * (1 - p)) * 290; naveEm(g, nave, sx, sy, 1.6, -.5, 1);
    }
    private void rumo(Graphics2D g) { // de tras da nave: o portal abrindo de frente
        g.setColor(new Color(40, 6, 10)); g.fillRect(0, 0, W, H); portal(g, W / 2, 250, 40 + e * 120, 1);
        double k = e / DUR[RUMO]; int s = (int) (90 * (1 - k * .7)), cx = W / 2, cy = (int) (430 - k * 160); // a nave de costas, encolhendo para dentro do portal
        g.setColor(new Color(150, 220, 255, 160)); g.fillOval(cx - s, cy - s / 3, s * 2, s * 2 / 3); g.setColor(new Color(220, 225, 235)); g.fillPolygon(new int[]{cx - s, cx, cx + s, cx}, new int[]{cy + s / 5, cy - s / 3, cy + s / 5, cy + s / 8}, 4);
        g.setColor(new Color(80, 170, 255)); g.fillOval(cx - s / 3 - 6, cy + s / 10, 12, 12); g.fillOval(cx + s / 3 - 6, cy + s / 10, 12, 12);
    }
    private static void portal(Graphics2D g, int cx, int cy, double r, double forca) {
        for (int k = 7; k >= 0; k--) { double rr = r * (1 + k * .22); g.setColor(k % 2 == 0 ? new Color(90, 140, 255, (int) (180 * forca)) : new Color(170, 80, 255, (int) (160 * forca))); g.setStroke(new BasicStroke(6 + k)); g.drawOval((int) (cx - rr), (int) (cy - rr * .8), (int) (rr * 2), (int) (rr * 1.6)); }
        g.setStroke(new BasicStroke(1)); g.setPaint(new RadialGradientPaint(cx, cy, (float) Math.max(2, r), new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, (int) (250 * forca)), new Color(150, 200, 255, 0)})); g.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
    }
    private void imagem(Graphics2D g, BufferedImage im, double z) { int w = (int) (W * z), h = (int) (H * z); g.drawImage(im, (W - w) / 2, (H - h) / 2, w, h, null); }
    private static void flash(Graphics2D g, double a) { if (a <= 0) return; g.setColor(new Color(255, 255, 255, (int) (255 * Math.min(1, a)))); g.fillRect(-20, -20, W + 40, H + 40); }
    private void naveEm(Graphics2D g, Nave nave, double cx, double cy, double esc, double ang, double aura) {
        if (aura > 0) { float r = (float) (80 * esc); g.setPaint(new RadialGradientPaint((float) cx, (float) cy, r, new float[]{0f, .5f, 1f}, new Color[]{new Color(255, 255, 255, 230), new Color(90, 170, 255, 150), new Color(40, 80, 255, 0)})); g.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
            g.setColor(new Color(120, 200, 255, 170)); for (int i = 0; i < 6; i++) g.fillRect((int) (cx - 60 * esc - i * 40), (int) (cy - 6 + i * 3 - 8), (int) (50 * esc), 4); }
        if (nave == null) { g.setColor(Color.WHITE); g.fillPolygon(new int[]{(int) cx - 40, (int) cx + 40, (int) cx - 40}, new int[]{(int) cy - 15, (int) cy, (int) cy + 15}, 3); return; }
        Graphics2D a = (Graphics2D) g.create(); a.translate(cx, cy); a.rotate(ang); a.scale(esc, esc); a.translate(-(nave.getX() + 44), -(nave.getY() + 28)); nave.desenhar(a); a.dispose();
    }
    // ---- dentro da cabine ----
    private void cabineAfundando(Graphics2D g, FugaCockpit cabine) {
        double nivel; // quanto da janela a lava ja cobriu (0 a 1)
        if (estado == FALHAS) nivel = .15 + .2 * e / DUR[FALHAS]; else if (estado == BOTAO) nivel = .35 + .35 * e / DUR[BOTAO]; else if (estado == APAGA) nivel = .7 + .3 * Math.min(1, e / 1.4); else nivel = .7 + .3 * Math.min(1, e / .8);
        final double niv = nivel; FugaFinal.pixel(g, lg -> { lg.setColor(new Color(50, 8, 8)); lg.fillRect(0, 0, W, H);
            if (MAO != null) { int fw = MAO.getWidth() / 3, fh = MAO.getHeight(), q = estado == FALHAS ? 0 : estado == BOTAO ? 1 : 2; lg.drawImage(MAO, 0, -10, W, 380, q * fw, 0, q * fw + fw, fh, null); }
            else garras(lg);
            lavaNoVidro(lg, niv); });
        f.semMaoDireita = soco >= 0; cabine.desenhar(g, f);
        if (estado == FALHAS || estado == BOTAO) { if (rnd.nextDouble() < .35) { g.setColor(new Color(0, 0, 0, 170 + rnd.nextInt(80))); g.fillRect(0, 330 + rnd.nextInt(60), W, H); } if (rnd.nextDouble() < .15) { g.setColor(new Color(0, 0, 0, 200)); g.fillRect(0, 0, W, H); } } // luzes piscando, painel morrendo
        if (estado == BOTAO) botao(g, cabine);
        if (estado == APAGA) { double a = Math.min(1, e / .25); g.setColor(new Color(0, 0, 0, (int) (200 * a))); g.fillRect(0, 330, W, H); botao(g, cabine); // o painel apaga de uma vez; o punho continua no botao
            double fim = Math.max(0, (e - 1.6) / (DUR[APAGA] - 1.6)); g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, fim)))); g.fillRect(-20, -20, W + 40, H + 40); } // a lava envolve tudo e vai esmaecendo
        if (estado == RUPTURA) { double k = e / DUR[RUPTURA]; g.setColor(new Color(255, 60, 20, (int) (160 * Math.min(1, k * 2)))); g.fillRect(0, 0, W, H); g.setColor(new Color(0, 0, 0, (int) (255 * Math.max(0, (k - .5) * 2)))); g.fillRect(-20, -20, W + 40, H + 40); }
    }
    private void garras(Graphics2D g) {
        for (int i = 0; i < 4; i++) { boolean esq = i < 2; int bx = esq ? 120 + i * 140 : 600 + (i - 2) * 160, by = -30 + (i % 2) * 20; Polygon p = new Polygon(new int[]{bx - 70, bx + 70, bx + 40, bx + 10, bx - 30}, new int[]{by, by, by + 170, by + 250, by + 170}, 5);
            g.setColor(new Color(40, 36, 44)); g.fillPolygon(p); g.setColor(new Color(255, 120, 30)); g.setStroke(new BasicStroke(3)); g.drawLine(bx - 30, by + 40, bx + 10, by + 110); g.drawLine(bx + 10, by + 110, bx - 10, by + 160); g.setStroke(new BasicStroke(1));
            g.setColor(new Color(220, 210, 190)); g.fillPolygon(new int[]{bx - 8, bx + 22, bx + 10}, new int[]{by + 230, by + 230, by + 300}, 3); }
    }
    private void lavaNoVidro(Graphics2D g, double nivel) {
        int topo = 360 - (int) (nivel * 340); Polygon p = new Polygon(); p.addPoint(0, H); for (int x = 0; x <= W; x += 24) p.addPoint(x, topo + (int) (Math.sin(x * .03 + t * 4) * 12 + Math.sin(x * .011 - t * 2) * 10)); p.addPoint(W, H);
        g.setPaint(new java.awt.GradientPaint(0, topo, new Color(255, 200, 60), 0, topo + 160, new Color(170, 30, 0))); g.fillPolygon(p);
        g.setColor(new Color(255, 240, 150)); for (int i = 0; i < p.npoints - 3; i++) g.fillRect(p.xpoints[i + 1], p.ypoints[i + 1] - 2, 24, 4); // crista brilhando escorrendo no vidro
    }
    /** O botao da dobra no meio do painel e o soco da mao direita. */
    private void botao(Graphics2D g, FugaCockpit cabine) {
        int bx = 480, by = 555; double pulsa = .5 + .5 * Math.sin(t * 14);
        g.setColor(new Color(20, 20, 20)); g.fillOval(bx - 52, by - 24, 104, 48); for (int i = 0; i < 12; i++) { g.setColor(i % 2 == 0 ? new Color(240, 200, 30) : Color.BLACK); g.fillArc(bx - 48, by - 21, 96, 42, i * 30, 30); }
        boolean afundado = soco > .18; g.setColor(new Color(120, 0, 0)); g.fillOval(bx - 30, by - 14 + (afundado ? 6 : 0), 60, 26); g.setColor(new Color(230, 30 + (int) (60 * pulsa), 20)); g.fillOval(bx - 30, by - 22 + (afundado ? 8 : 0), 60, 26);
        if (soco >= 0 && SOCO != null) { int fw = SOCO.getWidth() / 2, fh = SOCO.getHeight(), t0 = 200; // a mao DIREITA larga o manche, ergue o punho e esmaga o botao (arte espelhada: a manga vem da direita)
            int mx = W - 165 - FugaCockpit.MAO_X, my = H - 165 + 30, ex = bx + 20, ey = by - 250; // do manche ate o alto, e dali para o botao
            if (!afundado) { double k = Math.min(1, soco / .18); int x, y; if (k < .45) { double q = k / .45; x = (int) (mx + (ex - mx) * q); y = (int) (my + (ey - my) * q); } else { double q = (k - .45) / .55; x = (int) (ex + (bx - (int) (t0 * .42) - ex) * q); y = (int) (ey + (by - (int) (t0 * .84) + 6 - ey) * q * q); }
                if (k > .5) { Graphics2D r = (Graphics2D) g.create(); for (int i = 1; i < 4; i++) { r.setComposite(AlphaComposite.SrcOver.derive(.3f / i)); r.drawImage(SOCO, x + t0, y - i * 22, x, y - i * 22 + t0, 0, 0, fw, fh, null); } r.dispose(); } // rastro fraco da descida
                g.drawImage(SOCO, x + t0, y, x, y + t0, 0, 0, fw, fh, null); }
            else { int x = bx - (int) (t0 * .42), y = by - (int) (t0 * .84) + 6; g.setColor(new Color(214, 98, 22)); g.fillPolygon(new int[]{x + t0 - 2, x + t0 - 2, x + t0 + 90}, new int[]{y + (int) (t0 * .55), y + t0, y + t0 + 40}, 3); g.setColor(new Color(170, 70, 15)); g.drawLine(x + t0 + 60, y + t0 + 10, x + t0 - 2, y + (int) (t0 * .8)); // a manga continua ate a borda direita
                g.drawImage(SOCO, x + t0, y, x, y + t0, fw, 0, fw * 2, fh, null); }
            return; }
        if (soco >= 0) { double k = Math.min(1, soco / .18); int t0 = 190, x0 = W - 165 + 6, y0 = H - 135, x1 = bx - t0 / 2 + 10, y1 = by - t0 + 40 + (afundado ? 18 : 0); // a mao sai do manche e soca o botao
            cabine.maoDireita(g, 5, (int) (x0 + (x1 - x0) * k), (int) (y0 + (y1 - y0) * k), (int) (165 + (t0 - 165) * k));
            if (afundado && soco < .4) { g.setColor(new Color(255, 230, 120)); for (int i = 0; i < 10; i++) { double a = i * Math.PI / 5; g.drawLine(bx + (int) (Math.cos(a) * 40), by + (int) (Math.sin(a) * 20), bx + (int) (Math.cos(a) * 80), by + (int) (Math.sin(a) * 40)); } } }
    }
    private void prompt(Graphics2D g) {
        double rest = Math.max(0, DUR[BOTAO] - e); String s = "SOQUE O BOTAO DA DOBRA!  [ ESPACO ]";
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24)); FontMetrics fm = g.getFontMetrics(); int x = (W - fm.stringWidth(s)) / 2, y = 110;
        boolean pisca = (int) (t * 8) % 2 == 0; g.setColor(new Color(0, 0, 0, 200)); g.fillRect(x - 16, y - 30, fm.stringWidth(s) + 32, 64);
        g.setColor(pisca ? new Color(255, 230, 60) : new Color(255, 80, 40)); g.drawString(s, x, y);
        g.setColor(new Color(60, 20, 20)); g.fillRect(x, y + 12, fm.stringWidth(s), 12); g.setColor(rest < 1.5 ? new Color(255, 40, 30) : new Color(255, 200, 60)); g.fillRect(x, y + 12, (int) (fm.stringWidth(s) * rest / DUR[BOTAO]), 12);
    }
    private void cabineDobra(Graphics2D g, FugaCockpit cabine) {
        FugaFinal.pixel(g, g2 -> { Graphics2D gg = g2;
        gg.setColor(new Color(6, 6, 26)); gg.fillRect(-20, -20, W + 40, H + 40);
        if (estado == PORTAL && PORTAL_ARTE != null) { double k = e / DUR[PORTAL]; int fw = PORTAL_ARTE.getWidth() / 4, fh = PORTAL_ARTE.getHeight(), q = Math.min(3, (int) (k * 4)); double z = 1 + k * k * .35; int w = (int) (960 * z), h = (int) (w * fh / (double) fw);
            gg.drawImage(PORTAL_ARTE, 480 - w / 2, 200 - h / 2, 480 + w / 2, 200 + h / 2, q * fw, 0, q * fw + fw, fh, null); } // o portal abrindo pelo vidro, chegando mais perto
        else if (estado == PORTAL) { double k = e / DUR[PORTAL]; portal(gg, 480, 200, 30 + k * k * 520, 1); }
        else if (TUNEL_ARTE != null) { int fw = TUNEL_ARTE.getWidth() / 4, fh = TUNEL_ARTE.getHeight(), q = (int) (t * (14 - 9 * Math.min(1, e / DUR[TUNEL]))) % 4; /* gira rapido e vai acalmando */ gg.drawImage(TUNEL_ARTE, -40, -30, W + 40, 420, q * fw, 0, q * fw + fw, fh, null); }
        else { Random r = new Random(5); for (int i = 0; i < 140; i++) { double a = r.nextDouble() * Math.PI * 2 + t * (1 + r.nextDouble()), d = ((r.nextDouble() + t * 1.6) % 1); int x1 = 480 + (int) (Math.cos(a) * d * 700), y1 = 200 + (int) (Math.sin(a) * d * 450), x2 = 480 + (int) (Math.cos(a - .15) * d * 520), y2 = 200 + (int) (Math.sin(a - .15) * d * 340);
            gg.setColor(i % 3 == 0 ? new Color(255, 255, 255, 200) : i % 3 == 1 ? new Color(90, 150, 255, 200) : new Color(170, 90, 255, 200)); gg.setStroke(new BasicStroke((float) (1 + d * 5))); gg.drawLine(x1, y1, x2, y2); } gg.setStroke(new BasicStroke(1));
            gg.setPaint(new RadialGradientPaint(480, 200, 90, new float[]{0f, 1f}, new Color[]{Color.WHITE, new Color(120, 160, 255, 0)})); gg.fillOval(390, 110, 180, 180); }
        });
        f.semMaoDireita = false; f.empurrando = true; cabine.desenhar(g, f);
        if (estado == TUNEL && e < 4 && (int) (t * 10) % 3 == 0) { g.setColor(new Color(255, 255, 255, 60)); g.fillRect(0, 0, W, H); } // luzes piscando na turbulencia
    }
    private static BufferedImage ler(String n) { java.net.URL u = FugaClimax.class.getResource("/ImagemIcon/" + n); if (u == null) return null; try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; } }
}
