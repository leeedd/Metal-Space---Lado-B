package Jogo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Forma final do Malachar (o enxame se junta nele) (Citadela, Sinfonico): o Senhor do Eclipse, de guitarra. Riff (leque
 * de notas), eclipse que cai em cima da nave e explode em anel, coroa (espiral saindo do halo) e grito
 * (parede de som com brecha). Na furia, eclipse total: a tela escurece e so fica um circulo de luz em
 * volta da nave. Arte: senhor_eclipse.png (4 quadros, ja virado para a esquerda: parado, carregando,
 * grito, eclipse).
 */
final class SenhorEclipse {
    private static final BufferedImage ARTE = ler("senhor_eclipse.png");
    static boolean temArte() { return ARTE != null; }
    static BufferedImage quadro(int q) { return ARTE == null ? null : ARTE.getSubimage(q * (ARTE.getWidth() / 4), 0, ARTE.getWidth() / 4, ARTE.getHeight()); }
    private static final Color ROXO = new Color(150, 60, 230);
    private static final BufferedImage TREM = EnxameMalachar.trem(), ECLIPSE = ler("poder_eclipse.png"), MOLDURA = ler("poder_eclipse_total.png"), FUMACA = ler("palhaco_fumaca.png");
    private double boom;
    // trem maluco: atravessa uma faixa da tela buzinando (aviso antes), o ataque mais divertido do Malachar
    // rasante: 1 aviso (sobe e mira), 2 atravessa a tela voando, 3 volta pela direita
    private int voo; private double vooT, vooY, vooX, voltas, vvx, vvy;
    /** Asas (senhor_eclipse_asas.png, 4 quadros: asas abrindo, asas abertas, voando 1, voando 2), ja virado para a esquerda. */
    private static final BufferedImage ASAS = ler("senhor_eclipse_asas.png");
    private double tremX = -9999, tremY, tremAviso, esperaContato, sumido, tremAtaque; private List<TiroChefe> tiros_;
    private static final int W = Config.LARGURA, H = Config.ALTURA, TOPO = Config.HUD, ALT = 340;
    private double x = W + 40, y, tempo, entrada, recarga = 1.4, golpeT, flash, coroa, coroaT, escuro, eclipseX, eclipseY, eclipseT = -1, naveX = 100, naveY = 300, vidaFrac = 1;
    private int golpes, quadro;
    private boolean usouTotal;
    private final java.util.Random sorte = new java.util.Random(23);

    double entrada() { return entrada; }
    void vida(double f) { vidaFrac = f; }
    void ferido() { flash = .14; }

