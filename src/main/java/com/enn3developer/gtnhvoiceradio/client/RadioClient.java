package com.enn3developer.gtnhvoiceradio.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.world.WorldEvent;

import org.jetbrains.annotations.Nullable;

import com.enn3developer.gtnhvoice.api.client.GtnhVoiceClient;
import com.enn3developer.gtnhvoice.api.client.IAddonSource;
import com.enn3developer.gtnhvoice.api.client.VoiceFormat;
import com.enn3developer.gtnhvoiceradio.Config;
import com.enn3developer.gtnhvoiceradio.GtnhVoiceRadio;
import com.enn3developer.gtnhvoiceradio.audio.Mp3Stream;
import com.enn3developer.gtnhvoiceradio.block.TileRadio;
import com.enn3developer.gtnhvoiceradio.network.NowPlayingPacket;
import com.enn3developer.gtnhvoiceradio.server.Track;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * Plays the station through one gtnh-voice addon source parked on the nearest powered radio within range. One
 * source rather than one per radio: every radio plays the same stream, and several sources a few hundred ms
 * apart would just echo.
 * <p>
 * Threads: the client thread keeps the loaded-radio set and publishes the nearest powered one each tick; the
 * network thread publishes station updates; a daemon player thread streams, decodes and writes, paced by the
 * source's blocking write. The hand-offs are two volatile fields, both replaced wholesale.
 */
public final class RadioClient {

    public static final RadioClient INSTANCE = new RadioClient();

    private static final long IDLE_MILLIS = 100L;
    private static final long NO_SESSION_BACKOFF_MILLIS = 1_000L;
    private static final long ERROR_BACKOFF_MILLIS = 5_000L;
    private static final long ERROR_LOG_INTERVAL_MILLIS = 30_000L;
    // A re-sent update (e.g. on login) this close to the playing one is the same broadcast - don't restart.
    private static final long RESYNC_TOLERANCE_MILLIS = 1_500L;
    private static final short[] SILENCE = new short[VoiceFormat.FRAME_SAMPLES];

    /** The station's current track, anchored to this client's clock on receipt. */
    private record NowPlaying(Track track, long startMillis) {

        long elapsedMillis() {
            return System.currentTimeMillis() - startMillis;
        }

        boolean sameBroadcast(@Nullable NowPlaying other) {
            return other != null && track.url()
                .equals(other.track.url()) && Math.abs(startMillis - other.startMillis) < RESYNC_TOLERANCE_MILLIS;
        }
    }

    // Client thread only.
    private final Set<TileRadio> radios = new HashSet<>();

    private volatile @Nullable NowPlaying nowPlaying;
    private volatile double @Nullable [] nearestRadio;

    private final AtomicLong lastErrorLogMillis = new AtomicLong();
    private IAddonSource source;

    private RadioClient() {}

    public void start() {
        source = GtnhVoiceClient.addon("GTNH Voice Radio")
            .description("In-world radios tuned to the server's station")
            .register()
            .source()
            .distance(Config.range)
            .gain(Config.volume / 100f)
            .create();

        Thread thread = new Thread(this::runPlayer, "gtnhvoiceradio-player");
        thread.setDaemon(true);
        thread.start();
    }

    public void radioLoaded(TileRadio radio) {
        radios.add(radio);
    }

    public void radioUnloaded(TileRadio radio) {
        radios.remove(radio);
    }

    /** Network thread. */
    public void nowPlaying(NowPlayingPacket packet) {
        Track track = packet.track();
        NowPlaying update = track == null ? null
            : new NowPlaying(track, System.currentTimeMillis() - packet.elapsedMillis());
        if (update != null && update.sameBroadcast(nowPlaying)) return;
        nowPlaying = update;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            nearestRadio = null;
            return;
        }

