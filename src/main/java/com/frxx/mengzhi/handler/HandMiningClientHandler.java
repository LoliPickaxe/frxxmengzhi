package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.HandMiningTogglePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class HandMiningClientHandler {

    private static int tier = 0;
    private static boolean normalOn = false;
    private static boolean allOn = false;
    private static int power = 0;

    private static boolean jDown = false;

    public static void apply(int tier, boolean normalOn, boolean allOn, int power) {
        HandMiningClientHandler.tier = tier;
        HandMiningClientHandler.normalOn = normalOn;
        HandMiningClientHandler.allOn = allOn;
        HandMiningClientHandler.power = power;
    }

    /** 客户端缓存的挖掘信息，仅对本地玩家生效 */
    public static HandMiningHandler.MiningInfo cached(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || player != mc.player) return null;
        if (!normalOn && !allOn) return null;
        return HandMiningHandler.table(tier, allOn);
    }

    public static int cachedPower() {
        return power;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) {
            jDown = false;
            return;
        }
        boolean now = Keyboard.isKeyDown(Keyboard.KEY_J);
        if (now && !jDown) {
            jDown = true;
            if (mc.currentScreen == null) {
                boolean sneak = mc.player.isSneaking();
                FanRenXiuXianMengZhi.NETWORK.sendToServer(new HandMiningTogglePacket((byte) (sneak ? 1 : 0)));
            }
        } else if (!now) {
            jDown = false;
        }
    }
}
