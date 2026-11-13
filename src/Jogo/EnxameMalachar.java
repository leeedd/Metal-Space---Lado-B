package Jogo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Ato I do Malachar: ele se desfaz numa nuvem de morcegos que gira em volta de um ponto e ataca
 * rapido. No meio da nuvem voa um morcego diferente, o morcego-rei (coroado, azul): so ele sente o
 * dano do chefe. Os outros seguram os tiros e, depois de tres acertos, caem e voltam pela borda.
 * Golpes: mergulho (alguns piscam vermelho e mergulham na nave), revoada (a nuvem inteira passa
 * em fila pela tela) e guincho (o rei grita e os morcegos cospem tiros). No fim do ato os morcegos
 * se juntam num ponto e dali sai o Maestro (juntar()).
 * Arte: tiro_morcego_malachar.png (morcego comum) e malachar_morcego.png (rei, 4 quadros).
 */
final class EnxameMalachar {
    private static final BufferedImage COMUM = morcego(), ESTACAO = ler("estacao_eclipse.png"), TREM = trem();
    /** Trem maluco de circo (trem_maluco.png, 3 quadros); sem a arte nova, usa o trem antigo. */
    static BufferedImage trem() { BufferedImage t = ler("trem_maluco.png"); return t != null ? t : ler("malachar_trem.png"); }
    /** Apresentacao: estacao, o trem encosta, o Malachar desce e se desfaz em morcegos. */
    /** Cena completa da estacao (precisa da arte): vagao_conde.png (4 quadros: sentado, levantando, de pe na porta, vagao vazio),
     *  senhor_eclipse_anda.png (4 quadros andando para a esquerda) e senhor_eclipse_aponta.png (2 quadros: erguendo, apontando). */
    private static final BufferedImage VAGAO = ler("vagao_conde.png"), ANDA_ARTE = ler("senhor_eclipse_anda.png"), APONTA_ARTE = ler("senhor_eclipse_aponta.png");
    private static final boolean CENA = VAGAO != null && ANDA_ARTE != null && APONTA_ARTE != null;
    // tempos: trem para, conde sentado, levanta, vai a porta, desce, caminha, aponta 2x e vira morcegos
    private static final double PARA = 2.6, LEVANTA = 3.4, PORTA = 4.0, SAI = 4.7, DESCE = CENA ? 5.2 : 3.3, ANDA = CENA ? 6.6 : 4.9, DESFAZ = CENA ? 8.2 : 6.6, APR = DESFAZ + 1.6, MY = 330;
    private static final int TREM_PARA = CENA ? 20 : 420, TW = CENA ? 380 : 560, PORTA_X = 20 + 380 * 605 / 667 - 560 * 46 / 495 + 560 * 245 / 495, CHAO_Y = 565; // a porta fica no meio do vagao do conde
    private double naveXv = 120, naveYv = 300;
    private double condeX() { double x0 = CENA ? PORTA_X - 40 : 470, x1 = CENA ? 300 : 260; return apr < DESCE ? x0 : x0 - (x0 - x1) * Math.min(1, (apr - DESCE) / (ANDA - DESCE)); }
    private double apr;
    private static BufferedImage morcego() { BufferedImage m = ler("poder_morcego_eclipse.png"); if (m == null) return ler("tiro_morcego_malachar.png"); return m.getSubimage(m.getWidth() / 4, 0, m.getWidth() / 4, m.getHeight()); }
    static boolean temArte() { return COMUM != null && PosesChefes.morcegoMalachar() != null; }
    private static final int W = Config.LARGURA, H = Config.ALTURA, TOPO = Config.HUD, N = 16, CW = 52, RW = 96;

