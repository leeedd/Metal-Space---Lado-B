package Jogo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fuga do buraco negro, o mundo: corredor infernal em pseudo-3D (projecao em perspectiva com escala de sprites),
 * obstaculos, gargulas e os tiros da nave. Coordenadas: x lateral (-1.6 a 1.6), y vertical (-1.1 teto a 1.0 lava),
 * z distancia a frente da nave. A nave fica em (sx, sy, 0).
 */
final class FugaPista {
    static final int CX = 480, CY = 205; static final double F = 330, PAREDE = 1.6, TETO = -1.1, LAVA = 1.0, LONGE = 42;
    static final int ROCHA = 0, COLUNA = 1, GARGULA = 2, BOLHA = 3, DESMORONE = 4;
    static final class Obj { int tipo, vida, var; double x, y, z, vx, vy, r, t; boolean vivo = true, passou; }
    static final class Tiro { double x, y, z, vx, vy; boolean missil, vivo = true; Obj alvo; double t; }
    final List<Obj> objs = new ArrayList<>();
    final List<Tiro> tiros = new ArrayList<>();
    final List<double[]> estouros = new ArrayList<>(); // x, y, z, t, tamanho
    private final Random rnd = new Random(11);
    private double proxObj = 1.5, proxDesmorone = 9;
    double distancia;
    private static final BufferedImage GARGULA_ARTE = ler("demonio_gargula.png");

