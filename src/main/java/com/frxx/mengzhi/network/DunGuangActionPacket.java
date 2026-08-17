package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.DunGuangHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class DunGuangActionPacket implements IMessage {

    public enum Action {
        TOGGLE_ACTIVE,  // H：开启/关闭主动遁光
        TOGGLE_PASSIVE, // 潜行+H：开启/关闭被动护体
        SCROLL          // 滚轮调速 arg=delta(±1)
    }

    private Action action;
    private int arg;

    public DunGuangActionPacket() {
    }

    public DunGuangActionPacket(Action action) {
        this(action, 0);
    }

    public DunGuangActionPacket(Action action, int arg) {
        this.action = action;
        this.arg = arg;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int id = buf.readInt();
        Action[] values = Action.values();
        this.action = (id >= 0 && id < values.length) ? values[id] : null;
        this.arg = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action == null ? 0 : action.ordinal());
        buf.writeInt(arg);
    }

    public static class Handler implements IMessageHandler<DunGuangActionPacket, IMessage> {

        @Override
        public IMessage onMessage(final DunGuangActionPacket message, MessageContext ctx) {
            if (message.action == null) {
                return null;
            }
            final EntityPlayerMP player = ctx.getServerHandler().player;
            final Action action = message.action;
            final int arg = message.arg;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    DunGuangHandler.handleAction(player, action, arg);
                }
            });
            return null;
        }
    }
}