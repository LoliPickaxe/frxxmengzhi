package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.CaoKongClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CaoKongStatePacket implements IMessage {

    private boolean enabled;
    private double distance;
    private int heldEntities;

    public CaoKongStatePacket() {
    }

    public CaoKongStatePacket(boolean enabled, double distance, int heldEntities) {
        this.enabled = enabled;
        this.distance = distance;
        this.heldEntities = heldEntities;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        enabled = buf.readBoolean();
        distance = buf.readDouble();
        heldEntities = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeDouble(distance);
        buf.writeInt(heldEntities);
    }

    public static class Handler implements IMessageHandler<CaoKongStatePacket, IMessage> {

        @Override
        public IMessage onMessage(final CaoKongStatePacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    CaoKongClientHandler.update(message.enabled, message.distance, message.heldEntities);
                }
            });
            return null;
        }
    }
}