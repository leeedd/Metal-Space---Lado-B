package Jogo;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Tela da radio (Opcoes > Audio > Radio) e o aviso "tocando agora" que aparece por cima do jogo.
 * Itens: trilha do jogo, playlist, as estacoes e "adicionar radio" (cola o link copiado com V).
 */
final class TelaRadio {
    private static final Color OURO = new Color(255, 210, 110), BRANCO = Color.WHITE, CINZA = new Color(150, 165, 185),
            VERDE = new Color(110, 230, 120), VERMELHO = new Color(240, 100, 100), AZUL = new Color(120, 200, 255);
    private static final int VISIVEIS = 6;
    private static final long AVISO_MS = 6000;
    private static boolean aberta;
    private static int cursor, topo;
    private static String recado = "";
    private static long recadoEm;

    private TelaRadio() { }

    static boolean aberta() { return aberta; }
    static void fechar() { aberta = false; }

    static void abrir() {
        aberta = true;
        RadioMetal.Modo m = RadioMetal.modo();
        cursor = m == RadioMetal.Modo.TRILHA ? 0 : m == RadioMetal.Modo.PLAYLIST ? 1 : 2 + RadioMetal.estacaoAtual();
        ajustarTopo();
    }

    private static int itens() { return 2 + RadioMetal.estacoes().size() + 1; }