    /** Avanca o mundo: tudo vem na direcao da nave na velocidade dela; mais longe na fuga, mais denso. */
    void atualizar(double dt, double vel, double sx, double sy, double progresso, FugaFinal f) {
        distancia += vel * dt;
        double dens = .55 + progresso * 1.1; // a dificuldade cresce com a subida
        if ((proxObj -= dt * dens * (.6 + vel / 40)) <= 0) { proxObj = .32 + rnd.nextDouble() * .3; criar(progresso, sx, sy); }
        if ((proxDesmorone -= dt) <= 0 && progresso > .15) { proxDesmorone = 7 - progresso * 3 + rnd.nextDouble() * 3; Obj o = novo(DESMORONE); o.x = rnd.nextBoolean() ? -.85 : .85; o.y = 0; o.r = .9; o.vida = 99; }
        for (Obj o : objs) {
            o.t += dt; o.z -= vel * dt;
            if (o.tipo == ROCHA) o.y += o.vy * dt; // despenca do teto
            else if (o.tipo == GARGULA) { o.z -= 6 * dt; double k = Math.min(1, dt * (o.z < 9 ? 2.6 : .9)); o.x += (sx - o.x) * k; o.y += (sy - o.y) * k; } // vem para cima da nave
            else if (o.tipo == BOLHA) { o.y += o.vy * dt; o.vy += 2.2 * dt; o.x += o.vx * dt; } // a lava explode em arco
            if (!o.passou && o.z < .7) { o.passou = true; if (o.vivo && acerta(o, sx, sy)) { f.colidiu(o); o.vivo = false; estouro(o.x, o.y, Math.max(.8, o.z), 1.2); } }
            if (o.z < -1 || o.tipo == BOLHA && o.y > LAVA + .3) o.vivo = false;
        }
        objs.removeIf(o -> !o.vivo);
        for (Tiro t : tiros) {
            t.t += dt;
            if (t.missil) { if (t.alvo != null && t.alvo.vivo) { double k = Math.min(1, dt * 4); t.x += (t.alvo.x - t.x) * k; t.y += (t.alvo.y - t.y) * k; } t.z += (34 + vel) * dt; }
            else { t.z += (70 + vel) * dt; t.x += t.vx * dt; t.y += t.vy * dt; }
            for (Obj o : objs) if (o.vivo && o.tipo != COLUNA && o.tipo != DESMORONE && Math.abs(o.z - t.z) < (t.missil ? 2.2 : 1.6) && Math.abs(o.x - t.x) < o.r + .08 && Math.abs(o.y - t.y) < o.r + .08) {
                t.vivo = false; o.vida -= t.missil ? 4 : 1; if (o.vida <= 0) { o.vivo = false; estouro(o.x, o.y, o.z, o.tipo == GARGULA ? 1.6 : 1.1); f.abateu(o); } else estouro(t.x, t.y, t.z, .4); break; }
            if (t.z > LONGE || t.t > 2.5) t.vivo = false;
        }
        tiros.removeIf(t -> !t.vivo);
        for (double[] e : estouros) { e[3] += dt; e[2] -= vel * dt; } estouros.removeIf(e -> e[3] > .5 || e[2] < .3);
    }
    private Obj novo(int tipo) { Obj o = new Obj(); o.tipo = tipo; o.z = LONGE; o.var = rnd.nextInt(100); objs.add(o); return o; }
    private void criar(double p, double sx, double sy) {
        double r = rnd.nextDouble(); Obj o;
        if (r < .34) { o = novo(ROCHA); o.x = (rnd.nextDouble() * 2 - 1) * 1.2; o.y = TETO - .2; o.vy = .25 + rnd.nextDouble() * .35; o.r = .22 + rnd.nextDouble() * .18; o.vida = 3; }
        else if (r < .55) { o = novo(COLUNA); o.x = rnd.nextDouble() < .45 ? sx + (rnd.nextDouble() - .5) * .5 : (rnd.nextDouble() * 2 - 1) * 1.3; o.r = .2; o.vida = 99; }
        else if (r < .55 + .18 + p * .12) { o = novo(GARGULA); o.x = rnd.nextBoolean() ? -1.4 : 1.4; o.y = -.6 + rnd.nextDouble() * .8; o.r = .26; o.vida = 3; }
        else { o = novo(BOLHA); o.x = (rnd.nextDouble() * 2 - 1) * 1.2; o.y = LAVA; o.vy = -1.6 - rnd.nextDouble(); o.vx = (rnd.nextDouble() - .5) * .6; o.r = .17; o.vida = 1; }
    }
    private boolean acerta(Obj o, double sx, double sy) {
        if (o.tipo == COLUNA) return Math.abs(o.x - sx) < o.r + .12;
        if (o.tipo == DESMORONE) return o.x < 0 ? sx < o.x + o.r : sx > o.x - o.r; // a parede que cai fecha metade do corredor
        return Math.abs(o.x - sx) < o.r + .14 && Math.abs(o.y - sy) < o.r + .12;
    }
    void estouro(double x, double y, double z, double tam) { estouros.add(new double[]{x, y, z, 0, tam}); }
    /** Bomba: limpa o que estiver perto (menos as paredes). */
    int bomba() { int n = 0; for (Obj o : objs) if (o.z < 26 && o.tipo != DESMORONE && o.tipo != COLUNA) { o.vivo = false; estouro(o.x, o.y, o.z, 1.5); n++; } objs.removeIf(o -> !o.vivo); return n; }
    /** O alvo mais perto na frente da mira (para o missil). */
    Obj alvo(double sx, double sy) { Obj m = null; double best = 1e9; for (Obj o : objs) if (o.vivo && (o.tipo == GARGULA || o.tipo == ROCHA) && o.z > 2) { double d = o.z + 8 * Math.hypot(o.x - sx, o.y - sy); if (d < best) { best = d; m = o; } } return m; }

