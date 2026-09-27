package com.enn3developer.gtnhvoiceradio.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import com.enn3developer.gtnhvoiceradio.Tags;

public final class Http {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private static final int BUFFER_BYTES = 64 * 1024;

    private Http() {}

    /**
     * Opens a buffered stream for {@code url}. HTTP(S) must answer 2xx; other schemes (e.g. {@code file:} for a
     * playlist kept on the server's disk) are read as-is.
     */
    public static InputStream open(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "gtnh-voice-radio/" + Tags.VERSION);

        if (connection instanceof HttpURLConnection http) {
            http.setInstanceFollowRedirects(true);
            int status = http.getResponseCode();
            if (status / 100 != 2) {
                http.disconnect();
                throw new IOException("HTTP " + status + " for " + url);
            }
        }
        return new BufferedInputStream(connection.getInputStream(), BUFFER_BYTES);
    }

    /**
     * Resolves a playlist entry against the playlist's URL into an ASCII-safe absolute URL. Entries that already
     * contain percent-escapes are taken as encoded; anything else (raw spaces, accents) is encoded here.
     */
    public static String resolve(String base, String entry) throws IOException {
        try {
            if (entry.indexOf('%') >= 0) return new URI(base).resolve(entry)
                .toASCIIString();

            URL url = new URL(new URL(base), entry);
            return new URI(
                url.getProtocol(),
                url.getUserInfo(),
                url.getHost(),
                url.getPort(),
                url.getPath(),
                url.getQuery(),
                url.getRef()).toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new IOException("bad playlist entry '" + entry + "'", e);
        }
    }
}
