package Jogo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

/**
 * Radio do jogo: troca a musica de fundo por uma estacao de metal da internet ou pela playlist do jogador
 * (pasta musicas/playlist, qualquer formato que o FFmpeg leia). Tudo roda em threads proprias: se a radio
 * cair, o arquivo estiver corrompido ou o FFmpeg faltar, o jogo segue e a tela mostra o motivo.
 */
public final class RadioMetal {
    public enum Modo { TRILHA, RADIO, PLAYLIST }

    static final class Estacao {
        final String nome, url; final boolean doJogador;
        Estacao(String nome, String url, boolean doJogador) { this.nome = nome; this.url = url; this.doJogador = doJogador; }
    }

    /** Estacoes de metal que vem com o jogo (conferidas pelo TesteRadiosAoVivo). */
    static final List<Estacao> PADRAO = List.of(
            new Estacao("SOMAFM METAL DETECTOR", "https://ice5.somafm.com/metal-128-aac", false),
            new Estacao("FIP METAL", "https://icecast.radiofrance.fr/fipmetal-hifi.aac?id=radiofrance", false),
            new Estacao("ROCK ANTENNE HEAVY METAL", "https://stream.rockantenne.de/heavy-metal/stream/mp3", false),
            new Estacao("ROCK ANTENNE MODERN METAL", "https://stream.rockantenne.de/modern-metal/stream/mp3", false),
            new Estacao("RADIO BOB METAL", "http://streams.radiobob.de/bob-metal/mp3-192/streams.radiobob.de/", false),
            new Estacao("RADIO BOB POWER METAL", "http://streams.radiobob.de/powermetal/mp3-192/streams.radiobob.de/", false),
            new Estacao("RADIO BOB NU METAL", "https://streams.radiobob.de/numetal/mp3-192/", false),
            new Estacao("RADIO BOB GOTHIC", "http://streams.radiobob.de/gothic/mp3-192/streams.radiobob.de/", false),
            new Estacao("LA GROSSE RADIO METAL", "http://hd.lagrosseradio.info/lagrosseradio-metal-192.mp3", false),
            new Estacao("BADROCK HARD & HEAVY", "https://streams.badrockradio.net/hard-heavy", false),
            new Estacao("CHRONIX METALCORE", "http://usa17.fastcast4u.com:5508/stream", false),
            new Estacao("BIG R 80S METAL", "http://bigrradio.cdnstream1.com/5186_128", false));

    /** Os testes usam outro no (-Dmetalspace.radio.prefs) para nao mexer nas escolhas salvas do jogador. */
    private static final Preferences PREFS = Preferences.userNodeForPackage(RadioMetal.class).node(System.getProperty("metalspace.radio.prefs", "radio"));
    private static final List<Estacao> DO_JOGADOR = new ArrayList<>();
    private static final AtomicInteger GERACAO = new AtomicInteger();
    private static final float GANHO = .8f; // folga para a radio nao estourar junto com os efeitos
    /** Abrir o ffmpeg.exe leva uma fracao de segundo no Windows e um processo abre por vez: nao abrir a toa. */
    private static final long ESPERA_TROCA_MS = 150;

    private static volatile Modo modo = Modo.TRILHA;
    private static volatile int estacao;
    private static volatile boolean aleatorio = true;
    private static volatile String status = "", musica = "", artista = "", origem = "TRILHA DO JOGO";
    private static volatile long mudouEm;
    private static volatile int faixaPedida = Integer.MIN_VALUE;
    private static volatile Process processo;
    private static volatile Closeable conexao;
    private static volatile FonteContinua fonte;

    static {
        try {
            for (String l : PREFS.get("minhas", "").split("\n")) {
                int p = l.indexOf('|');
                if (p > 0) DO_JOGADOR.add(new Estacao(l.substring(0, p), l.substring(p + 1), true));
            }
            aleatorio = PREFS.getBoolean("aleatorio", true);
        } catch (Exception e) { /* preferencias indisponiveis: segue com o padrao */ }
    }

    private RadioMetal() { }

