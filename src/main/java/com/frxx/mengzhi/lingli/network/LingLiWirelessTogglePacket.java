package com.frxx.mengzhi.lingli.network;

import com.frxx.mengzhi.lingli.tile.TileLingLiBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class LingLiWirelessTogglePacket implements IMessage {

    private BlockPos pos;
    private boolean wireless;

    public LingLiWirelessTogglePacket() {
    }

    public LingLiWirelessTogglePacket(BlockPos pos, boolean wireless) {
        this.pos = pos;
        this.wireless = wireless;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.wireless = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeBoolean(this.wireless);
    }

    public static class Handler implements IMessageHandler<LingLiWirelessTogglePacket, IMessage> {

        @Override
        public IMessage onMessage(final LingLiWirelessTogglePacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    TileEntity tile = player.world.getTileEntity(message.pos);
                    if (tile instanceof TileLingLiBase) {
                        TileLingLiBase base = (TileLingLiBase) tile;
                        base.setWireless(message.wireless);
                        base.markDirty();
                        player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + (message.wireless ? "无线供电已开启" : "无线供电已关闭")));
                    }
                }
            });
            return null;
        }
    }
}