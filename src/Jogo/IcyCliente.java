package Jogo;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.SSLSocketFactory;

/**
 * Conexao com uma radio da internet (Icecast/Shoutcast, http ou https).
 * Pede os metadados ICY, segue redirecionamentos e links de playlist (.pls/.m3u) e entrega
 * so o audio, avisando o titulo "Artista - Musica" sempre que a estacao troca de faixa.
 */
final class IcyCliente implements Closeable {
    interface Ouvinte { void titulo(String titulo); }

    static final int TEMPO_CONEXAO = 8000, TEMPO_LEITURA = 12000;
    private static final int MAX_SALTOS = 6;

    final InputStream audio;
    /** Nome que a propria estacao informa (icy-name), ou vazio. */
    final String nome;
    final String tipo;
    private final Socket socket;

    private IcyCliente(Socket s, InputStream audio, String nome, String tipo) {
        socket = s; this.audio = audio; this.nome = nome; this.tipo = tipo;
    }

    static IcyCliente abrir(String endereco, Ouvinte ouvinte) throws IOException {
        String atual = endereco.trim();
        for (int salto = 0; salto < MAX_SALTOS; salto++) {
            URI u;
            try { u = new URI(atual); } catch (URISyntaxException e) { throw new IOException("Link invalido: " + atual); }
            String esquema = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
            if (!esquema.equals("http") && !esquema.equals("https")) throw new IOException("So links http/https: " + atual);
            boolean seguro = esquema.equals("https");
            String host = u.getHost();
            if (host == null) throw new IOException("Link sem servidor: " + atual);
            int porta = u.getPort() > 0 ? u.getPort() : seguro ? 443 : 80;
            String caminho = u.getRawPath() == null || u.getRawPath().isEmpty() ? "/" : u.getRawPath();
            if (u.getRawQuery() != null) caminho += "?" + u.getRawQuery();

            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(host, porta), TEMPO_CONEXAO);
                if (seguro) s = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(s, host, porta, true);
                s.setSoTimeout(TEMPO_LEITURA);
                boolean portaPadrao = porta == (seguro ? 443 : 80);
                String pedido = "GET " + caminho + " HTTP/1.0\r\n"
                        + "Host: " + host + (portaPadrao ? "" : ":" + porta) + "\r\n"
                        + "User-Agent: MetalSpace/0.9.8\r\n"
                        + "Accept: */*\r\n"
                        + "Icy-MetaData: 1\r\n"
                        + "Connection: close\r\n\r\n";
                OutputStream out = s.getOutputStream();
                out.write(pedido.getBytes(StandardCharsets.ISO_8859_1));
                out.flush();

                InputStream in = new BufferedInputStream(s.getInputStream(), 16384);
                String status = linha(in);
                String[] partes = status.split(" ");
                if (partes.length < 2) throw new IOException("Resposta invalida da radio");
                int codigo;
                try { codigo = Integer.parseInt(partes[1]); } catch (NumberFormatException e) { throw new IOException("Resposta invalida da radio"); }
                Map<String, String> cab = new HashMap<>();
                for (String l = linha(in); !l.isEmpty(); l = linha(in)) {
                    int p = l.indexOf(':');
                    if (p > 0) cab.put(l.substring(0, p).trim().toLowerCase(Locale.ROOT), l.substring(p + 1).trim());
                }
                if (codigo >= 300 && codigo < 400 && cab.containsKey("location")) {
                    s.close();
                    atual = u.resolve(cab.get("location")).toString();
                    continue;
                }
                if (codigo != 200) throw new IOException("Radio respondeu " + codigo);

                String tipo = cab.getOrDefault("content-type", "").toLowerCase(Locale.ROOT);
                if (ehPlaylist(tipo, caminho)) {
                    String proximo = primeiroLink(in);
                    s.close();
                    if (proximo == null) throw new IOException("Playlist sem link de audio");
                    atual = u.resolve(proximo).toString();
                    continue;
                }
                int metaint = 0;
                try { metaint = Integer.parseInt(cab.getOrDefault("icy-metaint", "0").trim()); } catch (NumberFormatException e) { metaint = 0; }
                InputStream audio = metaint > 0 ? new FiltroMetadados(in, metaint, ouvinte) : in;
                return new IcyCliente(s, audio, decodificar(cab.getOrDefault("icy-name", "").getBytes(StandardCharsets.ISO_8859_1)), tipo);
            } catch (IOException e) {
                try { s.close(); } catch (IOException ignorado) { }
                throw e;
            }
        }
        throw new IOException("Redirecionamentos demais");
    }

    @Override public void close() throws IOException { socket.close(); }

    private static boolean ehPlaylist(String tipo, String caminho) {
        String c = caminho.toLowerCase(Locale.ROOT);
        int q = c.indexOf('?');
        if (q >= 0) c = c.substring(0, q);
        return tipo.contains("mpegurl") || tipo.contains("scpls") || tipo.contains("x-pls")
                || ((c.endsWith(".pls") || c.endsWith(".m3u")) && !tipo.startsWith("audio/mpeg"));
    }

    /** Primeiro link de audio dentro de um .pls (FileN=...) ou .m3u (linha com http). */
    static String primeiroLink(InputStream in) throws IOException {
        byte[] b = in.readNBytes(65536);
        for (String l : new String(b, StandardCharsets.UTF_8).split("\\r?\\n")) {
            String t = l.trim();
            int p = t.indexOf('=');
            if (t.toLowerCase(Locale.ROOT).startsWith("file") && p > 0) t = t.substring(p + 1).trim();
            if (t.startsWith("http://") || t.startsWith("https://")) return t;
        }
        return null;
    }

    private static String linha(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1 && c != '\n') {
            if (c != '\r') b.write(c);
            if (b.size() > 8192) throw new IOException("Cabecalho grande demais");
        }
        if (c == -1 && b.size() == 0) throw new EOFException("Radio fechou a conexao");
        return b.toString(StandardCharsets.ISO_8859_1);
    }

    /** UTF-8 quando valido; senao Latin-1 (muitas radios antigas). */
    static String decodificar(byte[] b) {
        String s = new String(b, StandardCharsets.UTF_8);
        return s.indexOf('�') >= 0 ? new String(b, StandardCharsets.ISO_8859_1) : s;
    }

    /** Extrai o StreamTitle de um bloco de metadados ICY: StreamTitle='Artista - Musica';StreamUrl='...'; */
    static String tituloDe(String meta) {
        String chave = "StreamTitle='";
        int i = meta.indexOf(chave);
        if (i < 0) return null;
        i += chave.length();
        int f = meta.indexOf("';", i);
        if (f < 0) f = meta.lastIndexOf('\'');
        if (f < i) f = meta.length();
        return meta.substring(i, f).trim();
    }

    /** Tira os blocos de metadados do meio do audio (a cada 'metaint' bytes vem 1 byte de tamanho x16). */
    static final class FiltroMetadados extends FilterInputStream {
        private final int metaint;
        private final Ouvinte ouvinte;
        private int restante;

        FiltroMetadados(InputStream in, int metaint, Ouvinte ouvinte) {
            super(in); this.metaint = metaint; this.ouvinte = ouvinte; restante = metaint;
        }

        @Override public int read() throws IOException {
            byte[] b = new byte[1];
            int n = read(b, 0, 1);
            return n <= 0 ? -1 : b[0] & 255;
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (restante == 0) {
                int tam = in.read();
                if (tam < 0) return -1;
                if (tam > 0) {
                    byte[] meta = in.readNBytes(tam * 16);
                    if (meta.length < tam * 16) return -1;
                    int fim = meta.length;
                    while (fim > 0 && meta[fim - 1] == 0) fim--;
                    String t = tituloDe(decodificar(Arrays.copyOf(meta, fim)));
                    if (t != null && ouvinte != null) ouvinte.titulo(t);
                }
                restante = metaint;
            }
            int n = in.read(b, off, Math.min(len, restante));
            if (n > 0) restante -= n;
            return n;
        }
    }
}