    // ---------------- consultas para a tela ----------------
    static Modo modo() { return modo; }
    static int estacaoAtual() { return estacao; }
    static boolean aleatorio() { return aleatorio; }
    static String status() { return status; }
    static String musica() { return musica; }
    static String artista() { return artista; }
    /** Nome da estacao, "PLAYLIST" ou "TRILHA DO JOGO". */
    static String origem() { return origem; }
    /** Momento (ms) em que a musica ou a fonte mudou: a tela mostra o aviso por alguns segundos. */
    static long mudouEm() { return mudouEm; }
    static synchronized List<Estacao> estacoes() { List<Estacao> l = new ArrayList<>(PADRAO); l.addAll(DO_JOGADOR); return l; }
    static FonteContinua fonteAtual() { return fonte; }

    // ---------------- comandos ----------------
    /** Volta para a fonte salva na ultima vez (chamado ao abrir o jogo). */
    static void retomar() {
        try {
            Modo m = Modo.valueOf(PREFS.get("modo", "TRILHA"));
            int e = PREFS.getInt("estacao", 0);
            if (m == Modo.RADIO) tocarRadio(e);
            else if (m == Modo.PLAYLIST) tocarPlaylist();
        } catch (Exception e) { tocarTrilha(); }
    }

    static synchronized void tocarTrilha() {
        parar();
        modo = Modo.TRILHA; origem = "TRILHA DO JOGO"; status = ""; musica = artista = "";
        mudouEm = System.currentTimeMillis();
        salvar();
    }

    static synchronized void tocarRadio(int i) {
        List<Estacao> l = estacoes();
        parar();
        modo = Modo.RADIO;
        estacao = Math.floorMod(i, l.size());
        Estacao e = l.get(estacao);
        origem = e.nome; musica = artista = ""; status = "CONECTANDO...";
        mudouEm = System.currentTimeMillis();
        salvar();
        iniciar("MetalSpace-Radio", g -> executarRadio(g, e));
    }

    static synchronized void tocarPlaylist() {
        parar();
        modo = Modo.PLAYLIST; origem = "PLAYLIST"; musica = artista = ""; status = "PROCURANDO MUSICAS...";
        mudouEm = System.currentTimeMillis();
        salvar();
        iniciar("MetalSpace-Playlist", RadioMetal::executarPlaylist);
    }

    /** F11: proxima estacao (radio) ou proxima musica (playlist); na trilha, liga a radio. */
    static void proxima() {
        if (modo == Modo.PLAYLIST) pular(+1);
        else if (modo == Modo.RADIO) tocarRadio(estacao + 1);
        else tocarRadio(estacao);
    }

    /** F12: estacao ou musica anterior. */
    static void anterior() {
        if (modo == Modo.PLAYLIST) pular(-1);
        else if (modo == Modo.RADIO) tocarRadio(estacao - 1);
        else tocarRadio(estacao);
    }

    static void alternarAleatorio() { aleatorio = !aleatorio; salvar(); }

    /** Adiciona uma radio pelo link (o nome vem da propria estacao na primeira conexao). Devolve o indice. */
    static synchronized int adicionar(String link) {
        String url = link == null ? "" : link.trim();
        if (!(url.startsWith("http://") || url.startsWith("https://")) || url.contains("|") || url.contains("\n")) return -1;
        for (int i = 0; i < estacoes().size(); i++) if (estacoes().get(i).url.equals(url)) return i;
        String nome = url.replaceFirst("^https?://", "");
        int b = nome.indexOf('/');
        if (b > 0) nome = nome.substring(0, b);
        DO_JOGADOR.add(new Estacao(nome.toUpperCase(Locale.ROOT), url, true));
        salvar();
        return PADRAO.size() + DO_JOGADOR.size() - 1;
    }

    /** Remove uma radio adicionada pelo jogador (as do jogo nao saem). */
    static synchronized boolean remover(int i) {
        int k = i - PADRAO.size();
        if (k < 0 || k >= DO_JOGADOR.size()) return false;
        boolean tocando = modo == Modo.RADIO && estacao == i;
        DO_JOGADOR.remove(k);
        salvar();
        if (tocando) tocarTrilha();
        else if (modo == Modo.RADIO && estacao > i) estacao--;
        return true;
    }

