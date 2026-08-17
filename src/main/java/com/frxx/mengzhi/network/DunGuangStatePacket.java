package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.DunGuangClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class DunGuangStatePacket implements IMessage {

    private boolean active;   // 主动遁光状态
    private boolean passive;  // 被动护体状态
    private double speed;     // 当前倍速档位 (2~50)
    private int realm;        // 境界 1~5

    public DunGuangStatePacket() {
    }

    public DunGuangStatePacket(boolean active, boolean passive, double speed, int realm) {
        this.active = active;
        this.passive = passive;
        this.speed = speed;
        this.realm = realm;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        active = buf.readBoolean();
        passive = buf.readBoolean();
        speed = buf.readDouble();
        realm = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeBoolean(passive);
        buf.writeDouble(speed);
        buf.writeInt(realm);
    }

    public static class Handler implements IMessageHandler<DunGuangStatePacket, IMessage> {

        @Override
        public IMessage onMessage(final DunGuangStatePacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    DunGuangClientHandler.update(message.active, message.passive, message.speed, message.realm);
                }
            });
            return null;
        }
    }
}