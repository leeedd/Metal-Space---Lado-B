package Jogo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;

/**
 * Cabine em primeira pessoa da fuga: moldura e painel com a cara de cada nave, so as maos do piloto (sem rosto),
 * e instrumentos ligados aos dados reais da fuga: velocimetro, potencia, altitude, temperatura, municao, misseis,
 * bombas, recarga, escudo, dobra, radar, mapa da subida e diagnostico de danos.
 * Ate a arte chegar (cockpit_N.png e maos_piloto.png), tudo e desenhado aqui.
 */
final class FugaCockpit {
    static final int W = 960, H = 600, PAINEL = 395;
    private static final Polygon VIDRO = new Polygon(new int[]{70, 890, 935, 25}, new int[]{38, 38, PAINEL, PAINEL}, 4);
    // paleta por nave: 0 Raptor, 1 Shadow, 2 Valkyrie, 3 Berserker (militar verde), 4 Metal God
    private static final Color[][] PALETA = {
        {new Color(205, 205, 210), new Color(150, 150, 160), new Color(200, 40, 40), new Color(120, 255, 140)},
        {new Color(28, 28, 34), new Color(16, 16, 20), new Color(220, 30, 40), new Color(255, 70, 70)},
        {new Color(225, 220, 205), new Color(170, 150, 110), new Color(215, 175, 60), new Color(120, 200, 255)},
        {new Color(78, 92, 52), new Color(52, 62, 34), new Color(150, 130, 70), new Color(120, 255, 90)},
        {new Color(26, 22, 22), new Color(12, 10, 10), new Color(210, 165, 50), new Color(255, 120, 40)}};
    private static final BufferedImage[] ARTE = new BufferedImage[5]; private static final BufferedImage MAOS = ler("maos_piloto.png");
    static { for (int i = 0; i < 5; i++) ARTE[i] = ler("cockpit_" + i + ".png"); }
    private final int modelo; private final Color metal, escuro, detalhe, luz;
    FugaCockpit(int modelo) { this.modelo = Math.floorMod(modelo, 5); Color[] p = PALETA[this.modelo]; metal = p[0]; escuro = p[1]; detalhe = p[2]; luz = p[3]; }

