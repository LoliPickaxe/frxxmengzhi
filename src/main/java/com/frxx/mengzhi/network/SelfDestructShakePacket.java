package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.SelfDestructClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SelfDestructShakePacket implements IMessage {

    private int ticks;
    private float amplitude;
    private int color;

    public SelfDestructShakePacket() {
    }

    public SelfDestructShakePacket(int ticks, float amplitude, int color) {
        this.ticks = ticks;
        this.amplitude = amplitude;
        this.color = color;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        ticks = buf.readInt();
        amplitude = buf.readFloat();
        color = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(ticks);
        buf.writeFloat(amplitude);
        buf.writeInt(color);
    }

    public static class Handler implements IMessageHandler<SelfDestructShakePacket, IMessage> {

        @Override
        public IMessage onMessage(final SelfDestructShakePacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    SelfDestructClientHandler.onShake(message.ticks, message.amplitude, message.color);
                }
            });
            return null;
        }
    }
}
