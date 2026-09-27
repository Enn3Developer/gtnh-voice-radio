package com.enn3developer.gtnhvoiceradio.network;

import org.jetbrains.annotations.Nullable;

import com.enn3developer.gtnhvoiceradio.GtnhVoiceRadio;
import com.enn3developer.gtnhvoiceradio.server.Track;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server to client: the station's current track and how far into it the station is, or "off air" when no track
 * is set. Sent on login and on every track change. Elapsed time rather than a timestamp, so client and server
 * clocks never have to agree - the client anchors it to its own clock on receipt.
 */
public class NowPlayingPacket implements IMessage {

    private @Nullable Track track;
    private long elapsedMillis;

    public NowPlayingPacket() {}

    public NowPlayingPacket(@Nullable Track track, long elapsedMillis) {
        this.track = track;
        this.elapsedMillis = elapsedMillis;
    }

    public @Nullable Track track() {
        return track;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(track != null);
        if (track == null) return;
        ByteBufUtils.writeUTF8String(buf, track.url());
        ByteBufUtils.writeUTF8String(buf, track.title());
        buf.writeLong(track.durationMillis());
        buf.writeLong(elapsedMillis);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (!buf.readBoolean()) return;
        track = new Track(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf), buf.readLong());
        elapsedMillis = buf.readLong();
    }

    public static class Handler implements IMessageHandler<NowPlayingPacket, IMessage> {

        @Override
        public IMessage onMessage(NowPlayingPacket message, MessageContext ctx) {
            GtnhVoiceRadio.proxy.nowPlaying(message);
            return null;
        }
    }
}
