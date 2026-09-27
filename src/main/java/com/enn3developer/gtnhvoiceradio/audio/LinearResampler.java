package com.enn3developer.gtnhvoiceradio.audio;

/**
 * Streaming linear-interpolation resampler for mono 16-bit PCM. Crude next to a windowed-sinc, but for music
 * coming out of a Minecraft radio it's inaudible, and it carries its phase across blocks so there are no seams.
 * A no-op copy when the rates match (the common 48kHz case).
 */
final class LinearResampler {

    private final double step;
    // Position of the next output sample, in input samples, relative to the current block where index -1 is
    // `previous` (the last sample of the prior block).
    private double position;
    private short previous;

    LinearResampler(int inputRate, int outputRate) {
        this.step = (double) inputRate / outputRate;
    }

    void process(short[] input, int length, ShortQueue output) {
        if (step == 1.0) {
            output.add(input, length);
            return;
        }

        while (position < length) {
            int index = (int) position;
            double fraction = position - index;
            double a = index == 0 ? previous : input[index - 1];
            double b = input[index];
            output.add((short) Math.round(a + (b - a) * fraction));
            position += step;
        }
        position -= length;
        if (length > 0) previous = input[length - 1];
    }
}
