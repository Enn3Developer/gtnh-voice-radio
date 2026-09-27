package com.enn3developer.gtnhvoiceradio;

import com.enn3developer.gtnhvoiceradio.block.BlockRadio;
import com.enn3developer.gtnhvoiceradio.block.TileRadio;
import com.enn3developer.gtnhvoiceradio.compat.GregTechRecipes;
import com.enn3developer.gtnhvoiceradio.network.NowPlayingPacket;
import com.enn3developer.gtnhvoiceradio.network.RadioNetwork;
import com.enn3developer.gtnhvoiceradio.server.RadioCommand;
import com.enn3developer.gtnhvoiceradio.server.RadioStation;
import com.enn3developer.gtnhvoiceradio.server.ServerEvents;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public static BlockRadio radio;

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        radio = new BlockRadio();
        GameRegistry.registerBlock(radio, "radio");
        GameRegistry.registerTileEntity(TileRadio.class, GtnhVoiceRadio.MODID + ":radio");
        RadioNetwork.init();
    }

    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerEvents());
    }

    /** GregTech's items exist by now; without GregTech the radio has no recipe (creative/NEI only). */
    public void postInit(FMLPostInitializationEvent event) {
        if (Loader.isModLoaded("gregtech")) GregTechRecipes.register();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new RadioCommand());
        RadioStation.INSTANCE.reload();
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        RadioStation.INSTANCE.stop();
    }

    /** A radio tile entity came into existence in a client world - no-op on the server side. */
    public void radioLoaded(TileRadio radio) {}

    /** A radio tile entity left a client world (broken or chunk unloaded) - no-op on the server side. */
    public void radioUnloaded(TileRadio radio) {}

    /** Station update from the server - no-op on the server side. */
    public void nowPlaying(NowPlayingPacket packet) {}
}