    static File pastaPlaylist() { return new File(System.getProperty("metalspace.playlist", "musicas" + File.separator + "playlist")); }

    /** Abre a pasta da playlist no Explorer (cria se nao existir). */
    static void abrirPasta() {
        Thread t = new Thread(() -> {
            try { File p = pastaPlaylist(); p.mkdirs(); java.awt.Desktop.getDesktop().open(p); }
            catch (Exception e) { status = "NAO CONSEGUI ABRIR A PASTA: " + pastaPlaylist().getPath(); }
        }, "MetalSpace-Pasta");
        t.setDaemon(true);
        t.start();
    }

    /** Onde esta o ffmpeg: -Dmetalspace.ffmpeg, a pasta ffmpeg ao lado do jogo ou o PATH. */
    static File ffmpeg() {
        String p = System.getProperty("metalspace.ffmpeg");
        if (p != null) { File f = new File(p); return f.isFile() ? f : null; }
        List<File> lugares = new ArrayList<>();
        lugares.add(new File("ffmpeg", "ffmpeg.exe"));
        lugares.add(new File("ffmpeg", "ffmpeg"));
        try {
            File jar = new File(RadioMetal.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = jar.getParentFile(); // jar: pasta do jogo; Eclipse (bin/): pasta do projeto
            if (dir != null) { lugares.add(new File(dir, "ffmpeg" + File.separator + "ffmpeg.exe")); lugares.add(new File(dir, "ffmpeg" + File.separator + "ffmpeg")); }
        } catch (Exception e) { /* sem local do jar */ }
        for (File f : lugares) if (f.isFile()) return f;
        String path = System.getenv("PATH");
        if (path != null) for (String d : path.split(File.pathSeparator)) {
            for (String n : new String[]{"ffmpeg.exe", "ffmpeg"}) { File f = new File(d, n); if (f.isFile()) return f; }
        }
        return null;
    }

    // ---------------- execucao ----------------
    private interface Tarefa { void rodar(int geracao) throws Exception; }

    private static void iniciar(String nome, Tarefa tarefa) {
        int g = GERACAO.incrementAndGet();
        FonteContinua f = new FonteContinua(12, 3.0);
        fonte = f;
        SomJogo.fonteMusica(f, GANHO);
        Thread t = new Thread(() -> {
            try {
                // F11 apertado varias vezes seguidas: so a ultima escolha chega a abrir o ffmpeg
                Thread.sleep(ESPERA_TROCA_MS);
                if (vigente(g)) tarefa.rodar(g);
            }
            catch (InterruptedException e) { /* parou */ }
            catch (Exception e) { if (vigente(g)) status = "ERRO: " + resumo(e); }
            finally { f.fechar(); }
        }, nome);
        t.setDaemon(true);
        t.start();
    }

    private static boolean vigente(int g) { return GERACAO.get() == g; }

    /** Para a fonte atual: fecha a conexao, mata o ffmpeg e devolve a musica para a trilha. */
    private static void parar() {
        GERACAO.incrementAndGet();
        FonteContinua f = fonte;
        fonte = null;
        if (f != null) f.fechar();
        fecharAtual();
        SomJogo.fonteMusica(null, 1);
    }

    private static void fecharAtual() {
        Closeable c = conexao; conexao = null;
        if (c != null) try { c.close(); } catch (IOException e) { /* ja fechada */ }
        Process p = processo; processo = null;
        if (p != null) p.destroyForcibly();
    }

    private static void executarRadio(int g, Estacao e) throws Exception {
        int tentativa = 0;
        while (vigente(g)) {
            File ff = ffmpeg();
            if (ff == null) { status = "RADIO INDISPONIVEL: FFMPEG NAO ENCONTRADO"; return; }
            status = tentativa == 0 ? "CONECTANDO..." : "RECONECTANDO...";
            long tocou = 0;
            try {
                tocou = tocarEstacao(g, e, ff);
                if (tocou > FonteContinua.TAXA * 5L) tentativa = 0; // tocou de verdade antes de cair
            } catch (IOException ex) {
                if (!vigente(g)) return;
                status = "SEM SINAL: " + resumo(ex);
            }
            if (!vigente(g)) return;
            tentativa++;
            int espera = Math.min(30, 2 << Math.min(tentativa, 4)); // 4, 8, 16, 30 s
            for (int s = espera; s > 0 && vigente(g); s--) {
                status = "SEM SINAL - TENTANDO DE NOVO EM " + s + "s";
                Thread.sleep(1000);
            }
        }
    }

    /** Toca a estacao ate a conexao cair; devolve quantas amostras saíram. */
    private static long tocarEstacao(int g, Estacao e, File ff) throws Exception {
        String u = e.url.toLowerCase(Locale.ROOT);
        if (u.contains(".m3u8")) return tocarDireto(g, e.url, ff); // HLS: o proprio ffmpeg busca os pedacos
        IcyCliente c = IcyCliente.abrir(e.url, titulo -> { if (vigente(g)) definirTitulo(titulo); });
        synchronized (RadioMetal.class) {
            if (!vigente(g)) { c.close(); return 0; }
            conexao = c;
        }
        if (!c.nome.isEmpty() && e.doJogador && e.nome.equals(nomePeloHost(e.url))) renomear(e, c.nome);
        Process p = processo(g, ff, "-i", "pipe:0");
        if (p == null) { c.close(); return 0; }
        Thread bomba = new Thread(() -> {
            try (OutputStream out = p.getOutputStream()) { c.audio.transferTo(out); }
            catch (IOException ex) { /* conexao ou ffmpeg encerrados */ }
            finally { try { c.close(); } catch (IOException ex) { /* ja fechada */ } }
        }, "MetalSpace-Radio-Rede");
        bomba.setDaemon(true);
        bomba.start();
        status = "TOCANDO";
        long n = bombear(g, p);
        if (!vigente(g)) return n;
        if (n == 0) throw new IOException(erroDo(p, "a radio nao mandou audio"));
        throw new IOException("a conexao caiu");
    }

    private static long tocarDireto(int g, String url, File ff) throws Exception {
        Process p = processo(g, ff, "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5", "-i", url);
        if (p == null) return 0;
        status = "TOCANDO";
        long n = bombear(g, p);
        if (!vigente(g)) return n;
        throw new IOException(n == 0 ? erroDo(p, "a radio nao mandou audio") : "a conexao caiu");
    }

    private static void executarPlaylist(int g) throws Exception {
        File ff = ffmpeg();
        if (ff == null) { status = "PLAYLIST INDISPONIVEL: FFMPEG NAO ENCONTRADO"; return; }
        File pasta = pastaPlaylist();
        List<File> arquivos = listarMusicas(pasta);
        if (arquivos.isEmpty()) { status = "PASTA VAZIA: COLOQUE MUSICAS EM " + pasta.getPath().toUpperCase(Locale.ROOT); return; }
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < arquivos.size(); i++) ordem.add(i);
        if (aleatorio) Collections.shuffle(ordem);
        int pos = 0, falhasSeguidas = 0;
        while (vigente(g)) {
            if (falhasSeguidas >= arquivos.size()) { status = "NENHUMA MUSICA DA PASTA PODE SER TOCADA"; return; }
            File arq = arquivos.get(ordem.get(Math.floorMod(pos, ordem.size())));
            if (!vigente(g)) return;
            String[] info = metadados(ff, arq);
            if (!vigente(g)) return;
            definirFaixa(info[0], info[1]);
            status = "TOCANDO";
            faixaPedida = Integer.MIN_VALUE;
            Process p = processo(g, ff, "-i", arq.getAbsolutePath());
            if (p == null) return;
            long n = bombear(g, p);
            if (!vigente(g)) return;
            int pedido = faixaPedida;
            if (pedido != Integer.MIN_VALUE) {          // F11/F12: pula sem esperar o fim
                FonteContinua f = fonte;
                if (f != null) f.limpar();
                pos += pedido;
                falhasSeguidas = 0;
                continue;
            }
            if (n == 0) {                               // formato nao suportado ou arquivo corrompido
                falhasSeguidas++;
                status = "NAO CONSEGUI TOCAR: " + arq.getName().toUpperCase(Locale.ROOT);
                Thread.sleep(1500);
            } else falhasSeguidas = 0;
            pos++;
        }
    }

