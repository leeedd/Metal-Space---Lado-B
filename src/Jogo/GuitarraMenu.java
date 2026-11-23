package Jogo;

import java.util.Random;

/**
 * Guitarra distorcida do menu, sintetizada no codigo (corda de Karplus-Strong + distorcao + caixa do amp).
 * Nada gravado: o som e gerado na hora. Cada opcao do menu tem a sua nota: passar o cursor da um "chug"
 * abafado (palm mute) e confirmar da o power chord aberto, cordas batidas uma depois da outra.
 */
final class GuitarraMenu {
    static final int TAXA = 44100;
    /** Notas de cada opcao (INICIAR, HANGAR, CONQUISTAS, OPCOES, CREDITOS, SAIR): E2 G2 A2 B2 D3 E3. */
    static final double[] NOTAS = {82.41, 98.00, 110.00, 123.47, 146.83, 164.81};
    private static final float[][] PASSAR = new float[NOTAS.length][], CONFIRMAR = new float[NOTAS.length][];

    static {
        for (int i = 0; i < NOTAS.length; i++) {
            PASSAR[i] = acorde(NOTAS[i], .22, true, 4, 1000L + i);
            CONFIRMAR[i] = acorde(NOTAS[i], 1.25, false, 14, 2000L + i);
        }
    }

    private GuitarraMenu() { }

    /** Cursor passou para a opcao i. */
    static void passar(int i) { SomJogo.tocarAmostra(PASSAR[Math.floorMod(i, NOTAS.length)], .55f, 3); }

    /** Opcao i confirmada (abaixa a musica um instante, como um golpe de guitarra). */
    static void confirmar(int i) { SomJogo.tocarAmostra(CONFIRMAR[Math.floorMod(i, NOTAS.length)], .75f, 4); }

    static float[] amostraPassar(int i) { return PASSAR[i]; }
    static float[] amostraConfirmar(int i) { return CONFIRMAR[i]; }

    /**
     * Power chord (fundamental, quinta, oitava) com distorcao.
     * @param abafado palm mute: corda morre rapido e som mais escuro
     * @param palhetadaMs atraso entre uma corda e a proxima (palhetada para baixo)
     */
    static float[] acorde(double f0, double segundos, boolean abafado, double palhetadaMs, long semente) {
        int n = (int) (TAXA * segundos);
        double[] mix = new double[n];
        double[] cordas = {f0, f0 * 1.4983, f0 * 2.0};
        Random r = new Random(semente);
        for (int c = 0; c < cordas.length; c++) {
            int ini = (int) (c * palhetadaMs * TAXA / 1000.0);
            int periodo = Math.max(2, (int) Math.round(TAXA / cordas[c]));
            double[] linha = new double[periodo];
            double ant = 0;
            for (int k = 0; k < periodo; k++) { double v = r.nextDouble() * 2 - 1; ant = ant * .4 + v * .6; linha[k] = ant; } // ataque menos chiado
            double perda = abafado ? .975 : .9965, peso = c == 0 ? 1 : .8;
            int p = 0;
            for (int i = ini; i < n; i++) {
                int q = (p + 1) % periodo;
                double s = linha[p];
                linha[p] = perda * .5 * (linha[p] + linha[q]);
                p = q;
                mix[i] += s * peso;
            }
        }
        // distorcao (saturacao) + caixa do amplificador (passa-baixas) + corte dos graves que embolam
        float[] out = new float[n];
        double ganho = abafado ? 7 : 9, corte = abafado ? .18 : .32, lp = 0, hpEnt = 0, hpSai = 0, pico = 1e-9;
        for (int i = 0; i < n; i++) {
            double x = Math.tanh(mix[i] * ganho);
            lp += corte * (x - lp);
            double hp = .995 * (hpSai + lp - hpEnt); // remove DC e grave excessivo
            hpEnt = lp; hpSai = hp;
            double env = 1;
            int fim = n - i;
            if (fim < TAXA * .04) env = fim / (TAXA * .04);         // sem estalo no fim
            if (i < TAXA * .003) env *= i / (TAXA * .003);            // nem no comeco
            out[i] = (float) (hp * env);
            pico = Math.max(pico, Math.abs(out[i]));
        }
        float k = (float) (.9 / pico);
        for (int i = 0; i < n; i++) out[i] *= k;
        return out;
    }
}
