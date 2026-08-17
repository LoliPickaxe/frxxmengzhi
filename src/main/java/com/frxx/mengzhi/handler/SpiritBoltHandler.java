package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.entity.EntitySpiritBolt;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SpiritBoltHandler {

    public static final String TAG_CD = "FrxxSpiritBoltCd";
    public static final String TAG_MSG_CD = "FrxxSpiritBoltMsgCd";
    private static final int COOLDOWN_TICKS = 6;
    private static final double POWER_COST = 10.0;
    private static final int MSG_CD_TICKS = 40;

    public static void fire(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (data.getDouble("JingJieNum") < 1.0) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u60a8\u5c1a\u672a\u4fee\u4ed9\uff0c\u65e0\u6cd5\u53d1\u5c04\u7075\u6c14\u5f39"));
            return;
        }
        long worldTime = player.getServerWorld().getTotalWorldTime();
        if (worldTime < data.getLong(TAG_CD)) {
            return;
        }
        double power = data.getDouble("Power");
        if (power < POWER_COST) {
            if (data.getInteger(TAG_MSG_CD) <= 0) {
                data.setInteger(TAG_MSG_CD, MSG_CD_TICKS);
                player.sendMessage(new TextComponentString(TextFormatting.RED
                    + "\u7075\u529b\u4e0d\u8db3\uff0c\u65e0\u6cd5\u53d1\u5c04\u7075\u6c14\u5f39\uff01\u9700\u8981 " + (int) POWER_COST + " \u70b9\u7075\u529b"));
            }
            return;
        }
        data.setDouble("Power", power - POWER_COST);
        data.setLong(TAG_CD, worldTime + COOLDOWN_TICKS);
        double dmg = data.getDouble("MagicAttack") * 0.5;
        EntitySpiritBolt bolt = new EntitySpiritBolt(player.getServerWorld(), player, dmg);
        player.getServerWorld().spawnEntity(bolt);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        NBTTagCompound data = event.player.getEntityData();
        int msgCd = data.getInteger(TAG_MSG_CD);
        if (msgCd > 0) {
            data.setInteger(TAG_MSG_CD, msgCd - 1);
        }
    }
}
