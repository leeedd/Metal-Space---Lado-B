package Jogo;

/**
 * Buffer circular de audio (mono, 44100 Hz) entre o decodificador da radio e o mixador do jogo.
 * Quem decodifica escreve (e espera se o buffer encher); o mixador le sem nunca esperar:
 * se faltar audio, toca silencio e junta um pouco antes de voltar (evita picotar).
 */
final class FonteContinua {
    static final int TAXA = 44100;
    private final float[] buf;
    private final int minimo;
    private int ini, qtd, faltas;
    private boolean tocando, terminou, fechada;

    /** @param segundos capacidade do buffer; @param preBuffer segundos juntados antes de comecar a tocar */
    FonteContinua(int segundos, double preBuffer) {
        buf = new float[TAXA * segundos];
        minimo = (int) (TAXA * preBuffer);
    }

    /** Escreve n amostras; bloqueia enquanto o buffer estiver cheio. Devolve false se a fonte foi fechada. */
    synchronized boolean escrever(float[] a, int n) throws InterruptedException {
        int i = 0;
        while (i < n) {
            while (qtd == buf.length && !fechada) wait(100);
            if (fechada) return false;
            int fim = (ini + qtd) % buf.length;
            int k = Math.min(buf.length - qtd, Math.min(n - i, buf.length - fim));
            System.arraycopy(a, i, buf, fim, k);
            qtd += k;
            i += k;
        }
        return true;
    }

    /** Le ate n amostras para dst; devolve quantas leu (0 = silencio). Nunca bloqueia. */
    synchronized int ler(float[] dst, int n) {
        if (!tocando) {
            if (qtd >= minimo || (terminou && qtd > 0)) tocando = true;
            else return 0;
        }
        int k = Math.min(n, qtd);
        int p1 = Math.min(k, buf.length - ini);
        System.arraycopy(buf, ini, dst, 0, p1);
        if (k > p1) System.arraycopy(buf, 0, dst, p1, k - p1);
        ini = (ini + k) % buf.length;
        qtd -= k;
        if (qtd == 0 && !terminou) { tocando = false; faltas++; }
        notifyAll();
        return k;
    }

    /** Descarta o que esta no buffer (ao pular de musica) e volta a juntar antes de tocar. */
    synchronized void limpar() { ini = qtd = 0; tocando = false; terminou = false; notifyAll(); }
    /** A origem acabou (fim do arquivo): toca o que sobrou mesmo abaixo do pre-buffer. */
    synchronized void marcarFim() { terminou = true; }
    synchronized void fechar() { fechada = true; notifyAll(); }
    synchronized boolean fechada() { return fechada; }
    synchronized int amostras() { return qtd; }
    /** Quantas vezes o audio faltou depois de comecar (picotes). */
    synchronized int faltas() { return faltas; }
}
