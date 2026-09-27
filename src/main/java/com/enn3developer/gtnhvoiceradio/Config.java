package com.enn3developer.gtnhvoiceradio;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    private static final String CATEGORY_SERVER = "server";
    private static final String CATEGORY_CLIENT = "client";

    /** Server: the station's playlist - an M3U (optionally extended) or plain list of track URLs. */
    public static String playlistUrl = "";
    public static boolean shuffle = true;

    /** Client: radio volume in percent and the distance in blocks a radio can be heard from. */
    public static int volume = 100;
    public static int range = 24;

    private static File configFile;

    public static void synchronizeConfiguration(File file) {
        configFile = file;
        Configuration configuration = new Configuration(file);

        playlistUrl = configuration.getString(
            "playlistUrl",
            CATEGORY_SERVER,
            playlistUrl,
            "URL of the station playlist: an .m3u file (#EXTINF durations are used when present, otherwise every "
                + "track is downloaded once to measure it) or a plain list of URLs, one per line. Relative entries "
                + "resolve against the playlist URL. Empty disables the station. Tracks must be MP3.");
        shuffle = configuration.getBoolean("shuffle", CATEGORY_SERVER, shuffle, "Play the playlist in random order");

        volume = configuration.getInt("volume", CATEGORY_CLIENT, volume, 0, 200, "Radio volume in percent");
        range = configuration.getInt("range", CATEGORY_CLIENT, range, 4, 64, "Blocks a radio can be heard from");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    /** Re-reads the config file picked up at pre-init - for {@code /radio reload}. */
    public static void reload() {
        synchronizeConfiguration(configFile);
    }
}
