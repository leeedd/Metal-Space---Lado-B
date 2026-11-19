package Jogo;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Radio e playlist sem depender da internet: uma radio falsa local (ICY, redirecionamento, .pls, queda no meio)
 * e musicas de teste em varios formatos geradas pelo ffmpeg. Precisa do ffmpeg em ffmpeg/ffmpeg.exe
 * (ou -Dmetalspace.ffmpeg=...). Usa um no de preferencias proprio: nao mexe nas escolhas do jogador.
 */
public final class TesteRadio {
    static int n;
    static void ok(boolean b, String m) { if (!b) throw new AssertionError(m); n++; }

    public static void main(String[] a) throws Exception {
        System.setProperty("metalspace.audio.offline", "true");   // sem som saindo da caixa durante o teste
        System.setProperty("metalspace.radio.prefs", "radio_teste");
        File ff = RadioMetal.ffmpeg();
        ok(ff != null, "ffmpeg nao encontrado: coloque em ffmpeg/ffmpeg.exe (baixar_ffmpeg.ps1) ou use -Dmetalspace.ffmpeg");
        Path tmp = Files.createTempDirectory("metalspace_radio");
        Consumidor mixer = new Consumidor();
        mixer.start();
        try {
            fonteContinua();
            metadadosIcy();
            semFfmpeg(ff, tmp);
            playlist(ff, tmp);
            radioLocal(ff, tmp);
            trocasRapidas(ff, tmp);
            RadioMetal.tocarTrilha();
            ok(RadioMetal.modo() == RadioMetal.Modo.TRILHA && RadioMetal.fonteAtual() == null, "volta para a trilha");
        } finally {
            mixer.parar = true;
            RadioMetal.tocarTrilha();
            try { java.util.prefs.Preferences.userNodeForPackage(RadioMetal.class).node("radio_teste").removeNode(); } catch (Exception e) { /* ja removido */ }
            apagar(tmp);
        }
        System.out.println("OK radio: " + n + " verificacoes");
        System.exit(0);
    }

    /** Faz o papel do mixador: consome a fonte da radio o mais rapido possivel. */
    static final class Consumidor extends Thread {
        volatile boolean parar;
        final AtomicLong amostras = new AtomicLong();
        Consumidor() { super("Teste-Mixer"); setDaemon(true); }
        @Override public void run() {
            float[] b = new float[4096];
            while (!parar) {
                FonteContinua f = RadioMetal.fonteAtual();
                int k = f == null ? 0 : f.ler(b, b.length);
                if (k > 0) amostras.addAndGet(k);
                else try { Thread.sleep(2); } catch (InterruptedException e) { return; }
            }
        }
    }

    // ---------------- buffer ----------------
    static void fonteContinua() throws Exception {
        FonteContinua f = new FonteContinua(1, 0.005); // pre-buffer de 220 amostras
        float[] um = new float[300], saida = new float[1000];
        Arrays.fill(um, .5f);
        ok(f.ler(saida, 1000) == 0, "sem audio: silencio");
        ok(f.escrever(um, 100) && f.ler(saida, 1000) == 0, "junta o pre-buffer antes de tocar");
        f.limpar();
        ok(f.escrever(um, 300), "escreve");
        ok(f.ler(saida, 1000) == 300 && saida[299] == .5f, "le o que foi escrito");
        ok(f.faltas() == 1, "conta o picote quando esvazia");
        // cheio: o escritor espera; fechar libera
        FonteContinua cheia = new FonteContinua(1, 0.01);
        float[] seg = new float[FonteContinua.TAXA];
        ok(cheia.escrever(seg, seg.length), "enche");
        AtomicBoolean devolveu = new AtomicBoolean(true);
        Thread t = new Thread(() -> { try { devolveu.set(cheia.escrever(um, 300)); } catch (InterruptedException e) { /* fim */ } });
        t.start();
        Thread.sleep(200);
        ok(t.isAlive(), "escritor espera com o buffer cheio");
        cheia.fechar();
        t.join(2000);
        ok(!t.isAlive() && !devolveu.get(), "fechar libera o escritor");
        cheia.limpar();
        ok(cheia.amostras() == 0, "limpar descarta");
    }