        double best = (double) Config.range * Config.range;
        TileRadio nearest = null;
        for (TileRadio radio : radios) {
            if (!radio.isOn() || radio.isInvalid() || radio.getWorldObj() != mc.theWorld) continue;
            double distance = player.getDistanceSq(radio.xCoord + 0.5, radio.yCoord + 0.5, radio.zCoord + 0.5);
            if (distance < best) {
                best = distance;
                nearest = radio;
            }
        }

        double[] current = nearestRadio;
        if (nearest == null) {
            nearestRadio = null;
        } else if (current == null || current[0] != nearest.xCoord + 0.5
            || current[1] != nearest.yCoord + 0.5
            || current[2] != nearest.zCoord + 0.5) {
                nearestRadio = new double[] { nearest.xCoord + 0.5, nearest.yCoord + 0.5, nearest.zCoord + 0.5 };
            }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) radios.removeIf(radio -> radio.getWorldObj() == event.world);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        nowPlaying = null;
    }

    private void runPlayer() {
        Mp3Stream stream = null;
        // The broadcast `stream` belongs to (or that finished); null forces a (re)open synced to the station.
        NowPlaying streaming = null;
        double[] placedAt = null;
        boolean noSession = false;

        while (true) {
            try {
                NowPlaying station = nowPlaying;
                double[] radio = nearestRadio;
                if (station == null || radio == null) {
                    if (stream != null) {
                        stream = close(stream);
                        source.flush();
                    }
                    streaming = null;
                    Thread.sleep(IDLE_MILLIS);
                    continue;
                }

                if (radio != placedAt) {
                    source.setPosition(radio[0], radio[1], radio[2]);
                    placedAt = radio;
                }

                // No voice session: probe with silence instead of re-downloading the track every retry.
                if (noSession) {
                    if (!source.write(SILENCE)) {
                        Thread.sleep(NO_SESSION_BACKOFF_MILLIS);
                        continue;
                    }
                    noSession = false;
                    streaming = null;
                }

                if (station != streaming) {
                    stream = close(stream);
                    source.flush();
                    streaming = station;
                    stream = openSynced(station);
                }
                if (stream == null) {
                    // Track over; waiting for the station to announce the next one.
                    Thread.sleep(IDLE_MILLIS);
                    continue;
                }

                short[] frame = stream.nextFrame();
                if (frame == null) {
                    stream = close(stream);
                } else if (!source.write(frame)) {
                    stream = close(stream);
                    noSession = true;
                }
            } catch (InterruptedException e) {
                close(stream);
                return;
            } catch (IOException | RuntimeException e) {
                logThrottled(e);
                stream = close(stream);
                streaming = null;
                if (!sleepQuietly(ERROR_BACKOFF_MILLIS)) return;
            }
        }
    }

    /**
     * Opens the track at the station's position, then skips again by however long the open took - skipping means
     * downloading, which can take seconds deep into a track on a slow link. Null if the track is already over.
     */
    private static @Nullable Mp3Stream openSynced(NowPlaying station) throws IOException {
        long offset = station.elapsedMillis();
        if (offset >= station.track()
            .durationMillis()) return null;

        Mp3Stream stream = Mp3Stream.open(
            station.track()
                .url(),
            offset);
        long lag = station.elapsedMillis() - offset;
        if (lag > VoiceFormat.FRAME_MILLIS) stream.skipMillis(lag);
        return stream;
    }

    private void logThrottled(Exception e) {
        long now = System.currentTimeMillis();
        long last = lastErrorLogMillis.get();
        if (now - last < ERROR_LOG_INTERVAL_MILLIS || !lastErrorLogMillis.compareAndSet(last, now)) return;
        GtnhVoiceRadio.LOG.warn("[Radio] Playback failed, retrying in {}s", ERROR_BACKOFF_MILLIS / 1000, e);
    }

    private static @Nullable Mp3Stream close(@Nullable Mp3Stream stream) {
        if (stream == null) return null;
        try {
            stream.close();
        } catch (IOException ignored) {
            // Nothing left to salvage from a stream being dropped.
        }
        return null;
    }

    private static boolean sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