    /** Teclas da tela da radio; ESC/BACKSPACE volta para a aba Audio. */
    static void tecla(int k) {
        int n = itens();
        if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_BACK_SPACE) { aberta = false; SomJogo.tocar(SomJogo.Efeito.VOLTAR); return; }
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) { cursor = Math.floorMod(cursor - 1, n); ajustarTopo(); SomJogo.tocar(SomJogo.Efeito.MENU); return; }
        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) { cursor = (cursor + 1) % n; ajustarTopo(); SomJogo.tocar(SomJogo.Efeito.MENU); return; }
        if (k == KeyEvent.VK_P) { RadioMetal.abrirPasta(); recado("ABRINDO A PASTA DA PLAYLIST"); return; }
        if (k == KeyEvent.VK_V) { colar(); return; }
        if (k == KeyEvent.VK_DELETE) {
            if (RadioMetal.remover(cursor - 2)) { recado("RADIO REMOVIDA"); cursor = Math.min(cursor, itens() - 1); ajustarTopo(); SomJogo.tocar(SomJogo.Efeito.CONFIRMAR); }
            else { recado("SO AS RADIOS QUE VOCE ADICIONOU PODEM SER REMOVIDAS"); SomJogo.tocar(SomJogo.Efeito.ERRO); }
            return;
        }
        boolean lado = k == KeyEvent.VK_LEFT || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_A || k == KeyEvent.VK_D;
        if (lado && cursor == 1) { RadioMetal.alternarAleatorio(); SomJogo.tocar(SomJogo.Efeito.CONFIRMAR); return; }
        if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            if (cursor == 0) RadioMetal.tocarTrilha();
            else if (cursor == 1) RadioMetal.tocarPlaylist();
            else if (cursor == n - 1) { colar(); return; }
            else RadioMetal.tocarRadio(cursor - 2);
            SomJogo.tocar(SomJogo.Efeito.CONFIRMAR);
        }
    }

    /** V: le o link da area de transferencia e adiciona a radio. */
    private static void colar() {
        String t = "";
        try { t = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); }
        catch (Exception e) { /* nada copiado ou sem acesso */ }
        int i = RadioMetal.adicionar(t);
        if (i < 0) { recado("COPIE O LINK DA RADIO (HTTP...) E APERTE V"); SomJogo.tocar(SomJogo.Efeito.ERRO); return; }
        cursor = 2 + i;
        ajustarTopo();
        RadioMetal.tocarRadio(i);
        recado("RADIO ADICIONADA");
        SomJogo.tocar(SomJogo.Efeito.COMPRA);
    }

    private static void recado(String s) { recado = s; recadoEm = System.currentTimeMillis(); }

    private static void ajustarTopo() {
        if (cursor < topo) topo = cursor;
        if (cursor >= topo + VISIVEIS) topo = cursor - VISIVEIS + 1;
        topo = Math.max(0, Math.min(topo, Math.max(0, itens() - VISIVEIS)));
    }

    /** Conteudo da tela da radio dentro do painel de opcoes. */
    static void desenhar(Graphics2D g, int x, int y, int w) {
        List<RadioMetal.Estacao> est = RadioMetal.estacoes();
        int n = itens();
        RadioMetal.Modo modo = RadioMetal.modo();
        for (int v = 0; v < VISIVEIS && topo + v < n; v++) {
            int i = topo + v, ly = y + v * 21;
            boolean sel = i == cursor;
            boolean tocando = (i == 0 && modo == RadioMetal.Modo.TRILHA) || (i == 1 && modo == RadioMetal.Modo.PLAYLIST)
                    || (i >= 2 && i < n - 1 && modo == RadioMetal.Modo.RADIO && RadioMetal.estacaoAtual() == i - 2);
            String nome;
            Color cor = sel ? OURO : BRANCO;
            if (i == 0) nome = "TRILHA DO JOGO";
            else if (i == 1) nome = "PLAYLIST  < ALEATORIO: " + (RadioMetal.aleatorio() ? "SIM" : "NAO") + " >";
            else if (i == n - 1) { nome = "+ ADICIONAR RADIO (COPIE O LINK E APERTE V)"; if (!sel) cor = AZUL; }
            else { RadioMetal.Estacao e = est.get(i - 2); nome = e.nome + (e.doJogador ? "  (SUA)" : ""); }
            if (sel) { g.setColor(new Color(255, 210, 110, 40)); g.fillRect(x - 4, ly - 1, w + 8, 20); }
            g.setFont(fonte(13));
            String marca = tocando ? "TOCANDO" : "";
            int wm = g.getFontMetrics().stringWidth(marca);
            texto(g, cortar(g, (sel ? "> " : "  ") + nome, w - wm - 10), x, ly + 14, cor);
            if (tocando) texto(g, marca, x + w - wm, ly + 14, VERDE);
        }
        if (topo > 0) texto(g, "^", x + w / 2, y - 3, CINZA, 11);
        if (topo + VISIVEIS < n) texto(g, "v", x + w / 2, y + VISIVEIS * 21 + 4, CINZA, 11);

        // o que esta tocando agora
        int by = y + VISIVEIS * 21 + 12;
        g.setColor(new Color(8, 12, 22, 220)); g.fillRect(x - 4, by, w + 8, 66);
        g.setColor(new Color(90, 120, 170, 170)); g.drawRect(x - 4, by, w + 7, 65);
        g.setFont(fonte(13));
        texto(g, cortar(g, RadioMetal.origem(), w - 8), x + 4, by + 17, OURO);
        g.setFont(fonte(12));
        if (modo == RadioMetal.Modo.TRILHA) texto(g, "MUSICA ORIGINAL DO METAL SPACE", x + 4, by + 36, BRANCO);
        else {
            texto(g, cortar(g, "MUSICA:  " + vazio(RadioMetal.musica()), w - 8), x + 4, by + 36, BRANCO);
            texto(g, cortar(g, "ARTISTA: " + vazio(RadioMetal.artista()), w - 8), x + 4, by + 53, BRANCO);
        }
        String st = RadioMetal.status();
        boolean recente = System.currentTimeMillis() - recadoEm < 3500;
        g.setFont(fonte(11));
        String linha = recente ? recado : st.isEmpty() || st.equals("TOCANDO") ? "F11: PROXIMA   F12: ANTERIOR   P: ABRIR PASTA DA PLAYLIST" : st;
        Color c = recente ? AZUL : st.startsWith("SEM SINAL") || st.contains("INDISPONIVEL") || st.startsWith("NAO") || st.startsWith("ERRO") || st.startsWith("PASTA VAZIA") ? VERMELHO : CINZA;
        texto(g, cortar(g, linha, w), x, by + 84, c);
    }

    /** Aviso "tocando agora" no canto da tela por alguns segundos depois de cada troca. */
    static void desenharAviso(Graphics2D tela) {
        long desde = System.currentTimeMillis() - RadioMetal.mudouEm();
        if (RadioMetal.mudouEm() == 0 || desde > AVISO_MS) return;
        Graphics2D g = (Graphics2D) tela.create();
        float alfa = desde > AVISO_MS - 600 ? (AVISO_MS - desde) / 600f : 1f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alfa))));
        String l1 = "RADIO: " + RadioMetal.origem();
        String m = RadioMetal.musica(), a = RadioMetal.artista(), st = RadioMetal.status();
        String l2 = RadioMetal.modo() == RadioMetal.Modo.TRILHA ? "MUSICA ORIGINAL DO METAL SPACE"
                : !m.isEmpty() ? (a.isEmpty() ? m : a + " - " + m) : st;
        g.setFont(fonte(13));
        FontMetrics fm = g.getFontMetrics();
        int w = Math.min(430, Math.max(fm.stringWidth(l1), fm.stringWidth(l2)) + 24), h = 44, x = 960 - w - 10, y = 116;
        g.setColor(new Color(2, 5, 14, 225)); g.fillRect(x, y, w, h);
        g.setColor(new Color(255, 170, 60)); g.drawRect(x, y, w - 1, h - 1);
        g.setColor(new Color(120, 40, 20)); g.drawRect(x + 2, y + 2, w - 5, h - 5);
        contorno(g, cortar(g, l1, w - 20), x + 12, y + 18, OURO);
        contorno(g, cortar(g, l2, w - 20), x + 12, y + 36, BRANCO);
        g.dispose();
    }

    private static String vazio(String s) { return s == null || s.isEmpty() ? "-" : s; }

    /** Corta o texto com "..." para caber na largura. */
    static String cortar(Graphics2D g, String s, int largura) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= largura) return s;
        String r = s;
        while (r.length() > 1 && fm.stringWidth(r + "...") > largura) r = r.substring(0, r.length() - 1);
        return r + "...";
    }

    private static Font fonte(int t) { return new Font(Font.MONOSPACED, Font.BOLD, t); }

    private static void texto(Graphics2D g, String s, int x, int y, Color c, int t) {
        g.setFont(fonte(t));
        texto(g, s, x - g.getFontMetrics().stringWidth(s) / 2, y, c);
    }

    private static void texto(Graphics2D g, String s, int x, int y, Color c) {
        g.setColor(new Color(0, 0, 0, 170)); g.drawString(s, x + 1, y + 1);
        g.setColor(c); g.drawString(s, x, y);
    }

    private static void contorno(Graphics2D g, String s, int x, int y, Color c) {
        g.setColor(Color.BLACK);
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (dx != 0 || dy != 0) g.drawString(s, x + dx, y + dy);
        g.setColor(c); g.drawString(s, x, y);
    }
}
