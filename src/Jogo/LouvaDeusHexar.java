package Jogo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Ato II do Hexar (Cinzas de Orion, Thrash): o louva-a-deus guitarrista. Quem ataca e o amplificador
 * (ondas, parede de som, microfonia); o corpo segura os tiros, so as 4 patas sentem o dano. Cada pata
 * quebrada faz o amp pifar e o bicho cambalear; sem patas ele desaba, a guitarra parte e sai o verme
 * que vivia dentro dele. O verme corre por baixo do chao, sai do buraco, rodopia cuspindo um anel,
 * abre a boca apontando para a nave e da o bote: a cabeca voa com o corpo atras e cai num buraco novo.
 * Cada ciclo e mais rapido. No verme, so a cabeca sente o dano.
 * Arte: louva_deus_guitarrista.png (4 quadros, cortado acima das patas), louva_deus_pata.png,
 * amplificador_inseto.png (2 quadros), verme_orion.png (4 quadros saindo do chao), verme_cabeca.png, verme_segmento.png.
 */
final class LouvaDeusHexar {
    private static final BufferedImage CORPO = ler("louva_deus_guitarrista.png"), PATA = ler("louva_deus_pata.png"), AMP = ler("amplificador_inseto.png"),
            VERME = ler("verme_orion.png"), CABECA = ler("verme_cabeca.png"), SEGMENTO = ler("verme_segmento.png");
    static boolean temArte() { return CORPO != null && PATA != null && AMP != null && VERME != null && CABECA != null && SEGMENTO != null; }
    private static final int W = Config.LARGURA, H = Config.ALTURA, TOPO = Config.HUD, CHAO = H - 58, PATAS = 4, SEG = 10;
    // medidas da arte original (quadro de 500 px): raiz e pe de cada pata, pe no chao em y=468
    private static final double ESC = .7; private static final int[] RAIZ_X = {150, 235, 355, 430}, PE_X = {90, 200, 385, 470}; private static final int RAIZ_Y = 335, PE_Y = 468;

    private final double vidaPata; private final double[] pata = new double[PATAS], queda = new double[PATAS], flashPata = new double[PATAS];
    private double bx = W + 60, tempo, entrada, recarga = 1.6, pifado, cambaleio, flash, microfonia, microAviso, microY;
    private final double by = CHAO - PE_Y * ESC;
    private int golpes, ultimaPata = -1; private boolean bloqueou; private String aviso;
    // verme: 0 por baixo da terra, 1 saindo, 2 rodopio, 3 apontando, 4 bote, 5 mergulhando
    private int fase = 1, estado; private double desaba, buraco, alvoX, estadoT, velocidade = 560, hx, hy, vx, vy, mira, esperaContato;
    private final double[][] rastro = new double[SEG * 5][2]; private int iRastro, nRastro;
    private final java.util.Random sorte = new java.util.Random(17);

    LouvaDeusHexar(int vidaPorForma) { vidaPata = vidaPorForma * .11; for (int i = 0; i < PATAS; i++) pata[i] = vidaPata; }
    String consumirAviso() { String a = aviso; aviso = null; return a; }
    double entrada() { return entrada; }
    boolean verme() { return fase == 3; }

