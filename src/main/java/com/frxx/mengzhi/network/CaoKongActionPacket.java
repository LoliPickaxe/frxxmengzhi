package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.CaoKongHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CaoKongActionPacket implements IMessage {

    public enum Action {
        TOGGLE,      // 开关操控
        CLICK,       // 右键：拿起目视生物 / 对空群控；已持有则丢弃
        SCROLL_DIST, // 滚轮调距 arg=delta
        DROP         // 扔出（蹲下右键 = 蓄力投掷）arg=chargeTicks
    }

    private Action action;
    private int arg;

    public CaoKongActionPacket() {
    }

    public CaoKongActionPacket(Action action) {
        this(action, 0);
    }

    public CaoKongActionPacket(Action action, int arg) {
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

    public static class Handler implements IMessageHandler<CaoKongActionPacket, IMessage> {

        @Override
        public IMessage onMessage(final CaoKongActionPacket message, MessageContext ctx) {
            if (message.action == null) {
                return null;
            }
            final EntityPlayerMP player = ctx.getServerHandler().player;
            final Action action = message.action;
            final int arg = message.arg;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    CaoKongHandler.handleAction(player, action, arg);
                }
            });
            return null;
        }
    }
}