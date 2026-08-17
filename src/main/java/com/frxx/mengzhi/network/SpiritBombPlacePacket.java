package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.SpiritBombHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SpiritBombPlacePacket implements IMessage {

    public static final byte TYPE_ENTITY = 0;
    public static final byte TYPE_BLOCK = 1;
    public static final byte TYPE_ITEM = 2;

    private byte bombType;
    private int targetId;
    private int x;
    private int y;
    private int z;

    public SpiritBombPlacePacket() {
    }

    public SpiritBombPlacePacket(byte bombType, int targetId, int x, int y, int z) {
        this.bombType = bombType;
        this.targetId = targetId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        bombType = buf.readByte();
        targetId = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(bombType);
        buf.writeInt(targetId);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public static class Handler implements IMessageHandler<SpiritBombPlacePacket, IMessage> {

        @Override
        public IMessage onMessage(final SpiritBombPlacePacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    SpiritBombHandler.placeBomb(player, message.bombType, message.targetId, message.x, message.y, message.z);
                }
            });
            return null;
        }
    }
}
