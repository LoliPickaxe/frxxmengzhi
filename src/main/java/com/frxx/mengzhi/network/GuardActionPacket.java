package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.GuardHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class GuardActionPacket implements IMessage {

    public enum Action {
        TOGGLE,
        CHARGE
    }

    private Action action;

    public GuardActionPacket() {
    }

    public GuardActionPacket(Action action) {
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

    public static class Handler implements IMessageHandler<GuardActionPacket, IMessage> {

        @Override
        public IMessage onMessage(final GuardActionPacket message, MessageContext ctx) {
            if (message.action == null) {
                return null;
            }
            final EntityPlayerMP player = ctx.getServerHandler().player;
            final Action action = message.action;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    GuardHandler.handleAction(player, action);
                }
            });
            return null;
        }
    }
}
