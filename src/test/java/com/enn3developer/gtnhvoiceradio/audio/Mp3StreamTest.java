package com.enn3developer.gtnhvoiceradio.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.enn3developer.gtnhvoice.api.client.VoiceFormat;

/** Runs the real decoder over a 2s 440Hz tone encoded at 44.1kHz stereo - downmix and resampling included. */
class Mp3StreamTest {

    static String fixture(String name) throws URISyntaxException {
        return Mp3StreamTest.class.getResource("/fixtures/" + name)
            .toURI()
            .toString();
    }

    @Test
    void measuresTheDuration() throws Exception {
        long millis = Mp3Stream.measureMillis(fixture("tone 440.mp3"));
        // LAME pads a frame or two of encoder delay onto the 2000ms of tone.
        assertTrue(millis >= 2000 && millis <= 2100, "measured " + millis + "ms");
    }

    @Test
    void decodesTheWholeTrackIntoVoiceFrames() throws Exception {
        int frames = countFrames(0);
        assertTrue(frames >= 99 && frames <= 105, "decoded " + frames + " frames");
    }

    @Test
    void seekingSkipsTheOffset() throws Exception {
        // 1s is 50 voice frames; seeking rounds up to whole ~26ms MP3 frames, so allow one of those on top.
        int skipped = countFrames(0) - countFrames(1000);
        assertTrue(skipped >= 50 && skipped <= 52, "a 1s seek skipped " + skipped + " frames");
    }

    @Test
    void decodedAudioIsA440HzTone() throws Exception {
        try (Mp3Stream stream = Mp3Stream.open(fixture("tone 440.mp3"), 0)) {
            // Skip the start (encoder delay, fade-in) and look at 0.5s of steady tone.
            for (int i = 0; i < 10; i++) stream.nextFrame();
            int crossings = 0;
            int peak = 0;
            short previous = 0;
            for (int f = 0; f < 25; f++) {
                for (short sample : stream.nextFrame()) {
                    if ((previous < 0) != (sample < 0)) crossings++;
                    peak = Math.max(peak, Math.abs(sample));
                    previous = sample;
                }
            }
            double hz = crossings / 2.0 / (25 * VoiceFormat.FRAME_MILLIS / 1000.0);
            assertEquals(440, hz, 5, "frequency");
            assertTrue(peak > 1000, "peak " + peak);
        }
    }

    private static int countFrames(long offsetMillis) throws IOException, URISyntaxException {
        int frames = 0;
        try (Mp3Stream stream = Mp3Stream.open(fixture("tone 440.mp3"), offsetMillis)) {
            short[] frame;
            while ((frame = stream.nextFrame()) != null) {
                assertEquals(VoiceFormat.FRAME_SAMPLES, frame.length);
                frames++;
            }
        }
        return frames;
    }
}