    private static void pular(int d) {
        faixaPedida = d;
        Process p = processo;
        if (p != null) p.destroyForcibly();
    }

    /** Todas as musicas da pasta (e subpastas), em ordem de nome; o ffmpeg decide o que consegue tocar. */
    static List<File> listarMusicas(File pasta) {
        List<File> l = new ArrayList<>();
        if (!pasta.isDirectory()) return l;
        try (var s = Files.walk(pasta.toPath(), 4)) {
            s.filter(Files::isRegularFile).map(Path::toFile)
                    .filter(f -> !f.getName().startsWith(".") && !ignorado(f.getName()))
                    .sorted(Comparator.comparing(f -> f.getPath().toLowerCase(Locale.ROOT)))
                    .forEach(l::add);
        } catch (IOException e) { /* pasta inacessivel */ }
        return l;
    }

    private static boolean ignorado(String n) {
        String x = n.toLowerCase(Locale.ROOT);
        for (String ext : new String[]{".txt", ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".ini", ".db", ".nfo", ".cue", ".log", ".m3u", ".pls", ".lrc", ".url", ".pdf"})
            if (x.endsWith(ext)) return true;
        return false;
    }

    /**
     * {musica, artista}: das tags do arquivo (gerais ou do fluxo de audio, como no OGG/OPUS);
     * sem tags, do nome "Artista - Musica.ext". Le o relatorio que o ffmpeg imprime ao abrir o arquivo.
     */
    static String[] metadados(File ff, File arq) {
        String titulo = "", autor = "";
        try {
            Process p = new ProcessBuilder(ff.getAbsolutePath(), "-hide_banner", "-nostdin", "-i", arq.getAbsolutePath())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            byte[] b;
            try (InputStream in = p.getErrorStream()) { b = in.readNBytes(262144); }
            if (!p.waitFor(10, TimeUnit.SECONDS)) p.destroyForcibly();
            boolean emMetadados = false;
            for (String l : new String(b, StandardCharsets.UTF_8).split("\\r?\\n")) {
                String t = l.trim();
                if (t.equals("Metadata:")) { emMetadados = true; continue; }
                int i = t.indexOf(':');
                if (!emMetadados || i <= 0 || !l.startsWith("    ")) { emMetadados = t.equals("Metadata:"); continue; }
                String k = t.substring(0, i).trim().toLowerCase(Locale.ROOT), v = t.substring(i + 1).trim();
                if (v.isEmpty()) continue;
                if (k.equals("title") && titulo.isEmpty()) titulo = v;
                else if ((k.equals("artist") || k.equals("album_artist") || k.equals("author")) && autor.isEmpty()) autor = v;
            }
        } catch (Exception e) { /* sem tags: usa o nome do arquivo */ }
        if (titulo.isEmpty()) {
            String n = arq.getName();
            int ponto = n.lastIndexOf('.');
            if (ponto > 0) n = n.substring(0, ponto);
            int sep = n.indexOf(" - ");
            if (sep > 0 && autor.isEmpty()) { autor = n.substring(0, sep).trim(); titulo = n.substring(sep + 3).trim(); }
            else titulo = n.trim();
        }
        return new String[]{titulo, autor};
    }

