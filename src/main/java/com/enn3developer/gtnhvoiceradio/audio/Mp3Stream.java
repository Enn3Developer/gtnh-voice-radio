package com.enn3developer.gtnhvoiceradio.audio;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import org.jetbrains.annotations.Nullable;

import com.enn3developer.gtnhvoice.api.client.VoiceFormat;

/**
 * An MP3 streamed over HTTP and decoded into {@link VoiceFormat} frames: mono (stereo is downmixed), resampled to
 * 48kHz, {@link VoiceFormat#FRAME_SAMPLES} samples each. Seeking skips whole MP3 frames without decoding them,
 * so it only costs the download.
 */
public final class Mp3Stream implements Closeable {

    private static final int PRIME_FRAMES = 4;

    private final InputStream input;
    private final Bitstream bitstream;
    private final Decoder decoder = new Decoder();
    private final ShortQueue pending = new ShortQueue();
    private @Nullable LinearResampler resampler;
    private short[] mono = new short[0];

    private Mp3Stream(InputStream input) {
        this.input = input;
        this.bitstream = new Bitstream(input);
    }

    /** Opens {@code url} and skips the first {@code offsetMillis} of audio. */
    public static Mp3Stream open(String url, long offsetMillis) throws IOException {
        Mp3Stream stream = new Mp3Stream(Http.open(url));
        try {
            stream.skip(offsetMillis);
            return stream;
        } catch (IOException | RuntimeException e) {
            stream.close();
            throw e;
        }
    }

    /**
     * Downloads {@code url} once and returns its exact duration, summed frame by frame - the only reliable way for
     * VBR files without a Xing header. Nothing is decoded.
     */
    public static long measureMillis(String url) throws IOException {
        try (Mp3Stream stream = new Mp3Stream(Http.open(url))) {
            return stream.skip(Long.MAX_VALUE);
        }
    }

    /** The next frame, or {@code null} at the end of the track. */
    public short @Nullable [] nextFrame() throws IOException {
        while (pending.size() < VoiceFormat.FRAME_SAMPLES) {
            if (!decodeOne()) return null;
        }
        short[] frame = new short[VoiceFormat.FRAME_SAMPLES];
        pending.take(frame);
        return frame;
    }

    /** Skips up to {@code millis} of audio without decoding it - e.g. to catch up after a slow open. */
    public void skipMillis(long millis) throws IOException {
        skip(millis);
    }

    /**
     * Skips up to {@code millis} of audio; returns how much was actually skipped (less at end of stream). Layer III
     * frames borrow bits from the frames before them (the bit reservoir), so the decoder emits nothing for the
     * first few frames after a raw skip - the last {@value #PRIME_FRAMES} frames before the target are therefore
     * decoded and thrown away to prime it, instead of silently losing ~100ms of audio past the target.
     */
    private long skip(long millis) throws IOException {
        double skipped = 0;
        double primeFrom = Double.NaN;
        while (skipped < millis) {
            Header header = readHeader();
            if (header == null) break;

            double frame = frameMillis(header);
            if (Double.isNaN(primeFrom)) primeFrom = millis - PRIME_FRAMES * frame;
            try {
                if (skipped >= primeFrom) decoder.decodeFrame(header, bitstream);
            } catch (DecoderException e) {
                // Priming is best-effort.
            } finally {
                bitstream.closeFrame();
            }
            skipped += frame;
        }
        return (long) skipped;
    }

    private boolean decodeOne() throws IOException {
        Header header = readHeader();
        if (header == null) return false;

        try {
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            appendDownmixed(output, header.frequency());
        } catch (DecoderException e) {
            // A corrupt frame only costs its own ~25ms; the stream carries on.
        } finally {
            bitstream.closeFrame();
        }
        return true;
    }

    private void appendDownmixed(SampleBuffer output, int frequency) {
        if (resampler == null) resampler = new LinearResampler(frequency, VoiceFormat.SAMPLE_RATE);

        short[] samples = output.getBuffer();
        int channels = output.getChannelCount();
        // getBufferLength() counts interleaved samples across all channels.
        int frames = output.getBufferLength() / channels;
        if (mono.length < frames) mono = new short[frames];

        if (channels == 1) {
            System.arraycopy(samples, 0, mono, 0, frames);
        } else {
            for (int i = 0; i < frames; i++) {
                mono[i] = (short) ((samples[2 * i] + samples[2 * i + 1]) >> 1);
            }
        }
        resampler.process(mono, frames, pending);
    }

    private @Nullable Header readHeader() throws IOException {
        try {
            return bitstream.readFrame();
        } catch (BitstreamException e) {
            throw new IOException("bad MP3 stream", e);
        }
    }

    /** Duration of one MP3 frame: layer I holds 384 samples, layer II 1152, layer III 1152 (MPEG-1) or 576. */
    private static double frameMillis(Header header) {
        int samples = switch (header.layer()) {
            case 1 -> 384;
            case 2 -> 1152;
            default -> header.version() == Header.MPEG1 ? 1152 : 576;
        };
        return samples * 1000.0 / header.frequency();
    }

    @Override
    public void close() throws IOException {
        try {
            bitstream.close();
        } catch (BitstreamException e) {
            // Closing the raw stream below is what matters.
        }
        input.close();
    }
}
