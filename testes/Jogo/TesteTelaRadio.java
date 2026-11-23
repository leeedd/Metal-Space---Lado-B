package Jogo;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/**
 * Tela da radio (F7 / Opcoes > Audio > Radio), guitarra do menu, fonte pixel, animacao da tela inicial
 * e "o jogo sempre abre com som". Sem internet e sem ffmpeg (nao toca nada de verdade).
 */
public final class TesteTelaRadio {
    static int n;
    static void ok(boolean b, String m) { if (!b) throw new AssertionError(m); n++; }

    public static void main(String[] a) throws Exception {
        System.setProperty("metalspace.audio.offline", "true");
        System.setProperty("metalspace.radio.prefs", "radio_teste_tela");
        System.setProperty("metalspace.ffmpeg", "nao_existe.exe");
        try {
            telaRadio();
            guitarra();
            fonte();
            animacao();
            ok(!SomJogo.mudo(), "o jogo abre com som");
            SomJogo.alternarMudo();
            ok(SomJogo.mudo(), "F10 silencia");
            SomJogo.alternarMudo();
        } finally {
            RadioMetal.tocarTrilha();
            try { java.util.prefs.Preferences.userNodeForPackage(RadioMetal.class).node("radio_teste_tela").removeNode(); } catch (Exception e) { /* ja removido */ }
        }
        System.out.println("OK tela da radio: " + n + " verificacoes");
        System.exit(0);
    }

