package com.frxx.mengzhi.network;

import com.frxx.mengzhi.elixir.ItemAttributeAssistant;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class AssistantConfigPacket implements IMessage {

    private int shieldMax;
    private int shieldCurrent;
    private int shieldRegen;
    private int qiToShield;
    private int absorptionRatio;

    public AssistantConfigPacket() {
    }

    public AssistantConfigPacket(int shieldMax, int shieldCurrent, int shieldRegen, int qiToShield, int absorptionRatio) {
        this.shieldMax = shieldMax;
        this.shieldCurrent = shieldCurrent;
        this.shieldRegen = shieldRegen;
        this.qiToShield = qiToShield;
        this.absorptionRatio = absorptionRatio;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        shieldMax = buf.readInt();
        shieldCurrent = buf.readInt();
        shieldRegen = buf.readInt();
        qiToShield = buf.readInt();
        absorptionRatio = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(shieldMax);
        buf.writeInt(shieldCurrent);
        buf.writeInt(shieldRegen);
        buf.writeInt(qiToShield);
        buf.writeInt(absorptionRatio);
    }

    public static class Handler implements IMessageHandler<AssistantConfigPacket, IMessage> {

        @Override
        public IMessage onMessage(AssistantConfigPacket message, MessageContext ctx) {
            if (ctx.getServerHandler() == null) {
                return null;
            }
            ItemAttributeAssistant.saveConfigToPlayer(ctx.getServerHandler().player,
                message.shieldMax, message.shieldCurrent, message.shieldRegen,
                message.qiToShield, message.absorptionRatio);
            return null;
        }
    }
}
