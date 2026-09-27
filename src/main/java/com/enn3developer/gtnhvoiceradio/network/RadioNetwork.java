package com.enn3developer.gtnhvoiceradio.network;

import com.enn3developer.gtnhvoiceradio.GtnhVoiceRadio;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class RadioNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(GtnhVoiceRadio.MODID);

    private RadioNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(NowPlayingPacket.Handler.class, NowPlayingPacket.class, 0, Side.CLIENT);
    }
}
