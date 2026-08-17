package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.GangQiHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class GangQiActionPacket implements IMessage {

    public enum Action {
        TOGGLE,
        MODE
    }

    private Action action;

    public GangQiActionPacket() {
    }

    public GangQiActionPacket(Action action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int id = buf.readInt();
        Action[] values = Action.values();
        this.action = (id >= 0 && id < values.length) ? values[id] : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action == null ? 0 : action.ordinal());
    }

    public static class Handler implements IMessageHandler<GangQiActionPacket, IMessage> {

        @Override
        public IMessage onMessage(final GangQiActionPacket message, MessageContext ctx) {
            if (message.action == null) {
                return null;
            }
            final EntityPlayerMP player = ctx.getServerHandler().player;
            final Action action = message.action;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    GangQiHandler.handleAction(player, action);
                }
            });
            return null;
        }
    }
}