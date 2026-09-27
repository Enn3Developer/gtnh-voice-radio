package com.enn3developer.gtnhvoiceradio.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.entity.player.EntityPlayerMP;

import org.jetbrains.annotations.Nullable;

import com.enn3developer.gtnhvoiceradio.Config;
import com.enn3developer.gtnhvoiceradio.GtnhVoiceRadio;
import com.enn3developer.gtnhvoiceradio.network.NowPlayingPacket;
import com.enn3developer.gtnhvoiceradio.network.RadioNetwork;

/**
 * The one station every radio is tuned to. It keeps playing whether or not anyone listens - like a real radio -
 * on the wall clock rather than ticks, so server lag never stretches a song. All state is owned by the server
 * thread; the playlist loads on a background thread and is handed over through a volatile field.
 */
public final class RadioStation {

    public static final RadioStation INSTANCE = new RadioStation();

    private volatile @Nullable List<Track> loadedPlaylist;
    private volatile int loadGeneration;

    private List<Track> playlist = Collections.emptyList();
    private final List<Integer> order = new ArrayList<>();
    private int orderPosition = -1;
    private @Nullable Track current;
    private long trackStartMillis;

    private RadioStation() {}

    /** (Re)loads the configured playlist in the background; the station keeps its current track until it lands. */
    public void reload() {
        String url = Config.playlistUrl.trim();
        int generation = ++loadGeneration;
        if (url.isEmpty()) {
            GtnhVoiceRadio.LOG.info("[Radio] No playlistUrl configured - the station is off air");
            loadedPlaylist = Collections.emptyList();
            return;
        }

        GtnhVoiceRadio.LOG.info("[Radio] Loading playlist {}", url);
        CompletableFuture.supplyAsync(() -> {
            try {
                return Playlist.load(url);
            } catch (Exception e) {
                GtnhVoiceRadio.LOG.error("[Radio] Failed to load playlist {}", url, e);
                return null;
            }
        })
            .thenAccept(tracks -> {
                // A newer reload (or a server stop) supersedes this one.
                if (tracks == null || generation != loadGeneration) return;
                GtnhVoiceRadio.LOG.info("[Radio] Playlist loaded: {} tracks", tracks.size());
                loadedPlaylist = tracks;
            });
    }

    public void stop() {
        loadGeneration++;
        loadedPlaylist = null;
        playlist = Collections.emptyList();
        order.clear();
        orderPosition = -1;
        current = null;
    }

    /** Server thread, every tick: adopts a freshly loaded playlist and advances past finished tracks. */
    public void tick() {
        List<Track> loaded = loadedPlaylist;
        if (loaded != null) {
            loadedPlaylist = null;
            playlist = loaded;
            order.clear();
            orderPosition = -1;
            advance();
            return;
        }

        if (current != null && System.currentTimeMillis() - trackStartMillis >= current.durationMillis()) advance();
    }

    /** Skips to the next track (server thread). */
    public void skip() {
        if (!playlist.isEmpty()) advance();
    }

    /** The current track's title, or {@code null} when off air. Server thread. */
    public @Nullable String currentTitle() {
        return current == null ? null : current.title();
    }

    public void sendTo(EntityPlayerMP player) {
        RadioNetwork.CHANNEL.sendTo(nowPlaying(), player);
    }

    private void advance() {
        if (playlist.isEmpty()) {
            current = null;
        } else {
            if (++orderPosition >= order.size()) reshuffle();
            current = playlist.get(order.get(orderPosition));
            trackStartMillis = System.currentTimeMillis();
            GtnhVoiceRadio.LOG.info("[Radio] Now playing: {}", current.title());
        }
        RadioNetwork.CHANNEL.sendToAll(nowPlaying());
    }

    private void reshuffle() {
        Integer last = order.isEmpty() ? null : order.get(order.size() - 1);
        order.clear();
        for (int i = 0; i < playlist.size(); i++) order.add(i);
        if (Config.shuffle) {
            Collections.shuffle(order);
            // Never repeat a song across the reshuffle boundary.
            if (order.size() > 1 && order.get(0)
                .equals(last)) Collections.swap(order, 0, order.size() - 1);
        }
        orderPosition = 0;
    }

    private NowPlayingPacket nowPlaying() {
        return current == null ? new NowPlayingPacket(null, 0)
            : new NowPlayingPacket(current, System.currentTimeMillis() - trackStartMillis);
    }
}