    /**
     * Inicia o ffmpeg decodificando a entrada para PCM 16 bits mono 44,1 kHz na saida padrao.
     * Devolve null (sem abrir nada) se a fonte ja foi trocada; se for trocada enquanto abre, fecha na hora.
     */
    private static Process processo(int g, File ff, String... entrada) throws IOException {
        if (!vigente(g)) return null;
        List<String> cmd = new ArrayList<>(List.of(ff.getAbsolutePath(), "-hide_banner", "-loglevel", "error"));
        if (!Arrays.asList(entrada).contains("pipe:0")) cmd.add("-nostdin");
        cmd.addAll(Arrays.asList(entrada));
        cmd.addAll(List.of("-vn", "-f", "s16le", "-acodec", "pcm_s16le", "-ac", "1", "-ar", String.valueOf(FonteContinua.TAXA), "pipe:1"));
        Process p = new ProcessBuilder(cmd).start();
        synchronized (RadioMetal.class) {          // mesma trava das trocas: ou registra para a fonte atual, ou fecha
            if (!vigente(g)) { p.destroyForcibly(); return null; }
            processo = p;
        }
        Thread erros = new Thread(() -> guardarErro(p), "MetalSpace-FFmpeg-Erros");
        erros.setDaemon(true);
        erros.start();
        return p;
    }