    // ---------------- metadados ICY ----------------
    static void metadadosIcy() throws Exception {
        ok("Banda X - Faixa Y".equals(IcyCliente.tituloDe("StreamTitle='Banda X - Faixa Y';StreamUrl='';")), "StreamTitle simples");
        ok("Rock 'n' Roll - Vivo".equals(IcyCliente.tituloDe("StreamTitle='Rock 'n' Roll - Vivo';")), "titulo com apostrofo");
        ok(IcyCliente.tituloDe("StreamUrl='x';") == null, "sem StreamTitle");
        ok("Canção".equals(IcyCliente.decodificar("Canção".getBytes(StandardCharsets.ISO_8859_1))), "latin-1");
        ok("Canção".equals(IcyCliente.decodificar("Canção".getBytes(StandardCharsets.UTF_8))), "utf-8");
        // fluxo com metadados no meio: o filtro entrega so o audio e avisa o titulo
        ByteArrayOutputStream bruto = new ByteArrayOutputStream(), esperado = new ByteArrayOutputStream();
        int metaint = 100;
        for (int bloco = 0; bloco < 3; bloco++) {
            for (int i = 0; i < metaint; i++) { bruto.write(bloco * 7 + i); esperado.write(bloco * 7 + i); }
            byte[] meta = bloco == 1 ? icy("Banda Teste - Faixa " + bloco) : new byte[0];
            bruto.write(meta.length / 16);
            bruto.write(meta);
        }
        List<String> titulos = new ArrayList<>();
        InputStream in = new IcyCliente.FiltroMetadados(new ByteArrayInputStream(bruto.toByteArray()), metaint, titulos::add);
        ok(Arrays.equals(in.readAllBytes(), esperado.toByteArray()), "filtro tira os metadados do audio");
        ok(titulos.equals(List.of("Banda Teste - Faixa 1")), "filtro avisa o titulo");
        RadioMetal.definirTitulo("Banda Z - Musica W");
        ok("Banda Z".equals(RadioMetal.artista()) && "Musica W".equals(RadioMetal.musica()), "separa artista e musica");
    }

    static byte[] icy(String titulo) {
        byte[] t = ("StreamTitle='" + titulo + "';").getBytes(StandardCharsets.UTF_8);
        return Arrays.copyOf(t, (t.length + 15) / 16 * 16);
    }

    // ---------------- sem ffmpeg / pasta vazia ----------------
    static void semFfmpeg(File ff, Path tmp) throws Exception {
        System.setProperty("metalspace.ffmpeg", tmp.resolve("nao_existe.exe").toString());
        RadioMetal.tocarRadio(0);
        ok(esperar(() -> RadioMetal.status().contains("FFMPEG NAO ENCONTRADO"), 5000), "radio sem ffmpeg avisa: " + RadioMetal.status());
        RadioMetal.tocarPlaylist();
        ok(esperar(() -> RadioMetal.status().contains("FFMPEG NAO ENCONTRADO"), 5000), "playlist sem ffmpeg avisa: " + RadioMetal.status());
        System.setProperty("metalspace.ffmpeg", ff.getAbsolutePath());
        Path vazia = Files.createDirectories(tmp.resolve("vazia"));
        Files.writeString(vazia.resolve("leia-me.txt"), "nada aqui");
        System.setProperty("metalspace.playlist", vazia.toString());
        RadioMetal.tocarPlaylist();
        ok(esperar(() -> RadioMetal.status().startsWith("PASTA VAZIA"), 5000), "pasta vazia avisa: " + RadioMetal.status());
        ok(RadioMetal.adicionar("abc") < 0 && RadioMetal.adicionar("ftp://x/y") < 0, "link invalido nao entra");
    }

    // ---------------- playlist em varios formatos ----------------
    static final String[][] FORMATOS = {
            {"01 teste.mp3", "libmp3lame"}, {"02 teste.ogg", "libvorbis"}, {"03 teste.flac", "flac"}, {"04 teste.wav", "pcm_s16le"},
            {"05 teste.m4a", "aac"}, {"06 teste.opus", "libopus"}, {"07 teste.wma", "wmav2"}, {"08 teste.aiff", "pcm_s16be"}};

