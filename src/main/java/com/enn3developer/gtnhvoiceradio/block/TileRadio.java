package com.enn3developer.gtnhvoiceradio.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.enn3developer.gtnhvoiceradio.GtnhVoiceRadio;

/**
 * A radio's only state is whether it's switched on; it syncs to clients through the vanilla description packet.
 * Client-side instances register themselves with the proxy while loaded, so the client can find the nearest
 * powered radio without scanning the world.
 */
public class TileRadio extends TileEntity {

    private static final String TAG_ON = "on";

    private boolean on;

    public boolean isOn() {
        return on;
    }

    /** Server side: flips the radio and pushes the new state to watching clients. Returns the new state. */
    public boolean toggle() {
        on = !on;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        return on;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void validate() {
        super.validate();
        if (worldObj != null && worldObj.isRemote) GtnhVoiceRadio.proxy.radioLoaded(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null && worldObj.isRemote) GtnhVoiceRadio.proxy.radioUnloaded(this);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (worldObj != null && worldObj.isRemote) GtnhVoiceRadio.proxy.radioUnloaded(this);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean(TAG_ON, on);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        on = tag.getBoolean(TAG_ON);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(TAG_ON, on);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        on = packet.func_148857_g()
            .getBoolean(TAG_ON);
    }
}
