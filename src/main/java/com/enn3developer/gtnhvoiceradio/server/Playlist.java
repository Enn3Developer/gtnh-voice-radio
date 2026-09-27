package com.enn3developer.gtnhvoiceradio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.enn3developer.gtnhvoiceradio.GtnhVoiceRadio;
import com.enn3developer.gtnhvoiceradio.audio.Http;
import com.enn3developer.gtnhvoiceradio.audio.Mp3Stream;

/**
 * Loads a station playlist: an M3U - extended or not - or a plain URL-per-line list. {@code #EXTINF} supplies
 * the title and duration; entries without a positive duration are downloaded once and measured. Blocking, so it
 * runs off the server thread.
 */
final class Playlist {

    private static final String EXTINF = "#EXTINF:";

    private Playlist() {}

    static List<Track> load(String playlistUrl) throws IOException {
        List<Track> tracks = new ArrayList<>();
        long pendingDuration = -1;
        String pendingTitle = null;

        for (String raw : fetchLines(playlistUrl)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith(EXTINF)) {
                int comma = line.indexOf(',');
                pendingDuration = parseSeconds(line.substring(EXTINF.length(), comma < 0 ? line.length() : comma));
                pendingTitle = comma < 0 ? null
                    : line.substring(comma + 1)
                        .trim();
                continue;
            }
            if (line.startsWith("#")) continue;

            try {
                String url = Http.resolve(playlistUrl, line);
                String title = pendingTitle == null || pendingTitle.isEmpty() ? titleFromUrl(url) : pendingTitle;
                long duration = pendingDuration > 0 ? pendingDuration : Mp3Stream.measureMillis(url);
                if (duration <= 0) throw new IOException("no audio frames");
                tracks.add(new Track(url, title, duration));
            } catch (IOException e) {
                GtnhVoiceRadio.LOG.warn("[Radio] Skipping playlist entry '{}': {}", line, e.getMessage());
            }
            pendingDuration = -1;
            pendingTitle = null;
        }
        return tracks;
    }

    private static List<String> fetchLines(String url) throws IOException {
        List<String> lines = new ArrayList<>();
        try (InputStream in = Http.open(url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(lines.isEmpty() ? line.replace("﻿", "") : line);
            }
        }
        return lines;
    }

    private static long parseSeconds(String value) {
        try {
            return Math.round(Double.parseDouble(value.trim()) * 1000);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String titleFromUrl(String url) {
        String path = URI.create(url)
            .getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
