package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.LingLiSkillHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 客户端 -> 服务端：轻功冲刺（双击移动键）/ 多段跳（双击空格） */
public class LingLiSkillPacket implements IMessage {

    public static final int TYPE_DASH = 0;
    public static final int TYPE_JUMP = 1;

    private int type;
    private int dir;

    public LingLiSkillPacket() {
    }

    public LingLiSkillPacket(int type, int dir) {
        this.type = type;
        this.dir = dir;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.type = buf.readInt();
        this.dir = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.type);
        buf.writeInt(this.dir);
    }

    public static class Handler implements IMessageHandler<LingLiSkillPacket, IMessage> {

        @Override
        public IMessage onMessage(final LingLiSkillPacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LingLiSkillHandler.handle(player, message.type, message.dir);
                }
            });
            return null;
        }
    }
}