package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.SpiritBombHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SpiritBombDetonatePacket implements IMessage {

    public SpiritBombDetonatePacket() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<SpiritBombDetonatePacket, IMessage> {

        @Override
        public IMessage onMessage(final SpiritBombDetonatePacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    SpiritBombHandler.detonateAll(player);
                }
            });
            return null;
        }
    }
}
