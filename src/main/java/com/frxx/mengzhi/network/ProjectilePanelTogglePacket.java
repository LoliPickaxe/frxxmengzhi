package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.ProjectilePanelHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ProjectilePanelTogglePacket implements IMessage {

    public ProjectilePanelTogglePacket() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<ProjectilePanelTogglePacket, IMessage> {

        @Override
        public IMessage onMessage(final ProjectilePanelTogglePacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    ProjectilePanelHandler.togglePanelDamage(player);
                }
            });
            return null;
        }
    }
}