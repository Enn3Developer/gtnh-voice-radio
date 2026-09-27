package com.enn3developer.gtnhvoiceradio.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlaylistTest {

    @Test
    void parsesExtendedAndPlainEntriesAndSkipsBrokenOnes() throws Exception {
        String url = PlaylistTest.class.getResource("/fixtures/playlist.m3u")
            .toURI()
            .toString();

        List<Track> tracks = Playlist.load(url);

        assertEquals(2, tracks.size(), "missing.mp3 is skipped");
        assertEquals(
            "Tone With Title",
            tracks.get(0)
                .title());
        assertEquals(
            2000,
            tracks.get(0)
                .durationMillis(),
            "EXTINF duration is taken as-is");
        assertEquals(
            "tone 440",
            tracks.get(1)
                .title());
        assertTrue(
            tracks.get(1)
                .durationMillis() >= 2000,
            "measured duration");
        assertTrue(
            tracks.get(1)
                .url()
                .endsWith("/fixtures/tone%20440.mp3"),
            tracks.get(1)
                .url());
    }
}