    private final double[] bx = new double[N], by = new double[N], ang = new double[N], raio = new double[N], vx = new double[N], vy = new double[N],
            aviso = new double[N], flash = new double[N], volta = new double[N];
    private final int[] estado = new int[N], vida = new int[N]; // estado: 0 nuvem, 1 aviso, 2 mergulho, 3 caido (volta pela borda)
    private int rei, golpes, ultimoAtingido = -1;
    private double cx = W - 230, cy = 300, tempo, entrada, recarga = 1.4, trocaRei = 3, esperaContato, guincho, flashRei, vidaFrac = 1, juntar = -1;
    private boolean bloqueou, fim;
    private double escondido, jorro, mx, my; // enxurrada do ato II: o Maestro se esconde na multidao
    private final java.util.Random sorte = new java.util.Random(11);

    EnxameMalachar() {
        for (int i = 0; i < N; i++) {
            ang[i] = i * Math.PI * 2 / N + sorte.nextDouble() * .3; raio[i] = 70 + (i % 4) * 34 + sorte.nextInt(20);
            bx[i] = W + 60 + sorte.nextInt(260); by[i] = TOPO + 40 + sorte.nextInt(H - TOPO - 80); vida[i] = 3;
        }
        rei = 5;
    }

    double entrada() { return entrada; }
    void vida(double f) { vidaFrac = f; }
    /** Comeca a juntar os morcegos num ponto (transicao para o Maestro). */
    void juntar(double alvoX, double alvoY) { juntar = 0; cx = alvoX; cy = alvoY; }
    /** Ato II: o Maestro se desfaz numa enxurrada e o rei se disfarca de morcego comum no meio dela. */
    void esconder(double ox, double oy) {
        mx = ox; my = oy; juntar = -1; entrada = 1; escondido = 7; recarga = .5; jorro = 0; fim = false; cx = ox; cy = oy;
        for (int i = 0; i < N; i++) { estado[i] = 0; vida[i] = 3; bx[i] = ox; by[i] = oy; }
        rei = sorte.nextInt(N); SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE);
    }
    boolean escondido() { return escondido > 0; }
    /** Acertaram o rei disfarcado: o Maestro volta na hora. */
    void revelar() { if (escondido > 0) { escondido = 0; juntar(mx, my); fim = true; } }
    /** true uma vez quando a enxurrada acaba (o chefe faz a volta do Maestro). */
    boolean consumirFim() { boolean f = fim; fim = false; return f; }
    /** Ato II: o Maestro rege e os morcegos saem em formacao. */
    void revoada(int tipo, double ox, double oy, double nx, double ny, List<TiroChefe> tiros) {
        if (tipo == 0) for (int i = -4; i <= 4; i++) { double a = Math.PI + i * .13; tiros.add(new TiroChefe(ox, oy, Math.cos(a) * 330, Math.sin(a) * 330, false, TiroChefe.MORCEGO_MALACHAR, TiroChefe.Movimento.ONDA)); }
        else if (tipo == 1) for (int k = 0; k < 6; k++) for (int lado = -1; lado <= 1; lado += 2) { // pinca: duas filas, de cima e de baixo, fecham na nave
            double sx = W + 30 + k * 45, sy = lado < 0 ? TOPO + 10 : H - 10, a = Math.atan2(ny - sy, nx - sx);
            tiros.add(new TiroChefe(sx, sy, Math.cos(a) * 380, Math.sin(a) * 380, false, TiroChefe.MORCEGO_MALACHAR, TiroChefe.Movimento.RETO)); }
        else { double giro = tempo * 1.7; for (int i = 0; i < 12; i++) { double a = giro + i * Math.PI / 6; tiros.add(new TiroChefe(ox, oy, Math.cos(a) * 240, Math.sin(a) * 240, false, TiroChefe.MORCEGO_MALACHAR, TiroChefe.Movimento.ONDA)); } }
    }

    private double slotX(int i) { return cx + Math.cos(ang[i]) * raio[i]; }
    private double slotY(int i) { return cy + Math.sin(ang[i]) * raio[i] * .72; }

    void atualizar(double dt, int naveX, int naveY, Dificuldade dif, List<TiroChefe> tiros) {
        tempo += dt; esperaContato = Math.max(0, esperaContato - dt); guincho = Math.max(0, guincho - dt); flashRei = Math.max(0, flashRei - dt);
        double nx = naveX + 22, ny = naveY + 18, furia = vidaFrac < .5 ? 1.35 : 1;
        if (apr < APR) { apr += dt; naveXv = nx; naveYv = ny; if (apr < DESFAZ) { for (int i = 0; i < N; i++) { bx[i] = condeX(); by[i] = MY; } if (apontou(apr - dt) != apontou(apr) && apontou(apr) > 0) SomJogo.tocar(SomJogo.Efeito.ALERTA); return; } if (apr < DESFAZ + .05) SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); }
        if (juntar >= 0) { // todos voam para o mesmo ponto
            juntar = Math.min(1, juntar + dt / 1.3);
            for (int i = 0; i < N; i++) { double k = Math.min(1, dt * (3 + juntar * 8)); bx[i] += (cx - bx[i]) * k; by[i] += (cy - by[i]) * k; ang[i] += dt * 6; }
            return;
        }
        if (escondido > 0) { // enxurrada: a nuvem varre a tela de um lado a outro e jorra morcegos
            escondido -= dt; jorro -= dt;
            if (jorro <= 0) { jorro = .09; tiros.add(new TiroChefe(W + 30, TOPO + 20 + sorte.nextInt(H - TOPO - 40), -480 - sorte.nextInt(160), 0, false, TiroChefe.MORCEGO_MALACHAR, TiroChefe.Movimento.ONDA)); }
            if (escondido <= 0) { // ninguem achou o rei: grito final e o Maestro volta
                for (int i = 0; i < 16; i++) { double a = i * Math.PI / 8; tiros.add(new TiroChefe(bx[rei], by[rei], Math.cos(a) * 300, Math.sin(a) * 300, true, TiroChefe.ENERGIA, TiroChefe.Movimento.RETO).comCor(new Color(190, 90, 255))); }
                escondido = 0; juntar(mx, my); fim = true; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); return;
            }
            cx = W * .5 + Math.sin(tempo * .9) * (W * .32);
        }
        entrada = apr < APR ? Math.min(.9, entrada + dt / 1.4) : Math.min(1, entrada + dt / 1.4);
        // o centro da nuvem passeia pela metade direita e acompanha a nave devagar
        if (escondido <= 0) cx += (W - 250 + Math.sin(tempo * .55) * 110 - cx) * Math.min(1, dt * 1.2);
        cy += (Math.max(TOPO + 150, Math.min(H - 150, ny)) - cy) * Math.min(1, dt * .9);
        for (int i = 0; i < N; i++) {
            flash[i] = Math.max(0, flash[i] - dt);
            ang[i] += dt * (1.5 + (i % 3) * .35) * furia * (i % 2 == 0 ? 1 : -1) * .9;
            if (estado[i] == 0) { double k = Math.min(1, dt * (entrada < 1 ? 2.2 : 5)); bx[i] += (slotX(i) - bx[i]) * k; by[i] += (slotY(i) - by[i]) * k; }
            else if (estado[i] == 1) { aviso[i] -= dt; bx[i] += (slotX(i) - bx[i]) * Math.min(1, dt * 5); by[i] += (slotY(i) - by[i]) * Math.min(1, dt * 5);
                if (aviso[i] <= 0) { double a = Math.atan2(ny - by[i], nx - bx[i]), v = 760 * furia; vx[i] = Math.cos(a) * v; vy[i] = Math.sin(a) * v; estado[i] = 2; SomJogo.tocar(SomJogo.Efeito.CHEFE_TIRO); } }
            else if (estado[i] == 2) { bx[i] += vx[i] * dt; by[i] += vy[i] * dt;
                if (bx[i] < -60 || bx[i] > W + 60 || by[i] < TOPO - 60 || by[i] > H + 60) { estado[i] = 3; volta[i] = .6; vida[i] = 3; } }
            else { volta[i] -= dt; if (volta[i] <= 0) { estado[i] = 0; bx[i] = W + 50; by[i] = TOPO + 30 + sorte.nextInt(H - TOPO - 60); } }
        }
        // o rei troca de lugar na nuvem para o jogador ter que segui-lo
        trocaRei -= dt;
        if (trocaRei <= 0 && estado[rei] == 0) { int novo = sorte.nextInt(N); if (novo != rei && estado[novo] == 0) { double a = ang[rei], r = raio[rei]; ang[rei] = ang[novo]; raio[rei] = raio[novo]; ang[novo] = a; raio[novo] = r; } trocaRei = 2.2 + sorte.nextDouble() * 1.5; }
        if (entrada < 1) return;
        recarga -= dt * dif.cadenciaInimigos * furia;
        if (recarga > 0) return;
        int g = golpes++ % 4;
        if (g == 0 || g == 2) { // mergulho: 3 (ou 5 na furia) piscam e mergulham
            int n = vidaFrac < .5 ? 5 : 3;
            for (int k = 0, t = 0; k < n && t < 40; t++) { int i = sorte.nextInt(N); if (i != rei && estado[i] == 0) { estado[i] = 1; aviso[i] = .38 + k * .1; k++; } }
            recarga = .9;
        } else if (g == 1) { // guincho: o rei grita e os comuns cospem um tiro roxo na nave
            guincho = .5; flashRei = .5; SomJogo.tocar(SomJogo.Efeito.ERRO);
            for (int i = 0; i < N; i++) if (i != rei && estado[i] == 0 && i % 2 == golpes / 4 % 2) {
                double a = Math.atan2(ny - by[i], nx - bx[i]);
                tiros.add(new TiroChefe(bx[i], by[i], Math.cos(a) * 300, Math.sin(a) * 300, false, TiroChefe.ENERGIA, TiroChefe.Movimento.RETO).comCor(new Color(190, 90, 255)));
            }
            recarga = 1.1;
        } else { // revoada: a nuvem inteira mergulha em fila, um atras do outro
            for (int i = 0, k = 0; i < N; i++) if (i != rei && estado[i] == 0) { estado[i] = 1; aviso[i] = .45 + k++ * .07; }
            recarga = 2.4;
        }
    }

    private Rectangle caixa(int i) { int s = i == rei && escondido <= 0 ? RW : CW; return new Rectangle((int) bx[i] - s / 2 + 6, (int) by[i] - s / 3, s - 12, s * 2 / 3); }
    /** O alvo do chefe e o rei. */
    Rectangle bounds() { return caixa(rei); }

    /** Tiro do jogador: o rei recebe o dano; um morcego comum segura o tiro. */
    boolean colide(Rectangle r) {
        if (juntar >= 0 || apr < APR) return false;
        if (r.intersects(caixa(rei))) return true;
        for (int i = 0; i < N; i++) if (i != rei && estado[i] != 3 && r.intersects(caixa(i))) { bloqueou = true; ultimoAtingido = i; return true; }
        return false;
    }
    boolean consumirBloqueio() {
        if (!bloqueou) return false; bloqueou = false;
        int i = ultimoAtingido; if (i >= 0) { flash[i] = .12; if (--vida[i] <= 0) { estado[i] = 3; volta[i] = 2.5; vida[i] = 3; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO); } }
        return true;
    }
    void ferido() { flashRei = .16; }
    /** Encosto: morcego mergulhando ou a propria nuvem machuca, com pausa entre um dano e outro. */
    boolean contato(Rectangle nave) {
        if (esperaContato > 0 || juntar >= 0 || apr < APR) return false;
        for (int i = 0; i < N; i++) if (estado[i] != 3 && nave.intersects(caixa(i))) { esperaContato = .8; if (estado[i] == 2) { estado[i] = 3; volta[i] = .8; } return true; }
        return false;
    }

    void desenhar(Graphics2D tela, float alfa) {
        if (COMUM == null) return;
        Graphics2D g = (Graphics2D) tela.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (apr < APR) { apresentacao(g); if (apr < DESFAZ) { g.dispose(); return; } }
        if (alfa < 1) g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, alfa)));
        for (int i = 0; i < N; i++) {
            if (i == rei && escondido <= 0 || estado[i] == 3) continue;
            double bate = .7 + .3 * Math.abs(Math.sin(tempo * 16 + i));
            int w = CW, h = (int) (CW * COMUM.getHeight() / (double) COMUM.getWidth() * bate), x = (int) bx[i], y = (int) by[i];
            boolean praDireita = estado[i] == 2 && vx[i] > 0;
            if (praDireita) g.drawImage(COMUM, x + w / 2, y - h / 2, -w, h, null); else g.drawImage(COMUM, x - w / 2, y - h / 2, w, h, null);
            if (estado[i] == 1 && (int) (tempo * 20) % 2 == 0) { g.setColor(new Color(255, 40, 40)); g.fillRect(x - 7, y - 4, 4, 4); g.fillRect(x + 3, y - 4, 4, 4); } // olhos piscando antes do mergulho
            if (flash[i] > 0) { g.setColor(new Color(255, 255, 255, 170)); g.fillRect(x - 8, y - 6, 16, 12); }
            if (i == rei) { g.setColor(new Color(255, 210, 70)); g.fillRect(x - 6, y - h / 2 - 4, 3, 5); g.fillRect(x - 1, y - h / 2 - 6, 3, 7); g.fillRect(x + 4, y - h / 2 - 4, 3, 5); g.fillRect(x - 6, y - h / 2, 13, 2); } // disfarcado: so a coroinha entrega
        }
        BufferedImage[] q = PosesChefes.morcegoMalachar();
        if (q != null && escondido <= 0) {
            BufferedImage f = q[(int) (tempo * 9) % 4];
            int w = RW + (guincho > 0 ? 10 : 0), h = w * f.getHeight() / f.getWidth(), x = (int) bx[rei], y = (int) by[rei];
            if (juntar < 0) { g.setColor(new Color(170, 225, 255)); // faiscas de pixel em volta do rei
                for (int k = 0; k < 6; k++) { double aa = tempo * 2.5 + k * Math.PI / 3; int px = x + (int) (Math.cos(aa) * w * .55), py = y + (int) (Math.sin(aa) * h * .5);
                    if ((int) (tempo * 8 + k) % 3 != 0) { g.fillRect(px - 1, py - 4, 3, 9); g.fillRect(px - 4, py - 1, 9, 3); } } }
            g.drawImage(f, x - w / 2, y - h / 2, w, h, null);
            if (flashRei > 0) { g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, flashRei * 4) * .5f * Math.max(0, alfa))); g.drawImage(f, x - w / 2 - 2, y - h / 2 - 2, w + 4, h + 4, null); }
        }
        g.dispose();
    }

    /** Com a arte: o conde desce da porta do vagao, caminha pela plataforma (na frente do trem), ve a nave e aponta 2x. */
    private void condeCena(Graphics2D g, float a) {
        if (apr > DESFAZ + .5) return;
        if (apr < SAI) return; // ate aqui o conde aparece dentro do proprio vagao (sentado, levantando, na porta)
        int mh = 250; BufferedImage f; double cx = condeX(), pe = CHAO_Y;
        int ap = apontou(apr);
        if (ap > 0) { float am0 = (float) ((apr > DESFAZ ? Math.max(0, 1 - (apr - DESFAZ) / .5) : 1) * a); g.setComposite(AlphaComposite.SrcOver.derive(am0)); desenharAponta(g, cx, pe, mh, ap); f = null; }
        else if (apr < ANDA) { int fw = ANDA_ARTE.getWidth() / 4; f = ANDA_ARTE.getSubimage((int) (apr * 8) % 4 * fw, 0, fw, ANDA_ARTE.getHeight());
            if (apr < DESCE) pe = CHAO_Y - 45 * (1 - (apr - SAI) / (DESCE - SAI)); } // descendo os degraus do vagao
        else f = SenhorEclipse.quadro(0);
        if (f == null) return;
        int mw = mh * f.getWidth() / f.getHeight();
        float am = (float) ((apr > DESFAZ ? Math.max(0, 1 - (apr - DESFAZ) / .5) : 1) * a);
        g.setComposite(AlphaComposite.SrcOver.derive(am)); g.drawImage(f, (int) cx - mw / 2, (int) pe - mh, mw, mh, null);
        if (ap > 0) { double k = (apr - (ap == 1 ? ANDA + .2 : ANDA + .95)) / .55; g.setStroke(new java.awt.BasicStroke(3)); g.setColor(new Color(255, 80, 200, (int) (200 * (1 - k))));
            int rr = (int) (34 - 20 * k); g.drawOval((int) naveXv + 22 - rr, (int) naveYv + 18 - rr, 2 * rr, 2 * rr); }
    }
    /** Pose de apontar (senhor_eclipse_aponta.png, 2 quadros quadrados): ergue o braco e depois aponta; o corpo fica alinhado com a pose normal. */
    private void desenharAponta(Graphics2D g, double cx, double pe, int mhNormal, int ap) {
        double ini = ap == 1 ? ANDA + .2 : ANDA + .95; int q = apr - ini < .18 ? 0 : 1, fw = APONTA_ARTE.getWidth() / 2, fh = APONTA_ARTE.getHeight();
        int h = (int) (mhNormal * .95), w = fw * h / fh, x = (int) (cx - w * (q == 0 ? .62 : .57)), y = (int) pe - h;
        g.drawImage(APONTA_ARTE, x, y, x + w, y + h, q * fw, 0, q * fw + fw, fh, null);
    }
    /** 1 ou 2 durante cada apontada para a nave, 0 fora delas. */
    private int apontou(double t) { if (t > ANDA + .2 && t < ANDA + .75) return 1; if (t > ANDA + .95 && t < ANDA + 1.5) return 2; return 0; }
    private void apresentacao(Graphics2D g) {
        float a = (float) Math.max(0, Math.min(1, Math.min(apr / .8, (APR - apr) / .8)));
        g.setComposite(AlphaComposite.SrcOver.derive(a));
        if (ESTACAO != null) g.drawImage(ESTACAO, 0, 0, W, H, null);
        if (TREM != null) { // o trem maluco chega sacudindo, freia e para na plataforma (os palhacos nao param)
            int fw = TREM.getWidth() / 3, fh = TREM.getHeight(), tw = TW, th = tw * fh / fw;
            double p = Math.min(1, Math.max(0, (apr - .2) / (PARA - .2))); p = 1 - Math.pow(1 - p, 3);
            double pula = p < 1 ? Math.abs(Math.sin(apr * 11)) * 9 : Math.abs(Math.sin(apr * 6)) * 3;
            int tx = (int) (W + 20 - (W + 20 - TREM_PARA) * p), ty = 470 - th + 40 - (int) pula, q = (int) (apr * (p < 1 ? 9 : 5)) % 3;
            g.drawImage(TREM, tx, ty, tx + tw, ty + th, q * fw, 0, q * fw + fw, fh, null);
            if (CENA) { int vf = VAGAO.getWidth() / 4, vh = VAGAO.getHeight(), vtw = 560, vth = vtw * vh / vf, vx = tx + tw * 605 / 667 - vtw * 46 / vf, vy = ty + th * 539 / 667 - vth * 263 / vh; // engate no engate, rodas na mesma linha // o vagao do conde vem engatado no fim
                int qv = apr < LEVANTA ? 0 : apr < PORTA ? 1 : apr < SAI ? 2 : 3; // sentado, levantando, na porta, vazio
                g.drawImage(VAGAO, vx, vy, vx + vtw, vy + vth, qv * vf, 0, qv * vf + vf, vh, null); }
            if (apr > PARA) confete(g, tx + tw / 2, ty + 60, apr - PARA);
        }
        if (CENA) { condeCena(g, a); g.setComposite(AlphaComposite.SrcOver); return; }
        BufferedImage m = SenhorEclipse.quadro(0);
        if (m != null && apr > PARA && apr < DESFAZ + .5) { // desce do vagao, caminha para a luta, aponta 2x e vira morcegos
            int ap = apontou(apr); if (ap > 0) m = SenhorEclipse.quadro(2);
            double d = Math.min(1, (apr - PARA) / (DESCE - PARA)); int mh = 250, mw = mh * m.getWidth() / m.getHeight();
            double anda = apr > DESCE && apr < ANDA ? Math.abs(Math.sin(apr * 9)) * 6 : 0;
            int cx = (int) condeX(), my = (int) (MY - mh / 2 - 70 * (1 - d) - anda);
            float am = (float) (Math.min(1, d * 2) * (apr > DESFAZ ? Math.max(0, 1 - (apr - DESFAZ) / .5) : 1) * a);
            g.setComposite(AlphaComposite.SrcOver.derive(am));
            if (ap > 0 && APONTA_ARTE != null) desenharAponta(g, cx, my + mh, mh, ap); else g.drawImage(m, cx - mw / 2, my, mw, mh, null);
            if (ap > 0) { // o dedo aponta para a nave: mira piscando nela
                double hx = cx - mw * .38, hy = my + mh * .3, k = Math.max(0, Math.min(1, (apr - (ap == 1 ? ANDA + .2 : ANDA + .95)) / .55));
                if (APONTA_ARTE == null) { g.setColor(new Color(220, 140, 255, (int) (230 * (1 - k)))); int r = (int) (10 + 26 * k); g.fillOval((int) hx - r, (int) hy - r, 2 * r, 2 * r); }
                g.setStroke(new java.awt.BasicStroke(3)); g.setColor(new Color(255, 80, 200, (int) (200 * (1 - k))));
                int rr = (int) (34 - 20 * k); g.drawOval((int) naveXv + 22 - rr, (int) naveYv + 18 - rr, 2 * rr, 2 * rr);
                g.drawLine((int) naveXv + 22 - rr - 8, (int) naveYv + 18, (int) naveXv + 22 + rr + 8, (int) naveYv + 18); g.drawLine((int) naveXv + 22, (int) naveYv + 18 - rr - 8, (int) naveXv + 22, (int) naveYv + 18 + rr + 8);
            }
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** Confete de circo saindo do trem quando ele para. */
    static void confete(Graphics2D g, int cx, int cy, double t) {
        if (t < 0 || t > 2.5) return; java.util.Random r = new java.util.Random(5); Color[] cores = {new Color(255, 70, 90), new Color(255, 220, 60), new Color(80, 220, 255), new Color(140, 255, 110), new Color(255, 140, 230)};
        for (int k = 0; k < 60; k++) { double vx = (r.nextDouble() - .5) * 520, vy = -200 - r.nextDouble() * 320; int x = cx + (int) (vx * t), y = cy + (int) (vy * t + 330 * t * t);
            g.setColor(cores[k % cores.length]); if ((int) (t * 12 + k) % 2 == 0) g.fillRect(x, y, 6, 3); else g.fillRect(x, y, 3, 6); }
    }

    private static BufferedImage ler(String n) {
        java.net.URL u = EnxameMalachar.class.getResource("/ImagemIcon/" + n);
        if (u == null) return null;
        try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; }
    }
}
