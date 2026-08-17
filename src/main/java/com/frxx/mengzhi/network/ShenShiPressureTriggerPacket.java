package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.ShenShiPressureHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ShenShiPressureTriggerPacket implements IMessage {

    private boolean sneak;

    public ShenShiPressureTriggerPacket() {
    }

    public ShenShiPressureTriggerPacket(boolean sneak) {
        this.sneak = sneak;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        sneak = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(sneak);
    }

    public static class Handler implements IMessageHandler<ShenShiPressureTriggerPacket, IMessage> {

        @Override
        public IMessage onMessage(final ShenShiPressureTriggerPacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    ShenShiPressureHandler.cast(player, message.sneak);
                }
            });
            return null;
        }
    }
}