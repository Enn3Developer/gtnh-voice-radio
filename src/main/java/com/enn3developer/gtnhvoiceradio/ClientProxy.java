package com.enn3developer.gtnhvoiceradio;

import net.minecraftforge.common.MinecraftForge;

import com.enn3developer.gtnhvoiceradio.block.TileRadio;
import com.enn3developer.gtnhvoiceradio.client.RadioClient;
import com.enn3developer.gtnhvoiceradio.network.NowPlayingPacket;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        RadioClient client = RadioClient.INSTANCE;
        client.start();
        FMLCommonHandler.instance()
            .bus()
            .register(client);
        MinecraftForge.EVENT_BUS.register(client);
    }

    @Override
    public void radioLoaded(TileRadio radio) {
        RadioClient.INSTANCE.radioLoaded(radio);
    }

    @Override
    public void radioUnloaded(TileRadio radio) {
        RadioClient.INSTANCE.radioUnloaded(radio);
    }

    @Override
    public void nowPlaying(NowPlayingPacket packet) {
        RadioClient.INSTANCE.nowPlaying(packet);
    }
}