    static void playlist(File ff, Path tmp) throws Exception {
        Path pasta = Files.createDirectories(tmp.resolve("playlist"));
        Map<String, String[]> esperado = new LinkedHashMap<>();
        for (String[] f : FORMATOS) {
            String ext = f[0].substring(f[0].lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
            gerar(ff, pasta.resolve(f[0]), f[1], "Musica " + ext, "Banda " + ext, 1.2);
            esperado.put("Musica " + ext, new String[]{"Banda " + ext});
        }
        gerar(ff, pasta.resolve("09 Banda Sem Tag - Faixa Pelo Nome.mp3"), "libmp3lame", null, null, 1.2);
        esperado.put("Faixa Pelo Nome", new String[]{"09 Banda Sem Tag"});
        byte[] lixo = new byte[4096];
        new Random(1).nextBytes(lixo);
        Files.write(pasta.resolve("10 quebrado.mp3"), lixo);           // corrompido: tem de pular
        Path sub = Files.createDirectories(pasta.resolve("subpasta"));
        gerar(ff, sub.resolve("11 sub.flac"), "flac", "Musica Da Subpasta", "Banda Sub", 1.2);
        esperado.put("Musica Da Subpasta", new String[]{"Banda Sub"});
        Files.writeString(pasta.resolve("capa.txt"), "ignorado");

        System.setProperty("metalspace.playlist", pasta.toString());
        if (RadioMetal.aleatorio()) RadioMetal.alternarAleatorio();
        ok(RadioMetal.listarMusicas(pasta.toFile()).size() == 11, "lista as musicas e ignora o .txt");
        Map<String, String> visto = new LinkedHashMap<>();
        AtomicBoolean pulouQuebrado = new AtomicBoolean();
        RadioMetal.tocarPlaylist();
        long fim = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < fim && !visto.keySet().containsAll(esperado.keySet())) {
            String m = RadioMetal.musica(), art = RadioMetal.artista(), st = RadioMetal.status();
            if (!m.isEmpty()) visto.putIfAbsent(m, art);
            if (st.contains("QUEBRADO")) pulouQuebrado.set(true);
            Thread.sleep(3);
        }
        for (Map.Entry<String, String[]> e : esperado.entrySet())
            ok(e.getValue()[0].equals(visto.get(e.getKey())), "tocou e mostrou '" + e.getKey() + "' / '" + e.getValue()[0] + "' (visto: " + visto + ")");
        ok(pulouQuebrado.get(), "avisou e pulou o arquivo corrompido");
        ok(RadioMetal.modo() == RadioMetal.Modo.PLAYLIST, "continua na playlist depois do corrompido");

        // F11 na playlist: pula para a proxima sem esperar a musica acabar
        Path longa = Files.createDirectories(tmp.resolve("longa"));
        gerar(ff, longa.resolve("1.mp3"), "libmp3lame", "Longa Um", "Banda L", 40);
        gerar(ff, longa.resolve("2.mp3"), "libmp3lame", "Longa Dois", "Banda L", 40);
        System.setProperty("metalspace.playlist", longa.toString());
        RadioMetal.tocarPlaylist();
        ok(esperar(() -> "Longa Um".equals(RadioMetal.musica()), 10000), "comeca na primeira");
        Thread.sleep(300);
        RadioMetal.proxima();
        ok(esperar(() -> "Longa Dois".equals(RadioMetal.musica()), 5000), "F11 pula para a proxima");
        RadioMetal.anterior();
        ok(esperar(() -> "Longa Um".equals(RadioMetal.musica()), 5000), "F12 volta para a anterior");
    }

    static void gerar(File ff, Path destino, String codec, String titulo, String artista, double segundos) throws Exception {
        List<String> cmd = new ArrayList<>(List.of(ff.getAbsolutePath(), "-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi",
                "-i", "sine=frequency=440:duration=" + segundos, "-c:a", codec));
        if (codec.equals("libopus")) cmd.addAll(List.of("-ar", "48000"));
        if (codec.equals("pcm_s16be")) cmd.addAll(List.of("-write_id3v2", "1")); // AIFF: artista so via ID3, como nos programas de musica
        if (titulo != null) cmd.addAll(List.of("-metadata", "title=" + titulo, "-metadata", "artist=" + artista));
        else cmd.addAll(List.of("-map_metadata", "-1"));
        cmd.add(destino.toString());
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String saida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        ok(p.waitFor() == 0 && Files.size(destino) > 0, "gerou " + destino.getFileName() + ": " + saida);
    }

