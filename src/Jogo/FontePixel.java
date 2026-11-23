package Jogo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.Normalizer;
import java.util.*;

/**
 * Fonte em pixel art (16-bit): cada letra e uma grade de 5x7 quadradinhos, desenhada em blocos de 'escala' px,
 * com contorno escuro de 1 bloco para ler sobre qualquer arte. Acentos (agudo, grave, circunflexo, til) e
 * cedilha sao desenhados em cima/embaixo da letra. Textos ja desenhados ficam guardados (cache).
 */
final class FontePixel {
    static final int COLUNAS = 5, LINHAS = 7, ACIMA = 2, ABAIXO = 2;
    private static final Map<Character, String[]> G = new HashMap<>();
    private static final Map<String, BufferedImage> CACHE = new LinkedHashMap<String, BufferedImage>(32, .75f, true) {
        private static final long serialVersionUID = 1L;
        @Override protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> e) { return size() > 200; }
    };

    static {
        g('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        g('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
        g('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        g('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        g('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        g('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####");
        g('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('I', ".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###.");
        g('J', "..###", "...#.", "...#.", "...#.", "#..#.", "#..#.", ".##..");
        g('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        g('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        g('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
        g('N', "#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#");
        g('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        g('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
        g('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        g('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        g('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        g('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        g('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "#.#.#", ".#.#.");
        g('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        g('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
        g('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");
        g('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
        g('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.");
        g('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
        g('3', "#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###.");
        g('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
        g('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
        g('6', "..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###.");
        g('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
        g('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
        g('9', ".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##..");
        g(' ', ".....", ".....", ".....", ".....", ".....", ".....", ".....");
        g('.', ".....", ".....", ".....", ".....", ".....", ".##..", ".##..");
        g(',', ".....", ".....", ".....", ".....", ".##..", "..#..", ".#...");
        g(':', ".....", ".##..", ".##..", ".....", ".##..", ".##..", ".....");
        g(';', ".....", ".##..", ".##..", ".....", ".##..", "..#..", ".#...");
        g('-', ".....", ".....", ".....", "#####", ".....", ".....", ".....");
        g('+', ".....", "..#..", "..#..", "#####", "..#..", "..#..", ".....");
        g('=', ".....", ".....", "#####", ".....", "#####", ".....", ".....");
        g('_', ".....", ".....", ".....", ".....", ".....", ".....", "#####");
        g('/', "....#", "....#", "...#.", "..#..", ".#...", "#....", "#....");
        g('[', ".###.", ".#...", ".#...", ".#...", ".#...", ".#...", ".###.");
        g(']', ".###.", "...#.", "...#.", "...#.", "...#.", "...#.", ".###.");
        g('(', "...#.", "..#..", ".#...", ".#...", ".#...", "..#..", "...#.");
        g(')', ".#...", "..#..", "...#.", "...#.", "...#.", "..#..", ".#...");
        g('!', "..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#..");
        g('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..");
        g('>', ".#...", "..#..", "...#.", "....#", "...#.", "..#..", ".#...");
        g('<', "...#.", "..#..", ".#...", "#....", ".#...", "..#..", "...#.");
        g('\'', "..#..", "..#..", ".#...", ".....", ".....", ".....", ".....");
        g('%', "##...", "##..#", "...#.", "..#..", ".#...", "#..##", "...##");
        g('#', ".#.#.", "#####", ".#.#.", ".#.#.", ".#.#.", "#####", ".#.#.");
        g('|', "..#..", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        g('&', ".##..", "#..#.", ".##..", ".#...", "#.#.#", "#..#.", ".##.#");
    }

    private static final String[] AGUDO = {"...#.", "..#.."}, GRAVE = {".#...", "..#.."}, CIRCUNFLEXO = {"..#..", ".#.#."},
            TIL = {".#..#", "#.##."}, CEDILHA = {"..#..", ".##.."};

    private FontePixel() { }

    private static void g(char c, String... linhas) { G.put(c, linhas); }

    /** Largura em px do texto na escala dada (letra 5 blocos + 1 de espaco; contorno nao conta). */
    static int largura(String s, int escala) { return s.isEmpty() ? 0 : (s.length() * (COLUNAS + 1) - 1) * escala; }

    /** Altura das letras (sem acentos): 7 blocos. */
    static int altura(int escala) { return LINHAS * escala; }

    /** Desenha com o topo das letras em y (acentos sobem ate 2 blocos acima). */
    static void desenhar(Graphics2D g, String s, int x, int y, int escala, Color cor) {
        desenhar(g, s, x, y, escala, cor, new Color(8, 6, 14));
    }

    static void desenhar(Graphics2D g, String s, int x, int y, int escala, Color cor, Color contorno) {
        if (s == null || s.isEmpty()) return;
        g.drawImage(imagem(s, escala, cor, contorno), x - escala, y - (ACIMA + 1) * escala, null);
    }

    /** Centralizado em x. */
    static void centro(Graphics2D g, String s, int cx, int y, int escala, Color cor) {
        desenhar(g, s, cx - largura(s, escala) / 2, y, escala, cor);
    }

    private static synchronized BufferedImage imagem(String s, int e, Color cor, Color contorno) {
        String k = s + '\u0000' + e + '\u0000' + cor.getRGB() + '\u0000' + (contorno == null ? 0 : contorno.getRGB());
        BufferedImage b = CACHE.get(k);
        if (b != null) return b;
        int cols = s.length() * (COLUNAS + 1) + 1, rows = ACIMA + LINHAS + ABAIXO + 2;
        boolean[][] on = new boolean[rows][cols + 1];
        for (int i = 0; i < s.length(); i++) {
            char bruto = Character.toUpperCase(s.charAt(i));
            String base = Normalizer.normalize(String.valueOf(bruto), Normalizer.Form.NFD);
            char letra = base.charAt(0);
            String[] glifo = G.getOrDefault(letra, G.get('?'));
            int ox = 1 + i * (COLUNAS + 1), oy = 1 + ACIMA;
            marcar(on, glifo, ox, oy);
            for (int m = 1; m < base.length(); m++) {
                switch (base.charAt(m)) {
                    case '́': marcar(on, AGUDO, ox, oy - ACIMA); break;
                    case '̀': marcar(on, GRAVE, ox, oy - ACIMA); break;
                    case '̂': marcar(on, CIRCUNFLEXO, ox, oy - ACIMA); break;
                    case '̃': marcar(on, TIL, ox, oy - ACIMA); break;
                    case '̧': marcar(on, CEDILHA, ox, oy + LINHAS); break;
                    default: break;
                }
            }
        }
        b = new BufferedImage((cols + 1) * e, rows * e, BufferedImage.TYPE_INT_ARGB);
        Graphics2D d = b.createGraphics();
        if (contorno != null) {
            d.setColor(contorno);
            for (int r = 0; r < rows; r++) for (int c = 0; c <= cols; c++)
                if (on[r][c]) d.fillRect((c - 1) * e, (r - 1) * e, 3 * e, 3 * e);
        }
        d.setColor(cor);
        for (int r = 0; r < rows; r++) for (int c = 0; c <= cols; c++) if (on[r][c]) d.fillRect(c * e, r * e, e, e);
        d.dispose();
        CACHE.put(k, b);
        return b;
    }

    private static void marcar(boolean[][] on, String[] linhas, int ox, int oy) {
        for (int r = 0; r < linhas.length; r++) for (int c = 0; c < linhas[r].length(); c++)
            if (linhas[r].charAt(c) == '#' && oy + r >= 0 && oy + r < on.length && ox + c < on[0].length) on[oy + r][ox + c] = true;
    }
}
