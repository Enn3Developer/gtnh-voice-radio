package com.enn3developer.gtnhvoiceradio.audio;

import java.util.Arrays;

/** Minimal growable FIFO of PCM samples, bridging variable-size decoder output to fixed-size frames. */
final class ShortQueue {

    private short[] buffer = new short[8192];
    private int start;
    private int end;

    int size() {
        return end - start;
    }

    void add(short sample) {
        ensureRoom(1);
        buffer[end++] = sample;
    }

    void add(short[] samples, int length) {
        ensureRoom(length);
        System.arraycopy(samples, 0, buffer, end, length);
        end += length;
    }

    /** Moves exactly {@code target.length} samples into {@code target}; the caller checks {@link #size()} first. */
    void take(short[] target) {
        System.arraycopy(buffer, start, target, 0, target.length);
        start += target.length;
    }

    private void ensureRoom(int extra) {
        if (end + extra <= buffer.length) return;

        int size = size();
        if (size + extra > buffer.length) buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, size + extra));
        System.arraycopy(buffer, start, buffer, 0, size);
        start = 0;
        end = size;
    }
}
