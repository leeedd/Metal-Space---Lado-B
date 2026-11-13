package Jogo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** Disparo em leque com animacao de pulso e rastro de energia. */
public final class TiroChefe {
    public enum Movimento { RETO, ONDA, ACELERADO, PULSANTE, ARCO }
    private final Movimento movimento;
    private double x, y, tempo;
    private double vx, vy;
    /** Queda acumulada do movimento em ARCO (barris arremessados). */
    private double queda;
    private final boolean pesado;
    private final int estilo;
    /** Cor dos projeteis genericos (ENERGIA e NOTA) dos integrantes. */
    private Color cor = new Color(240, 80, 60);
    TiroChefe comCor(Color c) { if (c != null) cor = c; return this; }
    private int variante; private double duracao; private int resistencia = 2;
    static double alvoX = 150, alvoY = 300; // a nave, para o enxame de mortos perseguir
    boolean abativel() { return estilo == MORTO || estilo >= DIABRETE && estilo <= CUSPIDOR; }
    /** Demonios do inferno: quantos tiros aguentam. */
    TiroChefe demonio() { resistencia = estilo == DIABRETE ? 1 : estilo == CAO ? 2 : estilo == GARGULA ? 3 : estilo == CUSPIDOR ? 7 : resistencia; return this; }
    boolean cuspidorParado() { return estilo == CUSPIDOR && vx == 0 && visivel; }
    private boolean pulou;
    /** Leva um tiro; devolve true quando o morto desmonta. */
    boolean acertar() { return --resistencia <= 0; }
    TiroChefe comDuracao(double d) { duracao = d; return this; }
    TiroChefe comVariante(int v) { variante = v; return this; }
    private boolean visivel = true;

