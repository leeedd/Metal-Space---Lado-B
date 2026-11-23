package Jogo;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;

/**
 * Radio de bordo retro-futurista, embutido no painel do cockpit da nave (cockpit_N.png, com o espaco passando
 * pelo para-brisa). A mao enluvada do piloto faz os ajustes: segue o mouse, aperta os botoes e gira os knobs.
 * Radio (desenhado no codigo em pixel art, blocos de 2 px): alca, alto-falante com
 * neon azul que pulsa com a musica, dial analogico com ponteiro que desliza ate a estacao, botoes de girar
 * VOLUME e SINTONIA, chave TRILHA | RADIO | PLAYLIST e botoes de estacao 1-6.
 * Teclado: setas para os lados escolhem o controle, para cima/baixo giram ou apertam; mouse: arrastar ou rodinha
 * nos botoes de girar e clique no resto. Abre por F7 ou Opcoes > Audio > Radio; ESC volta.
 */
final class TelaRadio {
    // paleta: caixa escura, frente champanhe, cromo, neon azul, ponteiro vermelho (poucos tons por cor)
    private static final Color CONTORNO = new Color(8, 6, 10), CAIXA_1 = new Color(34, 24, 22), CAIXA_2 = new Color(58, 40, 34),
            CAIXA_3 = new Color(86, 60, 48), FRENTE_1 = new Color(150, 128, 96), FRENTE_2 = new Color(190, 168, 128),
            FRENTE_3 = new Color(222, 204, 164), FRENTE_4 = new Color(244, 232, 200), GRADE_1 = new Color(26, 24, 30),
            GRADE_2 = new Color(54, 52, 60), GRADE_3 = new Color(88, 86, 96), CROMO_1 = new Color(70, 74, 84),
            CROMO_2 = new Color(130, 136, 150), CROMO_3 = new Color(190, 196, 208), CROMO_4 = new Color(240, 244, 250),
            DIAL_FUNDO = new Color(14, 18, 34), DIAL_LINHA = new Color(20, 26, 46), DIAL_TEXTO = new Color(150, 210, 255),
            DIAL_APAGADO = new Color(70, 96, 140), PONTEIRO = new Color(255, 52, 40), AMBAR = new Color(255, 176, 50),
            OURO = new Color(255, 206, 90), BRANCO = new Color(240, 242, 248), CINZA = new Color(160, 168, 186);
    private static final int E = 2;
    private static final long AVISO_MS = 6000;
    private static final int X = 106, Y = 150, W = 748, H = 428;   // embaixo: encaixado no painel do cockpit
    // controles (posicoes fixas; desenho e mouse usam as mesmas)
    private static int KR = 46, VOL_X = X + 92, KNOB_Y = Y + 352, SIN_X = X + W - 92;
    private static Rectangle DIAL = new Rectangle(X + 40, Y + 170, W - 80, 118), GRADE = new Rectangle(X + 40, Y + 38, W - 80, 118),
            CHAVE = new Rectangle(X + 214, Y + 302, 320, 28);

    // ---------------- arte do radio (GPT) ----------------
    private static final java.awt.image.BufferedImage CORPO = img("radio_corpo.png"), KNOB = img("radio_knob.png"),
            BOTAO = img("radio_botao.png"), TRILHO = img("radio_chave_trilho.png"), CURSOR = img("radio_chave_cursor.png"),
            PONTEIRO_ARTE = img("radio_ponteiro.png");
    /** Com a arte: o corpo ocupa a parte de baixo da tela (encaixado no painel); medidas tiradas do radio_corpo.png (1554x950). */
    private static final boolean ARTE = CORPO != null && KNOB != null && BOTAO != null && TRILHO != null && CURSOR != null && PONTEIRO_ARTE != null;
    private static final double S = .52;
    private static final int AX = 76, AY = 600 - (int) (950 * S), AW = (int) (1554 * S), AH = (int) (950 * S);
    private static final String[] FUNCAO_CURTA = {"MISTURA", "+ RÁDIO", "PASTA"};

    private static java.awt.image.BufferedImage img(String n) {
        java.net.URL u = TelaRadio.class.getResource("/ImagemIcon/" + n);
        if (u == null) return null;
        try { return javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { return null; }
    }
    private static int ax(double v) { return AX + (int) Math.round(v * S); }
    private static int ay(double v) { return AY + (int) Math.round(v * S); }
    private static final Rectangle[] PRESETS = new Rectangle[6], FUNCOES = new Rectangle[3];
    private static final String[] FUNCAO = {"MISTURAR", "+ RÁDIO", "PASTA"};
    private static final String[] POSICOES = {"TRILHA", "RÁDIO", "PLAYLIST"};
    static {
        if (ARTE) {   // posicoes das pecas dentro das janelas e encaixes da arte do corpo
            DIAL = new Rectangle(ax(165), ay(478), ax(1385) - ax(165), ay(598) - ay(478));
            GRADE = new Rectangle(ax(226), ay(148), ax(1323) - ax(226), ay(411) - ay(148));
            VOL_X = ax(319); SIN_X = ax(1234); KNOB_Y = ay(755); KR = 46;
            int px = ax(471) + 8, pw = ax(1077) - ax(471) - 16, py = ay(674) + 6;
            CHAVE = new Rectangle(px, py, pw, 28);
            int bw = (pw - 5 * 5) / 6;
            for (int i = 0; i < 6; i++) PRESETS[i] = new Rectangle(px + i * (bw + 5), py + 36, bw, 28);
            int fw = (pw - 2 * 6) / 3;
            for (int i = 0; i < 3; i++) FUNCOES[i] = new Rectangle(px + i * (fw + 6), py + 70, fw, 24);
        } else {
            for (int i = 0; i < 6; i++) PRESETS[i] = new Rectangle(X + 194 + i * 62, Y + 340, 54, 38);
            for (int i = 0; i < 3; i++) FUNCOES[i] = new Rectangle(X + 194 + i * 124, Y + 388, 116, 24);
        }
        try { Toolkit.getDefaultToolkit().addAWTEventListener(TelaRadio::mouse, java.awt.AWTEvent.MOUSE_EVENT_MASK
                | java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK | java.awt.AWTEvent.MOUSE_WHEEL_EVENT_MASK); }
        catch (Exception e) { /* sem mouse (teste sem tela) */ }
    }

    /** Ordem do foco no teclado: 0 VOLUME, 1 chave, 2..7 estacoes 1-6, 8..10 funcoes, 11 SINTONIA. */
    static final int F_VOLUME = 0, F_CHAVE = 1, F_PRESET = 2, F_FUNCAO = 8, F_SINTONIA = 11, FOCOS = 12;
    private static boolean aberta;
    private static int foco = F_SINTONIA;
    private static String recado = "";
    private static long recadoEm, ultimoQuadro;
    private static float vu;
    private static double ponteiroX = -1;           // posicao animada do ponteiro no dial
    private static double giroSintonia;             // angulo do botao de sintonia (gira a cada troca)
    private static double arrasteAngulo; private static int arrastando = -1; private static double arrasteAcumulado;

    // cockpit e espaco atras do radio
    private static final java.awt.image.BufferedImage[] COCKPIT = new java.awt.image.BufferedImage[5];
    private static int modeloCockpit = 1;
    private static final int[] EX = new int[90], EY = new int[90], EV = new int[90];
    private static double tempoEstrelas;
    // mao do piloto
    private enum Pose { APONTA, SEGURA }
    private static double maoX = 700, maoY = 760, alvoMaoX = 700, alvoMaoY = 760;
    private static Pose pose = Pose.APONTA;
    private static long apertouEm, girouEm, mouseEm, mexeuEm;
    static {
        for (int i = 0; i < 5; i++) {
            java.net.URL u = TelaRadio.class.getResource("/ImagemIcon/cockpit_" + i + ".png");
            if (u != null) try { COCKPIT[i] = javax.imageio.ImageIO.read(u); } catch (java.io.IOException e) { COCKPIT[i] = null; }
        }
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < EX.length; i++) { EX[i] = r.nextInt(960); EY[i] = r.nextInt(360); EV[i] = 20 + r.nextInt(3) * 40; }
    }