    void atualizar(double dt, int naveX, int naveY, Dificuldade dif, List<TiroChefe> tiros) {
        tempo += dt; flash = Math.max(0, flash - dt); esperaContato = Math.max(0, esperaContato - dt);
        for (int i = 0; i < PATAS; i++) { flashPata[i] = Math.max(0, flashPata[i] - dt); if (pata[i] <= 0) queda[i] += dt; }
        double nx = naveX + 22, ny = naveY + 18;
        if (fase == 1) {
            entrada = Math.min(1, entrada + dt / 1.6); bx = W + 60 - (W + 60 - (W - 350)) * (1 - Math.pow(1 - entrada, 3));
            cambaleio = Math.max(0, cambaleio - dt); pifado = Math.max(0, pifado - dt);
            if (microAviso > 0 && (microAviso -= dt) <= 0) { microfonia = .7; SomJogo.tocar(SomJogo.Efeito.LASER); }
            microfonia = Math.max(0, microfonia - dt);
            if (vivas() == 0) { fase = 2; desaba = 0; aviso = "A GUITARRA PARTIU... ALGO SAI DE DENTRO DELE!"; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); return; }
            if (entrada < 1 || pifado > 0) return;
            recarga -= dt * dif.cadenciaInimigos * (vivas() <= 2 ? 1.35 : 1);
            if (recarga > 0) return;
            double ax = ampX() + 20, ay = CHAO - 110; int g = golpes++ % 3;
            if (g == 0) { for (int i = -3; i <= 3; i++) { double a = Math.PI + i * .16; tiros.add(new TiroChefe(ax, ay, Math.cos(a) * 280, Math.sin(a) * 280, false, TiroChefe.ONDA_SOM, TiroChefe.Movimento.RETO)); } recarga = 1.3; }
            else if (g == 1) { int b = sorte.nextInt(5); for (int i = 0; i < 7; i++) if (i != b && i != b + 1) tiros.add(new TiroChefe(ax, TOPO + 40 + i * 70, -240, 0, true, TiroChefe.ONDA_SOM, TiroChefe.Movimento.RETO)); recarga = 1.6; }
            else { microAviso = .8; microY = ny; recarga = 2.0; }
            SomJogo.tocar(SomJogo.Efeito.CHEFE_TIRO);
        } else if (fase == 2) { // desaba, a guitarra parte e o verme rasga a carcaca
            desaba += dt;
            if (desaba > 2.2) { fase = 3; buraco = bx + 120; estado = 1; estadoT = 0; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); }
        } else verme(dt, nx, ny, tiros);
    }

    private double tempoEstado(double base) { return Math.max(base * .4, base - (velocidade - 560) / 1400 * base); }
    private void verme(double dt, double nx, double ny, List<TiroChefe> tiros) {
        estadoT += dt;
        if (estado == 0) { // por baixo da terra, ate ficar debaixo da nave
            double passo = velocidade * .9 * dt; buraco += Math.max(-passo, Math.min(passo, alvoX - buraco));
            if (Math.abs(alvoX - buraco) < 2) { estado = 1; estadoT = 0; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO); }
        } else if (estado == 1) { if (estadoT > tempoEstado(.4)) { estado = 2; estadoT = 0; anel(tiros); } }
        else if (estado == 2) { if (estadoT > tempoEstado(1.0)) { estado = 3; estadoT = 0; anel(tiros); } }
        else if (estado == 3) { // abre a boca e aponta
            double[] b = boca(); mira = Math.atan2(ny - b[1], nx - b[0]);
            if (estadoT > tempoEstado(.7)) { estado = 4; estadoT = 0; hx = b[0]; hy = b[1]; vx = Math.cos(mira) * velocidade; vy = Math.sin(mira) * velocidade; nRastro = 0; marca(buraco, CHAO); marca(hx, hy); SomJogo.tocar(SomJogo.Efeito.CHEFE_TIRO); }
        } else if (estado == 4) { // bote: voa em linha e cai em arco num buraco novo
            vy += 650 * dt; hx += vx * dt; hy += vy * dt; mira = Math.atan2(vy, vx); marca(hx, hy);
            if (hx < 30) { hx = 30; vx = Math.abs(vx) * .6; } else if (hx > W - 30) { hx = W - 30; vx = -Math.abs(vx) * .6; }
            if (hy < TOPO + 20) { hy = TOPO + 20; vy = Math.abs(vy) * .4; }
            if (hy > CHAO && vy > 0) { estado = 5; estadoT = 0; buraco = Math.max(90, Math.min(W - 90, hx)); SomJogo.tocar(SomJogo.Efeito.IMPACTO); }
        } else if (estadoT > tempoEstado(.45)) { // mergulhou: acelera e vai para baixo da nave
            velocidade = Math.min(1500, velocidade * 1.15); estado = 0; estadoT = 0; alvoX = Math.max(110, Math.min(W - 110, nx + (sorte.nextBoolean() ? 1 : -1) * sorte.nextInt(120)));
        }
    }
    private void anel(List<TiroChefe> tiros) {
        double[] b = boca(); int n = 10; double g0 = sorte.nextDouble();
        for (int i = 0; i < n; i++) { double a = g0 + i * Math.PI * 2 / n; tiros.add(new TiroChefe(b[0], b[1], Math.cos(a) * 230, Math.sin(a) * 230, false, TiroChefe.ENERGIA, TiroChefe.Movimento.RETO).comCor(new Color(200, 110, 255))); }
        SomJogo.tocar(SomJogo.Efeito.CHEFE_TIRO);
    }
    private void marca(double x, double y) { rastro[iRastro][0] = x; rastro[iRastro][1] = y; iRastro = (iRastro + 1) % rastro.length; nRastro = Math.min(rastro.length, nRastro + 1); }
    private double[] seg(int k) { int volta = Math.min(nRastro - 1, (k + 1) * 4); return rastro[Math.floorMod(iRastro - 1 - volta, rastro.length)]; }

    // geometria
    private static final int VALT = 290; // altura do verme de pe
    private double vEsc() { return VALT / (double) VERME.getHeight(); }
    private int vermeX() { return (int) (buraco - VERME.getWidth() / 4 * vEsc() * .45); }
    private int vermeY() { double sobe = estado == 1 ? Math.min(1, estadoT / Math.max(.1, tempoEstado(.4))) : estado == 5 ? 1 - Math.min(1, estadoT / Math.max(.1, tempoEstado(.45))) : 1; return (int) (CHAO + 12 - VALT * sobe); }
    private double[] boca() { return new double[]{vermeX() + VERME.getWidth() / 4 * vEsc() * .3, vermeY() + VALT * .38}; }
    private boolean dePe() { return fase == 3 && (estado == 1 || estado == 2 || estado == 3 || estado == 5); }
    private Rectangle cabeca() {
        if (estado == 4) return new Rectangle((int) hx - 42, (int) hy - 42, 84, 84);
        double[] b = boca(); return new Rectangle((int) b[0] - 60, (int) b[1] - 70, 120, 120);
    }
    private Rectangle corpoVerme() { return new Rectangle((int) buraco - 50, vermeY() + VALT / 2, 110, VALT / 2); }
    private int vivas() { int n = 0; for (double p : pata) if (p > 0) n++; return n; }
    private int ampX() { return (int) bx - 175; }
    private Rectangle caixaPata(int i) { int rx = (int) (bx + RAIZ_X[i] * ESC), px = (int) (bx + PE_X[i] * ESC); return new Rectangle(Math.min(rx, px) - 8, (int) (by + RAIZ_Y * ESC), Math.abs(px - rx) + 16, (int) ((PE_Y - RAIZ_Y) * ESC)); }
    private Rectangle corpo() { return new Rectangle((int) bx + 60, (int) by + 60, (int) (380 * ESC), (int) (270 * ESC)); }

    Rectangle bounds() { if (fase == 3) return cabeca(); for (int i = 0; i < PATAS; i++) if (pata[i] > 0) return caixaPata(i); return corpo(); }

    /** Tiro do jogador: pata (dano), corpo (segura), cabeca do verme (dano), corpo do verme (segura). */
    boolean colide(Rectangle r) {
        if (fase == 2 || entrada < 1) return false;
        if (fase == 1) {
            for (int i = 0; i < PATAS; i++) if (pata[i] > 0 && r.intersects(caixaPata(i))) { ultimaPata = i; return true; }
            if (r.intersects(corpo())) { bloqueou = true; return true; }
            return false;
        }
        if (estado == 0) return false;
        if (r.intersects(cabeca())) return true;
        if (dePe() && r.intersects(corpoVerme())) { bloqueou = true; return true; }
        if (estado == 4) for (int k = 0; k < SEG; k++) { double[] p = seg(k); if (r.intersects(new Rectangle((int) p[0] - 30, (int) p[1] - 30, 60, 60))) { bloqueou = true; return true; } }
        return false;
    }
    boolean consumirBloqueio() { boolean b = bloqueou; bloqueou = false; if (b) flash = .06; return b; }
    /** Aplica o dano do tiro: na pata, desconta da pata; devolve o dano que vai para a barra do chefe. */
    int dano(int d) {
        if (fase == 1 && ultimaPata >= 0) { int i = ultimaPata; ultimaPata = -1; d = (int) Math.min(d, Math.ceil(pata[i])); pata[i] -= d; flashPata[i] = .12;
            if (pata[i] <= 0) { pifado = 2.6; cambaleio = .8; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); } return d; }
        flash = .12; return d;
    }
    /** Encosto: microfonia do amp, verme de pe, cabeca e corpo no bote. */
    boolean contato(Rectangle nave) {
        if (esperaContato > 0) return false;
        boolean bate = false;
        if (fase == 1) bate = microfonia > 0 && nave.intersects(new Rectangle(0, (int) microY - 14, ampX(), 28));
        else if (fase == 3 && estado != 0) {
            bate = nave.intersects(cabeca()) || dePe() && nave.intersects(corpoVerme());
            if (estado == 4) for (int k = 0; k < SEG && !bate; k++) { double[] p = seg(k); bate = nave.intersects(new Rectangle((int) p[0] - 24, (int) p[1] - 24, 48, 48)); }
        }
        if (bate) esperaContato = .9; return bate;
    }

    void desenhar(Graphics2D tela) {
        Graphics2D g = (Graphics2D) tela.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (fase <= 2) louva(g); else verme(g);
        g.dispose();
    }

    private void louva(Graphics2D g) {
        double afunda = fase == 2 ? Math.min(1, desaba / .8) * 70 : 0, treme = cambaleio > 0 ? Math.sin(tempo * 60) * 6 : 0;
        int fw = CORPO.getWidth() / 4, fh = CORPO.getHeight(), cw = (int) (fw * ESC), ch = (int) (fh * ESC), x = (int) (bx + treme), y = (int) (by + afunda);
        // amplificador primeiro (fica atras do braco)
        if (fase == 1) {
            int aw = AMP.getWidth() / 2, ah = AMP.getHeight(), aAlt = 175, awd = aw * aAlt / ah, axp = ampX() - awd / 2, ayp = CHAO - aAlt; int qa = recarga < .25 || microfonia > 0 || microAviso > 0 ? 1 : 0;
            if (pifado > 0 && (int) (tempo * 12) % 2 == 0) g.setComposite(AlphaComposite.SrcOver.derive(.55f));
            g.drawImage(AMP, axp, ayp, axp + awd, ayp + aAlt, qa * aw, 0, qa * aw + aw, ah, null); g.setComposite(AlphaComposite.SrcOver);
            if (pifado > 0) { g.setColor(new Color(255, 230, 120)); for (int k = 0; k < 5; k++) g.fillRect(axp + sorte.nextInt(Math.max(1, awd)), ayp + sorte.nextInt(aAlt), 3, 3); }
            if (microAviso > 0 && (int) (tempo * 16) % 2 == 0) { g.setColor(new Color(255, 80, 200, 130)); g.fillRect(0, (int) microY - 2, axp + awd / 3, 4); }
            if (microfonia > 0) { g.setColor(new Color(255, 120, 230, 200)); g.fillRect(0, (int) microY - 12, axp + awd / 3, 24); g.setColor(new Color(255, 255, 255, 220)); g.fillRect(0, (int) microY - 4, axp + awd / 3, 8); }
        }
        // patas: das raizes (embaixo do abdomen) ate o chao; as da frente espelhadas
        for (int i = 0; i < PATAS; i++) {
            if (pata[i] <= 0 && queda[i] > 1.2) continue;
            double q = pata[i] <= 0 ? queda[i] : 0; int rx = (int) (x + RAIZ_X[i] * ESC), ry = (int) (y + RAIZ_Y * ESC - 6), alt = (int) ((PE_Y - RAIZ_Y) * ESC) + 14, larg = PATA.getWidth() * alt / PATA.getHeight() * 3 / 2; // patas mais grossas
            boolean frente = PE_X[i] < RAIZ_X[i];
            Graphics2D p = (Graphics2D) g.create(); p.translate(rx, ry + q * q * 380); p.rotate(q * 2.4 * (frente ? -1 : 1));
            if (q > 0) p.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, 1 - q / 1.2)));
            if (frente) p.drawImage(PATA, larg / 4, 0, -larg + larg / 4, alt, 0, 0, PATA.getWidth(), PATA.getHeight(), null);
            else p.drawImage(PATA, -larg / 4, 0, larg - larg / 4, alt, 0, 0, PATA.getWidth(), PATA.getHeight(), null);
            if (flashPata[i] > 0) { p.setColor(new Color(255, 240, 160)); for (int k = 0; k < 5; k++) p.fillRect(-6 + sorte.nextInt(14), sorte.nextInt(Math.max(1, alt)), 4, 4); } // faiscas do acerto
            p.dispose();
        }
        int q = fase == 2 || cambaleio > 0 ? 3 : pifado > 0 ? 0 : (int) (tempo * 6) % 3;
        g.drawImage(CORPO, x, y, x + cw, y + ch, q * fw, 0, q * fw + fw, fh, null);
        if (flash > 0) { g.setComposite(AlphaComposite.SrcOver.derive(.3f)); g.drawImage(CORPO, x, y, x + cw, y + ch, q * fw, 0, q * fw + fw, fh, null); g.setComposite(AlphaComposite.SrcOver); }
        if (fase == 2 && desaba > 1.2) { g.setColor(new Color(220, 160, 255)); for (int k = 0; k < 14; k++) { double a = k * Math.PI / 7, r = (desaba - 1.2) * 260; g.fillRect((int) (bx + 120 + Math.cos(a) * r), (int) (by + 120 + Math.sin(a) * r), 5, 5); } }
    }

    private void verme(Graphics2D g) {
        // terra: montinho correndo por baixo do chao, ou o buraco aberto
        g.setColor(new Color(120, 80, 50));
        if (estado == 0) { for (int k = 0; k < 7; k++) g.fillRect((int) buraco - 30 + k * 9, CHAO - 6 - (int) (Math.abs(Math.sin(tempo * 20 + k)) * 10), 7, 7); }
        else { g.setColor(new Color(30, 20, 30)); g.fillOval((int) buraco - 50, CHAO - 8, 100, 18); }
        if (estado == 4) { // bote: corpo saindo do buraco ate a cabeca
            for (int k = SEG - 1; k >= 0; k--) { double[] p = seg(k); int s = 74 - k * 3; g.drawImage(SEGMENTO, (int) p[0] - s / 2, (int) p[1] - s * 3 / 8, s, s * 3 / 4, null); }
            int cw = 110, ch = cw * CABECA.getHeight() / CABECA.getWidth();
            Graphics2D h = (Graphics2D) g.create(); h.translate(hx, hy); h.rotate(mira + Math.PI); h.drawImage(CABECA, -cw / 2, -ch / 2, cw, ch, null);
            if (flash > 0) { h.setComposite(AlphaComposite.SrcOver.derive(.4f)); h.drawImage(CABECA, -cw / 2, -ch / 2, cw, ch, null); } h.dispose();
        } else if (estado != 0) { // de pe no buraco
            int vw = VERME.getWidth() / 4, vh = VERME.getHeight(), q = estado == 1 ? 0 : estado == 2 ? 1 : estado == 3 ? 2 : 3, w = (int) (vw * vEsc()), x = vermeX(), y = vermeY();
            Graphics2D c = (Graphics2D) g.create(); c.clipRect(0, 0, W, CHAO + 12);
            boolean vira = estado == 2 && (int) (tempo * 14) % 2 == 0; // rodopio
            if (vira) c.drawImage(VERME, x + w, y, x, y + VALT, q * vw, 0, q * vw + vw, vh, null); else c.drawImage(VERME, x, y, x + w, y + VALT, q * vw, 0, q * vw + vw, vh, null);
            if (flash > 0) { c.setComposite(AlphaComposite.SrcOver.derive(.4f)); c.drawImage(VERME, x, y, x + w, y + VALT, q * vw, 0, q * vw + vw, vh, null); }
            c.dispose();
            if (estado == 3 && (int) (tempo * 14) % 2 == 0) { double[] b = boca(); g.setColor(new Color(255, 60, 60, 150)); g.fillOval((int) b[0] - 18, (int) b[1] - 18, 36, 36); }
        }
    }

    private static BufferedImage ler(String n) {
        java.net.URL u = LouvaDeusHexar.class.getResource("/ImagemIcon/" + n);
        if (u == null) return null;
        try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; }
    }
}