    private static final Map<Process, String> ERROS = Collections.synchronizedMap(new WeakHashMap<>());

    private static void guardarErro(Process p) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
            String l, ultima = null;
            while ((l = r.readLine()) != null) if (!l.isBlank()) ultima = l.trim();
            if (ultima != null) ERROS.put(p, ultima);
        } catch (IOException e) { /* processo encerrado */ }
    }

    private static String erroDo(Process p, String padrao) {
        try { p.waitFor(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String e = ERROS.get(p);
        return e == null ? padrao : e;
    }

    /** Le o PCM do ffmpeg e entrega ao mixador; devolve quantas amostras passaram. */
    private static long bombear(int g, Process p) throws InterruptedException {
        FonteContinua f = fonte;
        byte[] b = new byte[8192];
        float[] a = new float[4096];
        long total = 0;
        int sobra = -1;
        try (InputStream in = p.getInputStream()) {
            int n;
            while (vigente(g) && (n = in.read(b)) > 0) {
                int i = 0, k = 0;
                if (sobra >= 0) { a[k++] = (short) ((sobra & 255) | (b[0] << 8)) / 32768f; i = 1; sobra = -1; }
                for (; i + 1 < n; i += 2) a[k++] = (short) ((b[i] & 255) | (b[i + 1] << 8)) / 32768f;
                if (i < n) sobra = b[i];
                if (f == null || !f.escrever(a, k)) break;
                total += k;
            }
        } catch (IOException e) { /* processo encerrado */ }
        finally {
            if (p.isAlive()) p.destroyForcibly();
            try { p.waitFor(3, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return total;
    }

    /** Titulo ICY "Artista - Musica" (algumas radios mandam so o texto). */
    static void definirTitulo(String t) {
        String s = t == null ? "" : t.trim();
        int sep = s.indexOf(" - ");
        if (sep > 0) definirFaixa(s.substring(sep + 3).trim(), s.substring(0, sep).trim());
        else definirFaixa(s, "");
    }

    private static void definirFaixa(String m, String a) {
        if (m.equals(musica) && a.equals(artista)) return;
        musica = m; artista = a;
        mudouEm = System.currentTimeMillis();
    }

    private static String nomePeloHost(String url) {
        String n = url.replaceFirst("^https?://", "");
        int b = n.indexOf('/');
        return (b > 0 ? n.substring(0, b) : n).toUpperCase(Locale.ROOT);
    }

    private static synchronized void renomear(Estacao e, String nome) {
        int k = DO_JOGADOR.indexOf(e);
        if (k < 0) return;
        String n = nome.replace('|', ' ').replace('\n', ' ').trim().toUpperCase(Locale.ROOT);
        if (n.length() > 40) n = n.substring(0, 40);
        if (n.isEmpty()) return;
        DO_JOGADOR.set(k, new Estacao(n, e.url, true));
        if (modo == Modo.RADIO && estacao == PADRAO.size() + k) origem = n;
        salvar();
    }

    private static synchronized void salvar() {
        try {
            StringBuilder b = new StringBuilder();
            for (Estacao e : DO_JOGADOR) b.append(e.nome).append('|').append(e.url).append('\n');
            PREFS.put("minhas", b.toString());
            PREFS.put("modo", modo.name());
            PREFS.putInt("estacao", estacao);
            PREFS.putBoolean("aleatorio", aleatorio);
            PREFS.flush();
        } catch (Exception e) { /* preferencias indisponiveis */ }
    }

    private static String resumo(Throwable e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        m = m.toUpperCase(Locale.ROOT);
        return m.length() > 60 ? m.substring(0, 60) : m;
    }
}
