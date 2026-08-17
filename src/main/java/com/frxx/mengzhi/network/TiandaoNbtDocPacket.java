package com.frxx.mengzhi.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 服务端 -> 客户端：天道 NBT 文档（目标全部 NBT 的"键 = 值"文本） */
public class TiandaoNbtDocPacket implements IMessage {

    /** 客户端缓存：GUI 直接读取 */
    public static int cachedTargetId = -1;
    public static String cachedName = "";
    public static String cachedDoc = "";

    public int targetId;
    public String name;
    public String doc;

    public TiandaoNbtDocPacket() {
    }

    public TiandaoNbtDocPacket(int targetId, String name, String doc) {
        this.targetId = targetId;
        this.name = name == null ? "" : name;
        this.doc = doc == null ? "" : doc;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        targetId = buf.readInt();
        name = ByteBufUtils.readUTF8String(buf);
        doc = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(targetId);
        ByteBufUtils.writeUTF8String(buf, name);
        ByteBufUtils.writeUTF8String(buf, doc);
    }

    public static class Handler implements IMessageHandler<TiandaoNbtDocPacket, IMessage> {

        @Override
        public IMessage onMessage(final TiandaoNbtDocPacket message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    cachedTargetId = message.targetId;
                    cachedName = message.name;
                    cachedDoc = message.doc;
                }
            });
            return null;
        }
    }
}