    void desenhar(Graphics2D g, FugaFinal f) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        vidro(g, f);
        if (ARTE[modelo] != null) g.drawImage(ARTE[modelo], 0, 0, W, H, null); else moldura(g, f);
        instrumentos(g, f);
        maos(g, f);
        luzes(g, f);
        if (f.falaT > 0) texto(g, f.fala, 70, new Color(140, 255, 150));
        if (f.tempo < FugaFinal.INTRO + .7) { g.setColor(new Color(0, 0, 0, (int) (255 * Math.max(0, 1 - (f.tempo - FugaFinal.INTRO) / .7)))); g.fillRect(0, 0, W, H); } // entra na cabine
    }
    // ---------------- vidro: rachaduras, reflexo e a mira ----------------
    private void vidro(Graphics2D g, FugaFinal f) {
        Graphics2D a = (Graphics2D) g.create(); a.clip(VIDRO);
        a.setColor(new Color(255, 255, 255, 18)); a.fillPolygon(new int[]{140, 260, 120, 60}, new int[]{40, 40, 395, 395}, 4); // reflexo
        a.setColor(new Color(luz.getRed(), luz.getGreen(), luz.getBlue(), 150)); int cx = FugaPista.CX, cy = FugaPista.CY; a.drawOval(cx - 22, cy - 22, 44, 44); a.drawLine(cx - 34, cy, cx - 12, cy); a.drawLine(cx + 12, cy, cx + 34, cy); a.drawLine(cx, cy - 34, cx, cy - 12); // mira
        double e = f.sist[FugaFinal.ESTRUTURA]; if (e < 65) { a.setColor(new Color(230, 240, 255, 170)); a.setStroke(new BasicStroke(1.5f)); java.util.Random r = new java.util.Random(5);
            int n = e < 30 ? 4 : e < 50 ? 2 : 1; for (int k = 0; k < n; k++) { int ox = k % 2 == 0 ? 110 + r.nextInt(120) : 700 + r.nextInt(150), oy = 60 + r.nextInt(250); for (int i = 0; i < 7; i++) { double an = i * .9 + r.nextDouble(); int len = 30 + r.nextInt(90); a.drawLine(ox, oy, ox + (int) (Math.cos(an) * len), oy + (int) (Math.sin(an) * len)); } } }
        a.dispose();
    }
    // ---------------- moldura e painel (desenho provisorio de cada nave) ----------------
    private void moldura(Graphics2D g, FugaFinal f) {
        Area a = new Area(new Rectangle(0, 0, W, H)); a.subtract(new Area(VIDRO));
        g.setPaint(new GradientPaint(0, 0, metal, 0, H, escuro)); g.fill(a);
        g.setColor(escuro); g.setStroke(new BasicStroke(10)); g.drawPolygon(VIDRO); g.setColor(detalhe); g.setStroke(new BasicStroke(3)); g.drawPolygon(VIDRO);
        g.setColor(escuro); g.fillRect(470, 0, 20, 40); // coluna do meio
        g.setPaint(new GradientPaint(0, PAINEL, metal.darker(), 0, H, escuro.darker())); g.fillRect(0, PAINEL + 4, W, H - PAINEL);
        g.setColor(escuro.darker()); for (int x = 0; x < W; x += 160) g.drawLine(x, PAINEL + 4, x, H); g.drawLine(0, PAINEL + 6, W, PAINEL + 6);
        if (modelo == 3) { g.setColor(new Color(30, 34, 20)); for (int x = 14; x < W; x += 34) { g.fillOval(x, PAINEL + 12, 6, 6); g.fillOval(x, H - 14, 6, 6); } for (int y = 50; y < PAINEL; y += 34) { g.fillOval(10, y, 6, 6); g.fillOval(W - 16, y, 6, 6); } // rebites da blindagem
            g.setColor(new Color(40, 46, 26)); g.fillRect(0, 0, W, 16); g.setColor(new Color(170, 150, 60)); for (int x = 0; x < W; x += 40) g.fillPolygon(new int[]{x, x + 20, x + 28, x + 8}, new int[]{0, 0, 16, 16}, 4); } // faixa de alerta
        else if (modelo == 4) { g.setColor(detalhe); for (int x = 40; x < W; x += 120) { caveira(g, x, 20, 9); } for (int k = 0; k < 7; k++) g.fillPolygon(new int[]{30 + k * 140, 40 + k * 140, 35 + k * 140}, new int[]{PAINEL + 4, PAINEL + 4, PAINEL - 12}, 3); } // caveiras e espinhos
        else if (modelo == 1) { g.setColor(new Color(220, 30, 40, 120)); g.fillRect(0, PAINEL + 1, W, 2); g.fillRect(70, 36, 820, 2); } // filetes de luz discretos
        else if (modelo == 2) { g.setColor(detalhe); g.fillPolygon(new int[]{430, 480, 530, 480}, new int[]{16, 4, 16, 30}, 4); g.fillPolygon(new int[]{360, 440, 440}, new int[]{20, 10, 26}, 3); g.fillPolygon(new int[]{600, 520, 520}, new int[]{20, 10, 26}, 3); } // asas douradas
    }
    private static void caveira(Graphics2D g, int x, int y, int r) { g.fillOval(x - r, y - r, r * 2, r * 2); g.fillRect(x - r / 2, y + r / 2, r, r / 2 + 2); Color c = g.getColor(); g.setColor(Color.BLACK); g.fillOval(x - r / 2 - 1, y - 2, r / 2, r / 2); g.fillOval(x + 1, y - 2, r / 2, r / 2); g.setColor(c); }
    // ---------------- instrumentos ----------------
    private void instrumentos(Graphics2D g, FugaFinal f) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        // velocimetro
        mostrador(g, 92, 470, 46, f.vel / 48, "VEL", String.format("%03d", (int) (f.vel * 21)), f.vel > 40 ? new Color(255, 90, 60) : luz);
        // altitude e temperatura
        visor(g, 30, 530, 124, 26); g.setColor(luz); g.drawString("ALT " + String.format("%04d", (int) ((FugaPista.LAVA - f.sy) * 180 + f.progresso() * 6000)) + "m", 36, 547);
        barra(g, 30, 562, 124, "TEMP", f.temperatura / 120, f.temperatura > 90 ? new Color(255, 60, 40) : f.temperatura > 70 ? new Color(255, 170, 40) : luz);
        // potencia (escala ao lado da alavanca)
        visor(g, 176, 420, 30, 160); int ph = (int) (150 * f.potencia), pm = (int) (150 * f.potenciaMax()); g.setColor(new Color(255, 60, 40, 120)); g.fillRect(180, 425, 22, 150 - pm);
        g.setColor(f.potencia > .85 ? new Color(255, 200, 60) : luz); g.fillRect(180, 575 - ph, 22, ph); g.setColor(Color.WHITE); g.drawString("POT", 178, 592);
        // mapa da subida
        mapa(g, f, 286, 408, 74, 178);
        // radar
        radar(g, f, 470, 496, 82);
        // diagnostico de danos
        diagnostico(g, f, 700, 410);
        // municao e sistemas de armas
        int bx = 856, by = 410; visor(g, bx - 6, by - 4, 104, 184); g.setColor(luz);
        g.drawString("CANHAO " + f.municao, bx, by + 10); barra(g, bx, by + 16, 88, "", Math.min(1, f.calor / 100), f.calor > 80 ? new Color(255, 60, 40) : new Color(255, 170, 40));
        g.setColor(luz); g.drawString("MISSEIS " + f.misseis, bx, by + 44); barra(g, bx, by + 50, 88, "", 1 - f.recargaMissil, luz);
        g.setColor(luz); g.drawString("BOMBAS  " + f.bombas, bx, by + 78); for (int i = 0; i < 3; i++) { g.setColor(i < f.bombas ? new Color(255, 70, 40) : new Color(60, 30, 30)); g.fillOval(bx + i * 20, by + 84, 14, 14); }
        g.setColor(luz); g.drawString("ESCUDO", bx, by + 116); barra(g, bx, by + 122, 88, "", f.escudo / 100, new Color(90, 200, 255));
        g.setColor(luz); g.drawString("DOBRA " + (int) f.dobra + "%", bx, by + 150); barra(g, bx, by + 156, 88, "", f.dobra / 100, f.dobra >= 100 ? new Color(255, 255, 255) : new Color(200, 130, 255));
        if (f.travaArmas > 0) { g.setColor(new Color(255, 60, 40)); g.drawString("TRAVADO", bx + 20, by + 30); }
    }
    private void visor(Graphics2D g, int x, int y, int w, int h) { g.setColor(new Color(8, 12, 8)); g.fillRoundRect(x, y, w, h, 6, 6); g.setColor(escuro.darker()); g.drawRoundRect(x, y, w, h, 6, 6); }
    private void barra(Graphics2D g, int x, int y, int w, String nome, double v, Color c) { g.setColor(new Color(20, 24, 20)); g.fillRect(x, y, w, 8); g.setColor(c); g.fillRect(x, y, (int) (w * Math.max(0, Math.min(1, v))), 8); if (!nome.isEmpty()) { g.setColor(Color.WHITE); g.drawString(nome, x, y + 20); } }
    private void mostrador(Graphics2D g, int cx, int cy, int r, double v, String nome, String valor, Color c) {
        g.setColor(new Color(8, 12, 8)); g.fillOval(cx - r, cy - r, r * 2, r * 2); g.setColor(escuro.darker()); g.setStroke(new BasicStroke(3)); g.drawOval(cx - r, cy - r, r * 2, r * 2); g.setStroke(new BasicStroke(1));
        for (int i = 0; i <= 10; i++) { double an = Math.PI * .75 + i * Math.PI * 1.5 / 10; g.setColor(i >= 8 ? new Color(255, 70, 50) : luz); g.drawLine(cx + (int) (Math.cos(an) * (r - 4)), cy + (int) (Math.sin(an) * (r - 4)), cx + (int) (Math.cos(an) * (r - 10)), cy + (int) (Math.sin(an) * (r - 10))); }
        double an = Math.PI * .75 + Math.max(0, Math.min(1.05, v)) * Math.PI * 1.5; g.setColor(c); g.setStroke(new BasicStroke(3)); g.drawLine(cx, cy, cx + (int) (Math.cos(an) * (r - 8)), cy + (int) (Math.sin(an) * (r - 8))); g.setStroke(new BasicStroke(1));
        g.setColor(Color.WHITE); g.drawString(nome, cx - 9, cy + 20); g.setColor(c); g.drawString(valor, cx - 10, cy + 32);
    }
    private void mapa(Graphics2D g, FugaFinal f, int x, int y, int w, int h) {
        visor(g, x, y, w, h); boolean falha = f.sist[FugaFinal.NAVEGACAO] < 40 && (int) (f.tempo * 7) % 3 == 0;
        g.setColor(Color.WHITE); g.drawString("ROTA", x + 24, y + 12); if (falha) { g.setColor(new Color(255, 80, 60)); g.drawString("FALHA", x + 18, y + h / 2); return; }
        int top = y + 22, bot = y + h - 10; g.setColor(new Color(255, 110, 20, 160)); for (int i = 0; i < 20; i++) { int yy = bot - i * (bot - top) / 20, xx = x + w / 2 + (int) (Math.sin(i * .9) * 14); g.fillRect(xx - 3, yy, 6, (bot - top) / 20); } // o rio subindo
        for (int c = 1; c < 4; c++) { int yy = bot - c * (bot - top) / 4; g.setColor(new Color(255, 255, 255, 90)); g.drawLine(x + 6, yy, x + w - 6, yy); } // setores
        if ((int) (f.tempo * 4) % 2 == 0 || f.progresso() < .8) { g.setColor(new Color(255, 255, 255)); g.fillOval(x + w / 2 - 6, top - 6, 12, 8); g.drawString("SAIDA", x + w / 2 - 15, top - 8 + 22); } // a saida
        int ny = (int) (bot - f.progresso() * (bot - top)); g.setColor(luz); g.fillPolygon(new int[]{x + w / 2 - 6, x + w / 2 + 6, x + w / 2}, new int[]{ny + 5, ny + 5, ny - 6}, 3); // a nave
        int cy = (int) (bot - Math.max(0, f.colapsoDist / FugaFinal.DIST_TOTAL) * (bot - top)); g.setColor(new Color(255, 40, 20, 180)); g.fillRect(x + 4, cy, w - 8, Math.max(2, bot - cy)); // o colapso subindo atras
    }
    private void radar(Graphics2D g, FugaFinal f, int cx, int cy, int r) {
        g.setColor(new Color(6, 14, 8)); g.fillOval(cx - r, cy - r, r * 2, r * 2); g.setColor(escuro.darker()); g.setStroke(new BasicStroke(4)); g.drawOval(cx - r, cy - r, r * 2, r * 2); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(luz.getRed(), luz.getGreen(), luz.getBlue(), 70)); g.drawOval(cx - r / 2, cy - r / 2, r, r); g.drawLine(cx - r, cy, cx + r, cy); g.drawLine(cx, cy - r, cx, cy + r);
        double sw = f.tempo * 3; g.setColor(new Color(luz.getRed(), luz.getGreen(), luz.getBlue(), 120)); g.drawLine(cx, cy, cx + (int) (Math.cos(sw) * r), cy + (int) (Math.sin(sw) * r));
        boolean ruim = f.sist[FugaFinal.RADAR] < 40; boolean alerta = false;
        for (FugaPista.Obj o : f.pista.objs) { if (o.z > 30 || ruim && (o.var % 2 == 0)) continue; // a nave no centro; a frente para cima; os lados nos lados
            int x = cx + (int) ((o.x - f.sx) / 1.6 * r * .9), y = cy - (int) (o.z / 30 * r * .95); if (Math.hypot(x - cx, y - cy) > r - 4) continue;
            Color c = o.tipo == FugaPista.GARGULA ? new Color(255, 60, 60) : o.tipo == FugaPista.ROCHA ? new Color(255, 160, 60) : o.tipo == FugaPista.DESMORONE ? new Color(255, 255, 255) : new Color(255, 230, 80);
            g.setColor(c); int s = o.tipo == FugaPista.GARGULA || o.tipo == FugaPista.DESMORONE ? 7 : 5; g.fillRect(x - s / 2, y - s / 2, s, s); if (o.tipo == FugaPista.GARGULA && o.z < 10) alerta = true; }
        double atras = f.pista.distancia - f.colapsoDist; if (atras < 60) { g.setColor(new Color(255, 40, 20, (int) (220 * (1 - Math.max(0, atras) / 60)))); g.setStroke(new BasicStroke(5)); g.drawArc(cx - r + 6, cy - r + 6, r * 2 - 12, r * 2 - 12, 210, 120); g.setStroke(new BasicStroke(1)); } // ameaca por tras
        if (ruim) { java.util.Random rr = new java.util.Random((long) (f.tempo * 20)); g.setColor(new Color(luz.getRed(), luz.getGreen(), luz.getBlue(), 90)); for (int i = 0; i < 40; i++) g.fillRect(cx - r / 2 + rr.nextInt(r), cy - r / 2 + rr.nextInt(r), 2, 2); }
        g.setColor(luz); g.fillPolygon(new int[]{cx - 5, cx + 5, cx}, new int[]{cy + 5, cy + 5, cy - 6}, 3); g.setColor(Color.WHITE); g.drawString("RADAR", cx - 15, cy + r - 8);
        if (alerta && (int) (f.tempo * 6) % 2 == 0) { g.setColor(new Color(255, 60, 60)); g.drawString("AMEACA PROXIMA", cx - 42, cy - r + 16); }
    }
    private void diagnostico(Graphics2D g, FugaFinal f, int x, int y) {
        visor(g, x, y, 146, 178); g.setColor(Color.WHITE); g.drawString("DIAGNOSTICO", x + 36, y + 12);
        int cx = x + 46, cy = y + 92; double[] s = f.sist; boolean pisca = (int) (f.tempo * 6) % 2 == 0;
        g.setColor(cor(s[FugaFinal.ESCUDOS], pisca)); g.setStroke(new BasicStroke(2)); g.drawOval(cx - 40, cy - 60, 80, 120); g.setStroke(new BasicStroke(1)); // escudo em volta
        g.setColor(cor(s[FugaFinal.ARMAS], pisca)); g.fillPolygon(new int[]{cx - 6, cx - 34, cx - 34, cx - 6}, new int[]{cy - 4, cy + 22, cy + 32, cy + 16}, 4); g.fillPolygon(new int[]{cx + 6, cx + 34, cx + 34, cx + 6}, new int[]{cy - 4, cy + 22, cy + 32, cy + 16}, 4); // asas e canhoes
        g.setColor(cor(s[FugaFinal.ESTRUTURA], pisca)); g.fillRect(cx - 7, cy - 22, 14, 52); // casco
        g.setColor(cor(s[FugaFinal.NAVEGACAO], pisca)); g.fillPolygon(new int[]{cx - 7, cx + 7, cx}, new int[]{cy - 22, cy - 22, cy - 50}, 3); // nariz (navegacao)
        g.setColor(cor(s[FugaFinal.RADAR], pisca)); g.fillOval(cx - 4, cy - 34, 8, 8); // antena do radar
        g.setColor(cor(s[FugaFinal.MOTORES], pisca)); g.fillRect(cx - 12, cy + 30, 8, 12); g.fillRect(cx + 4, cy + 30, 8, 12); // motores
        for (int i = 0; i < 6; i++) { g.setColor(cor(s[i], pisca)); g.drawString(FugaFinal.SISTEMAS[i].substring(0, 4) + " " + (int) s[i], x + 92, y + 34 + i * 22); }
    }
    private static Color cor(double v, boolean pisca) { return v > 75 ? new Color(90, 230, 90) : v > 50 ? new Color(240, 220, 60) : v > 25 ? new Color(255, 140, 30) : pisca ? new Color(255, 40, 30) : new Color(120, 20, 15); }
    // ---------------- as maos: alavanca de potencia (esquerda) e manche (direita) ----------------
    private void maos(Graphics2D g, FugaFinal f) {
        int lx = 236, ly = (int) (575 - 140 * f.potencia); // trilho da alavanca
        g.setColor(new Color(15, 15, 15)); g.fillRoundRect(lx - 5, 425, 10, 160, 6, 6);
        g.setColor(escuro.darker()); g.fillRect(lx - 3, ly, 6, 590 - ly); g.setColor(detalhe); g.fillRoundRect(lx - 16, ly - 12, 32, 22, 8, 8); // cabo da alavanca
        g.setColor(f.chaveBomba > 0 ? new Color(255, 80, 40) : new Color(150, 30, 20)); g.fillRect(lx + 10, ly - 20, 10, 8); // chave da bomba
        if (MAOS == null) { Graphics2D m = (Graphics2D) g.create(); m.translate(lx, ly); m.scale(1.9, 1.9); m.translate(-lx, -ly); luva(m, lx, ly, false, false, f.chaveBomba > 0, true); m.dispose(); }
        // manche: inclina com a pilotagem
        int bx = 650, by = 600, tx = bx + (int) (f.manX * 46), ty = 478 + (int) (f.manY * 26);
        g.setColor(new Color(20, 20, 20)); g.fillOval(bx - 34, by - 18, 68, 36); g.setColor(escuro.darker()); g.setStroke(new BasicStroke(12)); g.drawLine(bx, by - 6, tx, ty + 20); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(30, 30, 30)); g.fillRoundRect(tx - 16, ty - 20, 32, 46, 12, 12); g.setColor(f.polegar > 0 ? new Color(255, 70, 40) : new Color(140, 30, 20)); g.fillOval(tx - 6, ty - 26, 12, 9); // botao do missil
        if (MAOS == null) { Graphics2D m = (Graphics2D) g.create(); m.translate(tx, ty); m.scale(1.9, 1.9); m.translate(-tx, -ty); luva(m, tx, ty, f.gatilho > 0, f.polegar > 0, false, false); m.dispose(); }
    }
    /** Luva do piloto segurando um cabo (x, y = centro da pegada). */
    private void luva(Graphics2D g, int x, int y, boolean gatilho, boolean polegar, boolean chave, boolean esquerda) {
        Color luva = modelo == 3 ? new Color(70, 62, 40) : modelo == 2 ? new Color(230, 225, 215) : new Color(40, 34, 30), sombra = luva.darker(), manga = new Color(200, 110, 30);
        int d = esquerda ? -1 : 1;
        g.setColor(manga); g.fillPolygon(new int[]{x - 20 * d, x + 6 * d, x + 40 * d, x + 10 * d}, new int[]{y + 18, y + 22, H, H}, 4); // manga do macacao
        g.setColor(sombra); g.fillRoundRect(x - 22, y - 6, 44, 30, 14, 14); // costas da mao
        g.setColor(luva); g.fillRoundRect(x - 20, y - 8, 40, 26, 12, 12);
        for (int i = 0; i < 4; i++) { int fy = y - 4 + i * 6; if (i == 0 && gatilho) { g.setColor(luva); g.fillRoundRect(x - 26 * d - (d < 0 ? 0 : 6), fy - 6, 12, 7, 5, 5); continue; } // indicador puxando o gatilho
            g.setColor(sombra); g.fillRoundRect(x - 24 * d - (d < 0 ? 0 : 8), fy, 14, 6, 5, 5); g.setColor(luva); g.fillRoundRect(x - 23 * d - (d < 0 ? 0 : 8), fy, 12, 5, 5, 5); }
        g.setColor(luva); int py = polegar || chave ? y - 22 : y - 14; g.fillRoundRect(x + (chave ? 4 * d : -6), py, 12, 14, 6, 6); // polegar (missil ou bomba)
        g.setColor(new Color(255, 255, 255, 40)); g.fillRoundRect(x - 14, y - 6, 20, 5, 4, 4);
    }
    // ---------------- luzes de painel e alarme ----------------
    private void luzes(Graphics2D g, FugaFinal f) {
        for (int i = 0; i < 12; i++) { boolean on = (int) (f.tempo * (3 + i % 4)) % 3 != 0; g.setColor(on ? (i % 5 == 0 ? new Color(255, 70, 50) : luz) : escuro.darker()); g.fillRect(330 + i * 24, PAINEL + 10, 12, 5); }
        if (f.alarme > 0 && (int) (f.tempo * 8) % 2 == 0) { g.setColor(new Color(255, 0, 0, 60)); g.fillRect(0, 0, W, H); g.setColor(new Color(255, 60, 40)); g.fillOval(20, PAINEL + 14, 16, 16); g.fillOval(W - 36, PAINEL + 14, 16, 16); }
        if (f.impacto > .3) { java.util.Random r = new java.util.Random((long) (f.tempo * 30)); g.setColor(new Color(255, 220, 120)); for (int i = 0; i < 14; i++) { int x = 300 + r.nextInt(380), y = PAINEL + r.nextInt(30); g.drawLine(x, y, x + r.nextInt(16) - 8, y - r.nextInt(18)); } } // faiscas no painel
    }
    void texto(Graphics2D g, String s, int y, Color c) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18)); FontMetrics fm = g.getFontMetrics(); int x = (W - fm.stringWidth(s)) / 2;
        g.setColor(new Color(0, 0, 0, 170)); g.fillRect(x - 10, y - 18, fm.stringWidth(s) + 20, 26); g.setColor(c); g.drawString(s, x, y);
    }
    // ---------------- transicao: a camera se aproxima da nave e entra pelo vidro da cabine ----------------
    void desenharIntro(Graphics2D g, FugaFinal f, Nave nave) {
        double e = FugaFinal.INTRO - f.intro, p = Math.min(1, e / (FugaFinal.INTRO - .5)), z = 1 + Math.pow(p, 3) * 12;
        f.pista.desenhar(g, 0, .1, f.tempo, 0);
        if (nave != null) { double ax = nave.getX() + 58, ay = nave.getY() + 16; Graphics2D a = (Graphics2D) g.create(); a.translate(W / 2.0 + (ax - W / 2.0) * (1 - p), H / 2.0 + (ay - H / 2.0) * (1 - p)); a.scale(z, z); a.translate(-ax, -ay); nave.desenhar(a); a.dispose(); }
        if (e > FugaFinal.INTRO - 1.0) { g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, (e - FugaFinal.INTRO + 1.0) / .8)))); g.fillRect(0, 0, W, H); }
        if (e < 1.6) texto(g, "O INFERNO ESTA DESABANDO!", 90, new Color(255, 120, 60));
    }
    private static BufferedImage ler(String n) { java.net.URL u = FugaCockpit.class.getResource("/ImagemIcon/" + n); if (u == null) return null; try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; } }
}