    private TelaRadio() { }

    /** Qual nave o jogador esta pilotando (o cockpit dela aparece atras do radio). */
    static void cockpit(int modelo) { modeloCockpit = Math.floorMod(modelo, 5); }

    static boolean aberta() { return aberta; }
    static void fechar() { aberta = false; arrastando = -1; }
    static void abrir() {
        aberta = true; foco = F_SINTONIA; ponteiroX = -1; mexeuEm = System.currentTimeMillis(); mouseEm = 0;
        maoY = 760; maoX = SIN_X;                 // a mao entra por baixo
        SomJogo.tocar(SomJogo.Efeito.MENU);
    }

    /** Gestos da mao: apertar (dedo afunda) e girar (dedos no knob). */
    private static void apertou() { apertouEm = mexeuEm = System.currentTimeMillis(); }
    private static void girou() { girouEm = mexeuEm = System.currentTimeMillis(); }

    // ======================================================================= comandos

    // ======================================================================= radio com a arte do GPT

    private static void desenharArte(Graphics2D g, long agora, double dt) {
        Graphics2D a = (Graphics2D) g.create();
        a.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        a.drawImage(CORPO, AX, AY, AW, AH, null);
        // neon do alto-falante pulsa com a musica
        Rectangle gr = GRADE;
        a.setColor(new Color(60, 140, 255, (int) (40 + 120 * vu)));
        a.fillRect(gr.x + 4, gr.y + 4, gr.width - 8, 4); a.fillRect(gr.x + 4, gr.y + gr.height - 8, gr.width - 8, 4);
        dialArte(a, agora, dt);
        knobArte(a, VOL_X, KNOB_Y, -2.35 + 4.7 * SomJogo.musica() / 100.0, foco == F_VOLUME);
        knobArte(a, SIN_X, KNOB_Y, giroSintonia, foco == F_SINTONIA);
        FontePixel.centro(a, "VOLUME", VOL_X, KNOB_Y + KR + 6, 1, foco == F_VOLUME ? OURO : CROMO_3);
        FontePixel.centro(a, "SINTONIA", SIN_X, KNOB_Y + KR + 6, 1, foco == F_SINTONIA ? OURO : CROMO_3);
        chaveArte(a);
        List<RadioMetal.Estacao> est = RadioMetal.estacoes();
        int pag = paginaAtual();
        for (int b = 0; b < 6; b++) {
            int i = pag * 6 + b;
            boolean ativo = RadioMetal.modo() == RadioMetal.Modo.RADIO && RadioMetal.estacaoAtual() == i;
            botaoArte(a, PRESETS[b], i < est.size() ? String.valueOf(b + 1) : "-", foco == F_PRESET + b, ativo);
        }
        for (int f = 0; f < 3; f++) botaoArte(a, FUNCOES[f], FUNCAO_CURTA[f], foco == F_FUNCAO + f, f == 0 && RadioMetal.aleatorio());
        a.dispose();
    }

    /** Knob da arte: escolhe o quadro (8 posicoes, 45 graus cada) pelo angulo. */
    private static void knobArte(Graphics2D g, int cx, int cy, double ang, boolean foco) {
        int n = 8, c = KNOB.getWidth() / n, q = Math.floorMod((int) Math.round(ang / (Math.PI / 4)), n), d = KR * 2;
        if (foco) { g.setColor(new Color(255, 206, 90, 120)); g.fillOval(cx - KR - 5, cy - KR - 5, d + 10, d + 10); }
        g.drawImage(KNOB, cx - KR, cy - KR, cx + KR, cy + KR, q * c, 0, q * c + c, KNOB.getHeight(), null);
    }

