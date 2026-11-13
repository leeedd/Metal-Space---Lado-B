package Jogo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Luta final, depois da fortaleza. Atos I e II: os Irmaos Ossada (kung fu e karate; depois a Danca Macabra).
 * Quando os dois caem de vez, as almas se fundem num buraco negro que engole tudo e a nave cai em dobra ate o inferno.
 * Ato III: Belzebu, o arauto baterista. Ato IV: Zaref, o Templario do Inferno.
 * Ato V: o Demonio das Correntes, com a corrente que agarra, o chicote e o mergulho.
 * Arte: arco_final_099.png (grade 4x4: Bardo, Monge, Belzebu e Zaref; quadros repouso,
 * preparo, golpe e bate-cabeca, todos virados para a esquerda).
 */
final class ChefeFinal {
    static final int BARDO = 1, MONGE = 2, BELZEBU = 3, ZAREF = 4, CORRENTES = 5;
    private static final BufferedImage ARCO = ler("arco_final_099.png"), ELO = ler("van_inferno_corrente_elo.png"),
            GANCHO_ABERTO = ler("van_inferno_gancho_aberto.png"), GANCHO_FECHADO = ler("van_inferno_gancho_fechado.png");
    /** Demonio do Demonio das Correntes (4 quadros: girando a corrente, preparando, chicoteando, mergulhando). Sem a arte, usa o Belzebu tingido. */
    private static final BufferedImage DEMONIO_CORRENTES = ler("demonio_correntes_demonio.png");
    /** Zaref de espada (4 quadros: espada erguida, preparando, corte com a meia-lua prateada, chifres). Sem a arte, usa a linha do Zaref da grade. */
    private static final BufferedImage ZAREF_ESPADA = ler("zaref_espada.png");
    /** Bardo e Monge de VIOLAO (4 quadros cada: repouso, preparo, golpe, bate-cabeca). Sem a arte, a grade (de guitarra). */
    private static final BufferedImage BARDO_VIOLAO = ler("bardo_violao.png"), MONGE_VIOLAO = ler("monge_violao.png");
    /** Irmaos Ossada (gemeos monges caveira): kung fu (4 quadros: postura, chute alto e rasteiro, soco, violao) e danca (8 quadros). */
    private static final BufferedImage GEMEOS_KF = ler("gemeos_kungfu.png"), GEMEOS_DANCA = ler("gemeos_danca.png"), TAPAS = ler("ossada_tapas.png"), CHUTES = ler("ossada_chutes.png"), ENTRADA = ler("ossada_entrada.png"), GUARDA_ARTE = ler("ossada_guarda.png"), GUARDA_ROXA = roxear(GUARDA_ARTE); // entrada: 6 quadros com os dois gemeos
    /** Lua cheia ao fundo na Danca Macabra (opcional) e lobisomens correndo (TiroChefe.LOBISOMEM, opcional). */
    private static final BufferedImage LUA = ler("lua_cheia.png");
    private static boolean gemeos() { return GEMEOS_KF != null && GEMEOS_DANCA != null; }
    private double danca, moon, horda, lobos, luaT; private int levas;
    /** Arte antiga do Belzebu (fallback; as folhas novas belzebu_*.png tem prioridade). */
    private static final BufferedImage BELZEBU_ARTE = ler("belzebu.png");
    /** Moldura da barra de vida (faixa preta da vida e dois escudos para as almas dos gemeos). */
    private static final BufferedImage BARRA = ler("barra_chefe_final.png");
    /** Portal do inferno (4 quadros girando). Sem a arte, um redemoinho de fagulhas. */
    private static final BufferedImage PORTAL = ler("portal_inferno.png");
    /** Duracao da cena da invocacao: metades erguidas, juntando, raio, portal abrindo, demonio saindo, portal fechando. */
    static final double INVOCACAO = 12.0, BURACO_FIM = 7.5, FUSAO_FIM = 3.0; // fusao das almas + buraco negro, depois a dobra ate o inferno
    private final double[] almaX = new double[2], almaY = new double[2];
    private static final BufferedImage ALMA_ARTE = ler("alma_ossada.png"), FUSAO = ler("fusao_almas.png"), BURACO = ler("buraco_negro.png"), CHEGADA = ler("inferno_chegada.png");
    private static void quadroArte(Graphics2D g, BufferedImage f, int n, int q, double cx, double cy, double w, float alfa) {
        int fw = f.getWidth() / n, fh = f.getHeight(); double h = w * fh / fw; Graphics2D a = (Graphics2D) g.create(); if (alfa < 1) a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alfa)));
        a.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        a.drawImage(f, (int) (cx - w / 2), (int) (cy - h / 2), (int) (cx + w / 2), (int) (cy + h / 2), q * fw, 0, q * fw + fw, fh, null); a.dispose(); }
    private static final int W = Config.LARGURA, H = Config.ALTURA, TOPO = Config.HUD;
    /** Tamanho na tela de cada ato (a celula da grade e quadrada). */
    private static final int[] TAM = {0, 250, 250, 400, 290, 360};

    private int ato = BARDO, dentes;
    private final int vidaBardo, vidaMonge, vidaBelzebu, vidaZaref, vidaCorrentes;
    private int vida;
    private double tempo, entrada, flash, gesto, intervalo = 1.6, transicao;
    private String aviso;
    private int golpes;
    private double x = W + 40, y;

    // Belzebu: investida com aviso
    private double investida, alvoInvX, alvoInvY, baseX, bateria, batida;
    // Zaref: raio arco-iris (carga e disparo), corrente do Demonio das Correntes e o demonio
    private double cargaRaio, raio, direcaoRaio;
    private int gancho; // 0 parado, 1 mirando, 2 voando, 3 preso, 4 recolhendo
    private double hx, hy, hvx, hvy, faseGancho, tempoPreso, faiscaCorrente;
    private int vidaCorrente, danoGancho;
    private boolean acertouCorrente;
    private double demonioX, demonioY, demonio, avisoDemonio; // mergulho: demonio > 0 atravessando a tela
    // chicote da corrente: aviso do arco e depois a corrente varre de cima para baixo (ou o contrario)
    private double avisoChicote, chicote, anguloChicote, inicioChicote, fimChicote;
    private boolean acertouChicote;

    ChefeFinal(Dificuldade dif) {
        double r = dif.resistenciaChefe;
        vidaBardo = (int) Math.round(230 * r); vidaMonge = (int) Math.round(250 * r);
        vidaBelzebu = (int) Math.round(480 * r); vidaZaref = (int) Math.round(340 * r); vidaCorrentes = (int) Math.round(460 * r);
        vida = vidaBardo; dividirVida();
        y = TOPO + (H - TOPO - TAM[BARDO]) / 2.0;
    }

    int ato() { return ato; }
    int dentes() { return dentes; }
    String consumirCena() { return null; }
    String consumirAviso() { String a = aviso; aviso = null; return a; }
    double getEntrada() { return entrada; }

    void atualizar(double dt, int naveX, int naveY, Dificuldade dif, List<TiroChefe> tiros) {
        if (dt <= 0) return; navX = naveX + 22; navY = naveY + 18;
        for (Queda q : quedas) q.t += dt; quedas.removeIf(q -> q.t > 2.6);
        tempo += dt; flash = Math.max(0, flash - dt); gesto = Math.max(0, gesto - dt); gestoV = Math.max(0, gestoV - dt); gestoR = Math.max(0, gestoR - dt);
        faiscaCorrente = Math.max(0, faiscaCorrente - dt);
        if (transicao > 0) { transicao = Math.max(0, transicao - dt); gancho = 0; raio = cargaRaio = demonio = avisoDemonio = 0; return; }
        if (rio > 0) { rio(dt, naveX, naveY, tiros); return; }
        if (apresB > 0) { apresentar(dt); return; }
        if (belzMorte > 0) { if (belzMorte == BZ_MORTE) { tiros.clear(); SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); } belzMorte = Math.max(0, belzMorte - dt); if (belzMorte == 0) { aviso = "O TRONO DESPERTA: ZAREF, O TEMPLARIO DO INFERNO!"; proximo(ZAREF, vidaZaref, 3.0); } return; } // o Belzebu desaba em cinzas
        int t = TAM[ato];
        double alvoX = W - t - (ato == BELZEBU ? (BELZEBU_ARTE != null ? 110 : 20) : 40);
        if (entrada < 1 && ato == MONGE && emDupla()) { // a lua levanta os ossos: os gemeos se remontam onde cairam
            x = alvoX; baseX = alvoX; if (entrada == 0) { SomJogo.tocar(SomJogo.Efeito.EVOLUCAO); renascer = new Queda[]{novaQueda(0), novaQueda(1)}; }
            entrada = Math.min(1, entrada + dt / 2.0); return; }
        if (entrada < 1) {
            entrada = Math.min(1, entrada + dt / 1.3);
            x = W + 40 - (W + 40 - alvoX) * (1 - Math.pow(1 - entrada, 3)); baseX = alvoX;
            return;
        }
        if (emDupla() && pose < POSE_DUR) { double antes = pose; pose += dt; // chegaram: pousam e fazem a pose de kung fu antes de lutar
            if (antes == 0) SomJogo.tocar(SomJogo.Efeito.IMPACTO); if (antes < POSE_KIAI && pose >= POSE_KIAI) SomJogo.tocar(SomJogo.Efeito.TROVAO); return; }
        double cad = dif.cadenciaInimigos;
        TiroChefe.alvoX = naveX + 22; TiroChefe.alvoY = naveY + 18;
        if (emDupla()) reagir(dt, naveX, naveY);
        boolean furia = vida < getVidaMaxima() * .35;
        if (ato == BARDO) bardo(dt, naveX, naveY, cad * (furia ? 1.3 : 1), tiros, furia);
        else if (ato == MONGE) monge(dt, naveX, naveY, cad * (furia ? 1.3 : 1), tiros, furia);
        else if (ato == BELZEBU) belzebu(dt, naveX, naveY, cad * (furia ? 1.25 : 1), tiros, furia);
        else if (ato == ZAREF) zaref(dt, naveX, naveY, cad * (furia ? 1.25 : 1), tiros, furia);
        else correntes(dt, naveX, naveY, cad * (furia ? 1.25 : 1), tiros, furia);
    }

    /** Acompanha a nave na vertical, devagar, balancando. */
    private void flutuar(double dt, int naveY, double vel) {
        int t = TAM[ato];
        double alvo = Math.max(TOPO, Math.min(H - t, naveY + 18 - t * .55));
        y += (alvo + Math.sin(tempo * 1.4) * 26 - y) * Math.min(1, dt * vel);
    }

    // ---------------- ato I: Irmaos Ossada no kung fu ----------------
    private void bardo(double dt, int naveX, int naveY, double cad, List<TiroChefe> tiros, boolean furia) {
        flutuar(dt, naveY, 1.1);
        x += (baseX + Math.sin(tempo * .8) * 30 - x) * Math.min(1, dt * 2);
        if (tiros.size() > 50) return;
        double nx = naveX + 22, ny = naveY + 18;
        // VERDE - KUNG FU: golpes circulares e fluidos, que ondulam e giram
        boolean sozinho = !vivo(0) || !vivo(1); double cadI = cad * (sozinho ? 1.45 : 1); // o que sobra fica furioso
        intervaloV -= dt * cadI;
        if (!livre(0)) rajadaV = 0; if (!livre(1)) rajadaR = 0;
        if (rajadaV > 0) { rajadaV -= dt; gestoV = .1; poseV = (int) (tempo * 20) % 2 == 0 ? 1 : 0; chuteT -= dt; // rajada de chutes: a perna em borrao, chutes curtos e rapidos
            if (chuteT <= 0) { chuteT = .055; double[] o = irmao(0); double a = Math.atan2(ny - o[1], nx - o[0]) + (Math.random() - .5) * .55;
                tiros.add(new TiroChefe(o[0] - 20, o[1] - 30 + (Math.random() - .5) * 70, Math.cos(a) * 520, Math.sin(a) * 520, false, TiroChefe.OSSO_PE, TiroChefe.Movimento.RETO).comCor(VERDE).comDuracao(.5)); } }
        if (vivo(0) && livre(0) && intervaloV <= 0 && rajadaV <= 0) { golpesV++; gestoV = .5; poseV = golpesV % 2 == 0 ? 1 : 2;
            if (golpesV % 3 == 2) { rajadaV = sozinho ? 1.8 : 1.3; chuteT = 0; SomJogo.tocar(SomJogo.Efeito.ALERTA); intervaloV = furia ? .9 : 1.2; } else { double[] o = irmao(0); double mira = Math.atan2(ny - o[1], nx - o[0]);
            if (golpesV % 2 == 0) { double g0 = tempo * 4; for (int k = 0; k < 6; k++) { double a = mira + Math.sin(g0 + k) * .35; tiro(tiros, o[0], o[1], a, 250 + k * 18, k == 0, TiroChefe.OSSO_PE, TiroChefe.Movimento.ONDA, VERDE); } } // chute giratorio: pes em espiral
            else for (int i = -2; i <= 2; i++) tiro(tiros, o[0], o[1], mira + i * .2, 260, false, TiroChefe.OSSO_SOCO, TiroChefe.Movimento.ONDA, VERDE); // garra do tigre: leque ondulando
            intervaloV = furia ? .9 : 1.2; } }
        // ROXO - KARATE: golpes secos, retos e pesados, com pausa (kiai)
        intervaloR -= dt * cadI;
        if (rajadaR > 0) { rajadaR -= dt; gestoR = .1; poseR = (int) (tempo * 22) % 2 == 0 ? 2 : 0; tapaT -= dt; // rajada de mil tapas: maos em borrao na frente dele
            if (tapaT <= 0) { tapaT = .045; double[] o = irmao(1); double a = Math.atan2(ny - o[1], nx - o[0]) + (Math.random() - .5) * .7;
                tiros.add(new TiroChefe(o[0] - 30, o[1] + (Math.random() - .5) * 60, Math.cos(a) * 560, Math.sin(a) * 560, false, TiroChefe.OSSO_SOCO, TiroChefe.Movimento.RETO).comCor(ROXO).comDuracao(.55)); } }
        if (vivo(1) && livre(1) && intervaloR <= 0 && rajadaR <= 0) { golpesR++; gestoR = .5; poseR = 2; double[] o = irmao(1);
            if (golpesR % 3 == 1) { rajadaR = sozinho ? 1.8 : 1.3; tapaT = 0; SomJogo.tocar(SomJogo.Efeito.ALERTA); } // rajada de mil tapas
            else if (golpesR % 3 == 2) { int buraco = (int) Math.max(0, Math.min(6, (ny - TOPO - 40) / 70)); for (int k = 0; k < 7; k++) if (k != buraco) tiros.add(new TiroChefe(o[0], TOPO + 50 + k * 70, -420, 0, true, TiroChefe.OSSO_SOCO, TiroChefe.Movimento.RETO).comCor(ROXO)); } // golpe de mao (shuto): parede reta e rapida com um vao
            else { double mira = Math.atan2(ny - o[1], nx - o[0]); for (int k = 0; k < 3; k++) tiro(tiros, o[0] - k * 40, o[1], mira, 520, true, TiroChefe.OSSO_SOCO, TiroChefe.Movimento.RETO, ROXO); } // soco reto com kiai: tres punhos em fila, muito rapidos
            intervaloR = furia ? 1.5 : 2.0; }
    }
    private static final Color VERDE = new Color(120, 255, 90), ROXO = new Color(200, 110, 255);
    // ----- queda de um gemeo: treme, desmonta em ossos que caem e se amontoam, a alma sobe e tudo some -----
    private static final class Queda { BufferedImage img; int dx, dy, dw, dh, i; double t; }
    private Queda[] renascer;
    private final java.util.List<Queda> quedas = new java.util.ArrayList<>();
    private Queda novaQueda(int i) {
        BufferedImage folha = ato == BARDO ? GEMEOS_KF : GEMEOS_DANCA; int n = ato == BARDO ? 4 : 8, t = TAM[ato];
        int fw = folha.getWidth() / n, fh = folha.getHeight(), h = (int) (t * 1.02), w = h * fw / fh, x0 = (int) (x + t - w + 30), y0 = (int) (y + t - h);
        Queda q = new Queda(); q.i = i; q.img = folha.getSubimage(i == 0 ? 0 : fw / 2, 0, fw / 2, fh); q.dx = x0 + (i == 0 ? 0 : w / 2) + (int) offX[i]; q.dy = y0 + (int) offY[i]; q.dw = w / 2; q.dh = h; return q;
    }
    private void desenharQueda(Graphics2D g, Queda q) {
        Color cor = q.i == 0 ? VERDE : ROXO; int cols = 8, lins = 10, sw = q.img.getWidth() / cols, sh = q.img.getHeight() / lins, tw = q.dw / cols, th = q.dh / lins;
        double tr = q.t - .3, chao = q.dy + q.dh; float al = (float) Math.max(0, Math.min(1, (2.6 - q.t) / .8));
        if (tr < 0) { int tremor = (int) (Math.sin(q.t * 90) * 6); Graphics2D a = (Graphics2D) g.create(); a.drawImage(q.img, q.dx + tremor, q.dy, q.dw, q.dh, null); // treme e acende
            a.setComposite(AlphaComposite.SrcOver.derive((float) (.6 * Math.abs(Math.sin(q.t * 40))))); a.setColor(Color.WHITE); a.fillRect(q.dx, q.dy, q.dw, q.dh); a.dispose(); return; }
        java.util.Random r = new java.util.Random(77 + q.i);
        for (int ly = 0; ly < lins; ly++) for (int lx = 0; lx < cols; lx++) {
            double rx = r.nextDouble(), ry = r.nextDouble(), rr = r.nextDouble(); int sx = lx * sw, sy = ly * sh;
            if (!temPixel(q.img, sx, sy, sw, sh)) continue;
            double px = q.dx + lx * tw + (rx - .5) * 320 * Math.min(tr, 1.1), py = q.dy + ly * th - 260 * ry * tr + 900 * tr * tr;
            double fundo = chao - th - (ry * 30); boolean pousou = py > fundo; if (pousou) py = fundo; // os ossos se amontoam no chao
            Graphics2D a = (Graphics2D) g.create(); a.setComposite(AlphaComposite.SrcOver.derive(al)); a.translate(px + tw / 2.0, py + th / 2.0);
            a.rotate((rr - .5) * 9 * Math.min(tr, pousou ? .35 : 1)); a.drawImage(q.img, -tw / 2, -th / 2, tw / 2 + tw % 2, th / 2 + th % 2, sx, sy, sx + sw, sy + sh, null); a.dispose();
        }
        if (ato == BELZEBU) return; // no fim da danca as almas sao da cena do buraco negro
        // a alma sobe em fogo-fatuo
        Graphics2D a = (Graphics2D) g.create(); float aa = (float) Math.max(0, Math.min(1, 1.6 - tr) * .7); a.setComposite(AlphaComposite.SrcOver.derive(aa)); a.setColor(cor);
        int cx = q.dx + q.dw / 2, cy = (int) (q.dy + q.dh * .3 - tr * 160); for (int k = 0; k < 7; k++) { int rr2 = 22 - k * 3; a.fillOval(cx - rr2 + (int) (Math.sin(tr * 9 + k) * 8), cy + k * 12, rr2 * 2, rr2 * 2); }
        a.dispose();
    }
    /** Troca o verde da tunica e do fogo por roxo (o mesmo gemeo, versao karate); osso, barba e madeira ficam. */
    private static BufferedImage roxear(BufferedImage a) {
        if (a == null) return null; BufferedImage o = new BufferedImage(a.getWidth(), a.getHeight(), BufferedImage.TYPE_INT_ARGB); float[] hsb = new float[3];
        for (int yy = 0; yy < a.getHeight(); yy++) for (int xx = 0; xx < a.getWidth(); xx++) { int c = a.getRGB(xx, yy), al = c >>> 24; if (al == 0) continue;
            Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, hsb);
            if (hsb[0] > .14f && hsb[0] < .48f && hsb[1] > .3f) c = (al << 24) | (Color.HSBtoRGB(.8f, Math.min(1f, hsb[1] * .9f), hsb[2]) & 0xFFFFFF);
            o.setRGB(xx, yy, c); }
        return o;
    }
    private static boolean temPixel(BufferedImage b, int x, int y, int w, int h) { for (int yy = y; yy < y + h; yy += 3) for (int xx = x; xx < x + w; xx += 3) if ((b.getRGB(xx, yy) >>> 24) > 40) return true; return false; }
    // ----- cada gemeo tem a propria vida; o ato so acaba quando os dois caem -----
    private double gestoV, gestoR, rajadaR, tapaT, rajadaV, chuteT; private int poseV, poseR; // cada gemeo anima o proprio golpe
    private int vidaV, vidaR, maxIrmao, alvoIrmao; private double quedaV, quedaR;
    // ----- cada gemeo se defende, se cura e parte pra cima da nave, cada um no seu tempo -----
    private static final int LIVRE = 0, GUARDA = 1, CURA = 2, INVESTIDA = 3;
    private final int[] est = new int[2], curas = new int[2]; private final double[] estT = new double[2], offX = new double[2], offY = new double[2], danoRecente = new double[2],
        cdGuarda = new double[2], cdCura = {6, 9}, cdInvest = {3.5, 6.5}, alvoX = new double[2], alvoY = new double[2], curaAcum = new double[2], bloqueio = new double[2];
    private double somBloqueio, pose; private static final double POSE_DUR = 2.4, POSE_KIAI = 1.1;
    private boolean livre(int i) { return est[i] == LIVRE; }
    private void reagir(double dt, int naveX, int naveY) {
        somBloqueio -= dt; boolean pausa = ato == MONGE && (loboT > 0 || moon > 0); boolean furia = vida < getVidaMaxima() * .35;
        for (int i = 0; i < 2; i++) {
            danoRecente[i] = Math.max(0, danoRecente[i] - dt * maxIrmao * .05); cdGuarda[i] -= dt; cdCura[i] -= dt; cdInvest[i] -= dt; bloqueio[i] = Math.max(0, bloqueio[i] - dt);
            int vi = i == 0 ? vidaV : vidaR, outro = 1 - i;
            if (vi <= 0 || pausa) { est[i] = LIVRE; offX[i] *= Math.max(0, 1 - dt * 6); offY[i] *= Math.max(0, 1 - dt * 6); continue; }
            if (est[i] == LIVRE) {
                offX[i] *= Math.max(0, 1 - dt * 4); offY[i] *= Math.max(0, 1 - dt * 4);
                if (danoRecente[i] > maxIrmao * .05 && cdGuarda[i] <= 0) { est[i] = GUARDA; estT[i] = 1.4; cdGuarda[i] = furia ? 3.5 : 5; SomJogo.tocar(SomJogo.Efeito.ESCUDO); } // apanhou muito rapido: guarda
                else if (vi < maxIrmao * .5 && cdCura[i] <= 0 && curas[i] < 3 && est[outro] != CURA) { est[i] = CURA; estT[i] = 2.4; cdCura[i] = 13; curas[i]++; curaAcum[i] = 0; SomJogo.tocar(SomJogo.Efeito.REPARO); } // senta e toca violao para se curar
                else if (cdInvest[i] <= 0) { est[i] = INVESTIDA; estT[i] = 0; cdInvest[i] = (furia ? 4.5 : 6.5) + Math.random() * 2; SomJogo.tocar(SomJogo.Efeito.ALERTA); } // parte pra cima da nave
                continue; }
            estT[i] += est[i] == INVESTIDA ? dt : -dt;
            if (est[i] == GUARDA) { if (estT[i] <= 0) est[i] = LIVRE; }
            else if (est[i] == CURA) { curaAcum[i] += dt * maxIrmao * .3 / 2.4; int ganho = (int) curaAcum[i]; curaAcum[i] -= ganho; // recupera 30% da vida
                if (i == 0) vidaV = Math.min(maxIrmao, vidaV + ganho); else vidaR = Math.min(maxIrmao, vidaR + ganho); vida = vidaV + vidaR;
                if (estT[i] <= 0) est[i] = LIVRE; }
            else { double e = estT[i]; double[] o = irmao(i); // 0-.5 prepara tremendo, .5-1.2 dispara ate onde a nave estava, depois volta
                if (e < .5) { alvoX[i] = naveX + 22; alvoY[i] = naveY + 18; offX[i] += Math.sin(e * 80) * 2.5; }
                else if (e < 1.2) { double vx = alvoX[i] - o[0], vy = alvoY[i] - o[1], d = Math.hypot(vx, vy), passo = Math.min(d, 950 * dt); if (d > 1) { offX[i] += vx / d * passo; offY[i] += vy / d * passo; } }
                else { offX[i] *= Math.max(0, 1 - dt * 3.2); offY[i] *= Math.max(0, 1 - dt * 3.2); if (Math.hypot(offX[i], offY[i]) < 6) { offX[i] = offY[i] = 0; est[i] = LIVRE; } } }
        }
    }
    /** Escudo da guarda, aura da cura e o rastro da investida, por cima dos gemeos. */
    private void desenharReacoes(Graphics2D g) {
        int t = TAM[ato];
        for (int i = 0; i < 2; i++) { if (!vivo(i)) continue; double[] o = irmao(i); Color c = i == 0 ? VERDE : ROXO; Graphics2D a = (Graphics2D) g.create();
            if (est[i] == GUARDA && GUARDA_ARTE == null) { float al = (float) Math.min(1, estT[i] / .3) * (.45f + (float) bloqueio[i] * 3); a.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, al)));
                a.setColor(c); a.setStroke(new BasicStroke(6)); int r = (int) (t * .38); for (int k = 0; k < 3; k++) a.drawArc((int) o[0] - r - k * 10 - 30, (int) o[1] - r, r * 2, r * 2, 110, 140); // arcos de osso na frente
                a.setColor(Color.WHITE); a.setStroke(new BasicStroke(3)); a.drawArc((int) o[0] - r - 30, (int) o[1] - r, r * 2, r * 2, 110, 140); }
            else if (est[i] == CURA) { a.setComposite(AlphaComposite.SrcOver.derive(.35f + .15f * (float) Math.sin(tempo * 8))); a.setColor(c); int r = (int) (t * .45); a.fillOval((int) o[0] - r, (int) o[1] - r, r * 2, r * 2);
                a.setComposite(AlphaComposite.SrcOver); for (int k = 0; k < 6; k++) { double f = (tempo * .9 + k / 6.0) % 1; int px = (int) (o[0] + Math.sin(k * 2.1 + tempo * 2) * t * .3), py = (int) (o[1] + t * .4 - f * t * .9); a.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * (1 - f))));
                    a.fillRect(px - 2, py - 8, 5, 17); a.fillRect(px - 8, py - 2, 17, 5); } } // cruzes de vida subindo
            else if (est[i] == INVESTIDA && estT[i] < .5) { a.setComposite(AlphaComposite.SrcOver.derive((float) (.3 + .3 * Math.sin(estT[i] * 40)))); a.setColor(new Color(255, 60, 40)); int r = (int) (t * .42); a.fillOval((int) o[0] - r, (int) o[1] - r, r * 2, r * 2); }
            else if (est[i] == INVESTIDA && estT[i] < 1.2) { a.setStroke(new BasicStroke(4)); for (int k = 0; k < 6; k++) { a.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 160 - k * 22)); int yy = (int) (o[1] - t * .3 + k * t * .12); a.drawLine((int) o[0] + 40, yy, (int) o[0] + 140 + k * 15, yy); } } // linhas de velocidade
            a.dispose(); }
    }
    private boolean emDupla() { return gemeos() && (ato == BARDO || ato == MONGE); }
    private void dividirVida() { if (!emDupla()) return; maxIrmao = (vida + 1) / 2; vidaV = vidaR = maxIrmao; vida = vidaV + vidaR; quedaV = quedaR = 0; }
    private boolean vivo(int i) { return !emDupla() || (i == 0 ? vidaV : vidaR) > 0; }
    /** Caixa de um gemeo (0 verde, na frente; 1 roxo, atras). */
    private Rectangle caixaIrmao(int i) { int t = TAM[ato]; double[] o = irmao(i); return new Rectangle((int) (o[0] - t * .24), (int) (y + t * .12 + offY[i]), (int) (t * .48), (int) (t * .86)); }
    // ----- lobisomens: na lua cheia os gemeos viram lobos; o verde corre pelo alto e o roxo por baixo, em sentidos opostos -----
    private static final BufferedImage LOBO = ler("lobisomem_corre.png"), LOBO_V = tingir(LOBO, VERDE), LOBO_R = tingir(LOBO, ROXO);
    private static final double LOBO_DUR = 5.4, PASSADA = 1.4, VIRA = .6;
    private double loboT; private final double[] loboSX = {-999, -999}, loboSY = {-999, -999}, loboVX = new double[2], loboVY = new double[2]; private final int[] loboK = {-1, -1};
    /** A cada passada o lobo sai de uma borda (o verde por cima, o roxo por baixo) e corre direto em cima da nave. */
    private void mirarLobos(int naveX, int naveY) {
        double e = LOBO_DUR - loboT - VIRA; if (e < 0) return; int k = (int) (e / PASSADA);
        for (int i = 0; i < 2; i++) { if (k == loboK[i]) continue; loboK[i] = k; boolean praEsq = (k + i) % 2 == 0;
            double sx = praEsq ? W + 150 : -150, sy = i == 0 ? TOPO + 20 : H - 40, dx = naveX + 22 - sx, dy = naveY + 18 - sy, d = Math.max(1, Math.hypot(dx, dy)), v = (d + 260) / PASSADA;
            loboSX[i] = sx; loboSY[i] = sy; loboVX[i] = dx / d * v; loboVY[i] = dy / d * v; if (vivo(i)) SomJogo.tocar(SomJogo.Efeito.ALERTA); }
    }
    private boolean lobisomens() { double e = LOBO_DUR - loboT; return loboT > 0 && e > VIRA && loboT > VIRA; }
    /** Retangulo do lobo i (0 verde no alto, 1 roxo embaixo) na passada atual. */
    private Rectangle lobo(int i) {
        double p = Math.max(0, (LOBO_DUR - loboT - VIRA) % PASSADA); double cx = loboSX[i] + loboVX[i] * p, cy = loboSY[i] + loboVY[i] * p;
        return new Rectangle((int) cx - 80, (int) cy - 40, 160, 80);
    }
    private void desenharLobos(Graphics2D g) {
        double e = LOBO_DUR - loboT;
        if (e < VIRA || loboT < VIRA) { // transformacao: os gemeos piscam e somem numa nevoa, ou voltam
            float a = (float) (e < VIRA ? 1 - e / VIRA : 1 - loboT / VIRA); corpo(g, MONGE, 0, x, y, TAM[MONGE], Math.max(.05f, a));
            java.util.Random r = new java.util.Random((long) (tempo * 20)); for (int k = 0; k < 30; k++) { g.setColor(k % 2 == 0 ? VERDE : ROXO); g.fillRect((int) x + r.nextInt(TAM[MONGE] * 2) - TAM[MONGE] / 2, (int) y + r.nextInt(TAM[MONGE]), 5, 5); }
            return;
        }
        for (int i = 0; i < 2; i++) { if (!vivo(i)) continue; Rectangle b = lobo(i); BufferedImage arte = i == 0 ? LOBO_V : LOBO_R; if (arte == null) continue;
            boolean praEsq = loboVX[i] < 0; double ang = Math.atan2(loboVY[i], loboVX[i]);
            int fw = arte.getWidth() / 4, fh = arte.getHeight(), q = (int) (tempo * 12) % 4, w = 260, h = w * fh / fw, cx = (int) b.getCenterX(), cy = (int) b.getCenterY();
            Graphics2D a = (Graphics2D) g.create(); a.translate(cx, cy); // inclinado na direcao da corrida
            if (praEsq) a.rotate(Math.max(-.6, Math.min(.6, ang - Math.PI * Math.signum(ang == 0 ? 1 : ang)))); else { a.rotate(Math.max(-.6, Math.min(.6, ang))); a.scale(-1, 1); }
            a.drawImage(arte, -w / 2, -h / 2, w / 2, h / 2, q * fw, 0, q * fw + fw, fh, null); a.dispose(); }
    }
    /** Copia da arte com um brilho de cor por cima (lobo verde e lobo roxo). */
    private static BufferedImage tingir(BufferedImage a, Color c) {
        if (a == null) return null; BufferedImage o = new BufferedImage(a.getWidth(), a.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int yy = 0; yy < a.getHeight(); yy++) for (int xx = 0; xx < a.getWidth(); xx++) { int v = a.getRGB(xx, yy), al = v >>> 24; if (al == 0) continue;
            int r = (v >> 16) & 255, gg = (v >> 8) & 255, b = v & 255; o.setRGB(xx, yy, (al << 24) | ((r * 6 + c.getRed() * 4) / 10 << 16) | ((gg * 6 + c.getGreen() * 4) / 10 << 8) | (b * 6 + c.getBlue() * 4) / 10); }
        return o;
    }
    private double intervaloV = 1.0, intervaloR = 1.9; private int golpesV, golpesR;
    /** Posicao de cada gemeo na arte (0 = verde, na frente; 1 = roxo, atras). */
    private double[] irmao(int i) { int t = TAM[ato]; double h = t * 1.02, w = h * 2.0, x0 = x + t - w + 30; return new double[]{x0 + w * (i == 0 ? .25 : .7) + offX[i], y + t * .45 + offY[i]}; }

    // ---------------- ato II: Irmaos Ossada na Danca Macabra ----------------
    private void monge(double dt, int naveX, int naveY, double cad, List<TiroChefe> tiros, boolean furia) {
        luaT += dt; danca += dt;
        if (loboT > 0) { loboT = Math.max(0, loboT - dt); mirarLobos(naveX, naveY); if (loboT == 0) SomJogo.tocar(SomJogo.Efeito.EVOLUCAO); return; } // viraram lobisomens: so as corridas
        if (moon > 0) { // moonwalk: deslizam para tras pela tela soltando notas
            moon = Math.max(0, moon - dt); x = baseX - Math.sin((1 - moon / 2.4) * Math.PI) * 340;
            if ((int) (moon * 8) != (int) ((moon + dt) * 8)) for (int pe = 0; pe < 2; pe++) tiros.add(new TiroChefe(x + TAM[MONGE] * (.35 + pe * .45), y + TAM[MONGE] * .97, 0, 0, false, TiroChefe.PEGADA, TiroChefe.Movimento.RETO)); // pegadas de fogo no moonwalk
            if ((int) (moon * 6) != (int) ((moon + dt) * 6)) { double oy = y + TAM[MONGE] * .5; tiro(tiros, x + 40, oy, Math.PI + (Math.random() - .5) * .6, 230, false, TiroChefe.OSSO_NOTA, TiroChefe.Movimento.ONDA, new Color(150, 255, 120)); }
            return;
        }
        flutuar(dt, naveY, .6); x += (baseX - x) * Math.min(1, dt * 2);
        if (horda > 0) { horda -= dt; if (horda <= 0 && levas > 0) { levas--; leva(tiros, naveY); horda = furia ? .85 : 1.15; } }
        if (lobos > 0 && (lobos -= dt) <= 0) { for (int lado = 0; lado < 2; lado++) { boolean esq = lado == 0; double ly = TOPO + 60 + Math.random() * (H - TOPO - 120);
                tiros.add(new TiroChefe(esq ? -40 : W + 40, ly, esq ? 520 : -520, 0, true, TiroChefe.LOBISOMEM, TiroChefe.Movimento.RETO)); } }
        if (tiros.size() < 60) { // cada gemeo tambem ataca sozinho, no proprio ritmo, enquanto a horda vem
            intervaloV -= dt * cad; intervaloR -= dt * cad;
            if (vivo(0) && livre(0) && intervaloV <= 0) { gestoV = .6; poseV = 3; double[] o = irmao(0); for (int k = 0; k < 4; k++) tiro(tiros, o[0] - k * 46, naveY + 18 + (k % 2 == 0 ? -40 : 40), Math.PI, 330, k == 0, TiroChefe.OSSO_PE, TiroChefe.Movimento.RETO, VERDE); intervaloV = furia ? 1.7 : 2.3; } // verde: chutes de danca em zigue-zague
            if (vivo(1) && livre(1) && intervaloR <= 0) { gestoR = .6; poseR = 6; double[] o = irmao(1); for (int k = 0; k < 10; k++) { double a = tempo * 3 + k * Math.PI / 5; tiro(tiros, o[0], o[1], a, 150 + (k % 2) * 40, false, TiroChefe.OSSO_NOTA, TiroChefe.Movimento.ONDA, ROXO); } intervaloR = furia ? 2.2 : 2.9; } // roxo: espiral de notas
        }
        intervalo -= dt * cad;
        if (intervalo > 0 || tiros.size() > 60 || horda > 0) return;
        golpes++; gesto = .5;
        int g = golpes % 3;
        if (g == 1) { horda = .3; levas = furia ? 4 : 3; danca = 0; SomJogo.tocar(SomJogo.Efeito.ALERTA); intervalo = 4.2; } // danca macabra: a horda sobe
        else if (g == 2) { moon = 2.4; intervalo = 3.0; }
        else { if (LOBO != null) { loboT = LOBO_DUR; loboK[0] = loboK[1] = -1; loboSX[0] = loboSX[1] = -999; SomJogo.tocar(SomJogo.Efeito.ALERTA); intervalo = 1.2; return; } // a lua transforma os gemeos em lobisomens
            if ((golpes / 3) % 2 == 0 && vivo(0) && livre(0) || !vivo(1) || !livre(1)) { gestoV = .6; poseV = 3; double[] o = irmao(0); for (int k = 0; k < 4; k++) tiro(tiros, o[0] - k * 46, naveY + 18 + (k % 2 == 0 ? -40 : 40), Math.PI, 330, k == 0, TiroChefe.OSSO_PE, TiroChefe.Movimento.RETO, VERDE); } // verde: chutes de dança, em zigue-zague
            else { gestoR = .6; poseR = 6; double[] o = irmao(1); for (int k = 0; k < 12; k++) { double a = tempo * 3 + k * Math.PI / 6; tiro(tiros, o[0], o[1], a, 150 + (k % 2) * 40, false, TiroChefe.OSSO_NOTA, TiroChefe.Movimento.ONDA, ROXO); } } // roxo: espiral de notas
            intervalo = furia ? 1.4 : 1.8; }
    }
    /** Uma fileira de mortos dancando da direita para a esquerda, com um buraco perto da nave. */
    private void leva(List<TiroChefe> tiros, int naveY) {
        double cy = TOPO + 80 + Math.random() * (H - TOPO - 160); // um bolo de mortos sai de tras dos gemeos e vem atras da nave
        for (int j = 0; j < 8; j++) { double a = j * 2.399, r = 18 + j * 9;
            tiros.add(new TiroChefe(W + 40 + Math.cos(a) * r, Math.max(TOPO + 40, Math.min(H - 40, cy + Math.sin(a) * r * 1.4)), -120, 0, false, TiroChefe.MORTO, TiroChefe.Movimento.RETO).comVariante((int) (Math.random() * 100))); }
    }

    // ---------------- ato III: Belzebu ----------------
    // Belzebu, o arauto baterista: alterna bateria (parado a direita), guitarra (a bateria some) e canto (anda pela tela)
    private static final BufferedImage BZ_BAT = ler("belzebu_bateria.png"), BZ_GUI = ler("belzebu_guitarra.png"), BZ_CAN = ler("belzebu_canto.png"), BZ_DER = ler("belzebu_derrota.png");
    private static final int BZ_BATERIA = 0, BZ_GUITARRA = 1, BZ_CANTO = 2; private static final double BZ_MORTE = 2.6;
    private int modoB; private double modoT, quadroB, troca, alvoBX, alvoBY, paradaB, belzMorte; private int passoB;
    private void belzebu(double dt, int naveX, int naveY, double cad, List<TiroChefe> tiros, boolean furia) {
        int t = TAM[BELZEBU]; double nx = naveX + 22, ny = naveY + 18; modoT += dt; troca = Math.max(0, troca - dt);
        double dur = modoB == BZ_BATERIA ? 7 : modoB == BZ_GUITARRA ? 6 : 8;
        if (modoT > dur) { modoB = (modoB + 1) % 3; modoT = 0; passoB = 0; batida = .6; troca = .5; paradaB = 0; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO); // fumaca na troca: a bateria some e volta
            if (modoB == BZ_CANTO) { alvoBX = x; alvoBY = y; } }
        if (modoB != BZ_CANTO) { // na bateria e na guitarra fica a direita, flutuando devagar
            double alvo = Math.max(TOPO - 20, Math.min(TiroChefe.LAVA_Y - t + 10, ny - t * .55));
            y += (alvo + Math.sin(tempo * 1.2) * 24 - y) * Math.min(1, dt * .8); x += (baseX + Math.sin(tempo * .7) * 16 - x) * Math.min(1, dt * 2); }
        if (tiros.size() > 55) return;
        double ox = x + t * .3, oy = y + t * .5;
        if (modoB == BZ_BATERIA) { // cada tempo do compasso e um golpe: tambor, prato, tambor, bumbo de fogo
            batida -= dt * cad; quadroB = batida > .2 ? 0 : quadroB;
            if (batida <= 0) { batida = furia ? .42 : .55; int b = passoB++ % 4; quadroB = b == 1 ? 2 : b == 3 ? 3 : 1; SomJogo.tocar(b == 3 ? SomJogo.Efeito.BATERIA_FORTE : SomJogo.Efeito.BATERIA_MEDIA);
                double mira = Math.atan2(ny - oy, nx - ox);
                if (b == 0 || b == 2) tiros.add(new TiroChefe(ox, oy + (b == 0 ? -50 : 50), Math.cos(mira) * 300, Math.sin(mira) * 300, false, TiroChefe.ONDA_BUMBO, TiroChefe.Movimento.RETO)); // onda do bumbo
                else if (b == 1) for (int i = -1; i <= 1; i += 2) tiro(tiros, ox, oy - 90, mira + i * .3, 270, true, TiroChefe.PRATO, TiroChefe.Movimento.ARCO, null); // pratos-serra
                else for (int i = 0; i < (furia ? 7 : 5); i++) tiros.add(new TiroChefe(x + t * .1 - i * 40, TiroChefe.LAVA_Y - 10, -260 - i * 15, 0, true, TiroChefe.PISTA_FOGO, TiroChefe.Movimento.RETO)); } // bumbo de fogo: a lava corre pela margem
        } else if (modoB == BZ_GUITARRA) { // riffs de fogo em rajada; no fim do solo ergue a guitarra e chove fogo
            batida -= dt * cad;
            if (modoT > dur - 1.4) { quadroB = 3; if (batida <= 0) { batida = .12; tiros.add(new TiroChefe(60 + Math.random() * (W - 120), TOPO - 10, -30, 120 + Math.random() * 80, false, TiroChefe.BOLA_FOGO, TiroChefe.Movimento.ARCO)); } }
            else { quadroB = (int) (modoT * 8) % 2 == 0 ? 0 : 1;
                if (batida <= 0) { batida = furia ? .75 : 1.0; quadroB = 2; double mira = Math.atan2(ny - oy, nx - ox); for (int i = -2; i <= 2; i++) tiro(tiros, ox - 40, oy - 30, mira + i * .14, 380, false, TiroChefe.RIFF, TiroChefe.Movimento.ONDA, null); } }
        } else { // canto: vai de ponto em ponto pela tela; em cada parada inspira, grita e berra
            if (paradaB <= 0) { double d = Math.hypot(alvoBX - x, alvoBY - y); if (d < 8) { paradaB = 2.0; passoB = 0; } else { double v = Math.min(d, 520 * dt); x += (alvoBX - x) / d * v; y += (alvoBY - y) / d * v; quadroB = 0; } }
            else { double antes = paradaB; paradaB -= dt; double e = 2.0 - paradaB;
                quadroB = e < .5 ? 0 : e < 1.1 ? 1 : e < 1.6 ? 2 : 3;
                if (antes > 1.5 && paradaB <= 1.5) { double mira = Math.atan2(ny - (y + t * .3), nx - (x + t * .25)); for (int i = -1; i <= 1; i++) tiro(tiros, x + t * .2, y + t * .3, mira + i * .28, 300, true, TiroChefe.GRITO, TiroChefe.Movimento.RETO, null); SomJogo.tocar(SomJogo.Efeito.TROVAO); }
                if (antes > .4 && paradaB <= .4) for (int i = 0; i < (furia ? 14 : 10); i++) tiro(tiros, x + t * .4, y + t * .35, i * Math.PI * 2 / (furia ? 14 : 10) + tempo, 220, false, TiroChefe.GRITO, TiroChefe.Movimento.RETO, null); // berro: onda em volta
                if (paradaB <= 0) { // proxima parada: as vezes em cima da nave, as vezes num canto
                    boolean perto = Math.random() < .45; alvoBX = perto ? Math.max(40, Math.min(W - t, nx + 160)) : W * .35 + Math.random() * (W * .65 - t); alvoBY = perto ? Math.max(TOPO - 20, Math.min(TiroChefe.LAVA_Y - t + 20, ny - t * .5)) : TOPO - 20 + Math.random() * (TiroChefe.LAVA_Y - t - TOPO + 40); } }
        }
    }
    // ----- apresentacao do Belzebu: sobe da lava com a bateria, faz a virada, estoura o bumbo e entra a cartela com o nome -----
    private static final double APRES_B = 5.2; private double apresB;
    private void apresentar(double dt) {
        double antes = APRES_B - apresB; apresB = Math.max(0, apresB - dt); double e = APRES_B - apresB; int t = TAM[BELZEBU];
        double yf = TOPO + (TiroChefe.LAVA_Y - TOPO - t) / 2.0 + 10, sobe = Math.min(1, e / 1.3), ss = 1 - Math.pow(1 - sobe, 3);
        y = TiroChefe.LAVA_Y + 30 + (yf - TiroChefe.LAVA_Y - 30) * ss; x = baseX;
        if (antes == 0) for (int k = 0; k < 6; k++) { Respingo r = new Respingo(); r.x = x + 40 + k * (t - 80) / 5.0; r.t = RESP_AVISO - .05 * k; r.alt = 150 + (k % 3) * 60; respingos.add(r); } // a lava espirra quando ele sai
        for (Respingo r : respingos) r.t += dt; respingos.removeIf(r -> r.t > RESP_AVISO + RESP_JATO);
        if (e < 1.3) quadroB = 0;
        else if (e < 2.7) { int b = (int) ((e - 1.3) / .14); quadroB = b % 2 == 0 ? 1 : 2; if ((int) ((antes - 1.3) / .14) != b && antes >= 1.3) SomJogo.tocar(SomJogo.Efeito.BATERIA_MEDIA); } // a virada
        else { quadroB = 3; if (antes < 2.7) { SomJogo.tocar(SomJogo.Efeito.BATERIA_FORTE); flash = .16; } } // bumbo de fogo
        if (antes < 2.9 && e >= 2.9) SomJogo.tocar(SomJogo.Efeito.TROVAO);
        if (apresB == 0) { intervalo = 1.0; modoT = 0; batida = .5; }
    }
    /** Cartela de apresentacao do chefe: faixa escura com o nome grande e o titulo embaixo. */
    private void cartela(Graphics2D g, String nome, String titulo, double e, double dur, Color cor) {
        if (e < 0 || e > dur) return; double entra = Math.min(1, e / .35), sai = Math.min(1, (dur - e) / .35), k = Math.min(entra, sai);
        Graphics2D a = (Graphics2D) g.create(); a.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int by = H - 205, bh = 92, off = (int) ((1 - entra) * -W + (1 - sai) * W);
        a.setComposite(AlphaComposite.SrcOver.derive((float) (.78 * k))); a.setColor(Color.BLACK); a.fillRect(0, by, W, bh);
        a.setComposite(AlphaComposite.SrcOver.derive((float) k)); a.setColor(cor); a.fillRect(0, by, W, 3); a.fillRect(0, by + bh - 3, W, 3);
        a.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 54)); FontMetrics fm = a.getFontMetrics(); int nx = (W - fm.stringWidth(nome)) / 2 + off, ny = by + 56;
        a.setColor(new Color(40, 0, 0)); for (int dx = -3; dx <= 3; dx += 3) for (int dy = -3; dy <= 3; dy += 3) a.drawString(nome, nx + dx, ny + dy);
        a.setColor(cor); a.drawString(nome, nx, ny);
        a.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18)); fm = a.getFontMetrics(); a.setColor(new Color(255, 220, 160)); a.drawString(titulo, (W - fm.stringWidth(titulo)) / 2 - off, by + 80); a.dispose();
    }
    /** Desenho do Belzebu com as folhas novas, na mesma escala (a bateria e a referencia). */
    private void desenharBelzebu(Graphics2D g) {
        int t = TAM[BELZEBU]; BufferedImage f; int q;
        if (belzMorte > 0) { f = BZ_DER; double e = 1 - belzMorte / BZ_MORTE; q = Math.min(3, (int) (e * 4)); }
        else { f = modoB == BZ_BATERIA ? BZ_BAT : modoB == BZ_GUITARRA ? BZ_GUI : BZ_CAN; q = (int) quadroB; }
        double esc = t * 1.05 / BZ_BAT.getHeight(); int fw = f.getWidth() / 4, fh = f.getHeight(), w = (int) (fw * esc), h = (int) (fh * esc), cx = (int) (x + t / 2.0), base = (int) (y + t);
        Graphics2D a = (Graphics2D) g.create(); if (belzMorte > 0 && belzMorte < .6) a.setComposite(AlphaComposite.SrcOver.derive((float) (belzMorte / .6)));
        a.drawImage(f, cx - w / 2, base - h, cx + w / 2, base, q * fw, 0, q * fw + fw, fh, null); a.dispose();
        if (troca > 0) { java.util.Random r = new java.util.Random(9); float al = (float) (troca / .5); for (int k = 0; k < 18; k++) { int rr = 30 + r.nextInt(50); g.setColor(new Color(90, 70, 70, (int) (180 * al))); g.fillOval(cx - 150 + r.nextInt(300) - rr / 2, base - 40 - r.nextInt(200) - rr / 2, rr, rr); } } // fumaca da troca
    }
    private boolean avisandoInvestida() { return ato == BELZEBU && investida > 1.1 || ato == ZAREF && investida > .8; }

    // ---------------- ato IV: Ronnie Hexar Zaref ----------------
    private double maoX() { return ato == CORRENTES ? x + TAM[CORRENTES] * (DEMONIO_CORRENTES != null ? .5 : .28) : x + TAM[ZAREF] * .32; }
    private double maoY() { return ato == CORRENTES ? y + TAM[CORRENTES] * (DEMONIO_CORRENTES != null ? .4 : .3) : y + TAM[ZAREF] * .42; }
    /** O arco-iris sai da mao erguida nos chifres (quadro 4 do Zaref de espada). */
    private double raioX() { return ZAREF_ESPADA != null ? x + TAM[ZAREF] * .86 : maoX(); }
    private double raioY() { return ZAREF_ESPADA != null ? y + TAM[ZAREF] * .11 : maoY(); }
    private static final double ALCANCE_RAIO = 1100;
    private void zaref(double dt, int naveX, int naveY, double cad, List<TiroChefe> tiros, boolean furia) {
        if (investida > 0) {
            // investida de espada: avanca ate o ponto travado e volta
            investida = Math.max(0, investida - dt);
            double p = investida > .8 ? 0 : 1 - investida / .8, arco = Math.sin(p * Math.PI);
            x = baseX + (alvoInvX - baseX) * arco; y += (alvoInvY - y) * Math.min(1, dt * 5) * arco;
            return;
        }
        flutuar(dt, naveY, .9);
        x += (baseX + Math.sin(tempo * .9) * 24 - x) * Math.min(1, dt * 2);
        double alvo = Math.atan2(naveY + 18 - raioY(), naveX + 22 - raioX()), alvoMao = Math.atan2(naveY + 18 - maoY(), naveX + 22 - maoX());
        if (cargaRaio > 0) {
            cargaRaio = Math.max(0, cargaRaio - dt); direcaoRaio = alvo;
            if (cargaRaio == 0) raio = furia ? 1.6 : 1.2;
            return;
        }
        if (raio > 0) {
            // o arco-iris sai da mao e vira atras da nave, mais devagar que ela
            raio = Math.max(0, raio - dt);
            double dif = Math.atan2(Math.sin(alvo - direcaoRaio), Math.cos(alvo - direcaoRaio)), giro = (furia ? .55 : .4) * dt;
            direcaoRaio += Math.max(-giro, Math.min(giro, dif));
            return;
        }
        intervalo -= dt * cad;
        if (intervalo > 0 || tiros.size() > 50) return;
        golpes++; gesto = .55;
        Color[] cores = {new Color(255, 60, 60), new Color(255, 160, 40), new Color(255, 240, 60), new Color(80, 230, 90), new Color(70, 150, 255), new Color(170, 90, 255)};
        int g = golpes % 4;
        if (g == 1) cargaRaio = 1.0;
        else if (g == 2) {
            // cortes da espada: meias-luas prateadas, duas levas
            for (int i = -1; i <= 1; i++) { tiro(tiros, maoX(), maoY(), alvoMao + i * .2, 330, true, TiroChefe.ENERGIA, TiroChefe.Movimento.ACELERADO, new Color(220, 235, 255)); if (furia) tiro(tiros, maoX() + 40, maoY(), alvoMao + i * .2 + .1, 280, true, TiroChefe.ENERGIA, TiroChefe.Movimento.ACELERADO, new Color(170, 200, 255)); }
        } else if (g == 3) {
            // orbes do arco-iris, cada uma de uma cor
            for (int i = 0; i < 6; i++) tiro(tiros, maoX(), maoY(), alvo + (i - 2.5) * .15, 250, false, TiroChefe.ENERGIA, TiroChefe.Movimento.ONDA, cores[i]);
        } else {
            investida = .8 + .6; int t = TAM[ZAREF];
            alvoInvX = Math.max(-40, naveX - t * .3); alvoInvY = Math.max(TOPO, Math.min(H - t, naveY + 18 - t * .45));
            SomJogo.tocar(SomJogo.Efeito.CHEFE_ENTRADA);
        }
        intervalo = furia ? 1.2 : 1.6;
    }

    // ---------------- ato V: o demonio do Demonio das Correntes ----------------
    private void correntes(double dt, int naveX, int naveY, double cad, List<TiroChefe> tiros, boolean furia) {
        atualizarGancho(dt, naveX, naveY);
        atualizarDemonio(dt, naveY);
        if (avisoChicote > 0) { avisoChicote = Math.max(0, avisoChicote - dt); if (avisoChicote == 0) { chicote = .75; acertouChicote = false; SomJogo.tocar(SomJogo.Efeito.CHEFE_TIRO); } return; }
        if (chicote > 0) { chicote = Math.max(0, chicote - dt); double p = 1 - chicote / .75; anguloChicote = inicioChicote + (fimChicote - inicioChicote) * p * p * (3 - 2 * p); if (chicote == 0) intervalo = Math.max(intervalo, .7); return; }
        if (demonio > 0 || avisoDemonio > 0) return; // mergulhando: o corpo esta atravessando a tela
        if (gancho != 3) flutuar(dt, naveY, .9);
        x += (baseX + Math.sin(tempo * .9) * 24 - x) * Math.min(1, dt * 2);
        if (gancho != 0) return;
        intervalo -= dt * cad;
        if (intervalo > 0 || tiros.size() > 50) return;
        golpes++; gesto = .55;
        int g = golpes % 3;
        if (g == 1) { gancho = 1; faseGancho = 0; }
        else if (g == 2) {
            // chicote: a corrente varre um arco grande a partir da mao; o aviso mostra o caminho
            boolean deCima = naveY + 18 < maoY(); avisoChicote = .8;
            inicioChicote = Math.PI + (deCima ? .95 : -.95); fimChicote = Math.PI + (deCima ? -.95 : .95); anguloChicote = inicioChicote;
            if (furia) { inicioChicote += deCima ? .25 : -.25; fimChicote += deCima ? -.25 : .25; }
        } else { avisoDemonio = 1.0; demonioY = Math.max(TOPO - 40, Math.min(H - TAM[CORRENTES] + 40, naveY + 18 - TAM[CORRENTES] / 2.0)); }
        intervalo = furia ? 1.15 : 1.5;
    }
    private static final double ALCANCE_CHICOTE = 760;
    private boolean noChicote(Rectangle r) {
        if (chicote <= 0) return false;
        double ax = r.getCenterX() - maoX(), ay = r.getCenterY() - maoY(), ux = Math.cos(anguloChicote), uy = Math.sin(anguloChicote);
        double ao = ax * ux + ay * uy; if (ao < 0 || ao > ALCANCE_CHICOTE) return false;
        return Math.abs(ax * uy - ay * ux) < 20;
    }
    /** Corrente do Demonio das Correntes: sai da mao do Zaref, agarra a nave e arrasta. Doze tiros na corrente soltam. */
    private void atualizarGancho(double dt, int naveX, int naveY) {
        if (gancho == 0) return;
        double bx = maoX(), by = maoY(), nx = naveX + 22, ny = naveY + 18;
        if (gancho == 1) {
            faseGancho += dt; hx = bx; hy = by;
            if (faseGancho >= .6) { double a = Math.atan2(ny - by, nx - bx); hvx = Math.cos(a) * 640; hvy = Math.sin(a) * 640; gancho = 2; faseGancho = 0; vidaCorrente = 12; SomJogo.tocar(SomJogo.Efeito.CHEFE_TIRO); }
        } else if (gancho == 2) {
            faseGancho += dt; hx += hvx * dt; hy += hvy * dt;
            if (Math.hypot(hx - nx, hy - ny) < 30) { gancho = 3; tempoPreso = 0; SomJogo.tocar(SomJogo.Efeito.IMPACTO); }
            else if (faseGancho > 1.5 || hx < -20 || hy < TOPO - 20 || hy > H + 20) gancho = 4;
        } else if (gancho == 3) {
            hx = nx; hy = ny; tempoPreso += dt;
            if (Math.hypot(nx - bx, ny - by) < 80) { gancho = 4; danoGancho = 4; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO); }
            else if (tempoPreso > 4.5) gancho = 4;
        } else {
            double d = Math.hypot(bx - hx, by - hy);
            if (d < 30) { gancho = 0; intervalo = Math.max(intervalo, .8); return; }
            hx += (bx - hx) / d * 900 * dt; hy += (by - hy) / d * 900 * dt;
        }
    }
    /** O demonio do Demonio das Correntes: aviso na faixa da nave e depois atravessa a tela da direita para a esquerda. */
    private void atualizarDemonio(double dt, int naveY) {
        if (avisoDemonio > 0) {
            avisoDemonio = Math.max(0, avisoDemonio - dt);
            if (avisoDemonio == 0) { demonio = 1; demonioX = W + 20; SomJogo.tocar(SomJogo.Efeito.CHEFE_ENTRADA); }
            return;
        }
        if (demonio <= 0) return;
        demonioX -= 820 * dt; x = demonioX; y = demonioY;
        if (demonioX < -TAM[CORRENTES] - 40) { demonio = 0; x = W + 40; entrada = 0; } // volta entrando pela direita
    }
    int consumirDanoGancho() { int d = danoGancho; danoGancho = 0; return d; }
    boolean naveAgarrada() { return ato == CORRENTES && gancho == 3; }
    double[] puxaoNave() { if (ato == BELZEBU && transicao > 0) { double t = INVOCACAO - transicao; if (t > 3.2 && t < BURACO_FIM) return new double[]{W / 2 + 60, TOPO + (H - TOPO) / 2}; if (t >= BURACO_FIM) { double d = t - BURACO_FIM, e = Math.min(1, d / MERGULHO), r = 300 * Math.pow(1 - e, 1.3), a = Math.PI + e * 7; return new double[]{TUNEL_X + Math.cos(a) * r - 22 + 22, TUNEL_Y + Math.sin(a) * r * .7}; } } // o buraco negro puxa a nave
        return ato == CORRENTES && gancho == 3 ? new double[]{maoX(), maoY()} : null; }
    private boolean naCorrente(Rectangle r) {
        if (gancho != 2 && gancho != 3) return false;
        double ax = maoX(), ay = maoY(), dx2 = hx - ax, dy2 = hy - ay, l2 = dx2 * dx2 + dy2 * dy2;
        double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((r.getCenterX() - ax) * dx2 + (r.getCenterY() - ay) * dy2) / l2));
        return Math.hypot(r.getCenterX() - (ax + dx2 * t), r.getCenterY() - (ay + dy2 * t)) < 16 && t < .92;
    }
    private boolean noRaio(double cx, double cy) {
        if (raio <= 0) return false;
        double ax = cx - raioX(), ay = cy - raioY(), ux = Math.cos(direcaoRaio), uy = Math.sin(direcaoRaio);
        double ao = ax * ux + ay * uy; if (ao < 0 || ao > ALCANCE_RAIO) return false;
        return Math.abs(ax * uy - ay * ux) < 22;
    }

    private void tiro(List<TiroChefe> tiros, double x0, double y0, double a, double v, boolean pesado, int estilo, TiroChefe.Movimento m, Color cor) {
        TiroChefe t = new TiroChefe(x0, y0, Math.cos(a) * v, Math.sin(a) * v, pesado, estilo, m);
        tiros.add(cor != null ? t.comCor(cor) : t);
    }

    // ---------------- dano e colisao ----------------
    boolean colide(Rectangle r) {
        if (rio > 0 || belzMorte > 0) return false;
        if (apresB > 0) return r.intersects(getBounds()); // os tiros batem nele, mas nao ferem
        if (emDupla() && transicao <= 0 && entrada >= 1) {
            if (lobisomens()) { for (int i = 0; i < 2; i++) if (vivo(i) && r.intersects(lobo(i))) { alvoIrmao = i; return true; } return false; }
            for (int i = 0; i < 2; i++) if (vivo(i) && r.intersects(caixaIrmao(i))) { alvoIrmao = i; return true; } return false;
        }
        if (ato == CORRENTES && naCorrente(r)) { acertouCorrente = true; return true; }
        return r.intersects(getBounds());
    }
    boolean receberDano(int dano) {
        if (rio > 0 || belzMorte > 0 || apresB > 0) return false;
        if (entrada < 1 || transicao > 0) { acertouCorrente = false; return false; }
        if (acertouCorrente) {
            acertouCorrente = false; faiscaCorrente = .12;
            if (--vidaCorrente <= 0 && (gancho == 2 || gancho == 3)) { gancho = 4; SomJogo.tocar(SomJogo.Efeito.IMPACTO); }
            return false;
        }
        flash = .16;
        if (emDupla()) { int i = vivo(alvoIrmao) ? alvoIrmao : 1 - alvoIrmao, d = Math.max(0, dano);
            if (est[i] == GUARDA || pose < POSE_DUR) { flash = 0; bloqueio[i] = .18; if (somBloqueio <= 0) { SomJogo.tocar(SomJogo.Efeito.IMPACTO); somBloqueio = .12; } return false; } // defendeu: o tiro bate no braco cruzado
            danoRecente[i] += d; if (est[i] == CURA && danoRecente[i] > maxIrmao * .07) { est[i] = LIVRE; estT[i] = 0; } // apanhou demais: a musica para
            if (i == 0) { vidaV = Math.max(0, vidaV - d); if (vidaV == 0 && quedaV == 0) { quedaV = .01; if (ato == MONGE) dentes = Math.min(2, dentes + 1); quedas.add(novaQueda(0)); if (vidaR > 0) { aviso = "OSSADA VERDE CAIU! O ROXO FICA FURIOSO!"; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); } } }
            else { vidaR = Math.max(0, vidaR - d); if (vidaR == 0 && quedaR == 0) { quedaR = .01; if (ato == MONGE) dentes = Math.min(2, dentes + 1); quedas.add(novaQueda(1)); if (vidaV > 0) { aviso = "OSSADA ROXO CAIU! O VERDE FICA FURIOSO!"; SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE); } } }
            vida = vidaV + vidaR;
        } else vida = Math.max(0, vida - Math.max(0, dano));
        if (vida > 0) return false;
        if (ato == CORRENTES) return true;
        SomJogo.tocar(SomJogo.Efeito.EXPLOSAO_GRANDE);
        if (ato == BARDO) { dentes = 0; aviso = gemeos() ? "OS IRMAOS OSSADA COMECAM A DANCA MACABRA!" : "BARDO CAIU!"; proximo(MONGE, vidaMonge, 2.2); }
        else if (ato == MONGE) { dentes = 2; for (int i = 0; i < 2; i++) { double[] o = irmao(i); almaX[i] = o[0]; almaY[i] = o[1]; } aviso = "AS ALMAS DOS IRMAOS OSSADA SE FUNDEM!"; proximo(BELZEBU, vidaBelzebu, INVOCACAO); }
        else if (ato == BELZEBU) { if (BZ_DER != null) { belzMorte = BZ_MORTE; return false; } aviso = "O TRONO DESPERTA: ZAREF, O TEMPLARIO DO INFERNO!"; proximo(ZAREF, vidaZaref, 3.0); }
        else { aviso = "ZAREF VIRA O DEMONIO DAS CORRENTES!"; proximo(CORRENTES, vidaCorrentes, 3.2); }
        return false;
    }
    // ---------------- o rio de lava: respingos, hordas de demonios e a bateria do Belzebu chegando ----------------
    static final double RIO_DUR = 40, RESP_AVISO = .9, RESP_JATO = 1.2;
    private double rio, proxRespingo, proxOnda, proxBatida; private int ondas;
    private static final class Respingo { double x, t, alt; Rectangle caixa() { double h = jato(); return new Rectangle((int) x - 22, (int) (TiroChefe.LAVA_Y - h), 44, (int) h + 10); }
        double jato() { double e = (t - RESP_AVISO) / RESP_JATO; return e <= 0 || e >= 1 ? 0 : alt * Math.sin(e * Math.PI); } }
    private final List<Respingo> respingos = new java.util.ArrayList<>();
    private final List<TiroChefe> cuspidores = new java.util.ArrayList<>();
    /** No inferno (depois da dobra) o fundo e o rio de lava. */
    boolean inferno() { return ato >= BELZEBU && transicao <= 0; }
    private void rio(double dt, int naveX, int naveY, List<TiroChefe> tiros) {
        rio = Math.max(0, rio - dt); double p = 1 - rio / RIO_DUR; x = W + 40;
        // respingos: alguns caem aleatorios, outros embaixo da nave para obrigar a mexer
        for (Respingo r : respingos) r.t += dt; respingos.removeIf(r -> r.t > RESP_AVISO + RESP_JATO);
        if ((proxRespingo -= dt) <= 0 && rio > 3) { proxRespingo = 1.4 - p * .6; Respingo r = new Respingo(); r.x = Math.random() < .4 ? naveX + 22 + (Math.random() - .5) * 60 : 60 + Math.random() * (W - 120); r.alt = 200 + Math.random() * 170; respingos.add(r); SomJogo.tocar(SomJogo.Efeito.FOGO); }
        // hordas: cada onda escolhe um tipo de demonio; ficam mais fortes perto do fim
        if ((proxOnda -= dt) <= 0 && rio > 5 && tiros.size() < 60) { ondas++; int tipo = ondas % 4; proxOnda = 4.2 - p * 1.4;
            if (tipo == 1) { double cy = TOPO + 60 + Math.random() * (TiroChefe.LAVA_Y - TOPO - 160); for (int k = 0; k < 6 + (int) (p * 4); k++) tiros.add(new TiroChefe(W + 30 + k * 26, cy + (k % 3 - 1) * 30, -150, 0, false, TiroChefe.DIABRETE, TiroChefe.Movimento.RETO).comVariante(k).demonio()); }
            else if (tipo == 2) { for (int k = 0; k < 2 + (int) (p * 2); k++) tiros.add(new TiroChefe(W + 60 + k * 170, TiroChefe.LAVA_Y - 26, -260, 0, true, TiroChefe.CAO, TiroChefe.Movimento.RETO).comVariante(k).demonio()); }
            else if (tipo == 3) { for (int k = 0; k < 3; k++) tiros.add(new TiroChefe(Math.max(120, Math.min(W - 80, naveX + 120 + k * 150)), TOPO + 10, 0, 0, true, TiroChefe.GARGULA, TiroChefe.Movimento.RETO).comVariante(k).demonio()); }
            else { TiroChefe c = new TiroChefe(W + 60, TOPO + 90 + Math.random() * 220, -70, 0, true, TiroChefe.CUSPIDOR, TiroChefe.Movimento.RETO).demonio(); tiros.add(c); cuspidores.add(c); } }
        cuspidores.removeIf(c -> !c.isVisible());
        for (TiroChefe c : cuspidores) if (c.cuspidorParado() && Math.random() < dt / 1.5) { c.cuspir(); double a = Math.atan2(naveY + 18 - c.getY(), naveX + 22 - c.getX()); tiro(tiros, c.getX() - 70, c.getY(), a, 280, true, TiroChefe.BOLA_FOGO, TiroChefe.Movimento.RETO, null); }
        // a bateria do Belzebu: de longe e devagar, ate o bumbo dobrado
        if ((proxBatida -= dt) <= 0) { proxBatida = 1.3 - p * .85; batidaVisual = 1; SomJogo.tocar(p < .35 ? SomJogo.Efeito.BATERIA_FRACA : p < .75 ? SomJogo.Efeito.BATERIA_MEDIA : SomJogo.Efeito.BATERIA_FORTE); }
        batidaVisual = Math.max(0, batidaVisual - dt * 4);
        if (rio == 0) { tiros.clear(); cuspidores.clear(); apresB = APRES_B; entrada = 1; int t = TAM[BELZEBU]; baseX = W - t - 20; x = baseX; modoB = BZ_BATERIA; modoT = 0; quadroB = 0; respingos.clear(); SomJogo.tocar(SomJogo.Efeito.CHEFE_ENTRADA); } // a apresentacao comeca
    }
    private double batidaVisual;
    /** Fundo do inferno: ceu de brasa, torres de ossos e o rio de lava embaixo (com a arte, quando chegar). */
    void desenharFundo(Graphics2D g) {
        if (!inferno()) return;
        if (FUNDO_ARTE != null) { int h = TiroChefe.LAVA_Y + 40, w = FUNDO_ARTE.getWidth() * h / FUNDO_ARTE.getHeight(), off = (int) (tempo * 22) % (w * 2); // rola devagar; um ladrilho sim, outro espelhado, para nao aparecer emenda
            for (int xx = -off, k = 0; xx < W; xx += w, k++) { if (k % 2 == 0) g.drawImage(FUNDO_ARTE, xx, 0, w, h, null); else g.drawImage(FUNDO_ARTE, xx + w, 0, -w, h, null); } }
        else if (RIO_ARTE != null) { int w = RIO_ARTE.getWidth() * H / RIO_ARTE.getHeight(), off = (int) (tempo * 40) % Math.max(1, w); for (int xx = -off; xx < W; xx += w) g.drawImage(RIO_ARTE, xx, 0, w, H, null); }
        else {
            g.setPaint(new java.awt.GradientPaint(0, TOPO, new Color(40, 0, 8), 0, H, new Color(150, 30, 10))); g.fillRect(0, 0, W, H);
            java.util.Random r = new java.util.Random(31); int off = (int) (tempo * 25);
            for (int k = 0; k < 14; k++) { int bx = Math.floorMod(r.nextInt(W + 300) - off, W + 300) - 150, bw = 40 + r.nextInt(70), bh = 120 + r.nextInt(240), by = TiroChefe.LAVA_Y - bh + 20;
                g.setColor(new Color(30 + r.nextInt(20), 6, 10)); g.fillPolygon(new int[]{bx, bx + bw / 2, bx + bw}, new int[]{TiroChefe.LAVA_Y + 10, by, TiroChefe.LAVA_Y + 10}, 3);
                if (k % 3 == 0) { g.setColor(new Color(200, 180, 150, 120)); for (int c = 0; c < 4; c++) g.fillOval(bx + bw / 2 - 8, by + 30 + c * 26, 16, 16); } } // torres de ossos
            for (int k = 0; k < 40; k++) { int ex = Math.floorMod(r.nextInt(W) - (int) (tempo * (30 + k)), W), ey = Math.floorMod(r.nextInt(H) - (int) (tempo * (40 + k * 2)), H - TOPO) + TOPO; g.setColor(new Color(255, 150 + r.nextInt(80), 40, 160)); g.fillRect(ex, ey, 3, 3); } // brasas subindo
        }
        desenharNavesCaidas(g); desenharPlateia(g);
        // o rio de lava: faixas brilhando que correm para a esquerda
        int ly = TiroChefe.LAVA_Y;
        if (LAVA_ARTE != null) { int fh = LAVA_ARTE.getHeight() / 4, q = (int) (tempo * 6) % 4, off = (int) (tempo * 90) % W, y0 = ly + 4 - 54; // a crista da arte fica onde a lava fere
            for (int xx = -off; xx < W; xx += W) g.drawImage(LAVA_ARTE, xx, y0, xx + W, Math.max(H, y0 + fh), 0, q * fh, LAVA_ARTE.getWidth(), q * fh + fh, null);
            if (batidaVisual > 0) { g.setColor(new Color(255, 40, 0, (int) (60 * batidaVisual * Math.min(1, (1 - rio / RIO_DUR) * 2)))); g.fillRect(0, TOPO, W, H - TOPO); }
            return; }
        g.setPaint(new java.awt.GradientPaint(0, ly, new Color(255, 200, 60), 0, H, new Color(180, 30, 0))); g.fillRect(0, ly, W, H - ly);
        for (int k = 0; k < 6; k++) { int yy = ly + 8 + k * 12; g.setColor(new Color(255, 240 - k * 25, 120 - k * 15, 150)); for (int xx = -80; xx < W; xx += 80) { int sx = Math.floorMod(xx - (int) (tempo * (120 + k * 30)), W + 80) - 40; g.fillRoundRect(sx, yy + (int) (Math.sin(tempo * 3 + xx) * 3), 46, 4, 4, 4); } }
        g.setColor(new Color(255, 230, 120)); for (int xx = 0; xx < W; xx += 6) g.fillRect(xx, ly - 2 + (int) (Math.sin(xx * .05 + tempo * 4) * 3), 6, 4); // a crista da lava
        if (batidaVisual > 0) { g.setColor(new Color(255, 40, 0, (int) (60 * batidaVisual * Math.min(1, (1 - rio / RIO_DUR) * 2)))); g.fillRect(0, TOPO, W, H - TOPO); } // cada batida pulsa a tela
    }
    private static final BufferedImage FUNDO_ARTE = ler("inferno_fundo.png"), LAVA_ARTE = ler("inferno_lava.png");
    private static final BufferedImage RIO_ARTE = ler("inferno_rio_lava.png"), RESPINGO_ARTE = ler("respingo_lava.png");
    /** Naves de outros pilotos, escurecidas e avermelhadas: de longe, no inferno. */
    private static final BufferedImage[] NAVES_LONGE = navesLonge();
    private static BufferedImage[] navesLonge() {
        List<BufferedImage> l = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) { BufferedImage a = ler("nave_" + i + "_estagio_0.png"); if (a == null) continue; BufferedImage o = new BufferedImage(a.getWidth(), a.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int yy = 0; yy < a.getHeight(); yy++) for (int xx = 0; xx < a.getWidth(); xx++) { int c = a.getRGB(xx, yy), al = c >>> 24; if (al == 0) continue; int r = (c >> 16) & 255, gg = (c >> 8) & 255, b = c & 255;
                o.setRGB(xx, yy, (al << 24) | (Math.min(255, r * 70 / 100 + 45) << 16) | (gg * 50 / 100 + 10 << 8) | (b * 45 / 100 + 15)); }
            l.add(o); }
        return l.toArray(new BufferedImage[0]);
    }
    private static void naveLonge(Graphics2D g, int modelo, double cx, double cy, double esc, double ang, boolean praDireita) {
        if (NAVES_LONGE.length == 0) return; BufferedImage n = NAVES_LONGE[Math.floorMod(modelo, NAVES_LONGE.length)]; int w = (int) (n.getWidth() * esc), h = (int) (n.getHeight() * esc);
        Graphics2D a = (Graphics2D) g.create(); a.translate(cx, cy); a.rotate(ang); if (!praDireita) a.scale(-1, 1); a.drawImage(n, -w / 2, -h / 2, w, h, null); a.dispose();
    }
    /** A plateia do inferno na margem: monstros batendo cabeca no ritmo, de mao chifrada pra cima. */
    private void desenharPlateia(Graphics2D g) {
        int ly = TiroChefe.LAVA_Y, per = W + 260; double bpm = 2.3, forca = 1 + batidaVisual * 1.5;
        java.util.Random r = new java.util.Random(77); Graphics2D a = (Graphics2D) g.create();
        for (int i = 0; i < 46; i++) { double esc = i % 7 == 3 ? 1.7 : i % 3 == 0 ? 1.25 : 1; int cor = r.nextInt(3); double fase = r.nextDouble() * .5, bx = Math.floorMod((int) (i * per / 46.0 - tempo * 32), per) - 130;
            double bate = Math.max(0, Math.sin((tempo * bpm + fase) * Math.PI * 2)) * 7 * forca * esc; // headbang
            int by = ly - 4, cw = (int) (24 * esc), ch = (int) (30 * esc), hx = (int) bx, hy = (int) (by - ch - 6 * esc + bate);
            a.setColor(cor == 0 ? new Color(45, 8, 12) : cor == 1 ? new Color(35, 10, 30) : new Color(50, 18, 8));
            a.fillRoundRect(hx - cw / 2, by - ch, cw, ch + 6, cw / 2, cw / 2); // corpo
            int hr = (int) (9 * esc); a.fillOval(hx - hr, hy - hr, hr * 2, hr * 2); // cabeca
            a.fillPolygon(new int[]{hx - hr, hx - hr - (int) (4 * esc), hx - hr / 3}, new int[]{hy - hr / 2, hy - hr - (int) (8 * esc), hy - hr}, 3); a.fillPolygon(new int[]{hx + hr, hx + hr + (int) (4 * esc), hx + hr / 3}, new int[]{hy - hr / 2, hy - hr - (int) (8 * esc), hy - hr}, 3); // chifres
            if (i % 2 == 0) { int ax = hx + (i % 4 == 0 ? cw / 2 : -cw / 2), sobe = (int) (Math.sin((tempo * bpm + fase) * Math.PI * 2) * 3); a.setStroke(new BasicStroke((float) (4 * esc))); a.drawLine(ax, by - ch + (int) (6 * esc), ax + (i % 4 == 0 ? 4 : -4), hy - (int) (16 * esc) + sobe); // braco pra cima
                int fx = ax + (i % 4 == 0 ? 4 : -4), fy = hy - (int) (18 * esc) + sobe; a.fillOval(fx - (int) (4 * esc), fy - (int) (3 * esc), (int) (8 * esc), (int) (7 * esc)); a.setStroke(new BasicStroke((float) (2 * esc))); a.drawLine(fx - (int) (3 * esc), fy, fx - (int) (4 * esc), fy - (int) (8 * esc)); a.drawLine(fx + (int) (3 * esc), fy, fx + (int) (4 * esc), fy - (int) (8 * esc)); } // mao chifrada
            a.setColor(new Color(255, 200, 60)); int ol = Math.max(1, (int) (2 * esc)); a.fillRect(hx - hr / 2, hy - 1, ol, ol); a.fillRect(hx + hr / 3, hy - 1, ol, ol); } // olhos acesos
        a.setPaint(new GradientPaint(0, ly - 40, new Color(255, 120, 30, 0), 0, ly, new Color(255, 120, 30, 90))); a.fillRect(0, ly - 40, W, 40); a.dispose(); // a lava ilumina a plateia por baixo
    }
    /** Fundo do inferno: destrocos de naves caidas pegando fogo e, de vez em quando, uma nave abatida la longe. */
    private void desenharNavesCaidas(Graphics2D g) {
        int ly = TiroChefe.LAVA_Y, per = W + 400;
        // destrocos fincados na margem, rolando com o fundo
        for (int k = 0; k < 4; k++) { double bx = Math.floorMod((int) (k * per / 4 + 120 - tempo * 25), per) - 200, by = ly - 18 - (k % 2) * 10, ang = k % 2 == 0 ? .55 : -.4;
            for (int f = 0; f < 6; f++) { double fs = ((tempo * .5 + f / 6.0 + k * .13) % 1); g.setColor(new Color(60, 45, 45, (int) (150 * (1 - fs)))); int rr = (int) (10 + fs * 34); g.fillOval((int) (bx - 6 + fs * 30) - rr / 2, (int) (by - 20 - fs * 170) - rr / 2, rr, rr); } // coluna de fumaca
            naveLonge(g, k + 1, bx, by, 1.5, ang, k % 2 == 0);
            for (int f = 0; f < 3; f++) { int fh = 8 + (int) (Math.abs(Math.sin(tempo * 13 + f * 2 + k)) * 12); g.setColor(f == 1 ? new Color(255, 220, 90, 220) : new Color(255, 110, 20, 210)); g.fillPolygon(new int[]{(int) bx - 14 + f * 12, (int) bx - 8 + f * 12, (int) bx - 2 + f * 12}, new int[]{(int) by - 4, (int) by - 4 - fh, (int) by - 4}, 3); } } // fogo nos destrocos
        // a cada 5,5 s uma nave atravessa la no fundo, e acertada por fogo vindo do chao e cai na lava
        double ciclo = 5.5; long k = (long) Math.floor(tempo / ciclo); double e = tempo - k * ciclo; java.util.Random r = new java.util.Random(k * 7919);
        double yv = TOPO + 40 + r.nextDouble() * 130, xa = W * (.35 + r.nextDouble() * .4), ex = xa - 160 - r.nextDouble() * 120, esc = .8 + r.nextDouble() * .3; int modelo = r.nextInt(5); double ga = r.nextDouble() * W * .6 + W * .2;
        java.util.function.DoubleFunction<double[]> pos = q -> { if (q < 1.6) return new double[]{W + 60 - (W + 60 - xa) * q / 1.6, yv, 0};
            double d = Math.min(1, (q - 1.6) / 2.0); return new double[]{xa + (ex - xa) * d, yv + (ly - 10 - yv) * d * d, -d * 2.6}; };
        if (e < 3.6) {
            if (e > 1.6) for (int f = 9; f >= 1; f--) { double q = e - f * .09; if (q < 1.6) continue; double[] p2 = pos.apply(q); int rr = (int) (6 + f * 3 * esc * 2); g.setColor(new Color(70, 55, 55, 170 - f * 15)); g.fillOval((int) p2[0] - rr / 2, (int) p2[1] - rr / 2, rr, rr); } // rastro de fumaca
            double[] p = pos.apply(e); naveLonge(g, modelo, p[0], p[1], esc, p[2], false);
            if (e > 1.6) { int fl = (int) (6 + Math.abs(Math.sin(tempo * 25)) * 6); g.setColor(new Color(255, 140, 30, 220)); g.fillOval((int) p[0] - fl / 2 + 8, (int) p[1] - fl / 2, fl, fl); } // em chamas
            if (e > 1.2 && e < 1.6) { double q = (e - 1.2) / .4, fx = ga + (xa - 30 - ga) * q, fy = ly + (yv - ly) * q; g.setColor(new Color(255, 90, 20)); g.fillOval((int) fx - 6, (int) fy - 6, 12, 12); g.setColor(new Color(255, 230, 120)); g.fillOval((int) fx - 3, (int) fy - 3, 6, 6); } // o fogo sobe do chao
            if (e > 1.6 && e < 1.85) { int rr = (int) (10 + (e - 1.6) * 160); g.setColor(new Color(255, 210, 120, (int) (230 * (1 - (e - 1.6) / .25)))); g.fillOval((int) xa - rr / 2, (int) yv - rr / 2, rr, rr); } } // o impacto
        else if (e < 4.4) { double q = (e - 3.6) / .8; int rr = (int) (16 + q * 70); g.setColor(new Color(255, 150, 40, (int) (220 * (1 - q)))); g.fillOval((int) ex - rr / 2, ly - 10 - rr / 2, rr, rr); // explode na lava
            g.setColor(new Color(255, 200, 80, (int) (220 * (1 - q)))); for (int f = 0; f < 6; f++) { double ang = -Math.PI / 2 + (f - 2.5) * .35; g.fillOval((int) (ex + Math.cos(ang) * q * 60), (int) (ly - 10 + Math.sin(ang) * q * 60 + q * q * 50), 6, 6); } }
    }
    private void desenharRespingos(Graphics2D g) {
        for (Respingo r : respingos) { int ly = TiroChefe.LAVA_Y;
            if (r.t < RESP_AVISO && RESPINGO_ARTE != null) { int fw = RESPINGO_ARTE.getWidth() / 4, fh = RESPINGO_ARTE.getHeight(), w = 90, h = w * fh / fw; double inf = .7 + .3 * Math.abs(Math.sin(r.t * 10)); // a bolha incha: aviso
                g.drawImage(RESPINGO_ARTE, (int) r.x - w / 2, ly + 12 - (int) (h * inf), (int) r.x + w / 2, ly + 12, 0, 0, fw, fh, null); continue; }
            if (r.t < RESP_AVISO) { double e = r.t / RESP_AVISO; int bw = (int) (20 + e * 30); g.setColor(new Color(255, 250, 180, (int) (120 + 120 * Math.abs(Math.sin(r.t * 18))))); g.fillOval((int) r.x - bw / 2, ly - 8, bw, 16); continue; } // a lava borbulha: aviso
            double h = r.jato(); if (RESPINGO_ARTE != null) { int fw = RESPINGO_ARTE.getWidth() / 4, fh = RESPINGO_ARTE.getHeight(); double e = (r.t - RESP_AVISO) / RESP_JATO; int q = e < .3 ? 1 : e < .7 ? 2 : 3, hh = (int) (r.alt * .95), w = hh * fw / fh;
                g.drawImage(RESPINGO_ARTE, (int) r.x - w / 2, ly + 12 - hh, (int) r.x + w / 2, ly + 12, q * fw, 0, q * fw + fw, fh, null); continue; }
            g.setPaint(new java.awt.GradientPaint(0, (float) (ly - h), new Color(255, 250, 170), 0, ly, new Color(230, 60, 0))); g.fillRoundRect((int) r.x - 16, (int) (ly - h), 32, (int) h + 6, 20, 20);
            g.setColor(new Color(255, 200, 60)); java.util.Random rr = new java.util.Random((long) (r.x * 7)); for (int k = 0; k < 8; k++) { double ang = -Math.PI / 2 + (rr.nextDouble() - .5) * 1.6, d = h * .2 + rr.nextDouble() * 40; g.fillOval((int) (r.x + Math.cos(ang) * d), (int) (ly - h + 10 + Math.sin(ang) * d * .4 + (r.t - RESP_AVISO) * 60), 8, 8); } }
    }
    private void proximo(int novo, int v, double pausa) {
        ato = novo; vida = v; dividirVida(); transicao = pausa; entrada = 0; pose = 0; x = W + 40; intervalo = 1.6; golpes = 0; investida = 0; bateria = 0;
        y = TOPO + (H - TOPO - TAM[novo]) / 2.0;
        if (novo == BELZEBU) {
            rio = RIO_DUR; respingos.clear(); cuspidores.clear(); proxRespingo = 2; proxOnda = 1.5; ondas = 0; proxBatida = 0; // antes do Belzebu: o rio de lava
        }
    }
    boolean estaVivo() { return ato != CORRENTES || vida > 0; }
    int getVida() { return vida; }
    int getVidaMaxima() { return ato == BARDO ? vidaBardo : ato == MONGE ? vidaMonge : ato == BELZEBU ? vidaBelzebu : ato == ZAREF ? vidaZaref : vidaCorrentes; }
    String getNome() { if (rio > 0) return rio < RIO_DUR * .3 ? "O BELZEBU SE APROXIMA..." : "RIO DE LAVA"; return emDupla() ? (ato == BARDO ? "IRMAOS OSSADA - KUNG FU E KARATE" : "IRMAOS OSSADA - DANCA MACABRA") : ato == BARDO ? "OSSADA VERDE - IRMAOS OSSADA" : ato == MONGE ? "OSSADA ROXO - IRMAOS OSSADA" : ato == BELZEBU ? "BELZEBU" : ato == ZAREF ? "ZAREF, O TEMPLARIO DO INFERNO" : "O DEMONIO DAS CORRENTES"; }
    Rectangle getBounds() {
        int t = TAM[ato];
        if (ato == BELZEBU || ato == CORRENTES) return new Rectangle((int) (x + t * .22), (int) (y + t * .12), (int) (t * .56), (int) (t * .78));
        if (ato == MONGE && lobisomens()) return lobo(vivo(0) ? 0 : 1);
        if (emDupla()) return caixaIrmao(vivo(0) ? 0 : 1);
        if ((ato == BARDO || ato == MONGE) && gemeos()) return new Rectangle((int) (x + t * .05 - (ato == MONGE && moon > 0 ? 0 : 0)), (int) (y + t * .12), (int) (t * 1.0), (int) (t * .86));
        if ((ato == BARDO ? BARDO_VIOLAO : ato == MONGE ? MONGE_VIOLAO : null) != null) return new Rectangle((int) (x + t * (PE_DUPLA - .22)), (int) (y + t * .1), (int) (t * .44), (int) (t * .82));
        return new Rectangle((int) (x + t * .42), (int) (y + t * .1), (int) (t * .42), (int) (t * .82));
    }
    boolean tocaNave(Rectangle nave) {
        if (inferno() && nave.y + nave.height > TiroChefe.LAVA_Y + 14) return true; // encostou na lava
        if (belzMorte > 0 || apresB > 0) return false;
        if (rio > 0) { for (Respingo r : respingos) if (r.t > RESP_AVISO && nave.intersects(r.caixa())) return true; return false; }
        if (transicao > 0 || entrada < 1) return false;
        if (ato == ZAREF && noRaio(nave.getCenterX(), nave.getCenterY())) return true;
        if (ato == CORRENTES && noChicote(nave) && !acertouChicote) { acertouChicote = true; return true; }
        if (ato == MONGE && lobisomens()) return vivo(0) && nave.intersects(lobo(0)) || vivo(1) && nave.intersects(lobo(1));
        if (emDupla()) return vivo(0) && nave.intersects(caixaIrmao(0)) || vivo(1) && nave.intersects(caixaIrmao(1));
        return nave.intersects(getBounds());
    }
    int getX() { return getBounds().x; }
    int getY() { return getBounds().y; }

    // ---------------- desenho ----------------
    void desenhar(Graphics2D tela) {
        Graphics2D g = (Graphics2D) tela.create();
        tempoGemeos = tempo; moonGemeos = moon; dancaGemeos = danca; golpeGemeos = golpes; poseDesV = gestoV > 0 ? poseV : -1; poseDesR = gestoR > 0 ? poseR : -1; tapasDes = rajadaR > 0; chutesDes = rajadaV > 0; saltoDes = emDupla() && entrada < 1 && ato == BARDO; entradaDes = entrada; poseDes = emDupla() && entrada >= 1 && pose < POSE_DUR ? pose : -1; for (int i = 0; i < 2; i++) { offDes[i] = (int) offX[i]; offDesY[i] = (int) offY[i]; estDes[i] = emDupla() ? est[i] : LIVRE; guardaQ[i] = estT[i] > 1.15 ? 0 : bloqueio[i] > 0 ? 2 : estT[i] < .25 ? 3 : 1; }
        if (quedaV > 0) quedaV += 1 / 60.0; if (quedaR > 0) quedaR += 1 / 60.0; alfaV = emDupla() && vidaV <= 0 ? 0f : 1f; alfaR = emDupla() && vidaR <= 0 ? 0f : 1f; // quem caiu vira a animacao de queda
        if (ato == MONGE && LUA != null && transicao <= 0) { float a0 = (float) Math.min(1, luaT / 2); float uiva = lobos > 0 && lobos < .9 ? (float) (1 - lobos / .9) : 0; g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, a0 * .6f + uiva * .4f))); int lw = (int) (230 * (1 + uiva * .15)); g.drawImage(LUA, W / 2 - 60 - lw / 2, TOPO + 8, lw, lw * LUA.getHeight() / LUA.getWidth(), null); g.setComposite(AlphaComposite.SrcOver); } // lua cheia da danca macabra
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (transicao > 0) desenharTransicao(g);
        else {
            if (ato == CORRENTES) desenharAvisos(g);
            int q = quadro();
            if (avisandoInvestida()) { int t = TAM[ato]; g.setColor(new Color(255, 40, 20, (int) (90 + 80 * Math.sin(tempo * 30)))); g.setStroke(new BasicStroke(3)); g.drawLine((int) (x + t * .4), (int) (y + t * .5), (int) alvoInvX + 60, (int) alvoInvY + t / 2); }
            if (ato == MONGE && loboT > 0) desenharLobos(g); else
            if (ato == MONGE && emDupla() && entrada < 1) { if (renascer != null) for (Queda r : renascer) { r.t = .3 + (1 - entrada) * 1.7; desenharQueda(g, r); } } // ossos subindo e se encaixando
            else if (ato == BELZEBU && BZ_BAT != null) desenharBelzebu(g);
            else corpo(g, ato, q, x, y + (q == 3 && ato != CORRENTES ? 4 : 0), TAM[ato], 1f);
            if (ato == ZAREF) {
                if (cargaRaio > 0) { int r = (int) (10 + (1 - cargaRaio) * 28 + Math.sin(tempo * 40) * 3); g.setColor(Color.getHSBColor((float) (tempo * 2 % 1), .8f, 1f)); g.fillOval((int) raioX() - r, (int) raioY() - r, r * 2, r * 2); }
                if (raio > 0) desenharRaio(g);
            }
            if (ato == CORRENTES) { if (gancho >= 2) desenharCorrente(g); if (chicote > 0) desenharChicote(g, anguloChicote, 1f); }
        }
        if (emDupla() && transicao <= 0 && !(ato == MONGE && loboT > 0)) desenharReacoes(g);
        if (emDupla() && transicao <= 0 && entrada >= 1 && pose < POSE_DUR) { int t = TAM[ato]; Graphics2D a = (Graphics2D) g.create(); double cx = (irmao(0)[0] + irmao(1)[0]) / 2, chao = y + t;
            if (pose < .6 && ENTRADA == null) { float al = (float) (1 - pose / .6); a.setComposite(AlphaComposite.SrcOver.derive(al * .7f)); a.setColor(new Color(190, 170, 140)); int rw = (int) (t * (1 + pose * 2.5)); a.fillOval((int) cx - rw, (int) chao - 18, rw * 2, 36); } // poeira do pouso
            double k = pose - POSE_KIAI; if (k >= 0 && k < .5) { float al = (float) (1 - k / .5); for (int i = 0; i < 2; i++) { double[] o = irmao(i); a.setComposite(AlphaComposite.SrcOver.derive(al * .8f)); a.setColor(i == 0 ? VERDE : ROXO); a.setStroke(new BasicStroke(5)); int r = (int) (40 + k * 500); a.drawOval((int) o[0] - r, (int) o[1] - r, r * 2, r * 2); } } // estouro do kiai na pose
            a.dispose(); }
        if (rio > 0 || apresB > 0) desenharRespingos(g);
        if (ato == BELZEBU && apresB > 0) cartela(g, "BELZEBU", "O ARAUTO DO INFERNO", APRES_B - apresB - 2.7, 2.5, new Color(255, 70, 30));
        for (Queda qd : quedas) desenharQueda(g, qd);
        if (flash > 0) {
            g.setColor(new Color(255, 220, 160, (int) (flash / .16 * 200))); Rectangle b = getBounds();
            for (int i = 0; i < 6; i++) { double an = i * Math.PI / 3; int r = 15 + (int) ((.16 - flash) * 180); g.fillRect((int) b.getCenterX() + (int) (Math.cos(an) * r), (int) b.getCenterY() + (int) (Math.sin(an) * r), 4, 4); }
        }
        g.dispose();
    }
    private int quadro() {
        if (ato == ZAREF) return raio > 0 || cargaRaio > 0 ? (ZAREF_ESPADA != null ? 3 : 2) : investida > 0 && investida <= .8 ? 2 : avisandoInvestida() ? 1 : gesto > .3 ? 2 : gesto > 0 ? 1 : 0;
        if (ato == CORRENTES) return demonio > 0 ? 3 : chicote > 0 || gancho >= 2 ? 2 : avisoChicote > 0 || gancho == 1 || avisoDemonio > 0 ? 1 : 0;
        if (ato == BELZEBU && bateria > 0) return BELZEBU_ARTE != null ? 3 : 1;
        if (avisandoInvestida()) return 1;
        if (gesto > .25) return 2;
        if (gesto > 0) return 1;
        return (int) (tempo * 1.6) % 4 == 3 ? 3 : 0;
    }
    /** Bardo e Monge tocando la no fundo enquanto o Belzebu luta (nao levam tiro). */
    private void dupla(Graphics2D g, float a) {
        int s = 150; int q = (int) (tempo * 2.4) % 2 == 0 ? 0 : 3;
        if (gemeos()) { corpo(g, BARDO, 1, W - s - 40, H - s - 4 + Math.sin(tempo * 2) * 6, s, a * .85f); return; } // os gemeos tocando violao no canto
        corpo(g, BARDO, q, W - s - 10, TOPO + 4 + Math.sin(tempo * 2) * 6, s, a * .85f);
        corpo(g, MONGE, q == 0 ? 3 : 0, W - s - 10, H - s - 4 + Math.sin(tempo * 2 + 1) * 6, s, a * .85f);
    }
    private void desenharTransicao(Graphics2D g) {
        // o ato que caiu some piscando
        double p = 1 - transicao / (ato == MONGE ? 2.2 : ato == BELZEBU ? INVOCACAO : ato == CORRENTES ? 3.2 : 3.0);
        int ant = ato - 1;
        if (ato == CORRENTES) {
            // o Zaref ergue os chifres, some na fumaca roxa e o demonio sai dela girando a corrente
            if (p < .6) corpo(g, ZAREF, 3, W - TAM[ZAREF] - 40, TOPO + (H - TOPO - TAM[ZAREF]) / 2.0, TAM[ZAREF], (float) Math.max(0, 1 - p / .6));
            java.util.Random r = new java.util.Random(5);
            int forca = (int) (220 * Math.sin(Math.min(1, p) * Math.PI));
            for (int k = 0; k < 90; k++) { double a = r.nextDouble() * Math.PI * 2 + p * (3 + r.nextDouble() * 3), d = 30 + r.nextInt(170) * (.4 + p); int q = 4 + r.nextInt(3) * 2;
                g.setColor(k % 4 == 0 ? new Color(255, 80, 60, forca) : new Color(110 + r.nextInt(70), 30, 150 + r.nextInt(80), forca)); g.fillRect(W - 210 + (int) (Math.cos(a) * d), H / 2 + (int) (Math.sin(a) * d * .8), q, q); }
            if (p > .45) { float a = (float) Math.min(1, (p - .45) / .4); int t = (int) (TAM[CORRENTES] * (.5 + .5 * a)); corpo(g, CORRENTES, 0, W - t - 30, TOPO + (H - TOPO - t) / 2.0, t, a); }
            return;
        }
        if (ato == BELZEBU) { invocacao(g, transicao > 0 ? INVOCACAO - transicao : INVOCACAO); return; }
        if (p < .5 && !((ant == BARDO || ant == MONGE) && gemeos())) {
            float a = (float) Math.max(0, 1 - p * 2);
            if ((int) (tempo * 20) % 2 == 0) corpo(g, ant, 3, W - TAM[ant] - 40, TOPO + (H - TOPO - TAM[ant]) / 2.0, TAM[ant], a);
        }
        if (ato == ZAREF) {
            // o trono desperta: aneis de fogo e o Templario surge
            if (p > .4) corpo(g, ZAREF, 0, W - TAM[ZAREF] - 40, TOPO + (H - TOPO - TAM[ZAREF]) / 2.0, TAM[ZAREF], (float) Math.min(1, (p - .4) / .4));
            // aneis de fogo abrindo
            int cx = W - TAM[BELZEBU] / 2 - 20, cy = TOPO + (H - TOPO) / 2;
            g.setStroke(new BasicStroke(5));
            for (int k = 0; k < 6; k++) {
                double r = (p * 1.4 - k * .08) * 520; if (r <= 0) continue;
                float al = (float) Math.max(0, Math.min(.8, 1 - r / 520));
                Color c = k % 2 == 0 ? new Color(255, 120 + k * 15, 30) : new Color(200, 20, 10);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * al)));
                g.drawOval(cx - (int) r, cy - (int) r, (int) r * 2, (int) r * 2);
            }
        }
    }
    /** Fim dos Irmaos Ossada, em segundos desde o ultimo cair: as duas almas saem dos ossos, se fundem num buraco negro
     *  supermassivo que engole tudo (a tela treme e a nave e puxada), e a nave fica sozinha no vazio, em dobra, ate o inferno. */
    private void invocacao(Graphics2D g, double t) {
        int cx = W / 2 + 60, cy = TOPO + (H - TOPO) / 2;
        if (t < BURACO_FIM) {
            double cresce = t < FUSAO_FIM ? 0 : Math.min(1, (t - FUSAO_FIM) / (BURACO_FIM - FUSAO_FIM - .5)), raio = 10 + cresce * cresce * 620;
            // disco de acrecao e o miolo negro
            if (t > FUSAO_FIM && BURACO != null) quadroArte(g, BURACO, 6, (int) (tempo * 9) % 6, cx, cy, raio * 6.8, (float) Math.min(1, (t - FUSAO_FIM) / .3)); // o buraco negro girando
            else if (t > FUSAO_FIM) { Graphics2D a = (Graphics2D) g.create(); int rr = (int) raio;
                for (int k = 5; k >= 0; k--) { int rx = (int) (rr * (1.9 + k * .18)), ry = (int) (rr * (.55 + k * .05)); float al = .18f + .1f * (5 - k);
                    a.setColor(k % 2 == 0 ? new Color(255, 120 + k * 15, 40, (int) (255 * al)) : new Color(170, 70, 255, (int) (255 * al))); a.setStroke(new BasicStroke(6 + k * 2));
                    a.rotate(t * .9 + k, cx, cy); a.drawOval(cx - rx, cy - ry, rx * 2, ry * 2); a.rotate(-(t * .9 + k), cx, cy); }
                a.setColor(new Color(255, 230, 200, 200)); a.setStroke(new BasicStroke(3)); a.drawOval(cx - rr - 4, cy - rr - 4, rr * 2 + 8, rr * 2 + 8); // anel de fotons
                a.setColor(Color.BLACK); a.fillOval(cx - rr, cy - rr, rr * 2, rr * 2); a.dispose(); }
            // as duas almas: sobem dos ossos (0-1.2 s), giram uma em volta da outra e se fundem no centro (1.2-2.4 s)
            if (t < 2.6) for (int i = 0; i < 2; i++) { double sobe = Math.min(1, t / 1.2), junta = t < 1.2 ? 0 : Math.min(1, (t - 1.2) / 1.2), jj = junta * junta;
                double ox = almaX[i], oy = almaY[i] - sobe * 120, ang = t * (4 + junta * 14) + i * Math.PI, rg = 70 * (1 - jj);
                double ax = ox + (cx - ox) * jj + Math.cos(ang) * rg * junta, ay = oy + (cy - oy) * jj + Math.sin(ang) * rg * junta;
                if (ALMA_ARTE != null) { if (t < 2.0) quadroArte(g, ALMA_ARTE, 8, i * 4 + (int) (tempo * 10) % 4, ax, ay, 64, (float) Math.min(1, t * 3) * (float) Math.min(1, (2.0 - t) / .25)); continue; }
                Color c = i == 0 ? VERDE : ROXO; Graphics2D a = (Graphics2D) g.create(); a.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, t * 3) * (t > 2.4 ? (float) ((2.6 - t) / .2) : 1f)));
                for (int k = 0; k < 7; k++) { int r2 = 24 - k * 3; a.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200 - k * 20)); a.fillOval((int) ax - r2 + (int) (Math.sin(t * 9 + k) * 4), (int) ay - r2 + k * 4, r2 * 2, r2 * 2); }
                a.setColor(Color.WHITE); a.fillOval((int) ax - 6, (int) ay - 6, 12, 12); a.dispose(); }
            if (FUSAO != null && t > 1.75 && t < FUSAO_FIM + .2) { double e = (t - 1.75) / (FUSAO_FIM - 1.75); quadroArte(g, FUSAO, 6, (int) Math.min(5, e * 6), cx, cy, 300, (float) Math.min(1, (t - 1.75) / .2) * (float) Math.min(1, (FUSAO_FIM + .2 - t) / .2)); }
            if (t > 2.3 && t < 2.8 && FUSAO == null) { float al = (float) (1 - Math.abs(t - 2.45) / .35); g.setColor(new Color(255, 255, 255, (int) (230 * Math.max(0, al)))); g.fillRect(0, TOPO, W, H - TOPO); } // clarao da fusao
            if (t > BURACO_FIM - 1.4) { g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, (t - BURACO_FIM + 1.4) / .7)))); g.fillRect(0, 0, W, H); } // tudo engolido
            return;
        }
        // dobra: o tunel do inferno (a arte de chegada) girando; a nave entra em espiral, encolhendo, e some no centro
        double d = t - BURACO_FIM; g.setColor(Color.BLACK); g.fillRect(0, 0, W, H);
        if (CHEGADA != null) { double z = d < MERGULHO ? 1.15 + d * .08 : 1.15 + MERGULHO * .08 + Math.pow((d - MERGULHO) / (INVOCACAO - BURACO_FIM - MERGULHO), 2) * 2.2, giro = d * .12;
            Graphics2D c = (Graphics2D) g.create(); c.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, d / .5)));
            c.translate(TUNEL_X, TUNEL_Y); c.rotate(giro); c.scale(z, z); c.translate(-W * .52, -H * .42); c.drawImage(CHEGADA, 0, 0, W, H, null); c.dispose(); }
        // rastro de cometa saindo da nave, para fora da espiral
        double[] ns = naveSugada(); if (ns != null && ns[0] > .05) { Graphics2D a = (Graphics2D) g.create(); double vx = navX - TUNEL_X, vy = navY - TUNEL_Y, dist = Math.max(1, Math.hypot(vx, vy)), ux = vx / dist, uy = vy / dist, len = 150 * ns[0], larg = 16 * ns[0];
            java.awt.geom.Path2D cauda = new java.awt.geom.Path2D.Double(); cauda.moveTo(navX - uy * larg, navY + ux * larg); cauda.lineTo(navX + ux * len, navY + uy * len); cauda.lineTo(navX + uy * larg, navY - ux * larg); cauda.closePath();
            a.setPaint(new java.awt.GradientPaint((float) navX, (float) navY, new Color(255, 190, 110, 210), (float) (navX + ux * len), (float) (navY + uy * len), new Color(170, 60, 255, 0))); a.fill(cauda);
            a.setPaint(new java.awt.RadialGradientPaint((float) navX, (float) navY, (float) Math.max(2, 40 * ns[0]), new float[]{0f, 1f}, new Color[]{new Color(255, 220, 160, 170), new Color(255, 120, 40, 0)})); a.fillOval((int) (navX - 40 * ns[0]), (int) (navY - 40 * ns[0]), (int) (80 * ns[0]) + 1, (int) (80 * ns[0]) + 1);
            a.dispose(); }
        if (d > MERGULHO - .45 && d < MERGULHO + .45) { // quase no fim a nave cintila como uma estrela e some
            double k = 1 - Math.abs(d - MERGULHO) / .45, braco = 20 + k * k * 150, giro = d * 1.5; float al = (float) Math.min(1, k * 1.6);
            double sx = d < MERGULHO ? navX : TUNEL_X, sy = d < MERGULHO ? navY : TUNEL_Y; Graphics2D c = (Graphics2D) g.create(); c.translate(sx, sy);
            c.setPaint(new java.awt.RadialGradientPaint(0f, 0f, (float) (braco * .45), new float[]{0f, 1f}, new Color[]{new Color(255, 245, 210, (int) (200 * al)), new Color(255, 150, 60, 0)})); c.fillOval((int) (-braco * .45), (int) (-braco * .45), (int) (braco * .9), (int) (braco * .9));
            for (int b = 0; b < 2; b++) { Graphics2D e = (Graphics2D) c.create(); e.rotate(giro + b * Math.PI / 4); double L = b == 0 ? braco : braco * .55, larg = b == 0 ? 7 : 4;
                for (int q = 0; q < 4; q++) { java.awt.geom.Path2D p2 = new java.awt.geom.Path2D.Double(); p2.moveTo(0, -larg); p2.lineTo(L, 0); p2.lineTo(0, larg); p2.closePath(); e.setColor(new Color(255, 255, 235, (int) (235 * al))); e.fill(p2); e.rotate(Math.PI / 2); }
                e.dispose(); }
            c.dispose(); }
    }
    private static final int TUNEL_X = 500, TUNEL_Y = 255; private static final double MERGULHO = 3.3;
    private double navX, navY;
    /** A nave sendo sugada: {escala, angulo}; null quando a nave e desenhada normal. */
    double[] naveSugada() {
        if (ato != BELZEBU || transicao <= 0) return null; double t = INVOCACAO - transicao;
        if (t < FUSAO_FIM + .6) return null;
        if (t < BURACO_FIM) { double e = (t - FUSAO_FIM - .6) / (BURACO_FIM - FUSAO_FIM - .6); return new double[]{Math.max(0, Math.min(1, (BURACO_FIM - 1.4 - t) / .8)), e * e * 9}; } // perde o controle, gira, e o buraco engole
        double d = t - BURACO_FIM, e = Math.min(1, d / MERGULHO); return new double[]{d > MERGULHO ? 0 : Math.max(.08, 1 - e * .92), 6 + d * (4 + e * 10)}; // em espiral para o centro do tunel
    }
    /** Na dobra a nave aparece sozinha por cima do vazio. */
    /** Tremor da tela durante o buraco negro (0 = parado). */
    double tremor() { if (apresB > 0) { double e = APRES_B - apresB; return e < 1.3 ? 5 : e > 2.7 && e < 3.2 ? 8 : 0; } if (rio > 0) return batidaVisual * (1 + (1 - rio / RIO_DUR) * 5); if (ato != BELZEBU || transicao <= 0) return 0; double t = INVOCACAO - transicao; return t > FUSAO_FIM && t < BURACO_FIM ? 3 + (t - FUSAO_FIM) * 3.5 : 0; }
    private static void brilho(Graphics2D g, double cx, double cy, double r) {
        for (int k = 3; k >= 1; k--) { int rr = (int) (r * k / 3); g.setColor(new Color(90, 255, 120, 40 + (3 - k) * 30)); g.fillOval((int) cx - rr, (int) cy - rr, rr * 2, rr * 2); }
    }
    /** Portal do inferno girando; sem a arte, aneis de fagulhas vermelhas e laranjas puxadas para o centro. */
    private void portal(Graphics2D tela, int cx, int cy, int d) {
        if (d <= 2) return;
        Graphics2D g = (Graphics2D) tela.create();
        if (PORTAL != null) { int fw = PORTAL.getWidth() / 4, fh = PORTAL.getHeight(), q = (int) (tempo * 10) % 4, w = d * fw / fh;
            g.drawImage(PORTAL, cx - w / 2, cy - d / 2, cx + w / 2, cy + d / 2, q * fw, 0, q * fw + fw, fh, null); g.dispose(); return; }
        g.setColor(new Color(20, 0, 10, 230)); g.fillOval(cx - d / 2 + d / 8, cy - d / 2 + d / 8, d * 3 / 4, d * 3 / 4);
        java.util.Random r = new java.util.Random(9);
        for (int k = 0; k < 220; k++) {
            double raio = d / 2.0 * (.35 + .65 * r.nextDouble()), a = r.nextDouble() * Math.PI * 2 + tempo * (2.5 - raio / d * 2);
            int x = cx + (int) (Math.cos(a) * raio), y = cy + (int) (Math.sin(a) * raio), tq = 3 + r.nextInt(3) * 2;
            g.setColor(r.nextInt(3) == 0 ? new Color(255, 210, 80) : r.nextBoolean() ? new Color(255, 90, 30) : new Color(190, 20, 30)); g.fillRect(x, y, tq, tq);
        }
        g.dispose();
    }
    private void desenharRaio(Graphics2D tela) {
        Graphics2D g = (Graphics2D) tela.create();
        double bx = raioX(), by = raioY(), ux = Math.cos(direcaoRaio), uy = Math.sin(direcaoRaio);
        float fim = (float) Math.min(1, raio / .25);
        Color[] cores = {new Color(255, 60, 60), new Color(255, 160, 40), new Color(255, 240, 60), new Color(80, 230, 90), new Color(70, 150, 255), new Color(170, 90, 255)};
        for (int k = 0; k < 6; k++) {
            double off = (k - 2.5) * 6 + Math.sin(tempo * 30 + k) * 1.5;
            g.setColor(new Color(cores[k].getRed(), cores[k].getGreen(), cores[k].getBlue(), (int) (220 * fim)));
            g.setStroke(new BasicStroke(7));
            g.drawLine((int) (bx - uy * off), (int) (by + ux * off), (int) (bx + ux * ALCANCE_RAIO - uy * off), (int) (by + uy * ALCANCE_RAIO + ux * off));
        }
        g.setColor(new Color(255, 255, 255, (int) (200 * fim))); g.setStroke(new BasicStroke(4));
        g.drawLine((int) bx, (int) by, (int) (bx + ux * ALCANCE_RAIO), (int) (by + uy * ALCANCE_RAIO));
        g.dispose();
    }
    private void desenharCorrente(Graphics2D tela) {
        Graphics2D g = (Graphics2D) tela.create();
        double ax = maoX(), ay = maoY(), d = Math.hypot(hx - ax, hy - ay), a = Math.atan2(hy - ay, hx - ax);
        if (ELO != null) {
            int ew = ELO.getWidth() * 3 / 4, eh = ELO.getHeight() * 3 / 4;
            Graphics2D c = (Graphics2D) g.create(); c.translate(ax, ay); c.rotate(a);
            for (double t = 0; t < d - 20; t += ew * .8) c.drawImage(ELO, (int) t, -eh / 2, ew, eh, null);
            c.dispose();
        } else { g.setColor(new Color(150, 150, 160)); g.setStroke(new BasicStroke(4)); g.drawLine((int) ax, (int) ay, (int) hx, (int) hy); }
        if (faiscaCorrente > 0) { g.setColor(new Color(255, 220, 120)); for (int k = 0; k < 6; k++) g.fillRect((int) (ax + (hx - ax) * (.3 + k * .1)) - 3, (int) (ay + (hy - ay) * (.3 + k * .1)) - 3, 4, 4); }
        BufferedImage gc = gancho == 3 ? GANCHO_FECHADO : GANCHO_ABERTO;
        if (gc != null) { Graphics2D h = (Graphics2D) g.create(); h.translate(hx, hy); h.rotate(a + Math.PI); int w = (int) (gc.getWidth() * .6), hh = (int) (gc.getHeight() * .6); h.drawImage(gc, -w / 3, -hh / 2, w, hh, null); h.dispose(); }
        g.dispose();
    }
    /** Avisos do demonio do Demonio das Correntes: faixa do mergulho e o arco que a corrente vai varrer. */
    private void desenharAvisos(Graphics2D g) {
        if (avisoDemonio > 0) {
            g.setColor(new Color(170, 40, 255, (int) (60 + 60 * Math.sin(tempo * 25))));
            g.fillRect(0, (int) (demonioY + TAM[CORRENTES] * .25), W, (int) (TAM[CORRENTES] * .5));
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18)); g.setColor(new Color(230, 180, 255));
            g.drawString("<<< MERGULHO", W - 200, (int) (demonioY + TAM[CORRENTES] * .5));
        }
        if (avisoChicote > 0) {
            Graphics2D a = (Graphics2D) g.create(); a.setColor(new Color(255, 60, 60, (int) (70 + 60 * Math.sin(tempo * 30))));
            int r = (int) ALCANCE_CHICOTE, gi = (int) Math.toDegrees(-inicioChicote), gf = (int) Math.toDegrees(-fimChicote);
            a.fillArc((int) maoX() - r, (int) maoY() - r, r * 2, r * 2, Math.min(gi, gf), Math.abs(gf - gi)); a.dispose();
            desenharChicote(g, inicioChicote, .5f);
        }
    }
    /** A corrente esticada a partir da mao, na direcao do angulo. */
    private void desenharChicote(Graphics2D tela, double ang, float alfa) {
        Graphics2D g = (Graphics2D) tela.create(); g.setComposite(AlphaComposite.SrcOver.derive(alfa));
        double ax = maoX(), ay = maoY();
        if (ELO != null) {
            int ew = ELO.getWidth() * 3 / 4, eh = ELO.getHeight() * 3 / 4;
            g.translate(ax, ay); g.rotate(ang);
            for (double t = 0; t < ALCANCE_CHICOTE; t += ew * .8) g.drawImage(ELO, (int) t, -eh / 2 + (int) (Math.sin(t * .02 + tempo * 20) * 4), ew, eh, null);
        } else { g.setColor(new Color(150, 150, 160)); g.setStroke(new BasicStroke(5)); g.drawLine((int) ax, (int) ay, (int) (ax + Math.cos(ang) * ALCANCE_CHICOTE), (int) (ay + Math.sin(ang) * ALCANCE_CHICOTE)); }
        g.dispose();
    }
    /** Corpo de cada ato: arte propria do Zaref de espada e do demonio do Demonio das Correntes quando existir; senao a grade. */
    private static void corpo(Graphics2D g, int ato, int q, double x, double y, int t, float alfa) {
        BufferedImage arte = ato == ZAREF ? ZAREF_ESPADA : ato == CORRENTES ? DEMONIO_CORRENTES : ato == BELZEBU ? BELZEBU_ARTE : ato == BARDO ? BARDO_VIOLAO : ato == MONGE ? MONGE_VIOLAO : null;
        if (arte == null && ato == CORRENTES && ARCO != null) arte = escuro();
        if ((ato == BARDO || ato == MONGE) && gemeos()) { gemeosDesenho(g, ato, q, x, y, t, alfa); return; }
        if (arte == null) { sprite(g, ato - 1, q, x, y, t, alfa); return; }
        int fw = arte.getWidth() / (arte == demonioEscuro ? 2 : 4), fh = arte.getHeight(), qq = arte == demonioEscuro ? (q == 2 || q == 3 ? 1 : 0) : q;
        int w = t * fw / fh;
        // Bardo e Monge: o golpe ocupa a esquerda do quadro; os pes ficam no mesmo ponto da caixa do ato
        int x0 = ato == BARDO || ato == MONGE ? (int) (x + t * PE_DUPLA - pe(arte, fw, fh) * w) : (int) x + (t - w) / 2;
        Graphics2D a = (Graphics2D) g.create();
        if (alfa < 1) a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alfa))));
        a.drawImage(arte, x0, (int) y, x0 + w, (int) y + t, qq * fw, 0, qq * fw + fw, fh, null);
        a.dispose();
    }
    private static float alfaV = 1, alfaR = 1; private static int poseDesV = -1, poseDesR = -1; private static boolean tapasDes, chutesDes; private static boolean saltoDes; private static double poseDes = -1, entradaDes; private static final int[] offDes = new int[2], offDesY = new int[2], estDes = new int[2], guardaQ = new int[2];
    private static double tempoGemeos; private static double moonGemeos, dancaGemeos;
    /** Desenha os dois gemeos (a celula tem os dois lado a lado), com os pes na mesma linha da caixa do ato. */
    private static void gemeosDesenho(Graphics2D g, int ato, int q, double x, double y, int t, float alfa) {
        BufferedImage folha = ato == BARDO ? GEMEOS_KF : GEMEOS_DANCA; int n = ato == BARDO ? 4 : 8;
        int fw = folha.getWidth() / n, fh = folha.getHeight(), h = (int) (t * 1.02), w = h * fw / fh, x0 = (int) (x + t - w + 30), y0 = (int) (y + t - h);
        if (saltoDes || poseDes >= 0) { // entrada: chegam saltando, pousam, giram os bracos, pose de kung fu (garca e tigre) e saudacao
            BufferedImage f = ENTRADA != null ? ENTRADA : GEMEOS_KF; int nf = ENTRADA != null ? 6 : 4, fw2 = f.getWidth() / nf, fh2 = f.getHeight(), he = (int) (h * (ENTRADA != null ? 1.3 : 1)), w2 = he * fw2 / fh2, xe = (int) (x0 + w * .475 - w2 / 2.0), qe; // centrado entre os dois gemeos
            if (ENTRADA != null) qe = saltoDes ? 0 : poseDes < .25 ? 1 : poseDes < .7 ? 2 : poseDes < 1.7 ? 3 : poseDes < 2.1 ? 4 : 5;
            else qe = saltoDes ? 1 : poseDes < .7 ? 2 : 0;
            int pulo = saltoDes ? (int) (-Math.sin(Math.min(1, entradaDes) * Math.PI) * 90) : 0;
            g.drawImage(f, xe, y0 + h - he + pulo, xe + w2, y0 + h + pulo, qe * fw2, 0, qe * fw2 + fw2, fh2, null); return; }
        Graphics2D g0 = g;
        for (int lado = 0; lado < 2; lado++) { float al = alfa * (lado == 0 ? alfaV : alfaR); if (al <= .01f) continue; // cada gemeo e a metade da celula, com o proprio quadro
            g = (Graphics2D) g0.create(); g.translate(offDes[lado], offDesY[lado]); int st = estDes[lado]; // cada um no seu lugar (a investida o leva ate a nave)
            if (st == GUARDA && GUARDA_ARTE != null) { BufferedImage ga = lado == 0 ? GUARDA_ARTE : GUARDA_ROXA; // guarda com a arte propria
                int tf = ga.getWidth() / 4, th = ga.getHeight(), hh = (int) (h * 1.05), ww = hh * tf / th, cx = x0 + (lado == 0 ? w / 4 : w * 3 / 4), qt = guardaQ[lado], tx = cx - ww * 60 / 100, ty = y0 + h - hh + hh / 12;
                Graphics2D a = (Graphics2D) g.create(); if (al < 1) a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, al)));
                a.drawImage(ga, tx, ty, tx + ww, ty + hh, qt * tf, 0, qt * tf + tf, th, null); a.dispose(); continue; }
            if (st != LIVRE) { int pose = st == GUARDA ? 0 : st == CURA ? (ato == BARDO ? 3 : 2) : (ato == BARDO ? 2 : (lado == 0 ? 3 : 6));
                Graphics2D a = (Graphics2D) g.create(); a.clipRect(lado == 0 ? x0 - 200 : x0 + w / 2, y0 - 50, w / 2 + 200, h + 100); if (al < 1) a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, al))));
                a.drawImage(folha, x0, y0, x0 + w, y0 + h, pose * fw, 0, pose * fw + fw, fh, null); a.dispose(); continue; }
            if (lado == 0 && chutesDes && CHUTES != null && ato == BARDO) { // rajada de chutes com a arte propria
                int tf = CHUTES.getWidth() / 4, th = CHUTES.getHeight(), hh = (int) (h * 1.05), ww = hh * tf / th, cx = x0 + w / 4, qt = (int) (tempoGemeos * 16) % 4, tx = cx - ww * 62 / 100, ty = y0 + h - hh + hh / 12;
                Graphics2D a = (Graphics2D) g.create(); if (al < 1) a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, al)));
                a.drawImage(CHUTES, tx, ty, tx + ww, ty + hh, qt * tf, 0, qt * tf + tf, th, null); a.dispose(); continue; }
            if (lado == 1 && tapasDes && TAPAS != null && ato == BARDO) { // rajada de tapas com a arte propria
                int tf = TAPAS.getWidth() / 4, th = TAPAS.getHeight(), hh = (int) (h * 1.05), ww = hh * tf / th, cx = x0 + w * 3 / 4, qt = (int) (tempoGemeos * 16) % 4, tx = cx - ww * 55 / 100, ty = y0 + h - hh + hh / 12;
                Graphics2D a = (Graphics2D) g.create(); if (al < 1) a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, al)));
                a.drawImage(TAPAS, tx, ty, tx + ww, ty + hh, qt * tf, 0, qt * tf + tf, th, null); a.dispose(); continue; }
            int pose = lado == 0 ? poseDesV : poseDesR, qq;
            if (ato == BARDO) qq = q == 1 && pose < 0 ? 3 : pose >= 0 ? pose : 0; // parado em guarda; o golpe de cada um so nele
            else if (moonGemeos > 0) qq = 4 + (int) (tempoGemeos * 6) % 2;
            else if (pose >= 0) qq = pose;
            else { int[] passos = lado == 0 ? new int[]{0, 1, 2, 7} : new int[]{2, 7, 0, 1}; qq = passos[(int) (dancaGemeos * 2.5) % 4]; } // cada um no seu passo
            Graphics2D a = (Graphics2D) g.create(); a.clipRect(lado == 0 ? x0 - 200 : x0 + w / 2, y0 - 50, w / 2 + 200, h + 100);
            if (al < 1) a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, al)) * ((int) (tempoGemeos * 20) % 2 == 0 || al >= 1 ? 1 : .4f)));
            a.drawImage(folha, x0, y0, x0 + w, y0 + h, qq * fw, 0, qq * fw + fw, fh, null); a.dispose(); }
    }
    private static int golpeGemeos;
    private static BufferedImage demonioEscuro;
    /** Quadros 0 e 1 do Belzebu tingidos de roxo: vira o demonio fantasma do Demonio das Correntes. */
    private static BufferedImage escuro() {
        if (demonioEscuro != null) return demonioEscuro;
        int c = ARCO.getWidth() / 4;
        BufferedImage b = new BufferedImage(c * 2, c, BufferedImage.TYPE_INT_ARGB);
        for (int yy = 0; yy < c; yy++) for (int xx = 0; xx < c * 2; xx++) {
            int p = ARCO.getRGB(xx, 2 * c + yy), al = p >>> 24; if (al == 0) continue;
            int v = ((p >> 16 & 255) + (p >> 8 & 255) + (p & 255)) / 3;
            b.setRGB(xx, yy, al << 24 | Math.min(255, v / 2 + 70) << 16 | (v / 4 + 10) << 8 | Math.min(255, v + 90));
        }
        return demonioEscuro = b;
    }
    /** Alma de um gemeo (0 verde, 1 roxa) acesa no escudo da barra: um fogo-fatuo pequeno. */
    private static void alma(Graphics2D g, int q, double x, double y, int t, float alfa) {
        Graphics2D a = (Graphics2D) g.create(); a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alfa))));
        Color c = q == 0 ? VERDE : ROXO; double tt = System.nanoTime() / 1e9; int cx = (int) x + t / 3, cy = (int) y + t / 2;
        for (int k = 0; k < 5; k++) { int r = t / 3 - k * t / 18; if (r <= 0) break; a.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 230 - k * 35)); a.fillOval(cx - r + (int) (Math.sin(tt * 8 + k + q) * 2), cy - r - k * t / 9, r * 2, r * 2); }
        a.setColor(new Color(255, 255, 255, 220)); a.fillOval(cx - t / 10, cy - t / 10, t / 5, t / 5); a.dispose();
    }
    /** Onde ficam os pes do Bardo e do Monge dentro da caixa do ato (com a arte de violao). */
    private static final double PE_DUPLA = .35;
    private static final java.util.Map<BufferedImage, Double> PES = new java.util.HashMap<>();
    /** Onde ficam os pes no primeiro quadro (fracao da largura), medido uma vez. */
    private static double pe(BufferedImage arte, int fw, int fh) {
        Double v = PES.get(arte); if (v != null) return v;
        long soma = 0, n = 0;
        for (int yy = fh * 9 / 10; yy < fh; yy++) for (int xx = 0; xx < fw; xx++) if ((arte.getRGB(xx, yy) >>> 24) > 0) { soma += xx; n++; }
        v = n == 0 ? .5 : soma / (double) n / fw; PES.put(arte, v); return v;
    }
    private static void sprite(Graphics2D g, int linha, int q, double x, double y, int t, float alfa) {
        if (ARCO == null) { g.setColor(new Color(150, 30, 30)); g.fillRect((int) x + t / 3, (int) y + t / 5, t / 2, t * 3 / 4); return; }
        int c = ARCO.getWidth() / 4;
        Graphics2D a = (Graphics2D) g.create();
        if (alfa < 1) a.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alfa))));
        a.drawImage(ARCO, (int) x, (int) y, (int) x + t, (int) y + t, q * c, linha * c, q * c + c, linha * c + c, null);
        a.dispose();
    }

    void desenharVida(Graphics2D g) {
        String[] atos = {"", "  [ATO I]", "  [ATO II]", "  [ATO III]", "  [ATO IV]", "  [ATO V]"};
        Color c = ato == BARDO ? new Color(255, 120, 40) : ato == MONGE ? new Color(80, 220, 90) : ato == BELZEBU ? new Color(220, 40, 30) : ato == ZAREF ? new Color(170, 90, 255) : new Color(120, 40, 200);
        double frac = Math.max(0, vida) / (double) Math.max(1, getVidaMaxima());
        if (BARRA != null) {
            boolean dupla = emDupla();
            // moldura da arte: a vida enche a faixa preta e as almas dos gemeos acendem nos escudos
            int bx = 140, bw = 680, bh = bw * BARRA.getHeight() / BARRA.getWidth(), by = TOPO - 2;
            int fx = bx + (int) (bw * .118), fw = (int) (bw * (.773 - .118)), fy = by + (int) (bh * .364), fh = (int) (bh * (.616 - .364));
            g.drawImage(BARRA, bx, by, bw, bh, null);
            // o miolo da moldura e preto opaco: a vida vai por cima dele
            if (dupla) { int meia = (fw - 8) / 2; // duas barras: verde e roxo
                for (int i = 0; i < 2; i++) { Color ci = i == 0 ? VERDE : ROXO; double fi = (i == 0 ? vidaV : vidaR) / (double) Math.max(1, maxIrmao); int bx0 = fx + 2 + i * (meia + 4);
                    g.setPaint(new GradientPaint(bx0, fy, ci.brighter(), bx0, fy + fh, ci.darker().darker())); g.fillRect(bx0, fy + 2, (int) (meia * fi), fh - 4);
                    g.setColor(new Color(255, 255, 255, 90)); g.fillRect(bx0, fy + 3, (int) (meia * fi), 2); }
                g.setColor(new Color(0, 0, 0, 220)); g.fillRect(fx + 2 + meia, fy + 2, 4, fh - 4);
            } else {
            g.setPaint(new GradientPaint(fx, fy, c.brighter(), fx, fy + fh, c.darker().darker()));
            g.fillRect(fx + 2, fy + 2, (int) ((fw - 4) * frac), fh - 4);
            g.setColor(new Color(255, 255, 255, 90)); g.fillRect(fx + 2, fy + 3, (int) ((fw - 4) * frac), 2); }
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); String nome = getNome() + atos[ato];
            int tx = fx + 10, ty = fy + fh / 2 + 5;
            g.setColor(new Color(0, 0, 0, 200)); g.drawString(nome, tx + 1, ty + 1); g.setColor(Color.WHITE); g.drawString(nome, tx, ty);
            double[] escudo = {.791, .84};
            for (int k = 0; k < 2; k++) { int sx = bx + (int) (bw * escudo[k]), sy = by + (int) (bh * .36), sh = (int) (bh * .26);
                alma(g, k, sx + 2, sy, sh, k < dentes ? 1f : .18f); }
            return;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); g.setColor(Color.WHITE);
        g.drawString(getNome() + atos[ato], 217, TOPO + 24);
        g.setColor(new Color(8, 22, 34, 222)); g.fillRoundRect(218, TOPO + 31, 526, 22, 10, 10);
        g.setPaint(new GradientPaint(218, TOPO + 32, c, 744, TOPO + 52, new Color(222, 36, 70)));
        g.fillRoundRect(220, TOPO + 33, (int) (522.0 * frac), 18, 8, 8);
        g.setColor(new Color(255, 255, 255, 130)); g.drawRoundRect(218, TOPO + 31, 526, 22, 10, 10);
        for (int k = 0; k < 2; k++) alma(g, k, 760 + k * 15, TOPO + 30, 26, k < dentes ? 1f : .25f);
    }

    private static BufferedImage ler(String n) {
        java.net.URL u = ChefeFinal.class.getResource("/ImagemIcon/" + n);
        if (u == null) return null;
        try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; }
    }
}