    static void telaRadio() {
        ok(!TelaRadio.aberta() && !PainelSom.aberto(), "comeca fechada");
        ok(PainelSom.tecla(KeyEvent.VK_F7) && TelaRadio.aberta() && PainelSom.aberto(), "F7 abre a radio (e o jogo manda as teclas para ela)");
        PainelSom.tecla(KeyEvent.VK_2);
        ok(RadioMetal.modo() == RadioMetal.Modo.RADIO && RadioMetal.estacaoAtual() == 1, "tecla 2 liga a estacao 2");
        ok(TelaRadio.focoTeste() == TelaRadio.F_PRESET + 1, "foco no botao 2");
        while (TelaRadio.focoTeste() != TelaRadio.F_SINTONIA) PainelSom.tecla(KeyEvent.VK_RIGHT);
        PainelSom.tecla(KeyEvent.VK_UP);
        ok(RadioMetal.estacaoAtual() == 2, "girar a SINTONIA para cima vai para a proxima estacao");
        PainelSom.tecla(KeyEvent.VK_DOWN); PainelSom.tecla(KeyEvent.VK_DOWN); PainelSom.tecla(KeyEvent.VK_DOWN);
        ok(RadioMetal.modo() == RadioMetal.Modo.TRILHA, "girar a SINTONIA ate o comeco do dial cai na trilha (JOGO)");
        PainelSom.tecla(KeyEvent.VK_D);
        ok(RadioMetal.modo() == RadioMetal.Modo.RADIO && RadioMetal.estacaoAtual() == 0, "D sintoniza a primeira estacao");
        int v = SomJogo.musica();
        PainelSom.tecla(KeyEvent.VK_E);
        ok(SomJogo.musica() == Math.min(100, v + 5), "E aumenta o volume da musica");
        PainelSom.tecla(KeyEvent.VK_Q);
        ok(SomJogo.musica() == v || v == 100, "Q diminui o volume");
        while (TelaRadio.focoTeste() != TelaRadio.F_CHAVE) PainelSom.tecla(KeyEvent.VK_RIGHT);
        PainelSom.tecla(KeyEvent.VK_UP);
        ok(RadioMetal.modo() == RadioMetal.Modo.PLAYLIST, "chave: de RADIO para PLAYLIST");
        PainelSom.tecla(KeyEvent.VK_UP);
        ok(RadioMetal.modo() == RadioMetal.Modo.TRILHA, "chave: de PLAYLIST para TRILHA");
        // mouse: clique no botao 3 e rodinha no VOLUME
        ok(TelaRadio.mouse(java.awt.event.MouseEvent.MOUSE_PRESSED, TelaRadio.pontoTeste(TelaRadio.F_PRESET + 2).x, TelaRadio.pontoTeste(TelaRadio.F_PRESET + 2).y, 0), "clique no botao 3");
        ok(RadioMetal.estacaoAtual() == 2 && RadioMetal.modo() == RadioMetal.Modo.RADIO, "clique liga a estacao 3");
        int antes = SomJogo.musica();
        TelaRadio.mouse(java.awt.event.MouseEvent.MOUSE_WHEEL, TelaRadio.pontoTeste(TelaRadio.F_VOLUME).x, TelaRadio.pontoTeste(TelaRadio.F_VOLUME).y, -1);
        ok(SomJogo.musica() == Math.min(100, antes + 5), "rodinha no VOLUME aumenta");
        TelaRadio.mouse(java.awt.event.MouseEvent.MOUSE_WHEEL, TelaRadio.pontoTeste(TelaRadio.F_VOLUME).x, TelaRadio.pontoTeste(TelaRadio.F_VOLUME).y, 1);
        // arrastar a SINTONIA em volta dela anda pelas estacoes
        int sx = TelaRadio.pontoTeste(TelaRadio.F_SINTONIA).x, sy = TelaRadio.pontoTeste(TelaRadio.F_SINTONIA).y, est = RadioMetal.estacaoAtual();
        TelaRadio.mouse(java.awt.event.MouseEvent.MOUSE_PRESSED, sx, sy - 40, 0);
        TelaRadio.mouse(java.awt.event.MouseEvent.MOUSE_DRAGGED, sx + 40, sy, 0);   // quarto de volta no sentido horario
        TelaRadio.mouse(java.awt.event.MouseEvent.MOUSE_RELEASED, sx + 40, sy, 0);
        ok(RadioMetal.estacaoAtual() > est, "arrastar a SINTONIA gira para as proximas estacoes");
        RadioMetal.tocarTrilha();
        desenha(TelaRadio::desenhar);
        PainelSom.tecla(KeyEvent.VK_ESCAPE);
        ok(!TelaRadio.aberta() && !PainelSom.aberto(), "ESC fecha a radio aberta pelo F7");
        // pelo painel: Opcoes > Audio > Radio, e o ESC volta para o painel
        PainelSom.tecla(KeyEvent.VK_F9);
        for (int i = 0; i < 3; i++) PainelSom.tecla(KeyEvent.VK_DOWN);
        PainelSom.tecla(KeyEvent.VK_ENTER);
        ok(TelaRadio.aberta(), "ENTER na linha RADIO abre a tela da radio");
        PainelSom.tecla(KeyEvent.VK_ESCAPE);
        ok(!TelaRadio.aberta() && PainelSom.aberto(), "ESC volta para as Opcoes");
        PainelSom.tecla(KeyEvent.VK_ESCAPE);
        ok(!PainelSom.aberto(), "ESC fecha as Opcoes");
        // mouse nas Opcoes: clicar na linha RADIO abre a radio; clicar no volume ajusta
        PainelSom.tecla(KeyEvent.VK_F9);
        ok(PainelSom.mouse(java.awt.event.MouseEvent.MOUSE_PRESSED, 300, 418), "clique na linha RADIO");
        ok(TelaRadio.aberta(), "o clique abre a radio");
        PainelSom.tecla(KeyEvent.VK_ESCAPE);
        ok(PainelSom.aberto() && !TelaRadio.aberta(), "ESC volta para as Opcoes");
        int musicaAntes = SomJogo.musica();
        boolean ajustou = false;
        for (int x = 520; x <= 700 && !ajustou; x += 4) { PainelSom.mouse(java.awt.event.MouseEvent.MOUSE_PRESSED, x, 260); ajustou = SomJogo.musica() != musicaAntes; }
        ok(ajustou, "clique no trilho do volume muda a musica");
        SomJogo.volumeMusica(musicaAntes);
        PainelSom.tecla(KeyEvent.VK_ESCAPE);
        RadioMetal.definirTitulo("Banda Teste - Faixa Teste");
        desenha(TelaRadio::desenharAviso);
    }

    interface Desenho { void em(Graphics2D g); }