    public TiroChefe(double x, double y, double vx, double vy, boolean pesado) {
        this(x, y, vx, vy, pesado, 0);
    }
    public TiroChefe(double x, double y, double vx, double vy, boolean pesado, int estilo) {
        this(x, y, vx, vy, pesado, estilo, Movimento.RETO);
    }
    public TiroChefe(double x, double y, double vx, double vy, boolean pesado,
                     int estilo, Movimento movimento) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.pesado = pesado;
        this.estilo = estilo; this.movimento = movimento;
    }
    public void mexer(double dt) {
        double velocidade = movimento == Movimento.ACELERADO ? 1 + Math.min(1.1, tempo * .47) : 1;
        double perpx = -vy / Math.max(1, Math.hypot(vx, vy));
        double perpy = vx / Math.max(1, Math.hypot(vx, vy));
        double curva = movimento == Movimento.ONDA ? Math.sin(tempo * 8) * 110 * dt
                : movimento == Movimento.PULSANTE ? Math.sin(tempo * 14) * 70 * dt : 0;
        if (estilo == MORTO && tempo < 6) { // enxame: cada morto cambaleia na direcao da nave, com um desvio proprio para nao virar fila
            double ang = variante * 2.399, ax = alvoX + Math.cos(ang) * 46, ay = alvoY + Math.sin(ang) * 46 + Math.sin(tempo * 3 + variante) * 30, dx = ax - x, dy = ay - y, d = Math.max(1, Math.hypot(dx, dy)), vel = 105 + variante % 5 * 12;
            vx += (dx / d * vel - vx) * Math.min(1, dt * 1.6); vy += (dy / d * vel - vy) * Math.min(1, dt * 1.6); }
        if (estilo == DIABRETE) { // bando: voa em zigue-zague atras da nave
            double dx = alvoX - x, dy = alvoY + Math.sin(tempo * 5 + variante) * 50 - y, d = Math.max(1, Math.hypot(dx, dy)), vel = 190 + variante % 4 * 15;
            vx += (dx / d * vel - vx) * Math.min(1, dt * 2.2); vy += (dy / d * vel - vy) * Math.min(1, dt * 2.2); }
        else if (estilo == CAO) { // corre pela margem da lava e salta em cima da nave
            if (!pulou && x < alvoX + 240) { pulou = true; double dt2 = Math.max(.35, (x - alvoX) / 420); vx = -420; vy = (alvoY - y) / dt2 - 280 * dt2; }
            if (pulou) vy += 560 * dt; else y = LAVA_Y - 26 + Math.abs(Math.sin(tempo * 12)) * -6; }
        else if (estilo == GARGULA) { // paira e depois mergulha em linha reta na nave
            if (tempo < .7) { vx = 0; vy = 30; } else if (!pulou) { pulou = true; double dx = alvoX - x, dy = alvoY - y, d = Math.max(1, Math.hypot(dx, dy)); vx = dx / d * 560; vy = dy / d * 560; } }
        else if (estilo == CUSPIDOR) { // entra devagar, para, flutua cuspindo fogo (o chefe cria as bolas) e vai embora
            if (tempo < 9 && x < Config.LARGURA - 190 && !pulou) { pulou = true; vx = 0; } if (tempo >= 9) vx = -110; vy = Math.sin(tempo * 2) * 30; }
        if (movimento == Movimento.ARCO) queda += 560 * dt;
        x += vx * dt * velocidade + perpx * curva;
        y += (vy + queda) * dt * velocidade + perpy * curva;
        tempo += dt;
        if (estilo == RASTRO && tempo > 1.4) visivel = false;
        if (duracao > 0 && tempo > duracao) visivel = false;
        if (estilo == PISTA_FOGO && tempo > PISTA_DURA) visivel = false;
        int margem = estilo == VAGAO ? 130 : estilo >= SOLDADO && estilo <= RAINHA ? 1200 : estilo == TINTA ? 160 : estilo == MORTO || estilo == LOBISOMEM || estilo >= DIABRETE && estilo <= CUSPIDOR ? 220 : estilo == OSSO_PE ? 90 : 40;
        if (estilo == PEGADA && tempo > 1.1) visivel = false;
        if (x < -margem || x > Config.LARGURA + margem || y < Config.HUD - margem || y > Config.ALTURA + margem)
            visivel = false;
    }
    public void desenhar(Graphics2D g) {
        int px = (int) x, py = (int) y;
        // Cada artista tem um projetil com forma propria, combinando com o golpe da prancha.
        // tiros dos vocalistas com arte propria (girada para onde voam)
        int linha = estilo == 2 ? SpritesExtras.VALDRAK : estilo == 3 ? SpritesExtras.KORVUS : estilo == 5 ? SpritesExtras.HEXAR : estilo == 6 ? SpritesExtras.MORGATH : estilo == 7 ? SpritesExtras.KANOA : -1;
        if (linha >= 0 && SpritesExtras.temTiros()) { SpritesExtras.tiro(g, linha, x, y, vx, vy, pesado ? 82 : 60, tempo); return; }
        switch (estilo) {
            case FLECHA: case LANCA: case CORTE: desenharArmaTormenta(g, px, py); return;
            case RAIO_CEU: desenharRaioCeu(g, px, py); return;
            case FUMACA: desenharFumaca(g, px, py); return;
            case VAGAO: desenharVagao(g, px, py); return;
            case FOGO_ROXO: desenharFogoRoxo(g, px, py); return;
            case BARRIL: desenharBarril(g, px, py); return;
            case RASTRO: desenharRastro(g, px, py); return;
            case PISTA_FOGO: desenharPistaFogo(g, px, py); return;
            case BOLA_FOGO: desenharBolaFogo(g, px, py); return;
            case ATADURA: desenharAtadura(g, px, py); return;
            case TINTA: desenharTinta(g, px, py); return;
            case DIABRETE: case CAO: case GARGULA: case CUSPIDOR: desenharDemonio(g, px, py); return;
            case MORTO: { java.awt.image.BufferedImage[] m = mortos(); if (m.length > 0 && quadros(g, m[Math.floorMod(variante, m.length)], 4, (int) (tempo * 7 + variante) % 4, 88, px, py + (int) (Math.abs(Math.sin(tempo * 7)) * -6), false)) return; break; } // dança macabra: os mortos avancam dancando
            case OSSO_PE: if (ARTE_PE != null) { boolean roxo = cor.getBlue() > cor.getGreen(); java.awt.image.BufferedImage f = roxo ? peRoxo() : ARTE_PE.getSubimage(0, 0, ARTE_PE.getWidth() / 4, ARTE_PE.getHeight());
                Graphics2D e = (Graphics2D) g.create(); e.translate(px, py); e.rotate(Math.atan2(vy, vx) + Math.PI / 2); int h = 70, w = h * f.getWidth() / f.getHeight(); e.drawImage(f, -w / 2, -h / 2, w, h, null); e.dispose(); return; } break;
            case OSSO_SOCO: if (quadros(g, ARTE_SOCO, 4, (cor.getBlue() > cor.getGreen() ? 2 : 0) + (int) (tempo * 10) % 2, 70, px, py, true)) return; break;
            case OSSO_NOTA: if (quadros(g, cor.getBlue() > cor.getGreen() && ARTE_ONOTA != null ? roxoDe(ARTE_ONOTA) : ARTE_ONOTA, 4, (int) (tempo * 9) % 4, 46, px, py, false)) return; break;
            case PEGADA: if (ARTE_PE != null) { int q = Math.min(3, (int) (tempo / .28)), fw = ARTE_PE.getWidth() / 4, h = 42, w = h * fw / ARTE_PE.getHeight(); g.drawImage(ARTE_PE, px - w / 2, py - h / 2, px + w / 2, py + h / 2, q * fw, 0, q * fw + fw, ARTE_PE.getHeight(), null); return; } break;
            case LOBISOMEM: if (quadros(g, ARTE_LOBO, 4, (int) (tempo * 12) % 4, 120, px, py, false)) return; break;
            case TORTA: if (quadros(g, ARTE_TORTA, 4, Math.min(2, (int) (tempo * 8) % 3), 54, px, py, true)) return; break;
            case CORNETA: if (quadros(g, ARTE_CORNETA, 3, Math.min(2, (int) (tempo * 6)), 62, px, py, false)) return; break;
            case CONFETE: if (quadros(g, ARTE_CONFETE, 4, tempo < .6 ? (int) (tempo * 6) % 2 : Math.min(3, 2 + (int) ((tempo - .6) * 6)), 60, px, py, false)) return; break;
            case ESTRELA_ECLIPSE: if (ARTE_COROA != null) { int fw = ARTE_COROA.getWidth() / 2, q = (int) (tempo * 10) % 2, t = 34; Graphics2D e = (Graphics2D) g.create(); e.translate(px, py); e.rotate(tempo * 9); e.drawImage(ARTE_COROA, -t / 2, -t / 2, t / 2, t / 2, q * fw, 0, q * fw + fw, ARTE_COROA.getHeight(), null); e.dispose(); return; } break;
            case BLOCO: case SOLDADO: case CARRUAGEM: case ESFINGE: case RAINHA: desenharEgito(g, px, py); return;
            case ONDA_SOM: desenharOndaKrovax(g, px, py); return;
            case PRATO: desenharPrato(g, px, py); return;
            case ENERGIA: desenharEnergia(g, px, py); return;
            case NOTA: desenharNota(g, px, py); return;
            case AS_ESPADAS: desenharAs(g, px, py); return;
            case MORCEGO_MALACHAR: desenharMorcegoMalachar(g, px, py); return;
            case 1: desenharMorcego(g, px, py); return;
            case 2: desenharFogo(g, px, py); return;
            case 3: desenharRaioCruzado(g, px, py); return;
            case 4: desenharOndaSom(g, px, py); return;
            case 5: desenharEsferaEletrica(g, px, py); return;
            case 6: desenharEstilhaco(g, px, py); return;
            case 7: desenharCristal(g, px, py); return;
            default: break;
        }
        int r = pesado ? 12 : 7;
        Color c = estilo == 8 ? new Color(255,116,12) : estilo == 1 ? new Color(199, 76, 232) : estilo == 2
                ? new Color(255, 156, 44) : estilo == 3 ? new Color(74, 228, 223)
                : estilo == 4 ? new Color(244, 128, 52) : estilo == 5 ? new Color(81, 166, 255)
                : estilo == 6 ? new Color(244, 64, 75) : estilo == 7 ? new Color(125, 239, 110)
                : pesado ? new Color(255, 91, 61) : new Color(190, 94, 255);
        double direcao = Math.atan2(vy, vx);
        for (int i = 1; i <= 3; i++) {
            int tx = px - (int) (Math.cos(direcao) * i * 9);
            int ty = py - (int) (Math.sin(direcao) * i * 9);
            int alpha = 35 + (3 - i) * 30;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            g.fillOval(tx - r + i, ty - r + i, 2 * (r - i), 2 * (r - i));
        }
        int pulso = (int) (3 * Math.sin(tempo * 17));
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 55));
        g.fillOval(px - r - 7 - pulso, py - r - 7 - pulso, 2 * (r + 7 + pulso), 2 * (r + 7 + pulso));
        g.setColor(c); g.fillOval(px - r, py - r, 2 * r, 2 * r);
        if (estilo == 8) {
            g.setColor(new Color(255,220,35));g.fillOval(px-r+2,py-r+2,r*2-4,r*2-4);
            g.setColor(new Color(255,245,172));g.drawOval(px-r,py-r,r*2,r*2);
        } else if (estilo == 4) {
            g.setColor(new Color(255, 215, 118, 185));
            g.drawOval(px - r - 5, py - r - 5, 2*r + 10, 2*r + 10);
            g.drawOval(px - r - 11, py - r - 11, 2*r + 22, 2*r + 22);
        } else if (estilo == 5) {
            g.setColor(new Color(177, 232, 255));
            g.drawPolyline(new int[]{px-14,px-5,px-12,px+7,px+14},
                    new int[]{py-10,py-2,py+5,py+1,py+13},5);
        } else if (estilo == 6) {
            g.setColor(new Color(255, 215, 82));
            g.fillPolygon(new int[]{px,px-5,px+5},new int[]{py+12,py-7,py-7},3);
        } else if (estilo == 7) {
            g.setColor(new Color(196, 255, 221));
            g.fillPolygon(new int[]{px,px+r+6,px,px-r-6},
                    new int[]{py-r-8,py,py+r+8,py},4);
        } else if (estilo == 1) {
            g.setColor(new Color(224, 140, 255));
            g.drawArc(px - r * 2, py - r * 2, 4 * r, 4 * r, -75, 150);
        } else if (estilo == 2) {
            g.setColor(new Color(255, 222, 106));
            g.drawLine(px - r - 5, py, px + 1, py - r);
        } else if (estilo == 3) {
            g.setColor(new Color(170, 252, 253));
            g.drawArc(px - r * 2, py - r * 2, 4 * r, 4 * r, -75, 150);
        }
        g.setColor(Color.WHITE); g.fillOval(px - 3, py - 3, 6, 6);
    }
    /** Morcegos do Malachar em pixel art: asas batendo, olhos voltados para onde voam. */
    private static final String[][] MORCEGO = {
        { // asas no alto
            "M...............M", "WM.............MW", "WWM...H...H...MWW", ".WWM..HBBBH..MWW.",
            ".WWWMBEBEBBBMWWW.", "..WWWWBBBBBWWWW..", "...WWWWBBBWWWW...", "....W.W.B.W.W...."
        }, { // asas abertas
            ".................", ".................", "......H...H......", "MMM...HBBBH...MMM",
            "WWWWMBEBEBBBMWWWW", ".WWWWWBBBBBWWWWW.", "..W.WWWBBBWWW.W..", "........B........"
        }, { // asas embaixo
            ".................", ".................", "......H...H......", "......HBBBH......",
            "...MMBEBEBBBMM...", "..MWWWBBBBBWWWM..", ".MWWWWWBBBWWWWWM.", "MWW.W.W.B.W.W.WWM"
        }
    };
    private static final Color MORCEGO_CORPO = new Color(44, 16, 58), MORCEGO_ASA = new Color(104, 42, 136),
            MORCEGO_BORDA = new Color(192, 110, 238), MORCEGO_OLHO = new Color(255, 58, 58),
            MORCEGO_ORELHA = new Color(78, 28, 100);

    private void desenharMorcego(Graphics2D g, int px, int py) { morcego(g, px, py, pesado ? 3 : 2, tempo, vx > 0, true); }

    /** Morcego em pixel art reutilizavel (tambem usado pela Revoada do jogador). */
    static void morcego(Graphics2D g, int px, int py, int p, double tempo, boolean paraDireita, boolean aura) {
        if (aura) { g.setColor(new Color(150, 60, 210, 42)); g.fillOval(px - 10 * p, py - 6 * p, 20 * p, 12 * p); }
        int[] ciclo = {0, 1, 2, 1};
        String[] quadro = MORCEGO[ciclo[(int) (Math.max(0, tempo) * 14) % 4]];
        int largura = quadro[0].length(), ox = px - largura * p / 2, oy = py - 4 * p;
        for (int linha = 0; linha < quadro.length; linha++) {
            for (int coluna = 0; coluna < largura; coluna++) {
                char c = quadro[linha].charAt(paraDireita ? largura - 1 - coluna : coluna);
                if (c == '.') continue;
                g.setColor(c == 'B' ? MORCEGO_CORPO : c == 'W' ? MORCEGO_ASA : c == 'M' ? MORCEGO_BORDA
                        : c == 'E' ? MORCEGO_OLHO : MORCEGO_ORELHA);
                g.fillRect(ox + coluna * p, oy + linha * p, p, p);
            }
        }
    }
    private double direcao() { return Math.atan2(vy, vx); }

    /** Valdrak: bola de fogo com rastro de chamas tremulando para tras. */
    private void desenharFogo(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        g.rotate(direcao(), px, py);
        int r = pesado ? 11 : 7;
        for (int i = 4; i >= 1; i--) {
            int comp = r * 2 + i * 6 + (int) (Math.sin(tempo * 31 + i) * 3);
            g.setColor(i > 2 ? new Color(200, 40, 20, 120) : new Color(255, 120, 30, 170));
            g.fillPolygon(new int[]{px - comp, px, px}, new int[]{py + (int) (Math.sin(tempo * 23 + i * 2) * 3), py - r + i, py + r - i}, 3);
        }
        g.setColor(new Color(255, 140, 30));
        g.fillOval(px - r, py - r, 2 * r, 2 * r);
        g.setColor(new Color(255, 226, 90));
        g.fillOval(px - r + 3, py - r + 3, 2 * r - 6, 2 * r - 6);
        g.setColor(new Color(255, 255, 220));
        g.fillOval(px - 2, py - 3, 5, 5);
        g.dispose();
    }

    /** Korvus: dois raios ciano em X girando, com nucleo branco. */
    private void desenharRaioCruzado(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        g.rotate(tempo * 7, px, py);
        int r = pesado ? 16 : 11;
        g.setColor(new Color(60, 220, 255, 60));
        g.fillOval(px - r, py - r, 2 * r, 2 * r);
        for (int k = 0; k < 2; k++) {
            g.rotate(Math.PI / 2, px, py);
            int[] xs = new int[7], ys = new int[7];
            for (int i = 0; i < 7; i++) {
                xs[i] = px - r + i * r / 3;
                ys[i] = py + (i % 2 == 0 ? -2 : 2) * (1 + (int) (Math.sin(tempo * 40 + i) * 1.5));
            }
            g.setStroke(new java.awt.BasicStroke(4));
            g.setColor(new Color(40, 190, 255));
            g.drawPolyline(xs, ys, 7);
            g.setStroke(new java.awt.BasicStroke(1.6f));
            g.setColor(new Color(225, 250, 255));
            g.drawPolyline(xs, ys, 7);
        }
        g.dispose();
    }

    /** Krovax: ondas de som em arco, abertas para tras, pulsando. */
    private void desenharOndaSom(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        g.rotate(direcao(), px, py);
        int r = pesado ? 16 : 11;
        g.setStroke(new java.awt.BasicStroke(pesado ? 4 : 3));
        for (int i = 0; i < 3; i++) {
            int raio = r - i * 4 + (int) (Math.sin(tempo * 18 - i) * 2);
            g.setColor(i == 0 ? new Color(255, 230, 130) : i == 1 ? new Color(255, 160, 50) : new Color(220, 90, 30, 190));
            g.drawArc(px - raio - i * 6, py - raio, raio * 2, raio * 2, -60, 120);
        }
        g.dispose();
    }

    /** Hexar: esfera eletrica azul soltando faiscas em volta. */
    private void desenharEsferaEletrica(Graphics2D g, int px, int py) {
        int r = pesado ? 10 : 7;
        g.setColor(new Color(60, 120, 255, 70));
        g.fillOval(px - r - 6, py - r - 6, 2 * r + 12, 2 * r + 12);
        g.setColor(new Color(70, 140, 255));
        g.fillOval(px - r, py - r, 2 * r, 2 * r);
        g.setColor(new Color(200, 225, 255));
        g.fillOval(px - r / 2, py - r / 2, r, r);
        for (int i = 0; i < 4; i++) {
            double a = tempo * 9 + i * Math.PI / 2 + Math.sin(tempo * 21 + i);
            int x1 = px + (int) (Math.cos(a) * r), y1 = py + (int) (Math.sin(a) * r);
            int x2 = px + (int) (Math.cos(a + .4) * (r + 8)), y2 = py + (int) (Math.sin(a + .4) * (r + 8));
            int x3 = px + (int) (Math.cos(a - .2) * (r + 14)), y3 = py + (int) (Math.sin(a - .2) * (r + 14));
            g.setColor(new Color(150, 200, 255));
            g.drawPolyline(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
        }
    }

    /** Morgath: estilhaco vermelho pontudo, apontando para onde voa. */
    private void desenharEstilhaco(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        g.rotate(direcao(), px, py);
        int c = pesado ? 18 : 13, l = pesado ? 6 : 4;
        g.setColor(new Color(255, 40, 40, 80));
        g.fillPolygon(new int[]{px - c * 2, px - c / 2, px - c / 2}, new int[]{py, py - l, py + l}, 3);
        g.setColor(new Color(140, 10, 20));
        g.fillPolygon(new int[]{px + c, px, px - c, px}, new int[]{py, py - l - 1, py, py + l + 1}, 4);
        g.setColor(new Color(240, 40, 50));
        g.fillPolygon(new int[]{px + c - 2, px, px - c + 3, px}, new int[]{py, py - l + 1, py, py + l - 1}, 4);
        g.setColor(new Color(255, 190, 180));
        g.drawLine(px - c / 2, py, px + c - 4, py);
        g.dispose();
    }

    /** Kanoa: cristal verde lapidado, girando devagar. */
    private void desenharCristal(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        g.rotate(Math.sin(tempo * 3) * .5, px, py);
        int a = pesado ? 16 : 11, b = pesado ? 11 : 8;
        g.setColor(new Color(90, 255, 120, 60));
        g.fillOval(px - a - 4, py - a - 4, 2 * a + 8, 2 * a + 8);
        g.setColor(new Color(20, 120, 50));
        g.fillPolygon(new int[]{px, px + b, px, px - b}, new int[]{py - a, py, py + a, py}, 4);
        g.setColor(new Color(60, 210, 90));
        g.fillPolygon(new int[]{px, px + b, px}, new int[]{py - a, py, py + a}, 3);
        g.setColor(new Color(170, 255, 180));
        g.fillPolygon(new int[]{px, px - b, px}, new int[]{py - a, py, py}, 3);
        g.setColor(new Color(235, 255, 235));
        g.drawLine(px - 2, py - a + 4, px - b / 2, py - 2);
        g.dispose();
    }

    Movimento getMovimentoTeste() { return movimento; }
    int getEstiloTeste() { return estilo; }
    double getVxTeste() { return vx; }
    double getVyTeste() { return vy; }
    public Rectangle getBounds() {
        if (estilo == PEGADA) return new Rectangle(-99, -99, 0, 0); // pegada de fogo: so enfeite
        if (estilo == OSSO_PE) return new Rectangle((int) x - 22, (int) y - 22, 44, 44);
        if (estilo == OSSO_SOCO) return new Rectangle((int) x - 20, (int) y - 18, 40, 36);
        if (estilo == MORTO) return new Rectangle((int) x - 20, (int) y - 30, 40, 64);
        if (estilo == DIABRETE) return new Rectangle((int) x - 18, (int) y - 16, 36, 32);
        if (estilo == CAO) return new Rectangle((int) x - 34, (int) y - 20, 68, 40);
        if (estilo == GARGULA) return new Rectangle((int) x - 30, (int) y - 28, 60, 56);
        if (estilo == CUSPIDOR) return new Rectangle((int) x - 46, (int) y - 46, 92, 92);
        if (estilo == LOBISOMEM) return new Rectangle((int) x - 40, (int) y - 25, 80, 50);
        if (estilo == TINTA) return new Rectangle(-99, -99, 0, 0); // a nuvem de tinta nao machuca: so esconde os tiros
        if (estilo == ATADURA) return new Rectangle((int) x - 14, (int) y - 8, 28, 16);
        if (estilo >= BLOCO && estilo <= RAINHA) { int[] t = tamEgito(); return new Rectangle((int) x - t[0] * 4 / 10, (int) y - t[1] * 4 / 10, t[0] * 8 / 10, t[1] * 8 / 10); }
        if (estilo == BOLA_FOGO) return new Rectangle((int) x - 10, (int) y - 10, 20, 20);
        if (estilo == PISTA_FOGO) { double f = fogoPista(); return f <= .05 ? new Rectangle(-99, -99, 0, 0) : new Rectangle((int) x - 19, (int) (y - 38 * f), 38, (int) (38 * f) + 6); }
        if (estilo == RAIO_CEU) return new Rectangle((int) x - 9, (int) y - 40, 18, 46);
        if (estilo == CORTE) return new Rectangle((int) x - 14, (int) y - 34, 28, 68);
        if (estilo == LANCA || estilo == FLECHA) { int r = estilo == LANCA ? 12 : 10; return new Rectangle((int) x - r, (int) y - r, r * 2, r * 2); }
        if (estilo == VAGAO) return new Rectangle((int) x - 70, (int) y - 26, 150, 52);
        if (estilo == ONDA_SOM) { int h = alturaOnda() * 3 / 4; return new Rectangle((int) x - 16, (int) y - h / 2, 32, h); }
        int r = estilo == ENERGIA ? 9 : estilo == NOTA ? 9 : estilo == PRATO ? 14 : estilo == AS_ESPADAS ? 14 : estilo == MORCEGO_MALACHAR ? 13 : estilo == BARRIL ? 13 : estilo == RASTRO ? 10 : estilo == FUMACA ? (int) (16 + Math.min(14, tempo * 16)) : estilo == FOGO_ROXO ? 13 : pesado ? 11 : 6;
        return new Rectangle((int) x - r, (int) y - r, r * 2, r * 2);
    }
    public int dano() { return estilo == ONDA_SOM ? 1 : estilo == VAGAO ? 5 : estilo == FOGO_ROXO ? 3 : pesado ? 3 : 2; }

    // ---- Kanoa (tormenta): flecha, lanca, corte do machado e raio do ceu ----
    static final int FLECHA = 31, LANCA = 32, CORTE = 33, RAIO_CEU = 34;
    private static final Color AZUL_RAIO = new Color(140, 210, 255), BRANCO_RAIO = new Color(235, 248, 255);
    private void desenharArmaTormenta(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create(); g.translate(px, py); g.rotate(Math.atan2(vy, vx));
        java.util.Random r = new java.util.Random((long) (tempo * 30));
        if (estilo == CORTE) {
            // meia-lua do machado: arco de luz grossa, com faiscas na borda
            g.setStroke(new java.awt.BasicStroke(10)); g.setColor(new Color(90, 170, 255, 120)); g.drawArc(-34, -36, 44, 72, -70, 140);
            g.setStroke(new java.awt.BasicStroke(5)); g.setColor(AZUL_RAIO); g.drawArc(-30, -34, 40, 68, -65, 130);
            g.setStroke(new java.awt.BasicStroke(2)); g.setColor(BRANCO_RAIO); g.drawArc(-27, -32, 36, 64, -60, 120);
            for (int k = 0; k < 4; k++) g.fillRect(4 + r.nextInt(8), -30 + r.nextInt(60), 3, 3);
        } else {
            int L = estilo == LANCA ? 58 : 44;
            // haste de raio em zigue-zague e ponta de cristal
            int[] xs = new int[6], ys = new int[6];
            for (int k = 0; k < 6; k++) { xs[k] = -L + k * (L - 8) / 5; ys[k] = k == 0 || k == 5 ? 0 : r.nextInt(7) - 3; }
            g.setStroke(new java.awt.BasicStroke(estilo == LANCA ? 7 : 5)); g.setColor(new Color(90, 170, 255, 120)); g.drawPolyline(xs, ys, 6);
            g.setStroke(new java.awt.BasicStroke(estilo == LANCA ? 3 : 2)); g.setColor(BRANCO_RAIO); g.drawPolyline(xs, ys, 6);
            int c = estilo == LANCA ? 16 : 13;
            g.setColor(AZUL_RAIO); g.fillPolygon(new int[]{-8, c, -8}, new int[]{-c / 2 - 2, 0, c / 2 + 2}, 3);
            g.setColor(BRANCO_RAIO); g.fillPolygon(new int[]{-4, c - 4, -4}, new int[]{-c / 4, 0, c / 4}, 3);
            if (estilo == FLECHA) { g.setColor(AZUL_RAIO); g.drawLine(-L, 0, -L - 6, -6); g.drawLine(-L, 0, -L - 6, 6); }
        }
        g.dispose();
    }
    private void desenharRaioCeu(Graphics2D tela, int px, int py) {
        // o raio desce do topo da tela ate a ponta, em zigue-zague
        Graphics2D g = (Graphics2D) tela.create();
        java.util.Random r = new java.util.Random((long) (tempo * 25) + px);
        int n = Math.max(2, (py - Config.HUD) / 28 + 1); int[] xs = new int[n], ys = new int[n];
        for (int k = 0; k < n; k++) { ys[k] = Config.HUD + (py - Config.HUD) * k / (n - 1); xs[k] = px + (k == n - 1 ? 0 : r.nextInt(17) - 8); }
        g.setStroke(new java.awt.BasicStroke(9)); g.setColor(new Color(90, 170, 255, 90)); g.drawPolyline(xs, ys, n);
        g.setStroke(new java.awt.BasicStroke(4)); g.setColor(AZUL_RAIO); g.drawPolyline(xs, ys, n);
        g.setStroke(new java.awt.BasicStroke(2)); g.setColor(BRANCO_RAIO); g.drawPolyline(xs, ys, n);
        g.setColor(new Color(235, 248, 255, 200)); g.fillOval(px - 8, py - 8, 16, 16);
        g.dispose();
    }

    // ---- Malachar (Trem Maluco e dragao) ----
    static final int FUMACA = 20, VAGAO = 21, FOGO_ROXO = 22, BARRIL = 23, RASTRO = 24, ONDA_SOM = 25, AS_ESPADAS = 26, MORCEGO_MALACHAR = 27, PRATO = 28, ENERGIA = 29, NOTA = 30;
    private void desenharEnergia(Graphics2D g, int px, int py) {
        double d = Math.atan2(vy, vx);
        for (int k = 4; k >= 0; k--) { int tx = px - (int) (Math.cos(d) * k * 6), ty = py - (int) (Math.sin(d) * k * 6), r = 10 - k * 2; g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 230 - k * 40)); g.fillRect(tx - r, ty - r, r * 2, r * 2); }
        g.setColor(Color.WHITE); g.fillRect(px - 3, py - 3, 6, 6);
    }
    private static final java.awt.image.BufferedImage ARTE_NOTA_EC = ler("poder_nota_eclipse.png");
    private void desenharNota(Graphics2D g, int px, int py) {
        if (cor != null && cor.getRed() == 150 && cor.getBlue() == 230 && quadros(g, ARTE_NOTA_EC, 2, (int) (tempo * 8) % 2, 46, px, py, false)) return; // nota do eclipse (roxa)
        int b = (int) (Math.sin(tempo * 12) * 2);
        g.setColor(cor.darker()); g.fillOval(px - 9, py + 2 + b, 12, 9); g.fillRect(px + 1, py - 14 + b, 3, 18); g.fillRect(px + 1, py - 14 + b, 9, 4);
        g.setColor(cor); g.fillOval(px - 8, py + 3 + b, 10, 7); g.fillRect(px + 2, py - 13 + b, 2, 16);
    }
    /** Prato de bateria girando como serra, em pixel art: disco dourado com dentes. */
    private void desenharPrato(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create(); g.rotate(tempo * 18, px, py);
        int r = 15;
        g.setColor(new Color(150, 100, 20)); for (int k = 0; k < 8; k++) { double a = k * Math.PI / 4; g.fillRect(px + (int) (Math.cos(a) * r) - 3, py + (int) (Math.sin(a) * r) - 3, 6, 6); }
        g.setColor(new Color(200, 150, 40)); g.fillOval(px - r, py - r, r * 2, r * 2);
        g.setColor(new Color(245, 205, 90)); g.fillOval(px - r + 4, py - r + 4, r * 2 - 8, r * 2 - 8);
        g.setColor(new Color(170, 120, 30)); g.drawOval(px - 8, py - 8, 16, 16);
        g.setColor(new Color(255, 245, 200)); g.fillRect(px - 6, py - 9, 5, 3); g.setColor(new Color(90, 60, 15)); g.fillRect(px - 2, py - 2, 4, 4);
        g.dispose();
    }
    private static final java.awt.image.BufferedImage ARTE_AS = ler("tiro_as_espadas.png"), ARTE_MORCEGO = ler("tiro_morcego_malachar.png");
    /** As de espadas em chamas do Krovax: gira como estrela ninja. */
    private void desenharAs(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create(); g.rotate(tempo * 13, px, py);
        if (ARTE_AS != null) { g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.drawImage(ARTE_AS, px - 25, py - 22, 50, 44, null); }
        else { g.setColor(new Color(240, 225, 190)); g.fillRect(px - 9, py - 12, 18, 24); g.setColor(Color.BLACK); g.fillOval(px - 4, py - 4, 8, 8); }
        g.dispose();
    }
    /** Morcego do Malachar: bate as asas (achata e abre) e olha para onde voa. */
    private void desenharMorcegoMalachar(Graphics2D tela, int px, int py) {
        if (quadros(tela, ARTE_MORCEGO_EC, 4, (int) (tempo * 12) % 4, 48, px, py, false)) return; // morcego do eclipse
        if (ARTE_MORCEGO == null) { desenharMorcego(tela, px, py); return; }
        Graphics2D g = (Graphics2D) tela.create();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double asa = .55 + .45 * Math.abs(Math.sin(tempo * 16)); int w = (int) (44 * (.85 + .15 * asa)), h = (int) (38 * asa);
        g.translate(px, py); g.rotate(Math.atan2(vy, Math.abs(vx)) * (vx > 0 ? 1 : -1) * .5);
        if (vx > 0) g.drawImage(ARTE_MORCEGO, w / 2, -h / 2, -w, h, null); else g.drawImage(ARTE_MORCEGO, -w / 2, -h / 2, w, h, null);
        g.dispose();
    }
    private static final java.awt.image.BufferedImage ARTE_ONDA = ler("onda_som_krovax.png");
    /** A onda do paredao de caixas empurra a nave em vez de so ferir. */
    boolean empurra() { return estilo == ONDA_SOM; }
    private int alturaOnda() { return 58 + (int) (Math.min(1, tempo * 1.2) * 46); }
    private void desenharOndaKrovax(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        int h = alturaOnda();
        if (ARTE_ONDA != null) {
            int fw = ARTE_ONDA.getWidth() / 4, k = Math.min(3, (int) (tempo * 5)), w = fw * h / ARTE_ONDA.getHeight();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(ARTE_ONDA, px - w / 3, py - h / 2, px - w / 3 + w, py + h / 2, k * fw, 0, k * fw + fw, ARTE_ONDA.getHeight(), null);
        } else { g.setColor(new Color(255, 150, 40)); g.setStroke(new java.awt.BasicStroke(5)); g.drawArc(px - 20, py - h / 2, 40, h, 90, 180); }
        g.dispose();
    }
    double getX() { return x; }
    double getY() { return y; }
    double idade() { return tempo; }
    int estilo() { return estilo; }
    /** Barril de Bardo arremessado pela janela da van: gira no ar e pisca antes de estourar. */
    private void desenharBarril(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create(); g.rotate(tempo * 7, px, py);
        g.setColor(new Color(70, 40, 20)); g.fillRect(px - 11, py - 14, 22, 28);
        g.setColor(new Color(120, 72, 34)); g.fillRect(px - 9, py - 14, 6, 28);
        g.setColor(new Color(40, 40, 46)); g.fillRect(px - 11, py - 9, 22, 3); g.fillRect(px - 11, py + 6, 22, 3);
        g.setColor(Color.BLACK); g.fillRect(px - 6, py - 4, 12, 8); g.setColor(Color.WHITE); g.fillRect(px - 4, py - 2, 8, 1); g.fillRect(px - 4, py + 1, 6, 1);
        if (tempo > .7 && (int) (tempo * 14) % 2 == 0) { g.setColor(new Color(255, 80, 40, 170)); g.fillOval(px - 16, py - 16, 32, 32); }
        g.dispose();
    }
    /** Pneu em chamas deixa fogo na pista: some sozinho em 1,4 s. */
    /** Pista em chamas da moto do Valdrak: sobe rapido, fica queimando e baixa no fim. */
    static final int PISTA_FOGO = 35, BOLA_FOGO = 36, ATADURA = 37, BLOCO = 38, SOLDADO = 39, CARRUAGEM = 40, ESFINGE = 41, RAINHA = 42, TINTA = 43, ESTRELA_ECLIPSE = 44, TORTA = 45, CORNETA = 46, CONFETE = 47, MORTO = 48, LOBISOMEM = 49, OSSO_PE = 50, OSSO_SOCO = 51, OSSO_NOTA = 52, PEGADA = 53, DIABRETE = 54, CAO = 55, GARGULA = 56, CUSPIDOR = 57;
    static final int LAVA_Y = Config.ALTURA - 78; private static final double PISTA_DURA = 2.6; // LAVA_Y: superficie do rio de lava do inferno
    private double fogoPista() { return Math.max(0, Math.min(1, Math.min(tempo / .15, (PISTA_DURA - tempo) / .6))); }
    private void desenharPistaFogo(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create(); double f = fogoPista();
        g.setColor(new Color(255, 120, 30, (int) (70 * f))); g.fillOval(px - 24, py - (int) (30 * f), 48, (int) (36 * f) + 8);
        for (int k = 0; k < 4; k++) {
            int h = (int) ((22 + (Math.sin(tempo * 18 + px * .13 + k * 1.7) + 1) * 9) * f), lx = px - 15 + k * 10;
            g.setColor(new Color(255, 70 + k * 35, 20, (int) (235 * f))); g.fillPolygon(new int[]{lx - 7, lx, lx + 7}, new int[]{py + 6, py + 6 - h, py + 6}, 3);
            g.setColor(new Color(255, 230, 140, (int) (220 * f))); g.fillPolygon(new int[]{lx - 3, lx, lx + 3}, new int[]{py + 6, py + 6 - h / 2, py + 6}, 3);
        }
        g.setColor(new Color(255, 200, 90, (int) (230 * f))); g.fillRect(px - 19, py + 2, 38, 5);
        g.dispose();
    }
    private int[] tamEgito() { switch (estilo) { case BLOCO: return new int[]{90, 82}; case SOLDADO: return new int[]{62, 102}; case CARRUAGEM: return new int[]{180, 120}; case ESFINGE: return new int[]{170, 155}; default: return new int[]{120, 98}; } }
    private void desenharEgito(Graphics2D tela, int px, int py) {
        java.awt.image.BufferedImage img = estilo == BLOCO ? FaraoBob.BLOCO : estilo == SOLDADO ? ((int) (tempo * 5) % 2 == 0 ? FaraoBob.SOLDADO0 : FaraoBob.SOLDADO1)
                : estilo == CARRUAGEM ? FaraoBob.CARRUAGEM : estilo == ESFINGE ? FaraoBob.ESFINGE : FaraoBob.RAINHA;
        if (img == null) return; int[] t = tamEgito();
        Graphics2D g = (Graphics2D) tela.create(); g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (estilo == BLOCO) g.rotate(Math.sin(tempo * 3) * .15, px, py);
        g.drawImage(img, px - t[0] / 2, py - t[1] / 2, t[0], t[1], null); g.dispose();
    }
    /** Atadura voando: uma faixa de linho ondulando. */
    private static final java.awt.image.BufferedImage ARTE_TORTA = ler("palhaco_torta.png"), ARTE_CORNETA = ler("palhaco_corneta.png"), ARTE_CONFETE = ler("palhaco_confete.png"), ARTE_MORCEGO_EC = ler("poder_morcego_eclipse.png");
    /** Desenha o quadro q de uma folha de n quadros com tamanho t, centrado no tiro (virado para onde vai, se pedido). */
    private boolean quadros(Graphics2D tela, java.awt.image.BufferedImage a, int n, int q, int t, int px, int py, boolean gira) {
        if (a == null) return false; int fw = a.getWidth() / n, fh = a.getHeight(), h = t * fh / fw; Graphics2D g = (Graphics2D) tela.create();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); g.translate(px, py);
        if (gira) g.rotate(Math.atan2(vy, vx) + Math.PI); if (!gira && vx > 0) g.scale(-1, 1);
        g.drawImage(a, -t / 2, -h / 2, t / 2, h / 2, q * fw, 0, q * fw + fw, fh, null); g.dispose(); return true;
    }
    /** Horda da Danca Macabra: a caveira e os mortos que existirem (4 quadros cada, virados para a esquerda). */
    private static java.awt.image.BufferedImage[] MORTOS;
    static final String[] NOMES_MORTOS = {"caveira_dancante", "morto_noiva", "morto_atleta", "morto_medico", "morto_advogado", "morto_sem_braco", "morto_sem_perna", "morto_cozinheiro", "morto_roqueiro"};
    static synchronized java.awt.image.BufferedImage[] mortos() {
        if (MORTOS == null) { java.util.List<java.awt.image.BufferedImage> l = new java.util.ArrayList<>(); for (String n : NOMES_MORTOS) { java.awt.image.BufferedImage b = ler(n + ".png"); if (b != null) l.add(b); } MORTOS = l.toArray(new java.awt.image.BufferedImage[0]); }
        return MORTOS;
    }
    private static final java.awt.image.BufferedImage ARTE_PE = ler("ossada_pegada.png"), ARTE_SOCO = ler("ossada_soco.png"), ARTE_ONOTA = ler("ossada_nota.png");
    private static final java.util.Map<java.awt.image.BufferedImage, java.awt.image.BufferedImage> ROXOS = new java.util.HashMap<>();
    /** Versao roxa de uma arte de fogo verde (o fogo-fatuo do gemeo roxo). */
    private static synchronized java.awt.image.BufferedImage roxoDe(java.awt.image.BufferedImage f) {
        return ROXOS.computeIfAbsent(f, k -> { java.awt.image.BufferedImage o = new java.awt.image.BufferedImage(k.getWidth(), k.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int yy = 0; yy < k.getHeight(); yy++) for (int xx = 0; xx < k.getWidth(); xx++) { int c = k.getRGB(xx, yy), al = c >>> 24, r = (c >> 16) & 255, gg = (c >> 8) & 255, b = c & 255;
                boolean fogo = gg > r + 25 && gg > b + 25; o.setRGB(xx, yy, fogo ? (al << 24) | (Math.min(255, gg * 8 / 10 + r / 3) << 16) | (Math.min(255, r / 2 + b / 3) << 8) | Math.min(255, gg + b / 2) : c); }
            return o; });
    }
    private static java.awt.image.BufferedImage peRoxoCache;
    /** O pe de fogo verde recolorido de roxo (o chute do outro gemeo). */
    private static synchronized java.awt.image.BufferedImage peRoxo() {
        if (peRoxoCache == null) { java.awt.image.BufferedImage f = ARTE_PE.getSubimage(0, 0, ARTE_PE.getWidth() / 4, ARTE_PE.getHeight()); peRoxoCache = new java.awt.image.BufferedImage(f.getWidth(), f.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int yy = 0; yy < f.getHeight(); yy++) for (int xx = 0; xx < f.getWidth(); xx++) { int c = f.getRGB(xx, yy), al = c >>> 24, r = (c >> 16) & 255, gg = (c >> 8) & 255, b = c & 255; peRoxoCache.setRGB(xx, yy, (al << 24) | (Math.min(255, gg * 8 / 10 + r / 3) << 16) | (Math.min(255, r / 2 + b / 3) << 8) | Math.min(255, gg + b / 2)); } }
        return peRoxoCache;
    }
    private static final java.awt.image.BufferedImage ARTE_DIABRETE = ler("demonio_diabrete.png"), ARTE_CAO = ler("demonio_cao.png"), ARTE_GARGULA = ler("demonio_gargula.png"), ARTE_CUSPIDOR = ler("demonio_cuspidor.png");
    /** Demonios do inferno: com a arte (4 quadros virados para a esquerda) ou um desenho simples ate ela chegar. */
    private void desenharDemonio(Graphics2D g, int px, int py) {
        java.awt.image.BufferedImage arte = estilo == DIABRETE ? ARTE_DIABRETE : estilo == CAO ? ARTE_CAO : estilo == GARGULA ? ARTE_GARGULA : ARTE_CUSPIDOR;
        int tam = estilo == DIABRETE ? 52 : estilo == CAO ? 90 : estilo == GARGULA ? 84 : 120, q = (int) (tempo * 10 + variante) % 4;
        if (arte != null && quadros(g, arte, 4, q, tam, px, py, false)) return;
        Graphics2D a = (Graphics2D) g.create(); a.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF); double bat = Math.sin(tempo * 22 + variante);
        if (estilo == DIABRETE) { a.setColor(new Color(90, 10, 20)); int asa = (int) (bat * 9);
            a.fillPolygon(new int[]{px - 4, px + 16, px + 22}, new int[]{py - 2, py - 16 - asa, py + 2}, 3); a.fillPolygon(new int[]{px + 2, px - 16, px - 20}, new int[]{py - 2, py - 16 + asa, py + 2}, 3);
            a.setColor(new Color(220, 40, 30)); a.fillOval(px - 9, py - 9, 18, 20); a.setColor(new Color(255, 230, 80)); a.fillRect(px - 5, py - 4, 3, 3); a.fillRect(px + 1, py - 4, 3, 3);
            a.setColor(new Color(60, 0, 0)); a.drawLine(px - 7, py - 8, px - 10, py - 15); a.drawLine(px + 5, py - 8, px + 8, py - 15); a.drawLine(px - 14, py + 8, px - 24, py + 4); }
        else if (estilo == CAO) { int pata = (int) (Math.sin(tempo * 18) * 6); a.setColor(new Color(60, 10, 0)); a.fillRect(px - 26, py + 6, 7, 16 + pata); a.fillRect(px + 16, py + 6, 7, 16 - pata);
            a.setColor(new Color(200, 60, 10)); a.fillRoundRect(px - 30, py - 12, 56, 26, 12, 12); a.fillOval(px - 44, py - 22, 28, 24); a.setColor(new Color(255, 200, 60)); a.fillRect(px - 36, py - 15, 4, 4);
            a.setColor(new Color(255, 120, 20, 200)); a.fillPolygon(new int[]{px - 44, px - 60, px - 44}, new int[]{py - 10, py - 4 + pata, py}, 3); a.setColor(new Color(255, 220, 120)); a.drawLine(px - 10, py - 8, px + 6, py + 4); a.drawLine(px + 6, py - 10, px + 18, py); }
        else if (estilo == GARGULA) { int asa = (int) (bat * 6); a.setColor(new Color(70, 70, 80)); a.fillPolygon(new int[]{px, px + 38, px + 30}, new int[]{py - 6, py - 30 - asa, py + 8}, 3); a.fillPolygon(new int[]{px, px - 34, px - 26}, new int[]{py - 6, py - 30 + asa, py + 8}, 3);
            a.setColor(new Color(110, 110, 120)); a.fillRoundRect(px - 16, py - 20, 32, 42, 10, 10); a.setColor(new Color(50, 50, 60)); a.drawRoundRect(px - 16, py - 20, 32, 42, 10, 10); a.setColor(new Color(255, 40, 30)); a.fillRect(px - 9, py - 12, 5, 4); a.fillRect(px + 4, py - 12, 5, 4); }
        else { int r = 44 + (int) (Math.sin(tempo * 6) * 3); a.setColor(new Color(120, 20, 0)); a.fillOval(px - r, py - r, r * 2, r * 2); a.setColor(new Color(230, 90, 10)); a.fillOval(px - r + 8, py - r + 8, r * 2 - 16, r * 2 - 22);
            a.setColor(new Color(255, 220, 90)); for (int k = 0; k < 4; k++) a.drawLine(px - 30 + k * 18, py - 30 + (k % 2) * 12, px - 20 + k * 18, py + 10); a.setColor(new Color(40, 0, 0)); a.fillOval(px - 34, py - 4, 26, 20);
            a.setColor(new Color(255, 255, 160)); a.fillRect(px - 14, py - 22, 6, 6); a.fillRect(px + 4, py - 22, 6, 6); }
        a.dispose();
    }
    private static final java.awt.image.BufferedImage ARTE_LOBO = ler("lobisomem_corre.png");
    private static final java.awt.image.BufferedImage ARTE_COROA = ler("poder_coroa.png");
    private static final java.awt.image.BufferedImage ARTE_TINTA = ler("nuvem_tinta.png");
    /** Nuvem de tinta da Lula (nuvem_tinta.png, 4 quadros): cresce no primeiro segundo e depois ondula; esconde o que passa por dentro. */
    private void desenharTinta(Graphics2D tela, int px, int py) {
        if (ARTE_TINTA == null) { tela.setColor(new Color(14, 6, 24, 238)); tela.fillOval(px - 60, py - 45, 120, 90); return; }
        Graphics2D g = (Graphics2D) tela.create(); g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int q = tempo < .25 ? 0 : tempo < .55 ? 1 : tempo < .9 ? 2 : 2 + (int) (tempo * 3) % 2, fw = ARTE_TINTA.getWidth() / 4, fh = ARTE_TINTA.getHeight(), t = 250;
        g.drawImage(ARTE_TINTA, px - t / 2, py - t / 2, px + t / 2, py + t / 2, q * fw, 0, q * fw + fw, fh, null);
        g.dispose();
    }
    private static final java.awt.image.BufferedImage ARTE_ATADURA = ler("atadura_voando.png");
    private void desenharAtadura(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create(); g.translate(px, py); g.rotate(Math.atan2(vy, vx));
        if (ARTE_ATADURA != null) { // 4 quadros em pixel art; a ponta desfiada fica para tras (o quadro 1 vem espelhado)
            int fw = ARTE_ATADURA.getWidth() / 4, fh = ARTE_ATADURA.getHeight(), q = (int) (tempo * 9) % 4, w = 62, h = w * fh / fw;
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if (q == 1) g.drawImage(ARTE_ATADURA, w / 2 - 10, -h / 2, -w / 2 - 10, h / 2, q * fw, 0, q * fw + fw, fh, null);
            else g.drawImage(ARTE_ATADURA, -w / 2 - 10, -h / 2, w / 2 - 10, h / 2, q * fw, 0, q * fw + fw, fh, null);
            g.dispose(); return;
        }
        int n = 7; int[] xs = new int[n], ys = new int[n];
        for (int k = 0; k < n; k++) { xs[k] = -k * 6; ys[k] = (int) (Math.sin(tempo * 18 + k) * 4); }
        g.setStroke(new java.awt.BasicStroke(7)); g.setColor(new Color(150, 120, 90)); g.drawPolyline(xs, ys, n);
        g.setStroke(new java.awt.BasicStroke(4)); g.setColor(new Color(232, 215, 175)); g.drawPolyline(xs, ys, n);
        g.dispose();
    }
    /** Bola de fogo com cauda de chamas apontando para tras do movimento. */
    private void desenharBolaFogo(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create(); g.translate(px, py); g.rotate(Math.atan2(vy + queda, vx));
        double fl = Math.sin(tempo * 40) * 2;
        g.setColor(new Color(255, 80, 20, 120)); g.fillPolygon(new int[]{6, -34 - (int) fl, 6}, new int[]{-11, 0, 11}, 3);
        g.setColor(new Color(255, 150, 30, 200)); g.fillPolygon(new int[]{5, -24 + (int) fl, 5}, new int[]{-8, 0, 8}, 3);
        g.setColor(new Color(255, 90, 20)); g.fillOval(-11, -11, 22, 22);
        g.setColor(new Color(255, 190, 60)); g.fillOval(-7, -7, 15, 15);
        g.setColor(new Color(255, 250, 210)); g.fillOval(-3, -4, 8, 8);
        g.dispose();
    }
    private void desenharRastro(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        double vida = 1 - tempo / 1.4; int a = (int) (230 * Math.max(0, Math.min(1, vida * 2)));
        for (int k = 0; k < 3; k++) {
            int h = 10 + (int) ((Math.sin(tempo * 22 + px * .1 + k * 2) + 1) * 7), lx = px - 9 + k * 6;
            g.setColor(new Color(255, 90 + k * 40, 20, a)); g.fillPolygon(new int[]{lx - 5, lx, lx + 5}, new int[]{py + 6, py + 6 - h, py + 6}, 3);
        }
        g.setColor(new Color(255, 230, 140, a)); g.fillRect(px - 6, py + 2, 12, 4);
        g.dispose();
    }
    private static final java.awt.image.BufferedImage ARTE_VAGAO = ler("malachar_trem_vagao.png");
    private static java.awt.image.BufferedImage ler(String n) { java.net.URL u = TiroChefe.class.getResource("/ImagemIcon/" + n); if (u == null) return null; try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; } }
    /** Baforada da chamine: nuvem roxa que cresce e se desfaz enquanto anda. */
    private void desenharFumaca(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        int r = (int) (16 + Math.min(14, tempo * 16));
        for (int k = 0; k < 6; k++) {
            double a = k * 1.05 + tempo * 2.2; int ox = (int) (Math.cos(a) * r * .55), oy = (int) (Math.sin(a) * r * .45), rr = r * (k % 2 == 0 ? 7 : 5) / 9;
            g.setColor(new Color(k % 3 == 0 ? 190 : 130, 60, k % 3 == 0 ? 240 : 190, 95));
            g.fillOval(px + ox - rr, py + oy - rr, rr * 2, rr * 2);
        }
        g.setColor(new Color(240, 200, 255, 150)); g.fillOval(px - r / 4, py - r / 4, r / 2, r / 2);
        g.dispose();
    }
    /** Vagao de espinhos arrancado do Trem Maluco, voando de nariz para a nave. */
    private void desenharVagao(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        double ang = Math.atan2(vy, vx) - Math.PI; // a arte ja aponta para a esquerda
        g.rotate(ang * .6, px, py);
        if (ARTE_VAGAO != null) {
            int w = 190, h = ARTE_VAGAO.getHeight() * w / ARTE_VAGAO.getWidth();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(ARTE_VAGAO, px - w / 2 + 10, py - h / 2, w, h, null);
        } else {
            g.setColor(new Color(40, 34, 44)); g.fillRect(px - 60, py - 22, 130, 44);
            g.setColor(new Color(200, 80, 255)); for (int k = 0; k < 5; k++) g.fillRect(px - 40 + k * 22, py - 10, 10, 14);
            g.setColor(new Color(170, 170, 180)); g.fillPolygon(new int[]{px - 60, px - 90, px - 60}, new int[]{py - 18, py, py + 18}, 3);
        }
        g.dispose();
    }
    /** Labareda roxa do dragao: nucleo claro, borda violeta, rastro curto. */
    private void desenharFogoRoxo(Graphics2D tela, int px, int py) {
        Graphics2D g = (Graphics2D) tela.create();
        double d = Math.atan2(vy, vx);
        for (int k = 4; k >= 0; k--) {
            int tx = px - (int) (Math.cos(d) * k * 7), ty = py - (int) (Math.sin(d) * k * 7), r = 13 - k * 2;
            g.setColor(new Color(150 + k * 10, 40, 230, 200 - k * 35)); g.fillRect(tx - r, ty - r, r * 2, r * 2);
        }
        int f = (int) (tempo * 20) % 2;
        g.setColor(new Color(225, 150, 255)); g.fillRect(px - 7 + f, py - 7, 14, 14);
        g.setColor(new Color(255, 235, 255)); g.fillRect(px - 3, py - 3, 6, 6);
        g.dispose();
    }
    public boolean isVisible() { return visivel; }
    public void setVisible(boolean v) { visivel = v; }
}
