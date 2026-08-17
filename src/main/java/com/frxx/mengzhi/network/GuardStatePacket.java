package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.GuardClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class GuardStatePacket implements IMessage {

    private boolean guardOn;
    private double guard;
    private double guardMax;

    public GuardStatePacket() {
    }

    public GuardStatePacket(boolean guardOn, double guard, double guardMax) {
        this.guardOn = guardOn;
        this.guard = guard;
        this.guardMax = guardMax;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        guardOn = buf.readBoolean();
        guard = buf.readDouble();
        guardMax = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(guardOn);
        buf.writeDouble(guard);
        buf.writeDouble(guardMax);
    }

    public static class Handler implements IMessageHandler<GuardStatePacket, IMessage> {

        @Override
        public IMessage onMessage(final GuardStatePacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    GuardClientHandler.update(message.guardOn, message.guard, message.guardMax);
                }
            });
            return null;
        }
    }
}
