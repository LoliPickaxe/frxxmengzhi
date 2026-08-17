package com.frxx.mengzhi.network;

import com.frxx.mengzhi.handler.TiandaoHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 天道面板操作包：0=切换模式 1=天劫 2=预设属性 3=天道之力开/关 4=自动攻击开/关 5=抹除状态 6=写入NBT */
public class TiandaoPanelPacket implements IMessage {

    public static final int ACTION_SWITCH_MODE = 0;
    public static final int ACTION_CAST_TRIBULATION = 1;
    public static final int ACTION_APPLY_ATTR = 2;
    public static final int ACTION_TOGGLE_FORCE = 3;
    public static final int ACTION_TOGGLE_AUTO = 4;
    public static final int ACTION_CLEAR_EFFECTS = 5;
    public static final int ACTION_SET_NBT = 6;
    public static final int ACTION_FETCH_NBT_DOC = 7;
    public static final int ACTION_SAVE_NBT_DOC = 8;

    public int action;
    public int targetId = -1;
    public int attrIndex;
    public double value;
    /** NBT 修改：键名与值（值可为数字/文本/true/false，自动识别类型） */
    public String key = "";
    public String strValue = "";

    public TiandaoPanelPacket() {
    }

    public TiandaoPanelPacket(int action, int targetId, int attrIndex, double value) {
        this(action, targetId, attrIndex, value, "", "");
    }

    public TiandaoPanelPacket(int action, int targetId, int attrIndex, double value, String key, String strValue) {
        this.action = action;
        this.targetId = targetId;
        this.attrIndex = attrIndex;
        this.value = value;
        this.key = key == null ? "" : key;
        this.strValue = strValue == null ? "" : strValue;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        targetId = buf.readInt();
        attrIndex = buf.readInt();
        value = buf.readDouble();
        key = net.minecraftforge.fml.common.network.ByteBufUtils.readUTF8String(buf);
        strValue = net.minecraftforge.fml.common.network.ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        buf.writeInt(targetId);
        buf.writeInt(attrIndex);
        buf.writeDouble(value);
        net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buf, key);
        net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buf, strValue);
    }

    public static class Handler implements IMessageHandler<TiandaoPanelPacket, IMessage> {

        @Override
        public IMessage onMessage(final TiandaoPanelPacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    TiandaoHandler.handle(player, message);
                }
            });
            return null;
        }
    }
}