    // ---------------- radio falsa local ----------------
    static void radioLocal(File ff, Path tmp) throws Exception {
        Path mp3 = tmp.resolve("radio.mp3");
        gerar(ff, mp3, "libmp3lame", null, null, 30);
        byte[] audio = Files.readAllBytes(mp3);
        try (RadioFalsa r = new RadioFalsa(audio)) {
            int i = RadioMetal.adicionar("http://127.0.0.1:" + r.porta() + "/redir");
            ok(i >= RadioMetal.PADRAO.size(), "adiciona radio do jogador");
            ok(RadioMetal.adicionar("http://127.0.0.1:" + r.porta() + "/redir") == i, "nao duplica a mesma radio");
            Gravador g = new Gravador();
            g.start();
            RadioMetal.tocarRadio(i);
            // a primeira conexao cai no meio: a radio tem de avisar e reconectar sozinha
            ok(esperar(() -> r.conexoes.get() >= 2 && RadioMetal.status().equals("TOCANDO"), 40000), "reconecta sozinha: " + RadioMetal.status() + " " + g.eventos);
            g.parar = true;
            g.join(1000);
            List<String> ev = new ArrayList<>(g.eventos);
            int um = ev.indexOf("musica:Banda Local / Faixa Um"), dois = ev.indexOf("musica:Banda Local / Faixa Dois");
            int caiu = -1;
            for (int k = 0; k < ev.size(); k++) if (ev.get(k).startsWith("status:SEM SINAL")) { caiu = k; break; }
            ok(um >= 0, "radio local: mostrou artista e musica " + ev);
            ok(dois > um, "trocou o titulo quando a radio trocou de faixa " + ev);
            ok(caiu > dois, "avisou SEM SINAL quando a conexao caiu " + ev);
            ok(ev.subList(caiu, ev.size()).contains("status:TOCANDO"), "voltou a tocar depois da queda " + ev);
            ok(r.caminhos.contains("/redir") && r.caminhos.contains("/lista.pls") && r.caminhos.contains("/radio"), "seguiu redirecionamento e .pls: " + r.caminhos);
            ok("RADIO TESTE LOCAL".equals(RadioMetal.origem()), "usa o nome que a radio informa: " + RadioMetal.origem());
            ok(RadioMetal.remover(i), "remove a radio do jogador");
            ok(!RadioMetal.remover(0), "radio do jogo nao pode ser removida");
            ok(RadioMetal.modo() == RadioMetal.Modo.TRILHA, "remover a radio tocando volta para a trilha");
        }
        // porta fechada: avisa sem travar
        int fechada;
        try (ServerSocket s = new ServerSocket(0)) { fechada = s.getLocalPort(); }
        int j = RadioMetal.adicionar("http://127.0.0.1:" + fechada + "/");
        RadioMetal.tocarRadio(j);
        ok(esperar(() -> RadioMetal.status().startsWith("SEM SINAL"), 15000), "radio fora do ar avisa: " + RadioMetal.status());
        RadioMetal.remover(j);
    }

    /** Servidor ICY minimo: /redir -> /lista.pls -> /radio; a primeira conexao de /radio cai no meio. */
    static final class RadioFalsa implements Closeable {
        final ServerSocket servidor = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        final List<String> caminhos = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger conexoes = new AtomicInteger();
        final byte[] audio;

        RadioFalsa(byte[] audio) throws IOException {
            this.audio = audio;
            Thread t = new Thread(() -> {
                while (!servidor.isClosed()) {
                    try { Socket s = servidor.accept(); Thread c = new Thread(() -> atender(s)); c.setDaemon(true); c.start(); }
                    catch (IOException e) { return; }
                }
            }, "RadioFalsa");
            t.setDaemon(true);
            t.start();
        }

        int porta() { return servidor.getLocalPort(); }

