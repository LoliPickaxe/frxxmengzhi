package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.SelfDestructHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SelfDestructTriggerPacket implements IMessage {

    private boolean physical;
    private boolean soul;

    public SelfDestructTriggerPacket() {
    }

    public SelfDestructTriggerPacket(boolean physical, boolean soul) {
        this.physical = physical;
        this.soul = soul;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        physical = buf.readBoolean();
        soul = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(physical);
        buf.writeBoolean(soul);
    }

    public static class Handler implements IMessageHandler<SelfDestructTriggerPacket, IMessage> {

        @Override
        public IMessage onMessage(final SelfDestructTriggerPacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    SelfDestructHandler.startSelfDestruct(player, message.physical, message.soul);
                }
            });
            return null;
        }
    }
}