    // ---------------- desenho ----------------
    static double px(double x, double z, double sx) { return CX + (x - sx) * F / z; }
    static double py(double y, double z, double sy) { return CY + (y - sy) * F / z; }
    void desenhar(Graphics2D g, double sx, double sy, double tempo, double colapso) {
        g.setColor(new Color(30, 4, 6)); g.fillRect(0, 0, 960, 600);
        // o corredor em fatias, do fundo para a frente; as faixas andam com a distancia (sensacao de velocidade)
        double[] zs = new double[34]; for (int i = 0; i < zs.length; i++) zs[i] = LONGE * Math.pow(.86, i); // fatias mais juntas perto
        for (int i = 0; i < zs.length - 1; i++) {
            double z0 = zs[i], z1 = zs[i + 1]; int banda = (int) Math.floor((z0 + distancia) / 2.2); float nev = (float) Math.min(1, z1 / LONGE * 1.4); // nevoa vermelha la no fundo
            Color lava = mistura(banda % 2 == 0 ? new Color(255, 120, 20) : new Color(220, 70, 10), new Color(90, 10, 5), nev);
            Color parede = mistura(banda % 2 == 0 ? new Color(70, 30, 30) : new Color(52, 22, 24), new Color(40, 6, 8), nev);
            Color teto = mistura(banda % 2 == 0 ? new Color(40, 18, 22) : new Color(30, 12, 16), new Color(30, 4, 6), nev);
            quad(g, lava, -PAREDE, LAVA, PAREDE, LAVA, z0, z1, sx, sy, true);
            quad(g, teto, -PAREDE, TETO, PAREDE, TETO, z0, z1, sx, sy, true);
            quad(g, parede, -PAREDE, TETO, -PAREDE, LAVA, z0, z1, sx, sy, false);
            quad(g, parede, PAREDE, TETO, PAREDE, LAVA, z0, z1, sx, sy, false);
            if (banda % 4 == 1) { g.setColor(new Color(255, 110, 30, (int) (170 * (1 - nev)))); for (int l = -1; l <= 1; l += 2) { double xw = l * PAREDE; g.drawLine((int) px(xw, z0, sx), (int) py(TETO + .4, z0, sy), (int) px(xw, z1, sx), (int) py(LAVA - .3, z1, sy)); } } // veios de lava nas paredes
            if (banda % 3 == 0) { g.setColor(new Color(255, 200, 80, (int) (120 * (1 - nev)))); int yl = (int) py(LAVA, z1, sy); g.drawLine((int) px(-PAREDE, z1, sx), yl, (int) px(PAREDE, z1, sx), yl); } // crista brilhando da lava
        }
        java.util.Random br = new java.util.Random(3); for (int i = 0; i < 70; i++) { double bx = br.nextDouble() * 3 - 1.5, by = br.nextDouble() * 2 - 1, bz = LONGE - ((br.nextDouble() * LONGE + distancia * 1.3) % LONGE); if (bz < .5) continue; // brasas voando na direcao da cabine
            int x = (int) px(bx, bz, sx), y = (int) py(by, bz, sy), s = (int) Math.max(1, 6 / bz * 3); g.setColor(new Color(255, 160 + br.nextInt(90), 60, 200)); g.fillRect(x, y, s, s); }
        // a saida la no fim: uma luz branca crescendo
        g.setColor(new Color(255, 230, 200, 60)); int ox = (int) px(0, LONGE, sx), oy = (int) py(-.1, LONGE, sy); g.fillOval(ox - 14, oy - 10, 28, 20);
        // objetos do fundo para a frente
        List<Obj> ord = new ArrayList<>(objs); ord.sort((a, b) -> Double.compare(b.z, a.z));
        for (Obj o : ord) if (o.z > .3) desenharObj(g, o, sx, sy, tempo);
        for (Tiro t : tiros) { if (t.z < .4) continue; int x = (int) px(t.x, t.z, sx), y = (int) py(t.y, t.z, sy), s = (int) Math.max(2, (t.missil ? 18 : 10) * 3 / t.z);
            if (t.missil) { g.setColor(new Color(200, 200, 210)); g.fillOval(x - s / 2, y - s / 3, s, s * 2 / 3); g.setColor(new Color(255, 180, 60, 200)); g.fillOval(x - s / 3, y - s / 3 + s / 2, s * 2 / 3, s); }
            else { g.setColor(new Color(150, 255, 120)); g.fillOval(x - s / 2, y - s / 2, s, s); g.setColor(Color.WHITE); g.fillOval(x - s / 4, y - s / 4, s / 2, s / 2); } }
        for (double[] e : estouros) { double z = Math.max(.4, e[2]); int x = (int) px(e[0], z, sx), y = (int) py(e[1], z, sy), s = (int) (e[4] * F / z * (.3 + e[3] * 1.6)); float a = (float) (1 - e[3] / .5);
            g.setColor(new Color(255, 200, 80, (int) (230 * a))); g.fillOval(x - s / 2, y - s / 2, s, s); g.setColor(new Color(255, 90, 20, (int) (200 * a))); g.fillOval(x - s / 3, y - s / 3, s * 2 / 3, s * 2 / 3); }
        if (colapso > 0) { g.setColor(new Color(255, 40, 0, (int) (90 * colapso))); g.fillRect(0, 0, 960, 600); } // o colapso alcancando por tras
    }
    private void quad(Graphics2D g, Color c, double x0, double y0, double x1, double y1, double za, double zb, double sx, double sy, boolean horizontal) {
        Polygon p = new Polygon();
        p.addPoint((int) px(x0, za, sx), (int) py(y0, za, sy)); p.addPoint((int) px(x1, za, sx), (int) py(y1, za, sy));
        p.addPoint((int) px(x1, zb, sx), (int) py(y1, zb, sy)); p.addPoint((int) px(x0, zb, sx), (int) py(y0, zb, sy));
        g.setColor(c); g.fillPolygon(p);
    }
    private void desenharObj(Graphics2D g, Obj o, double sx, double sy, double tempo) {
        double k = F / o.z; int x = (int) px(o.x, o.z, sx), y = (int) py(o.y, o.z, sy), s = (int) Math.max(2, o.r * 2 * k);
        float nev = (float) Math.min(.75, o.z / LONGE); Graphics2D a = (Graphics2D) g.create();
        switch (o.tipo) {
            case ROCHA: { Polygon p = new Polygon(); Random r = new Random(o.var); for (int i = 0; i < 9; i++) { double an = i * Math.PI * 2 / 9 + o.t * .8, rr = s / 2.0 * (.75 + r.nextDouble() * .35); p.addPoint(x + (int) (Math.cos(an) * rr), y + (int) (Math.sin(an) * rr)); }
                a.setColor(mistura(new Color(60, 45, 45), new Color(40, 6, 8), nev)); a.fillPolygon(p); a.setColor(new Color(255, 120, 30, (int) (200 * (1 - nev)))); a.setStroke(new BasicStroke(Math.max(1, s / 30f))); a.drawLine(x - s / 4, y - s / 6, x + s / 8, y + s / 5); a.drawLine(x + s / 8, y + s / 5, x + s / 3, y); break; }
            case COLUNA: { int yb = (int) py(LAVA, o.z, sy), yt = (int) py(TETO, o.z, sy), w = (int) Math.max(3, o.r * 2 * k);
                for (int i = 0; i < 3; i++) { a.setColor(i == 0 ? new Color(255, 80, 10, 200) : i == 1 ? new Color(255, 170, 40, 220) : new Color(255, 245, 170, 230)); int ww = w * (3 - i) / 3 + (int) (Math.sin(tempo * 30 + i) * w * .08); a.fillRect(x - ww / 2, yt, ww, yb - yt); } break; }
            case GARGULA: { if (GARGULA_ARTE != null) { int fw = GARGULA_ARTE.getWidth() / 4, fh = GARGULA_ARTE.getHeight(), q = o.z < 7 ? 3 : (int) (tempo * 9 + o.var) % 3, w = s * 2, h = w * fh / fw; a.drawImage(GARGULA_ARTE, x - w / 2, y - h / 2, x + w / 2, y + h / 2, q * fw, 0, q * fw + fw, fh, null); }
                else { a.setColor(new Color(90, 90, 100)); a.fillOval(x - s / 2, y - s / 2, s, s); } break; }
            case BOLHA: { a.setColor(new Color(255, 150, 30)); a.fillOval(x - s / 2, y - s / 2, s, s); a.setColor(new Color(255, 240, 150)); a.fillOval(x - s / 4, y - s / 4, s / 2, s / 2); break; }
            case DESMORONE: { double x0 = o.x < 0 ? -PAREDE : o.x - o.r, x1 = o.x < 0 ? o.x + o.r : PAREDE; Polygon p = new Polygon();
                p.addPoint((int) px(x0, o.z, sx), (int) py(TETO, o.z, sy)); p.addPoint((int) px(x1, o.z, sx), (int) py(TETO, o.z, sy)); p.addPoint((int) px(x1, o.z, sx), (int) py(LAVA, o.z, sy)); p.addPoint((int) px(x0, o.z, sx), (int) py(LAVA, o.z, sy));
                a.setColor(mistura(new Color(75, 35, 35), new Color(40, 6, 8), nev)); a.fillPolygon(p); a.setColor(new Color(255, 120, 30, (int) (160 * (1 - nev)))); a.setStroke(new BasicStroke((float) Math.max(1, k * .02)));
                for (int i = 1; i < 4; i++) { int yy = (int) py(TETO + i * .5, o.z, sy); a.drawLine(p.xpoints[0], yy, p.xpoints[1], yy + (int) (k * .05)); } break; }
        }
        a.dispose();
    }
    static Color mistura(Color a, Color b, float t) { return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t), (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t)); }
    private static BufferedImage ler(String n) { java.net.URL u = FugaPista.class.getResource("/ImagemIcon/" + n); if (u == null) return null; try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; } }
}
