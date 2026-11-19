package Jogo;

import java.util.List;

/**
 * Confere, pela internet, as radios que vem com o jogo: cada uma tem de conectar, mandar audio
 * e (quando a radio informa) mostrar artista e musica. Use quando quiser saber se alguma saiu do ar.
 * Precisa do ffmpeg (ffmpeg/ffmpeg.exe ou -Dmetalspace.ffmpeg). Nao toca som na caixa.
 */
public final class TesteRadiosAoVivo {
    public static void main(String[] a) throws Exception {
        System.setProperty("metalspace.audio.offline", "true");
        System.setProperty("metalspace.radio.prefs", "radio_teste_vivo");
        if (RadioMetal.ffmpeg() == null) throw new AssertionError("ffmpeg nao encontrado: coloque em ffmpeg/ffmpeg.exe (baixar_ffmpeg.ps1)");
        TesteRadio.Consumidor mixer = new TesteRadio.Consumidor();
        mixer.start();
        List<RadioMetal.Estacao> lista = RadioMetal.PADRAO;
        int falhas = 0;
        try {
            for (int i = 0; i < lista.size(); i++) {
                long antes = mixer.amostras.get(), t0 = System.currentTimeMillis();
                RadioMetal.tocarRadio(i);
                // tocando = pelo menos 3 s de audio decodificado
                boolean tocou = TesteRadio.esperar(() -> mixer.amostras.get() - antes > FonteContinua.TAXA * 3L, 20000);
                boolean titulo = tocou && TesteRadio.esperar(() -> !RadioMetal.musica().isEmpty(), 8000);
                long ms = System.currentTimeMillis() - t0;
                String info = titulo ? (RadioMetal.artista().isEmpty() ? RadioMetal.musica() : RadioMetal.artista() + " - " + RadioMetal.musica())
                        : tocou ? "(toca, mas nao informa a musica)" : RadioMetal.status();
                System.out.printf("%-26s %-6s %5.1fs  %s%n", lista.get(i).nome, tocou ? "OK" : "FALHOU", ms / 1000.0, info);
                if (!tocou) falhas++;
            }
        } finally {
            mixer.parar = true;
            RadioMetal.tocarTrilha();
            try { java.util.prefs.Preferences.userNodeForPackage(RadioMetal.class).node("radio_teste_vivo").removeNode(); } catch (Exception e) { /* ja removido */ }
        }
        System.out.println(falhas == 0 ? "OK radios ao vivo: " + lista.size() + " de " + lista.size() + " no ar"
                : "ATENCAO: " + falhas + " de " + lista.size() + " radios nao tocaram");
        System.exit(falhas == 0 ? 0 : 1);
    }
}