        void atender(Socket s) {
            try (s; OutputStream out = s.getOutputStream()) {
                BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
                String pedido = r.readLine();
                boolean pediuMeta = false;
                for (String l = r.readLine(); l != null && !l.isEmpty(); l = r.readLine()) if (l.toLowerCase(Locale.ROOT).startsWith("icy-metadata: 1")) pediuMeta = true;
                String caminho = pedido == null ? "/" : pedido.split(" ")[1];
                caminhos.add(caminho);
                if (caminho.equals("/redir")) { out.write("HTTP/1.0 302 Found\r\nLocation: /lista.pls\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)); return; }
                if (caminho.equals("/lista.pls")) {
                    out.write(("HTTP/1.0 200 OK\r\nContent-Type: audio/x-scpls\r\n\r\n[playlist]\nNumberOfEntries=1\nFile1=http://127.0.0.1:" + porta() + "/radio\n").getBytes(StandardCharsets.ISO_8859_1));
                    return;
                }
                if (!pediuMeta) { out.write("HTTP/1.0 400 Bad Request\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)); return; }
                int numero = conexoes.incrementAndGet();
                int metaint = 8192;
                out.write(("ICY 200 OK\r\nicy-name: Radio Teste Local\r\ncontent-type: audio/mpeg\r\nicy-metaint: " + metaint + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
                int limite = numero == 1 ? audio.length * 2 / 3 : Integer.MAX_VALUE; // 1a conexao cai a 2/3 do audio
                int enviado = 0, bloco = 0;
                while (enviado < limite) {
                    int ini = enviado % audio.length, k = Math.min(metaint, audio.length - ini);
                    byte[] pedaco = new byte[metaint];
                    System.arraycopy(audio, ini, pedaco, 0, k);
                    if (k < metaint) System.arraycopy(audio, 0, pedaco, k, metaint - k);
                    out.write(pedaco);
                    enviado += metaint;
                    byte[] meta = bloco == 0 ? icy("Banda Local - Faixa Um") : bloco == 8 ? icy("Banda Local - Faixa Dois") : new byte[0];
                    out.write(meta.length / 16);
                    out.write(meta);
                    bloco++;
                    Thread.sleep(100); // ritmo de radio: Faixa Dois no bloco 8, queda da 1a conexao a 2/3 do audio (~bloco 20)
                }
            } catch (Exception e) { /* cliente fechou */ }
        }

        @Override public void close() throws IOException { servidor.close(); }
    }

    // ---------------- trocas rapidas ----------------
    static void trocasRapidas(File ff, Path tmp) throws Exception {
        byte[] audio = Files.readAllBytes(tmp.resolve("radio.mp3"));
        try (RadioFalsa r = new RadioFalsa(audio)) {
            int local = RadioMetal.adicionar("http://127.0.0.1:" + r.porta() + "/radio");
            for (int i = 0; i < 30; i++) {
                if (i % 3 == 0) RadioMetal.tocarPlaylist(); else if (i % 3 == 1) RadioMetal.tocarRadio(local); else RadioMetal.tocarTrilha();
                Thread.sleep(15 + (i % 5) * 20);
            }
            RadioMetal.tocarRadio(local);
            ok(esperar(() -> RadioMetal.status().equals("TOCANDO") && !RadioMetal.musica().isEmpty(), 15000), "depois das trocas a radio ainda toca: " + RadioMetal.status());
            RadioMetal.remover(local);
        }
        RadioMetal.tocarTrilha();
        ok(esperar(() -> ProcessHandle.current().descendants().noneMatch(p -> p.info().command().orElse("").toLowerCase(Locale.ROOT).contains("ffmpeg")), 10000),
                "nenhum ffmpeg fica rodando depois de 30 trocas rapidas");
    }

    /** Anota cada status e cada musica que aparecem na tela, na ordem (nao perde eventos rapidos). */
    static final class Gravador extends Thread {
        volatile boolean parar;
        final List<String> eventos = Collections.synchronizedList(new ArrayList<>());
        Gravador() { super("Teste-Gravador"); setDaemon(true); }
        @Override public void run() {
            String st = null, mu = null;
            while (!parar) {
                String s = RadioMetal.status(), m = RadioMetal.artista() + " / " + RadioMetal.musica();
                if (!s.equals(st)) { eventos.add("status:" + s); st = s; }
                if (!m.equals(mu)) { eventos.add("musica:" + m); mu = m; }
                try { Thread.sleep(2); } catch (InterruptedException e) { return; }
            }
        }
    }

    // ---------------- utilidades ----------------
    interface Condicao { boolean ok() throws Exception; }

    static boolean esperar(Condicao c, long ms) throws Exception {
        long fim = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < fim) { if (c.ok()) return true; Thread.sleep(20); }
        return c.ok();
    }

    static void apagar(Path p) {
        try (var s = Files.walk(p)) { s.sorted(Comparator.reverseOrder()).forEach(x -> x.toFile().delete()); } catch (IOException e) { /* temporario */ }
    }
}
