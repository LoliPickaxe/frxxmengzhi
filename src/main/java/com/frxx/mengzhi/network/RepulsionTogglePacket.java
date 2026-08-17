package com.frxx.mengzhi.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 客户端 -> 服务端：排斥力场开/关（小键盘8） */
public class RepulsionTogglePacket implements IMessage {

    public RepulsionTogglePacket() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<RepulsionTogglePacket, IMessage> {

        @Override
        public IMessage onMessage(final RepulsionTogglePacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.frxx.mengzhi.handler.RepulsionFieldHandler.toggle(player);
                }
            });
            return null;
        }
    }
}