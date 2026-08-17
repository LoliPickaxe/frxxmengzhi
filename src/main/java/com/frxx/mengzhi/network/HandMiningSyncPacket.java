package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.HandMiningClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class HandMiningSyncPacket implements IMessage {

    private int tier;
    private boolean normalOn;
    private boolean allOn;
    private int power;

    public HandMiningSyncPacket() {
    }

    public HandMiningSyncPacket(int tier, boolean normalOn, boolean allOn, int power) {
        this.tier = tier;
        this.normalOn = normalOn;
        this.allOn = allOn;
        this.power = power;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tier = buf.readInt();
        normalOn = buf.readBoolean();
        allOn = buf.readBoolean();
        power = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(tier);
        buf.writeBoolean(normalOn);
        buf.writeBoolean(allOn);
        buf.writeInt(power);
    }

    public static class Handler implements IMessageHandler<HandMiningSyncPacket, IMessage> {

        @Override
        public IMessage onMessage(final HandMiningSyncPacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    HandMiningClientHandler.apply(message.tier, message.normalOn, message.allOn, message.power);
                }
            });
            return null;
        }
    }
}