    static void desenha(Desenho d) {
        BufferedImage b = new BufferedImage(960, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = b.createGraphics();
        d.em(g);
        g.dispose();
        long pintados = 0;
        for (int y = 0; y < 600; y += 3) for (int x = 0; x < 960; x += 3) if ((b.getRGB(x, y) & 0xFFFFFF) != 0) pintados++;
        ok(pintados > 50, "desenhou alguma coisa");
    }

    static void guitarra() {
        for (int i = 0; i < GuitarraMenu.NOTAS.length; i++) {
            for (float[] s : new float[][]{GuitarraMenu.amostraPassar(i), GuitarraMenu.amostraConfirmar(i)}) {
                double soma = 0, pico = 0;
                boolean validas = true;
                for (float v : s) { validas &= !Float.isNaN(v) && !Float.isInfinite(v); soma += v * v; pico = Math.max(pico, Math.abs(v)); }
                ok(validas, "amostras validas (sem NaN/infinito)");
                double rms = Math.sqrt(soma / s.length);
                ok(pico > .5 && pico <= 1 && rms > .05, "guitarra soa (pico " + pico + ", rms " + rms + ")");
                ok(Math.abs(s[s.length - 1]) < .02, "termina sem estalo");
            }
            ok(GuitarraMenu.amostraConfirmar(i).length > GuitarraMenu.amostraPassar(i).length * 3, "confirmar soa mais que passar");
        }
    }

    static void fonte() {
        ok(FontePixel.largura("ABC", 2) == (3 * 6 - 1) * 2, "largura da fonte pixel");
        BufferedImage b = new BufferedImage(400, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        FontePixel.desenhar(g, "OPÇÕES RÁDIO MÚSICA 0.9.8 [H] F9:", 4, 10, 1, Color.WHITE);
        g.dispose();
        // todo caractere usado nos textos existe na fonte (o desconhecido vira '?', que nao aparece aqui)
        String usados = "0.9.8 ALPHA[H] PLACARENTER: DECOLARF9: OPÇÕESF10: MUDOMETAL SPACE FMRÁDIO DE BORDO MS-98<< ANTTRILHAPRÓX >>PLAYLISTMISTURAR+ RÁDIOVUNO ARCH 03/12-";
        for (char c : usados.toCharArray()) {
            String base = java.text.Normalizer.normalize(String.valueOf(c), java.text.Normalizer.Form.NFD).substring(0, 1);
            BufferedImage x = new BufferedImage(20, 30, BufferedImage.TYPE_INT_ARGB), q = new BufferedImage(20, 30, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gx = x.createGraphics(), gq = q.createGraphics();
            FontePixel.desenhar(gx, base, 4, 8, 1, Color.WHITE, null);
            FontePixel.desenhar(gq, "?", 4, 8, 1, Color.WHITE, null);
            gx.dispose(); gq.dispose();
            boolean igual = true;
            for (int yy = 0; yy < 30 && igual; yy++) for (int xx = 0; xx < 20; xx++) if (x.getRGB(xx, yy) != q.getRGB(xx, yy)) { igual = false; break; }
            ok(base.equals("?") || !igual, "a fonte pixel tem o caractere '" + c + "'");
        }
    }

    static void animacao() {
        FundoInicioAnimado f = new FundoInicioAnimado(Assets.imagem("ui_inicio_neutro.png"), new Rectangle(610, 270, 330, 330));
        BufferedImage m = f.mascaraTeste();
        long livre = 0;
        for (int y = 0; y < 600; y += 2) for (int x = 0; x < 960; x += 2) if ((m.getRGB(x, y) >>> 24) > 0) livre++;
        ok(livre > 5000, "a mascara deixa espaco livre para as estrelas (" + livre + ")");
        ok((m.getRGB(700, 400) >>> 24) == 0, "botoes do menu ficam fora da animacao");
        for (int i = 0; i < 300; i++) f.atualizar(1 / 30.0);
        BufferedImage b = new BufferedImage(960, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = b.createGraphics();
        f.desenhar(g);
        g.dispose();
        long acesos = 0;
        for (int y = 0; y < 600; y++) for (int x = 0; x < 960; x++) if ((b.getRGB(x, y) & 0xFFFFFF) != 0) acesos++;
        ok(acesos > 200, "estrelas, asteroides e jatos aparecem depois de 10 s (" + acesos + " px)");
    }
}
