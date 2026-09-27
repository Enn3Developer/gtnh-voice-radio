package com.enn3developer.gtnhvoiceradio.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class LinearResamplerTest {

    @Test
    void matchingRatesPassThrough() {
        ShortQueue out = new ShortQueue();
        new LinearResampler(48_000, 48_000).process(new short[] { 1, 2, 3 }, 3, out);
        short[] taken = new short[3];
        out.take(taken);
        assertEquals("[1, 2, 3]", Arrays.toString(taken));
    }

    @Test
    void upsamplingProducesTheRatioOfSamplesAcrossBlocks() {
        LinearResampler resampler = new LinearResampler(44_100, 48_000);
        ShortQueue out = new ShortQueue();
        short[] block = new short[1152];
        for (int i = 0; i < 100; i++) resampler.process(block, block.length, out);

        double expected = 100 * 1152 * 48_000.0 / 44_100;
        assertTrue(Math.abs(out.size() - expected) <= 1, "got " + out.size() + ", expected ~" + expected);
    }

    @Test
    void constantSignalStaysConstantAcrossBlockSeams() {
        LinearResampler resampler = new LinearResampler(44_100, 48_000);
        ShortQueue out = new ShortQueue();
        short[] block = new short[100];
        Arrays.fill(block, (short) 1000);
        for (int i = 0; i < 5; i++) resampler.process(block, block.length, out);

        short[] taken = new short[out.size()];
        out.take(taken);
        // Only the very first samples interpolate up from the implicit leading silence.
        for (int i = 2; i < taken.length; i++) assertEquals(1000, taken[i], "sample " + i);
    }
}