    void atualizar(double dt, int nx, int ny, Dificuldade dif, List<TiroChefe> tiros) {
        tiros_ = tiros;
        tempo += dt; flash = Math.max(0, flash - dt); esperaContato = Math.max(0, esperaContato - dt);
        if (tremAviso > 0 && (tremAviso -= dt) <= 0) { tremX = W + 40; SomJogo.tocar(SomJogo.Efeito.TROVAO); }
        if (tremX > -9000) { tremX -= 520 * dt; tremAtaque -= dt; // os palhacos atacam ao acaso enquanto o trem passa
            if (tremAtaque <= 0 && tremX > 40 && tremX < W - 60) { tremAtaque = .28 + sorte.nextDouble() * .3; palhacos(tiros_); }
            if (tremX < -260) tremX = -9999; } golpeT = Math.max(0, golpeT - dt); naveX = nx + 22; naveY = ny + 18;
        int fw = ARTE.getWidth() / 4, w = fw * ALT / ARTE.getHeight();
        entrada = Math.min(1, entrada + dt / 1.4);
        double baseX = W - w - 10, baseY = TOPO + (H - TOPO - ALT) / 2.0 + Math.sin(tempo * 1.1) * 26;
        if (voo == 0) { x = W + 40 - (W + 40 - baseX) * (1 - Math.pow(1 - entrada, 3)); y = baseY; }
        else { vooT += dt;
            if (voo == 1) { y += (baseY - 30 - y) * Math.min(1, dt * 4); if (vooT > .8) { voo = 2; vooT = 0; double ox = x + w / 2.0, oy = y + ALT / 2.0, v = vidaFrac < .4 ? 1050 : 850, a = Math.atan2(naveY - oy, naveX - ox); vvx = Math.cos(a) * v; vvy = Math.sin(a) * v; SomJogo.tocar(SomJogo.Efeito.DOBRA); } } // abre as asas e mira
            else if (voo == 2) { x += vvx * dt; y += vvy * dt; // mergulha voando em cima da nave, deixando notas para tras
                if ((int) (vooT * 12) != (int) ((vooT - dt) * 12)) tiros.add(new TiroChefe(x + w / 2.0, y + ALT * .5, 60, (sorte.nextBoolean() ? 1 : -1) * 140, false, TiroChefe.NOTA, TiroChefe.Movimento.ONDA).comCor(ROXO));
                if (x < -w - 40 || x > W + 40 || y < TOPO - ALT - 40 || y > H + 40) { if (--voltas > 0) { voo = 1; vooT = 0; x = W + 40; y = baseY; } else { voo = 3; vooT = 0; x = W + 40; y = baseY; } } }
            else { x += (baseX - x) * Math.min(1, dt * 3); y += (baseY - y) * Math.min(1, dt * 3); if (Math.abs(x - baseX) < 4) voo = 0; }
            if (voo != 0) return; }
        if (escuro > 0) escuro = Math.max(0, escuro - dt);
        if (eclipseT >= 0) { eclipseT += dt; if (eclipseT > 1.1) { // o sol negro explode em anel
                for (int i = 0; i < 14; i++) { double a = i * Math.PI / 7; tiros.add(new TiroChefe(eclipseX, eclipseY, Math.cos(a) * 250, Math.sin(a) * 250, false, TiroChefe.ENERGIA, TiroChefe.Movimento.RETO).comCor(new Color(200, 110, 255))); }
                eclipseT = -1; boom = .3; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); } }
        boom = Math.max(0, boom - dt);
        if (coroa > 0) { coroa = Math.max(0, coroa - dt); coroaT -= dt; // espiral saindo do halo
            if (coroaT <= 0) { coroaT = .07; double[] h = halo(); double a = tempo * 5.5; for (int k = 0; k < 2; k++) { double b = a + k * Math.PI; tiros.add(new TiroChefe(h[0], h[1], Math.cos(b) * 210, Math.sin(b) * 210, false, TiroChefe.ESTRELA_ECLIPSE, TiroChefe.Movimento.RETO)); } } }
        if (entrada < 1) return;
        if (!usouTotal && vidaFrac < .4) { usouTotal = true; escuro = 6; golpeT = .8; SomJogo.tocar(SomJogo.Efeito.ALERTA); }
        recarga -= dt * dif.cadenciaInimigos * (vidaFrac < .4 ? 1.3 : 1);
        if (recarga > 0 || coroa > 0 || eclipseT >= 0 || tremAviso > 0 || voo != 0) return;
        double[] cabo = cabo(); int g = new int[]{0, 4, 5, 1, 2, 3}[golpes++ % 6]; golpeT = .5; // riff, trem, rasante, eclipse, coroa, grito
        if (g == 5) { voo = 1; vooT = 0; voltas = vidaFrac < .4 ? 2 : 1; vooY = Math.max(TOPO - 20, Math.min(H - ALT + 20, naveY - ALT * .5)); recarga = 1.3; SomJogo.tocar(SomJogo.Efeito.ALERTA); return; } // rasante pela tela
        if (g == 4) { tremY = sorte.nextBoolean() ? TOPO + 5 : H - 165; tremAviso = .6; recarga = 3.2; SomJogo.tocar(SomJogo.Efeito.ALERTA); return; } // chama o trem maluco (por cima ou por baixo)
        if (g == 0) { double a0 = Math.atan2(naveY - cabo[1], naveX - cabo[0]); for (int i = -3; i <= 3; i++) { double a = a0 + i * .14; tiros.add(new TiroChefe(cabo[0], cabo[1], Math.cos(a) * 290, Math.sin(a) * 290, i == 0, TiroChefe.NOTA, TiroChefe.Movimento.ONDA).comCor(ROXO)); } recarga = 1.2; }
        else if (g == 1) { eclipseX = naveX; eclipseY = naveY; eclipseT = 0; recarga = 1.5; }
        else if (g == 2) { coroa = 1.3; coroaT = 0; recarga = 1.6; }
        else { int b = sorte.nextInt(5); for (int i = 0; i < 7; i++) if (i != b && i != b + 1) tiros.add(new TiroChefe(x + 40, TOPO + 40 + i * 70, -260, 0, true, TiroChefe.ONDA_SOM, TiroChefe.Movimento.RETO)); recarga = 1.5; }
        SomJogo.tocar(SomJogo.Efeito.CHEFE_TIRO);
    }

    private int largura() { return ARTE.getWidth() / 4 * ALT / ARTE.getHeight(); }
    private double[] cabo() { return new double[]{x + largura() * .12, y + ALT * .2}; } // ponta da guitarra (a esquerda)
    private double[] halo() { return new double[]{x + largura() * .55, y + ALT * .12}; }
    Rectangle bounds() { int w = largura(); return new Rectangle((int) (x + w * .25), (int) (y + ALT * .12), (int) (w * .5), (int) (ALT * .8)); }

    void surgir(double cx, double cy) { entrada = 1; x = cx - largura() / 2.0; }
    double[] centro() { return new double[]{x + largura() / 2.0, y + ALT / 2.0}; }
    /** Encosto no trem maluco. */
    boolean contato(Rectangle nave) {
        if (voo == 2 && esperaContato <= 0 && nave.intersects(bounds())) { esperaContato = .8; return true; }
        if (esperaContato > 0 || tremX < -9000 || TREM == null) return false;
        if (nave.intersects(new Rectangle((int) tremX + 10, (int) tremY + 20, 190, 130))) { esperaContato = 1; return true; } return false;
    }
    /** Ataque ao acaso dos palhacos: torta mirada, corneta (leque de notas) ou bomba de confete. */
    private void palhacos(List<TiroChefe> tiros) {
        double ox = tremX + 40 + sorte.nextInt(150), oy = tremY + 60; int k = sorte.nextInt(3); double a = Math.atan2(naveY - oy, naveX - ox);
        if (k == 0) tiros.add(new TiroChefe(ox, oy, Math.cos(a) * 300, Math.sin(a) * 300, true, TiroChefe.TORTA, TiroChefe.Movimento.RETO)); // torta mirada
        else if (k == 1) { tiros.add(new TiroChefe(ox, oy, Math.cos(a) * 240, Math.sin(a) * 240, false, TiroChefe.CORNETA, TiroChefe.Movimento.RETO)); for (int i = -1; i <= 1; i += 2) tiros.add(new TiroChefe(ox, oy, Math.cos(a + i * .28) * 260, Math.sin(a + i * .28) * 260, false, TiroChefe.NOTA, TiroChefe.Movimento.ONDA).comCor(ROXO)); }
        else tiros.add(new TiroChefe(ox, oy, Math.cos(a) * 140, Math.sin(a) * 140 + 40, true, TiroChefe.CONFETE, TiroChefe.Movimento.RETO)); // bomba de confete
        SomJogo.tocar(SomJogo.Efeito.CHEFE_TIRO);
    }
    private void desenharSolNegro(Graphics2D g) {
        if (ECLIPSE != null && (eclipseT >= 0 || boom > 0)) { // sol negro (poder_eclipse.png): nasce nas asas, voa ate a nave, pulsa e explode
            double vai = Math.min(1, Math.max(0, (eclipseT - .25) / .35)), ex = boom > 0 ? eclipseX : halo()[0] + (eclipseX - halo()[0]) * vai, ey = boom > 0 ? eclipseY : halo()[1] + (eclipseY - halo()[1]) * vai;
            int fw = ECLIPSE.getWidth() / 4, q = boom > 0 ? 3 : eclipseT < .25 ? 1 : eclipseT < .6 ? 1 : 2, t = q == 3 ? 260 : 200;
            double pulso = q == 2 ? 1 + .06 * Math.sin(tempo * 30) : 1; int tt = (int) (t * pulso);
            g.drawImage(ECLIPSE, (int) ex - tt / 2, (int) ey - tt / 2, (int) ex + tt / 2, (int) ey + tt / 2, q * fw, 0, q * fw + fw, ECLIPSE.getHeight(), null);
        }
    }
    private void desenharTrem(Graphics2D g) {
        if (tremAviso > 0 && FUMACA != null) { int fw = FUMACA.getWidth() / 3, fh = FUMACA.getHeight(), q = (int) (tempo * 8) % 3, w = 300, h = w * fh / fw, fx = W - (int) ((.6 - tremAviso) * 300) - 60; // fumaca colorida chegando pela borda
            g.drawImage(FUMACA, fx, (int) tremY + 10, fx + w, (int) tremY + 10 + h, q * fw, 0, q * fw + fw, fh, null); }
        if (tremX < -9000 || TREM == null) return;
        int fw = TREM.getWidth() / 3, fh = TREM.getHeight(), th = 210, tw = fw * th / fh, q = (int) (tempo * 10) % 3, ty = (int) tremY + 150 - th - (int) Math.abs(Math.sin(tempo * 13) * 10);
        g.drawImage(TREM, (int) tremX, ty, (int) tremX + tw, ty + th, q * fw, 0, q * fw + fw, fh, null);
        EnxameMalachar.confete(g, (int) tremX + tw / 2, ty + 30, (tempo * 1.3) % 1.2);
    }
    void desenhar(Graphics2D tela, float alfa) {
        Graphics2D g = (Graphics2D) tela.create();
        desenharTrem(g);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (alfa < 1) g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, alfa)));
        desenharSolNegro(g);
        if (ECLIPSE == null && eclipseT >= 0) { // sol negro em cima da nave, crescendo e piscando antes de explodir
            int r = (int) (20 + eclipseT * 60); boolean pisca = (int) (eclipseT * 14) % 2 == 0;
            g.setColor(new Color(200, 120, 255, pisca ? 200 : 120)); g.fillOval((int) eclipseX - r - 5, (int) eclipseY - r - 5, 2 * r + 10, 2 * r + 10);
            g.setColor(new Color(10, 0, 20)); g.fillOval((int) eclipseX - r, (int) eclipseY - r, 2 * r, 2 * r);
        }
        int fw = ARTE.getWidth() / 4, fh = ARTE.getHeight(), w = largura();
        int q = voo == 2 ? 3 : voo == 1 ? 2 : escuro > 4.5 || coroa > 0 ? 3 : golpeT > 0 ? 2 : (int) (tempo * 2.5) % 2;
        boolean geraEclipse = voo == 0 && eclipseT >= 0 && eclipseT < .7; // abre as asas e o eclipse nasce delas
        if ((voo == 1 || voo == 2 || geraEclipse) && ASAS != null) { // asas: abrindo, abertas (com o eclipse), voando
            int af = ASAS.getWidth() / 4, ah = ASAS.getHeight(), aq = geraEclipse ? (eclipseT < .12 ? 0 : 1) : voo == 1 ? (vooT < .35 ? 0 : 1) : 2 + (int) (tempo * 10) % 2;
            int ha = (int) (ALT * 1.32), aw = af * ha / ah, ax = (int) (x + w / 2.0 - aw / 2.0), ay = (int) (y + ALT * .95 - ha * .9);
            if (voo == 2) for (int k = 1; k <= 3; k++) { g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, alfa) * (.3f - k * .08f))); g.drawImage(ASAS, ax - (int) (vvx * .04 * k), ay - (int) (vvy * .04 * k), ax - (int) (vvx * .04 * k) + aw, ay - (int) (vvy * .04 * k) + ha, aq * af, 0, aq * af + af, ah, null); }
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alfa)))); g.drawImage(ASAS, ax, ay, ax + aw, ay + ha, aq * af, 0, aq * af + af, ah, null); desenharSolNegro(g); g.dispose(); return;
        }
        if (voo == 2) for (int k = 1; k <= 4; k++) { g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, alfa) * (.32f - k * .07f))); g.drawImage(ARTE, (int) (x - vvx * .05 * k), (int) (y - vvy * .05 * k), (int) (x - vvx * .05 * k) + w, (int) (y - vvy * .05 * k) + ALT, q * fw, 0, q * fw + fw, fh, null); } // rastro
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alfa))));
        g.drawImage(ARTE, (int) x, (int) y, (int) x + w, (int) y + ALT, q * fw, 0, q * fw + fw, fh, null);
        if (flash > 0) { g.setComposite(AlphaComposite.SrcOver.derive(.35f * Math.max(0, alfa))); g.drawImage(ARTE, (int) x, (int) y, (int) x + w, (int) y + ALT, q * fw, 0, q * fw + fw, fh, null); }
        g.dispose();
    }
    /** Eclipse total (furia): escurece a tela inteira, menos um circulo de luz em volta da nave. Desenhar por ultimo. */
    void desenharEscuro(Graphics2D tela) {
        if (escuro <= 0) return;
        float a = (float) Math.min(.9, Math.min(escuro, 6 - escuro) * 1.2);
        Area sombra = new Area(new Rectangle(0, TOPO, W, H - TOPO)); int r = 120;
        sombra.subtract(new Area(new Ellipse2D.Double(naveX - r, naveY - r, 2 * r, 2 * r)));
        Graphics2D g = (Graphics2D) tela.create(); g.setComposite(AlphaComposite.SrcOver.derive(a)); g.setColor(new Color(6, 0, 14)); g.fill(sombra);
        g.setComposite(AlphaComposite.SrcOver.derive(a * .5f)); g.setColor(new Color(160, 80, 220)); g.drawOval((int) naveX - r, (int) naveY - r, 2 * r, 2 * r);
        if (MOLDURA != null) { int fw = MOLDURA.getWidth() / 2, q = (int) (tempo * 4) % 2; java.awt.image.BufferedImage f = MOLDURA.getSubimage(q * fw, 0, fw, MOLDURA.getHeight()); // moldura de sombra, morcegos e estrelas
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1, a * 1.1f))); g.drawImage(f, -10, TOPO - 70, W + 20, H - TOPO + 130, null); }
        g.dispose();
    }

    private static BufferedImage ler(String n) {
        java.net.URL u = SenhorEclipse.class.getResource("/ImagemIcon/" + n);
        if (u == null) return null;
        try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; }
    }
}
