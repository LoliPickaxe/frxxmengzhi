package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.GangQiClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class GangQiStatePacket implements IMessage {

    private boolean enabled;
    private int mode;

    public GangQiStatePacket() {
    }

    public GangQiStatePacket(boolean enabled, int mode) {
        this.enabled = enabled;
        this.mode = mode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        enabled = buf.readBoolean();
        mode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeInt(mode);
    }

    public static class Handler implements IMessageHandler<GangQiStatePacket, IMessage> {

        @Override
        public IMessage onMessage(final GangQiStatePacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    GangQiClientHandler.update(message.enabled, message.mode);
                }
            });
            return null;
        }
    }
}