    /** Tecla da arte: quadro 0 normal, 1 com foco (dourada), 2 apertada/ligada (LED azul). */
    private static void botaoArte(Graphics2D g, Rectangle r, String rotulo, boolean foco, boolean aceso) {
        int c = BOTAO.getWidth() / 3, q = aceso ? 2 : foco ? 1 : 0, af = aceso ? 1 : 0;
        g.drawImage(BOTAO, r.x, r.y, r.x + r.width, r.y + r.height, q * c, 0, q * c + c, BOTAO.getHeight(), null);
        if (foco && aceso) { g.setColor(OURO); g.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1); }
        int e = FontePixel.largura(rotulo, 2) <= r.width - 12 ? 2 : 1;
        FontePixel.centro(g, rotulo, r.x + r.width / 2, r.y + (r.height - FontePixel.altura(e)) / 2 + af, e, foco ? OURO : BRANCO);
    }

    /** Chave TRILHA | RADIO | PLAYLIST: trilho da arte e o cursor vermelho na posicao atual. */
    private static void chaveArte(Graphics2D g) {
        Rectangle r = CHAVE;
        int pos = RadioMetal.modo() == RadioMetal.Modo.TRILHA ? 0 : RadioMetal.modo() == RadioMetal.Modo.RADIO ? 1 : 2, w3 = r.width / 3;
        g.drawImage(TRILHO, r.x, r.y, r.width, r.height, null);
        for (int i = 0; i < 3; i++) if (i != pos) FontePixel.centro(g, POSICOES[i], r.x + i * w3 + w3 / 2, r.y + (r.height - 7) / 2, 1, foco == F_CHAVE ? OURO : CINZA);
        int ch = r.height + 4, cw = CURSOR.getWidth() * ch / CURSOR.getHeight(), cx = r.x + pos * w3 + (w3 - cw) / 2;
        g.drawImage(CURSOR, cx, r.y - 2, cw, ch, null);
        FontePixel.centro(g, POSICOES[pos], r.x + pos * w3 + w3 / 2, r.y + (r.height - 7) / 2, 1, BRANCO);
        if (foco == F_CHAVE) { g.setColor(OURO); g.drawRect(r.x - 3, r.y - 4, r.width + 5, r.height + 7); }
    }

    /** Janela do dial da arte: escala, estacoes, ponteiro deslizando, nome e musica, e o VU. */
    private static void dialArte(Graphics2D g, long agora, double dt) {
        Rectangle r = DIAL;
        Graphics2D c = (Graphics2D) g.create();
        c.clipRect(r.x, r.y, r.width, r.height);
        int n = RadioMetal.estacoes().size(), x0 = r.x + 52, x1 = r.x + r.width - 52, ty = r.y + 12;
        for (int x = x0; x <= x1; x += 8) { c.setColor(DIAL_APAGADO); c.fillRect(x, ty, 2, (x - x0) % 40 == 0 ? 6 : 3); }
        for (int k = 0; k <= 5; k++) FontePixel.centro(c, String.valueOf(88 + k * 4), x0 + (x1 - x0) * k / 5, r.y + 3, 1, DIAL_APAGADO);
        FontePixel.desenhar(c, "JOGO", r.x + 8, ty, 1, DIAL_TEXTO, null);
        FontePixel.desenhar(c, "MP3", r.x + r.width - 8 - FontePixel.largura("MP3", 1), ty, 1, DIAL_TEXTO, null);
        int atual = sintoniaAtual();
        for (int i = 0; i <= n + 1; i++) { c.setColor(i == atual ? AMBAR : new Color(255, 176, 50, 110)); c.fillRect(posicaoNoDial(i, n, r) - 2, ty + 8, 4, 2); }
        String nome = RadioMetal.origem(), st = RadioMetal.status(), m = RadioMetal.musica(), a = RadioMetal.artista();
        boolean noAr = RadioMetal.modo() != RadioMetal.Modo.TRILHA && "TOCANDO".equals(st);
        boolean recente = agora - recadoEm < 3500;
        int largTexto = r.width - 120;
        FontePixel.desenhar(c, cortar(nome, largTexto), r.x + 12, r.y + 24, E, OURO, null);
        String linha = recente ? recado : RadioMetal.modo() == RadioMetal.Modo.TRILHA ? "MÚSICA ORIGINAL DO METAL SPACE"
                : !m.isEmpty() ? (a.isEmpty() ? m : a + " - " + m) : st.isEmpty() ? "-" : st;
        rolar(c, linha, r.x + 12, r.y + 44, largTexto, E, recente ? OURO : DIAL_TEXTO, agora);
        if (noAr && (agora / 500) % 2 == 0) FontePixel.desenhar(c, "NO AR", r.x + r.width - 96, r.y + 24, 1, PONTEIRO, null);
        int vx = r.x + r.width - 96, vy = r.y + 40;
        for (int col = 0; col < 2; col++) for (int k = 0; k < 10; k++) {
            float nv = Math.max(0, Math.min(1, vu * (col == 0 ? 1 : .9f)));
            Color cor = k >= 8 ? new Color(240, 60, 50) : k >= 6 ? new Color(250, 200, 40) : new Color(80, 230, 110);
            c.setColor(k < Math.round(nv * 10) ? cor : new Color(cor.getRed() / 6, cor.getGreen() / 6, cor.getBlue() / 6));
            c.fillRect(vx + k * 8, vy + col * 8, 6, 5);
        }
        double alvo = posicaoNoDial(atual, n, r);
        if (ponteiroX < 0) ponteiroX = alvo;
        ponteiroX += (alvo - ponteiroX) * Math.min(1, dt * 8);
        int ph = r.height - 4, pw = Math.max(4, PONTEIRO_ARTE.getWidth() * ph / PONTEIRO_ARTE.getHeight());
        c.drawImage(PONTEIRO_ARTE, (int) ponteiroX - pw / 2, r.y + 2, pw, ph, null);
        c.dispose();
    }

    /** Posicao no dial: 0 = trilha (JOGO), 1..N = estacoes, N+1 = playlist (MP3). */
    private static int sintoniaAtual() {
        RadioMetal.Modo m = RadioMetal.modo();
        return m == RadioMetal.Modo.TRILHA ? 0 : m == RadioMetal.Modo.PLAYLIST ? RadioMetal.estacoes().size() + 1 : 1 + RadioMetal.estacaoAtual();
    }

    private static void sintonizar(int p) {
        int n = RadioMetal.estacoes().size();
        p = Math.max(0, Math.min(n + 1, p));
        if (p == sintoniaAtual()) return;
        giroSintonia += (p - sintoniaAtual()) * Math.toRadians(24);
        girou();
        if (p == 0) RadioMetal.tocarTrilha();
        else if (p == n + 1) RadioMetal.tocarPlaylist();
        else RadioMetal.tocarRadio(p - 1);
        SomJogo.tocar(SomJogo.Efeito.TROCA);
    }

    private static void girarVolume(int passos) {
        int v = Math.max(0, Math.min(100, SomJogo.musica() + passos * 5));
        girou();
        if (v != SomJogo.musica()) SomJogo.volumeMusica(v);
        recado("VOLUME DA MÚSICA: " + v + "%");
    }

    private static void chave(int pos) {
        int atual = RadioMetal.modo() == RadioMetal.Modo.TRILHA ? 0 : RadioMetal.modo() == RadioMetal.Modo.RADIO ? 1 : 2;
        apertou();
        if (pos == atual) return;
        if (pos == 0) RadioMetal.tocarTrilha();
        else if (pos == 1) RadioMetal.tocarRadio(RadioMetal.estacaoAtual());
        else RadioMetal.tocarPlaylist();
        SomJogo.tocar(SomJogo.Efeito.CONFIRMAR);
    }

    private static int paginaAtual() { return RadioMetal.modo() == RadioMetal.Modo.RADIO ? RadioMetal.estacaoAtual() / 6 : 0; }

    private static void preset(int b) {
        apertou();
        int i = paginaAtual() * 6 + b;
        if (i >= RadioMetal.estacoes().size()) { recado("BOTÃO VAZIO: COPIE UM LINK E APERTE V"); SomJogo.tocar(SomJogo.Efeito.ERRO); return; }
        giroSintonia += (1 + i - sintoniaAtual()) * Math.toRadians(24);
        RadioMetal.tocarRadio(i);
        SomJogo.tocar(SomJogo.Efeito.CONFIRMAR);
    }

    private static void funcao(int f) {
        apertou();
        if (f == 0) { RadioMetal.alternarAleatorio(); recado("MISTURAR: " + (RadioMetal.aleatorio() ? "LIGADO" : "DESLIGADO")); SomJogo.tocar(SomJogo.Efeito.CONFIRMAR); }
        else if (f == 1) colar();
        else { RadioMetal.abrirPasta(); recado("ABRINDO A PASTA DA PLAYLIST"); SomJogo.tocar(SomJogo.Efeito.CONFIRMAR); }
    }

    /** Teclas; devolve false quando ESC fecha (quem chamou decide para onde voltar). */
    static boolean tecla(int k) {
        if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_BACK_SPACE) { fechar(); SomJogo.tocar(SomJogo.Efeito.VOLTAR); return false; }
        mexeuEm = System.currentTimeMillis(); mouseEm = 0;     // teclado: a mao vai ate o controle escolhido
        if (k == KeyEvent.VK_LEFT) { foco = Math.floorMod(foco - 1, FOCOS); SomJogo.tocar(SomJogo.Efeito.MENU); }
        else if (k == KeyEvent.VK_RIGHT) { foco = (foco + 1) % FOCOS; SomJogo.tocar(SomJogo.Efeito.MENU); }
        else if (k == KeyEvent.VK_UP || k == KeyEvent.VK_DOWN) ajustar(k == KeyEvent.VK_UP ? 1 : -1);
        else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) ajustar(1);
        else if (k == KeyEvent.VK_Q) girarVolume(-1);
        else if (k == KeyEvent.VK_E) girarVolume(1);
        else if (k == KeyEvent.VK_A) sintonizar(sintoniaAtual() - 1);
        else if (k == KeyEvent.VK_D) sintonizar(sintoniaAtual() + 1);
        else if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_6) { foco = F_PRESET + k - KeyEvent.VK_1; preset(k - KeyEvent.VK_1); }
        else if (k == KeyEvent.VK_V) colar();
        else if (k == KeyEvent.VK_P) funcao(2);
        else if (k == KeyEvent.VK_DELETE) remover();
        return true;
    }

    /** Cima/baixo (ou ENTER) no controle com foco: gira, muda a chave ou aperta. */
    private static void ajustar(int d) {
        if (foco == F_VOLUME) girarVolume(d);
        else if (foco == F_SINTONIA) sintonizar(sintoniaAtual() + d);
        else if (foco == F_CHAVE) chave(Math.floorMod((RadioMetal.modo() == RadioMetal.Modo.TRILHA ? 0 : RadioMetal.modo() == RadioMetal.Modo.RADIO ? 1 : 2) + d, 3));
        else if (foco < F_FUNCAO) preset(foco - F_PRESET);
        else funcao(foco - F_FUNCAO);
    }

    private static void remover() {
        int i = foco >= F_PRESET && foco < F_FUNCAO ? paginaAtual() * 6 + foco - F_PRESET : RadioMetal.modo() == RadioMetal.Modo.RADIO ? RadioMetal.estacaoAtual() : -1;
        if (i >= 0 && RadioMetal.remover(i)) { recado("RÁDIO REMOVIDA"); SomJogo.tocar(SomJogo.Efeito.CONFIRMAR); }
        else { recado("SÓ AS RÁDIOS QUE VOCÊ ADICIONOU SAEM (DEL)"); SomJogo.tocar(SomJogo.Efeito.ERRO); }
    }

    /** V: le o link copiado e adiciona a radio. */
    private static void colar() {
        String t = "";
        try { t = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); }
        catch (Exception e) { /* nada copiado ou sem acesso */ }
        int i = RadioMetal.adicionar(t);
        if (i < 0) { recado("COPIE O LINK DA RÁDIO (HTTP...) E APERTE V"); SomJogo.tocar(SomJogo.Efeito.ERRO); return; }
        RadioMetal.tocarRadio(i);
        recado("RÁDIO ADICIONADA: CANAL " + (i + 1));
        SomJogo.tocar(SomJogo.Efeito.COMPRA);
    }

    private static void recado(String s) { recado = s; recadoEm = System.currentTimeMillis(); }

    // ======================================================================= mouse

    private static void mouse(java.awt.AWTEvent ev) {
        if (!aberta || !(ev instanceof MouseEvent)) return;
        MouseEvent e = (MouseEvent) ev;
        Component c = e.getComponent();
        javax.swing.JRootPane raiz = c == null ? null : javax.swing.SwingUtilities.getRootPane(c);
        if (raiz == null) return;
        Point p = javax.swing.SwingUtilities.convertPoint(c, e.getPoint(), raiz.getContentPane());
        if (mouse(e.getID(), p.x, p.y, ev instanceof MouseWheelEvent ? ((MouseWheelEvent) ev).getWheelRotation() : 0)) raiz.repaint();
    }

    /** Trata um evento de mouse em coordenadas da tela do jogo (960x600); devolve true se mudou algo. */
    static boolean mouse(int id, int x, int y, int rodinha) {
        if (!aberta) return false;
        if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED || id == MouseEvent.MOUSE_PRESSED) {
            mouseEm = mexeuEm = System.currentTimeMillis();     // com mouse, a mao do piloto e o cursor
            alvoMaoX = x; alvoMaoY = y;
        }
        int knob = dentro(x, y, VOL_X, KNOB_Y) ? F_VOLUME : dentro(x, y, SIN_X, KNOB_Y) ? F_SINTONIA : -1;
        if (id == MouseEvent.MOUSE_WHEEL && knob >= 0) { foco = knob; if (knob == F_VOLUME) girarVolume(-rodinha); else sintonizar(sintoniaAtual() - rodinha); return true; }
        if (id == MouseEvent.MOUSE_PRESSED) {
            if (knob >= 0) { foco = knob; arrastando = knob; arrasteAngulo = angulo(x, y, knob); arrasteAcumulado = 0; return true; }
            for (int i = 0; i < 6; i++) if (PRESETS[i].contains(x, y)) { foco = F_PRESET + i; preset(i); return true; }
            for (int i = 0; i < 3; i++) if (FUNCOES[i].contains(x, y)) { foco = F_FUNCAO + i; funcao(i); return true; }
            if (CHAVE.contains(x, y)) { foco = F_CHAVE; chave(Math.min(2, (x - CHAVE.x) * 3 / CHAVE.width)); return true; }
            return false;
        }
        if (id == MouseEvent.MOUSE_DRAGGED && arrastando >= 0) {   // girar o botao arrastando em volta dele
            double a = angulo(x, y, arrastando), d = Math.atan2(Math.sin(a - arrasteAngulo), Math.cos(a - arrasteAngulo));
            arrasteAngulo = a; arrasteAcumulado += d;
            double passo = Math.toRadians(arrastando == F_VOLUME ? 14 : 30);
            while (Math.abs(arrasteAcumulado) >= passo) {
                int s = arrasteAcumulado > 0 ? 1 : -1;
                arrasteAcumulado -= s * passo;
                if (arrastando == F_VOLUME) girarVolume(s); else sintonizar(sintoniaAtual() + s);
            }
            return true;
        }
        if (id == MouseEvent.MOUSE_RELEASED) { boolean era = arrastando >= 0; arrastando = -1; return era; }
        if (id == MouseEvent.MOUSE_MOVED) {
            int antes = foco;
            if (knob >= 0) foco = knob;
            for (int i = 0; i < 6; i++) if (PRESETS[i].contains(x, y)) foco = F_PRESET + i;
            for (int i = 0; i < 3; i++) if (FUNCOES[i].contains(x, y)) foco = F_FUNCAO + i;
            if (CHAVE.contains(x, y)) foco = F_CHAVE;
            return antes != foco;
        }
        return false;
    }

    private static boolean dentro(int x, int y, int cx, int cy) { return (x - cx) * (x - cx) + (y - cy) * (y - cy) <= (KR + 6) * (KR + 6); }
    private static double angulo(int x, int y, int knob) { return Math.atan2(y - KNOB_Y, x - (knob == F_VOLUME ? VOL_X : SIN_X)); }

    // ======================================================================= desenho

    static void desenhar(Graphics2D tela) {
        if (!aberta) return;
        Graphics2D g = (Graphics2D) tela.create();
        long agora = System.currentTimeMillis();
        double dt = ultimoQuadro == 0 ? 0 : Math.min(.1, (agora - ultimoQuadro) / 1000.0);
        ultimoQuadro = agora;
        float alvo = Math.min(1, SomJogo.nivelMusica() * 2.2f);
        vu = alvo > vu ? alvo : vu * .9f + alvo * .1f;
        espaco(g, dt);
        java.awt.image.BufferedImage ck = COCKPIT[modeloCockpit] != null ? COCKPIT[modeloCockpit] : COCKPIT[1];
        if (ck != null) g.drawImage(ck, 0, 0, 960, 600, null);
        g.setColor(new Color(0, 0, 0, 90)); g.fillRect(0, 330, 960, 270);        // painel na sombra: o radio em destaque
        if (ARTE) desenharArte(g, agora, dt);
        else {
            suportes(g);
            caixa(g);
            grade(g);
            dial(g, agora, dt);
            knob(g, VOL_X, KNOB_Y, -2.35 + 4.7 * SomJogo.musica() / 100.0, "VOLUME", foco == F_VOLUME, null);
            knob(g, SIN_X, KNOB_Y, giroSintonia, "SINTONIA", foco == F_SINTONIA, null);
            chaveFonte(g);
            presets(g);
            funcoes(g);
        }
        mao(g, dt, agora);
        g.setColor(new Color(0, 0, 0, 150)); g.fillRect(0, 0, 960, 22);
        FontePixel.centro(g, "MOUSE: A MÃO DO PILOTO   SETAS < >: ESCOLHER   CIMA/BAIXO: GIRAR   1-6: ESTAÇÃO   Q/E: VOLUME   A/D: SINTONIA   V: COLAR LINK   ESC: VOLTAR",
                480, 8, 1, CINZA);
        g.dispose();
    }

    /** Espaco passando pelo para-brisa: estrelas em blocos de 2 px correndo (a nave segue em frente). */
    private static void espaco(Graphics2D g, double dt) {
        tempoEstrelas += dt;
        g.setColor(new Color(4, 4, 14)); g.fillRect(0, 0, 960, 600);
        for (int i = 0; i < EX.length; i++) {
            int x = (int) Math.floorMod((long) (EX[i] - tempoEstrelas * EV[i]), 960L) / 2 * 2, y = EY[i] / 2 * 2;
            g.setColor(EV[i] > 80 ? new Color(220, 236, 255) : new Color(120, 140, 190));
            g.fillRect(x, y, EV[i] > 80 ? 6 : 2, 2);
        }
    }

    /** Suportes que prendem o radio no painel. */
    private static void suportes(Graphics2D g) {
        for (int lado = 0; lado < 2; lado++) {
            int sx = lado == 0 ? X - 40 : X + W + 8;
            g.setColor(CONTORNO); g.fillRect(sx - 2, Y + 60, 36, 120);
            g.setColor(CROMO_1); g.fillRect(sx, Y + 62, 32, 116);
            g.setColor(CROMO_2); g.fillRect(sx, Y + 62, 32, 4);
            for (int p = 0; p < 2; p++) { g.setColor(CONTORNO); g.fillRect(sx + 12, Y + 82 + p * 60, 8, 8); g.setColor(CROMO_3); g.fillRect(sx + 14, Y + 84 + p * 60, 4, 4); }
        }
    }

    /** Onde a mao deve ir: o cursor (mouse) ou o controle com foco (teclado); parada, ela desce e sai. */
    private static void alvoDaMao(long agora) {
        if (mouseEm > 0 && agora - mouseEm < 2500) { pose = arrastando >= 0 ? Pose.SEGURA : Pose.APONTA; return; }
        if (agora - mexeuEm > 3500) { alvoMaoY = 760; return; }
        if (foco == F_VOLUME || foco == F_SINTONIA) { alvoMaoX = foco == F_VOLUME ? VOL_X : SIN_X; alvoMaoY = KNOB_Y - KR + 8; pose = Pose.SEGURA; return; }
        pose = Pose.APONTA;
        Rectangle r = foco == F_CHAVE ? CHAVE : foco < F_FUNCAO ? PRESETS[foco - F_PRESET] : FUNCOES[foco - F_FUNCAO];
        if (foco == F_CHAVE) {
            int pos = RadioMetal.modo() == RadioMetal.Modo.TRILHA ? 0 : RadioMetal.modo() == RadioMetal.Modo.RADIO ? 1 : 2;
            alvoMaoX = r.x + pos * r.width / 3 + r.width / 6; alvoMaoY = r.y + r.height / 2;
        } else { alvoMaoX = r.x + r.width / 2; alvoMaoY = r.y + r.height / 2; }
    }

    /**
     * Mao direita enluvada (couro marrom, tira com fivela, manga laranja do macacao), em blocos de 2 px.
     * APONTA: dedo indicador esticado, a ponta no alvo; afunda um pouco quando aperta.
     * SEGURA: dedos fechados em cima do knob, balancando enquanto gira.
     */
    private static void mao(Graphics2D g, double dt, long agora) {
        alvoDaMao(agora);
        double k = Math.min(1, dt * 14);
        maoX += (alvoMaoX - maoX) * k; maoY += (alvoMaoY - maoY) * k;
        if (maoY > 700) return;
        boolean apertando = agora - apertouEm < 160, girando = agora - girouEm < 260;
        if (MAOS_ARTE != null) { maoArte(g, apertando, girando, agora); return; }
        int x = ((int) maoX) / 2 * 2, y = ((int) maoY) / 2 * 2 + (apertando ? 4 : 0);
        if (girando) x += (int) (Math.sin(agora / 30.0) * 3) / 2 * 2;
        Color luva = new Color(98, 60, 36), luvaSombra = new Color(66, 40, 24), luvaLuz = new Color(140, 92, 58),
                tira = new Color(48, 28, 18), fivela = new Color(196, 170, 96), manga = new Color(232, 120, 30),
                mangaSombra = new Color(176, 78, 18), mangaLuz = new Color(255, 172, 72);
        // manga do macacao ate a borda de baixo da tela
        int pulsoY = pose == Pose.APONTA ? y + 66 : y + 50;
        bloco(g, x - 8, pulsoY + 8, 52, 600 - pulsoY, CONTORNO);
        bloco(g, x - 6, pulsoY + 10, 48, 600 - pulsoY, manga);
        bloco(g, x + 30, pulsoY + 10, 12, 600 - pulsoY, mangaSombra);
        bloco(g, x - 6, pulsoY + 10, 6, 600 - pulsoY, mangaLuz);
        // punho: tira com fivela
        bloco(g, x - 8, pulsoY - 2, 52, 14, CONTORNO);
        bloco(g, x - 6, pulsoY, 48, 10, tira);
        bloco(g, x + 14, pulsoY, 10, 10, fivela);
        bloco(g, x + 16, pulsoY + 2, 6, 6, tira);
        if (pose == Pose.APONTA) {
            bloco(g, x - 6, y - 2, 14, 40, CONTORNO);                 // indicador esticado
            bloco(g, x - 4, y, 10, 38, luva);
            bloco(g, x - 4, y, 4, 36, luvaLuz);
            bloco(g, x - 10, y + 28, 50, 40, CONTORNO);                // costas da mao
            bloco(g, x - 8, y + 30, 46, 36, luva);
            for (int d = 0; d < 3; d++) {                              // dedos dobrados
                bloco(g, x + 8 + d * 10, y + 24, 12, 16, CONTORNO);
                bloco(g, x + 10 + d * 10, y + 26, 8, 12, luva);
                bloco(g, x + 10 + d * 10, y + 34, 8, 4, luvaSombra);
            }
            bloco(g, x - 16, y + 40, 12, 20, CONTORNO);                // polegar
            bloco(g, x - 14, y + 42, 8, 16, luva);
            bloco(g, x + 22, y + 44, 16, 22, luvaSombra);
            bloco(g, x - 8, y + 30, 46, 4, luvaLuz);
        } else {
            bloco(g, x - 30, y - 6, 62, 30, CONTORNO);                 // dedos fechados sobre o knob
            for (int d = 0; d < 4; d++) {
                bloco(g, x - 28 + d * 15, y - 4, 13, 28, luva);
                bloco(g, x - 28 + d * 15, y - 4, 13, 4, luvaLuz);
                bloco(g, x - 28 + d * 15, y + 18, 13, 6, luvaSombra);
            }
            bloco(g, x - 22, y + 20, 50, 34, CONTORNO);                // costas da mao
            bloco(g, x - 20, y + 22, 46, 30, luva);
            bloco(g, x + 12, y + 26, 14, 26, luvaSombra);
            bloco(g, x - 20, y + 22, 46, 4, luvaLuz);
        }
    }

    /** Mao do jogo (maos_radio.png, 5 quadros: aponta, aperta, garra, girando 1 e 2). Fundo magenta vira transparente. */
    private static final java.awt.image.BufferedImage[] MAOS_ARTE = lerMaos();
    /** Ponta do dedo (aponta) e centro dos dedos (garra) em fracao do quadro: e esse ponto que vai no alvo. */
    private static final double[][] PONTA = {{.55, .27}, {.50, .27}, {.50, .30}, {.50, .30}, {.50, .30}};

    private static java.awt.image.BufferedImage[] lerMaos() {
        java.net.URL u = TelaRadio.class.getResource("/ImagemIcon/maos_radio.png");
        if (u == null) return null;
        try {
            java.awt.image.BufferedImage a = javax.imageio.ImageIO.read(u);
            java.awt.image.BufferedImage b = new java.awt.image.BufferedImage(a.getWidth(), a.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) {
                int p = a.getRGB(x, y), r = (p >> 16) & 255, gg = (p >> 8) & 255, bb = p & 255;
                b.setRGB(x, y, r > 200 && bb > 200 && gg < 90 ? 0 : p | 0xFF000000);
            }
            int c = b.getWidth() / 5;
            java.awt.image.BufferedImage[] q = new java.awt.image.BufferedImage[5];
            for (int i = 0; i < 5; i++) q[i] = b.getSubimage(i * c, 0, c, b.getHeight());
            return q;
        } catch (java.io.IOException e) { return null; }
    }

    private static void maoArte(Graphics2D g, boolean apertando, boolean girando, long agora) {
        int q = pose == Pose.APONTA ? (apertando ? 1 : 0) : girando ? 3 + (int) ((agora / 90) % 2) : 2;
        java.awt.image.BufferedImage m = MAOS_ARTE[q];
        int h = 330, w = m.getWidth() * h / m.getHeight();
        int x = (int) (maoX - PONTA[q][0] * w), y = (int) (maoY - PONTA[q][1] * h);
        Graphics2D a = (Graphics2D) g.create();
        a.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        a.drawImage(m, x, y, w, h, null);
        // a manga continua ate a borda de baixo da tela (estica a ultima faixa da arte)
        if (y + h < 600) a.drawImage(m, x, y + h - 1, x + w, 600, 0, m.getHeight() - 3, m.getWidth(), m.getHeight() - 1, null);
        a.dispose();
    }

    private static void bloco(Graphics2D g, int x, int y, int w, int h, Color c) {
        g.setColor(c); g.fillRect(x / 2 * 2, y / 2 * 2, w / 2 * 2, h / 2 * 2);
    }

    /** Caixa de madeira escura, frente champanhe com chanfros e neon azul em volta. */
    private static void caixa(Graphics2D g) {
        g.setColor(CONTORNO); g.fillRect(X - 6, Y - 6, W + 12, H + 12);
        g.setColor(CAIXA_1); g.fillRect(X - 2, Y - 2, W + 4, H + 4);
        g.setColor(CAIXA_2); g.fillRect(X, Y, W, H);
        for (int y = Y + 6; y < Y + H; y += 10) { g.setColor(CAIXA_3); g.fillRect(X + 2, y, 6, 2); g.fillRect(X + W - 8, y + 4, 6, 2); } // veio da madeira
        int fx = X + 12, fy = Y + 12, fw = W - 24, fh = H - 24;
        g.setColor(FRENTE_1); g.fillRect(fx, fy, fw, fh);
        g.setColor(FRENTE_2); g.fillRect(fx + 2, fy + 2, fw - 4, fh - 4);
        g.setColor(FRENTE_3); g.fillRect(fx + 4, fy + 4, fw - 8, fh - 10);
        g.setColor(FRENTE_4); g.fillRect(fx + 4, fy + 4, fw - 8, 2);
        g.setColor(FRENTE_1); g.fillRect(fx + 4, fy + fh - 8, fw - 8, 2);
        // neon azul em volta da frente (pulsa com a musica)
        int a = (int) (150 + 105 * vu);
        g.setColor(new Color(40, 90, 255, Math.min(255, a / 2))); g.drawRect(fx + 8, fy + 8, fw - 17, fh - 17); g.drawRect(fx + 12, fy + 12, fw - 25, fh - 25);
        g.setColor(new Color(110, 160, 255, Math.min(255, a))); g.drawRect(fx + 10, fy + 10, fw - 21, fh - 21);
    }

    /** Alto-falante: frisos horizontais escuros com luz em cima. */
    private static void grade(Graphics2D g) {
        Rectangle r = GRADE;
        g.setColor(CONTORNO); g.fillRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8);
        g.setColor(GRADE_1); g.fillRect(r.x, r.y, r.width, r.height);
        for (int y = r.y + 6; y < r.y + r.height - 6; y += 8) {
            g.setColor(GRADE_3); g.fillRect(r.x + 10, y, r.width - 20, 2);
            g.setColor(GRADE_2); g.fillRect(r.x + 10, y + 2, r.width - 20, 2);
        }
        FontePixel.desenhar(g, "METAL SPACE", r.x + 14, r.y + r.height - 18, 1, CROMO_3, null);
        String modelo = "MS-98 RÁDIO DE BORDO";
        FontePixel.desenhar(g, modelo, r.x + r.width - 14 - FontePixel.largura(modelo, 1), r.y + r.height - 18, 1, CROMO_3, null);
    }

    /** Dial analogico: escala JOGO ... estacoes ... MP3, ponteiro deslizando, e a musica tocando. */
    private static void dial(Graphics2D g, long agora, double dt) {
        Rectangle r = DIAL;
        g.setColor(CONTORNO); g.fillRect(r.x - 6, r.y - 6, r.width + 12, r.height + 12);
        g.setColor(CROMO_2); g.fillRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8);
        g.setColor(CROMO_4); g.fillRect(r.x - 4, r.y - 4, r.width + 8, 2);
        g.setColor(DIAL_FUNDO); g.fillRect(r.x, r.y, r.width, r.height);
        for (int y = r.y; y < r.y + r.height; y += 4) { g.setColor(DIAL_LINHA); g.fillRect(r.x, y, r.width, 2); }
        int n = RadioMetal.estacoes().size(), x0 = r.x + 52, x1 = r.x + r.width - 52, ty = r.y + 26;
        // escala: risquinhos e numeros de faixa como num dial de FM
        for (int x = x0; x <= x1; x += 8) { g.setColor(DIAL_APAGADO); g.fillRect(x, ty, 2, (x - x0) % 40 == 0 ? 10 : 6); }
        for (int k = 0; k <= 5; k++) { String f = String.valueOf(88 + k * 4); FontePixel.centro(g, f, x0 + (x1 - x0) * k / 5, r.y + 10, 1, DIAL_APAGADO); }
        FontePixel.desenhar(g, "JOGO", r.x + 8, ty + 2, 1, DIAL_TEXTO, null);
        FontePixel.desenhar(g, "MP3", r.x + r.width - 8 - FontePixel.largura("MP3", 1), ty + 2, 1, DIAL_TEXTO, null);
        int atual = sintoniaAtual();
        for (int i = 0; i <= n + 1; i++) {                          // marcas das estacoes
            int mx = posicaoNoDial(i, n, r);
            g.setColor(i == atual ? AMBAR : new Color(255, 176, 50, 110));
            g.fillRect(mx - 2, ty + 12, 4, 4);
        }
        double alvo = posicaoNoDial(atual, n, r);
        if (ponteiroX < 0) ponteiroX = alvo;
        ponteiroX += (alvo - ponteiroX) * Math.min(1, dt * 8);      // desliza ate a estacao
        int px = ((int) ponteiroX) / 2 * 2;
        g.setColor(new Color(255, 60, 40, 70)); g.fillRect(px - 4, r.y + 4, 10, 44);
        g.setColor(PONTEIRO); g.fillRect(px, r.y + 4, 2, 44);
        g.setColor(new Color(255, 200, 190)); g.fillRect(px, r.y + 4, 2, 4);
        // o que esta tocando
        Graphics2D c = (Graphics2D) g.create();
        c.clipRect(r.x + 8, r.y + 54, r.width - 16, r.height - 58);
        String nome = RadioMetal.origem(), st = RadioMetal.status();
        boolean noAr = RadioMetal.modo() != RadioMetal.Modo.TRILHA && "TOCANDO".equals(st);
        FontePixel.desenhar(c, cortar(nome, r.width - 140), r.x + 12, r.y + 58, E, OURO, null);
        if (noAr && (agora / 500) % 2 == 0) FontePixel.desenhar(c, "NO AR", r.x + r.width - 120, r.y + 58, E, PONTEIRO, null);
        String m = RadioMetal.musica(), a = RadioMetal.artista();
        String linha = RadioMetal.modo() == RadioMetal.Modo.TRILHA ? "MÚSICA ORIGINAL DO METAL SPACE"
                : !m.isEmpty() ? (a.isEmpty() ? m : a + " - " + m) : st.isEmpty() ? "-" : st;
        rolar(c, linha, r.x + 12, r.y + 80, r.width - 130, E, DIAL_TEXTO, agora);
        boolean recente = agora - recadoEm < 3500;
        boolean erro = st.startsWith("SEM SINAL") || st.contains("INDISPONIVEL") || st.startsWith("NAO") || st.startsWith("ERRO") || st.startsWith("PASTA VAZIA");
        String baixo = recente ? recado : erro ? st : !m.isEmpty() || RadioMetal.modo() == RadioMetal.Modo.TRILHA ? "" : st;
        if (!baixo.isEmpty() && !baixo.equals(linha)) FontePixel.desenhar(c, cortar(baixo, r.width - 140, 1), r.x + 12, r.y + 102, 1, recente ? OURO : erro ? PONTEIRO : DIAL_APAGADO, null);
        c.dispose();
        // VU em leds, no canto do dial
        int vx = r.x + r.width - 104, vy = r.y + 84;
        for (int col = 0; col < 2; col++) for (int k = 0; k < 10; k++) {
            float nv = Math.max(0, Math.min(1, vu * (col == 0 ? 1 : .9f)));
            Color cor = k >= 8 ? new Color(240, 60, 50) : k >= 6 ? new Color(250, 200, 40) : new Color(80, 230, 110);
            g.setColor(k < Math.round(nv * 10) ? cor : new Color(cor.getRed() / 6, cor.getGreen() / 6, cor.getBlue() / 6));
            g.fillRect(vx + k * 9, vy + col * 10, 7, 6);
        }
        g.setColor(new Color(255, 255, 255, 22)); g.fillRect(r.x, r.y, r.width, 6);        // reflexo do vidro
    }

    private static int posicaoNoDial(int p, int n, Rectangle r) {
        if (p == 0) return r.x + 22;
        if (p == n + 1) return r.x + r.width - 24;
        int x0 = r.x + 52, x1 = r.x + r.width - 52;
        return n <= 1 ? (x0 + x1) / 2 : x0 + (x1 - x0) * (p - 1) / (n - 1);
    }

    /** Botao de girar cromado (serrilhado em volta, marca indicando a posicao). */
    private static void knob(Graphics2D g, int cx, int cy, double ang, String nome, boolean foco, String valor) {
        int r = KR;
        g.setColor(CONTORNO); g.fillOval(cx - r - 4, cy - r - 4, 2 * r + 8, 2 * r + 8);
        g.setColor(foco ? OURO : CROMO_1); g.fillOval(cx - r - 2, cy - r - 2, 2 * r + 4, 2 * r + 4);
        for (int k = 0; k < 28; k++) {                                // serrilhado
            double a = ang + k * Math.PI * 2 / 28;
            int sx = cx + (int) Math.round(Math.cos(a) * (r - 2)), sy = cy + (int) Math.round(Math.sin(a) * (r - 2));
            g.setColor(CROMO_2); g.fillRect(sx / 2 * 2 - 2, sy / 2 * 2 - 2, 4, 4);
        }
        g.setColor(CROMO_2); g.fillOval(cx - r + 6, cy - r + 6, 2 * r - 12, 2 * r - 12);
        g.setColor(CROMO_3); g.fillOval(cx - r + 10, cy - r + 10, 2 * r - 22, 2 * r - 22);
        g.setColor(CROMO_4); g.fillOval(cx - r + 16, cy - r + 14, 18, 10);   // brilho do cromo
        int mx = cx + (int) Math.round(Math.cos(ang - Math.PI / 2) * (r - 14)), my = cy + (int) Math.round(Math.sin(ang - Math.PI / 2) * (r - 14));
        g.setColor(PONTEIRO); g.fillRect(mx / 2 * 2 - 3, my / 2 * 2 - 3, 6, 6);
        g.setColor(CROMO_1); g.fillRect(cx - 3, cy - 3, 6, 6);
        FontePixel.centro(g, nome, cx, cy + r + 12, E, foco ? OURO : CAIXA_1);
        if (valor != null) FontePixel.centro(g, valor, cx, cy - r - 14, 1, CAIXA_1);
    }

    /** Chave deslizante TRILHA | RADIO | PLAYLIST. */
    private static void chaveFonte(Graphics2D g) {
        Rectangle r = CHAVE;
        int pos = RadioMetal.modo() == RadioMetal.Modo.TRILHA ? 0 : RadioMetal.modo() == RadioMetal.Modo.RADIO ? 1 : 2, w3 = r.width / 3;
        g.setColor(foco == F_CHAVE ? OURO : CONTORNO); g.fillRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8);
        g.setColor(GRADE_1); g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(CROMO_2); g.fillRect(r.x + pos * w3 + 2, r.y + 2, w3 - 4, r.height - 4);
        g.setColor(CROMO_4); g.fillRect(r.x + pos * w3 + 2, r.y + 2, w3 - 4, 2);
        for (int i = 0; i < 3; i++) FontePixel.centro(g, POSICOES[i], r.x + i * w3 + w3 / 2, r.y + 7, E, i == pos ? CAIXA_1 : CINZA);
    }

    private static void presets(Graphics2D g) {
        List<RadioMetal.Estacao> est = RadioMetal.estacoes();
        int pag = paginaAtual();
        for (int b = 0; b < 6; b++) {
            int i = pag * 6 + b;
            boolean ativo = RadioMetal.modo() == RadioMetal.Modo.RADIO && RadioMetal.estacaoAtual() == i;
            botao(g, PRESETS[b], String.valueOf(b + 1), i < est.size() ? est.get(i).nome : "VAZIO", foco == F_PRESET + b, ativo);
        }
    }

    private static void funcoes(Graphics2D g) {
        for (int f = 0; f < 3; f++) botao(g, FUNCOES[f], FUNCAO[f], null, foco == F_FUNCAO + f, f == 0 && RadioMetal.aleatorio());
    }

    /** Tecla de apertar: tampa clara, sombra, afunda com o foco; LED ambar quando ligada. */
    private static void botao(Graphics2D g, Rectangle r, String rotulo, String sub, boolean foco, boolean aceso) {
        int af = foco ? 2 : 0;
        g.setColor(CONTORNO); g.fillRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
        g.setColor(GRADE_1); g.fillRect(r.x, r.y + r.height - 4, r.width, 4);
        g.setColor(foco ? new Color(110, 84, 44) : GRADE_2); g.fillRect(r.x, r.y + af, r.width, r.height - 4);
        g.setColor(foco ? new Color(170, 128, 60) : GRADE_3); g.fillRect(r.x, r.y + af, r.width, 2);
        if (foco) { g.setColor(OURO); g.drawRect(r.x - 3, r.y - 3, r.width + 5, r.height + 5); }
        if (aceso) { g.setColor(AMBAR); g.fillRect(r.x + r.width - 12, r.y + 4 + af, 8, 4); }
        if (sub == null) FontePixel.centro(g, cortar(rotulo, r.width - 6), r.x + r.width / 2, r.y + (r.height - 4 - 14) / 2 + af, E, foco ? OURO : BRANCO);
        else {
            FontePixel.centro(g, rotulo, r.x + r.width / 2, r.y + 4 + af, E, foco ? OURO : BRANCO);
            FontePixel.centro(g, cortar(sub, r.width - 4, 1), r.x + r.width / 2, r.y + r.height - 14 + af, 1, foco ? OURO : CINZA);
        }
    }

    /** Texto que rola da direita para a esquerda quando nao cabe. */
    private static void rolar(Graphics2D g, String s, int x, int y, int largura, int escala, Color cor, long agora) {
        int tw = FontePixel.largura(s, escala);
        if (tw <= largura) { FontePixel.desenhar(g, s, x, y, escala, cor, null); return; }
        Graphics2D c = (Graphics2D) g.create();
        c.clipRect(x, y - 6, largura, FontePixel.altura(escala) + 12);
        int ciclo = tw + 140, desl = (int) ((agora / 25) % ciclo) / escala * escala;
        FontePixel.desenhar(c, s, x - desl, y, escala, cor, null);
        FontePixel.desenhar(c, s, x - desl + ciclo, y, escala, cor, null);
        c.dispose();
    }

    // ======================================================================= aviso por cima do jogo

    /** Aviso "tocando agora" no canto da tela por alguns segundos depois de cada troca (fonte pixel). */
    static void desenharAviso(Graphics2D tela) {
        long desde = System.currentTimeMillis() - RadioMetal.mudouEm();
        if (RadioMetal.mudouEm() == 0 || desde > AVISO_MS || aberta) return;
        Graphics2D g = (Graphics2D) tela.create();
        float alfa = desde > AVISO_MS - 600 ? (AVISO_MS - desde) / 600f : 1f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alfa))));
        String l1 = "RÁDIO: " + RadioMetal.origem();
        String m = RadioMetal.musica(), a = RadioMetal.artista(), st = RadioMetal.status();
        String l2 = RadioMetal.modo() == RadioMetal.Modo.TRILHA ? "MÚSICA ORIGINAL DO METAL SPACE"
                : !m.isEmpty() ? (a.isEmpty() ? m : a + " - " + m) : st;
        int maxTexto = 430 - 24;
        l1 = cortar(l1, maxTexto); l2 = cortar(l2, maxTexto);
        int w = Math.max(FontePixel.largura(l1, E), FontePixel.largura(l2, E)) + 24, h = 48, x = 960 - w - 10, y = 116;
        g.setColor(new Color(2, 5, 14, 225)); g.fillRect(x, y, w, h);
        g.setColor(new Color(110, 160, 255)); g.drawRect(x, y, w - 1, h - 1);
        g.setColor(new Color(40, 60, 140)); g.drawRect(x + 2, y + 2, w - 5, h - 5);
        FontePixel.desenhar(g, l1, x + 12, y + 8, E, OURO);
        FontePixel.desenhar(g, l2, x + 12, y + 28, E, BRANCO);
        g.dispose();
    }

    static String cortar(String s, int largura) { return cortar(s, largura, E); }

    static String cortar(String s, int largura, int escala) {
        if (FontePixel.largura(s, escala) <= largura) return s;
        int cabem = Math.max(1, (largura / escala + 1) / (FontePixel.COLUNAS + 1) - 3);
        return s.substring(0, Math.min(s.length(), cabem)).trim() + "...";
    }

    /** Usado pelos testes. */
    static int focoTeste() { return foco; }
    /** Centro de um controle (F_VOLUME, F_SINTONIA, F_PRESET + i...), para os testes clicarem. */
    static Point pontoTeste(int f) {
        if (f == F_VOLUME) return new Point(VOL_X, KNOB_Y);
        if (f == F_SINTONIA) return new Point(SIN_X, KNOB_Y);
        Rectangle r = f == F_CHAVE ? CHAVE : f < F_FUNCAO ? PRESETS[f - F_PRESET] : FUNCOES[f - F_FUNCAO];
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }
}
