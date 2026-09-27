package com.enn3developer.gtnhvoiceradio.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HttpTest {

    private static final String BASE = "https://files.example.org/music/playlist.m3u";

    @Test
    void encodesRawEntries() throws Exception {
        assertEquals(
            "https://files.example.org/music/L%E2%80%99Ombra%20del%20Warp.mp3",
            Http.resolve(BASE, "L’Ombra del Warp.mp3"));
    }

    @Test
    void keepsAlreadyEncodedEntries() throws Exception {
        assertEquals("https://files.example.org/music/Zero%20Joule.mp3", Http.resolve(BASE, "Zero%20Joule.mp3"));
    }

    @Test
    void keepsAbsoluteEntries() throws Exception {
        assertEquals("https://cdn.example.org/a.mp3", Http.resolve(BASE, "https://cdn.example.org/a.mp3"));
    }
}
