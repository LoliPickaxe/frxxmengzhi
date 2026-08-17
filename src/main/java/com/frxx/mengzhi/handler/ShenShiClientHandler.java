package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.ShenShiPressureTriggerPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ShenShiClientHandler {

    private static boolean zDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) {
            zDown = false;
            return;
        }
        boolean now = Keyboard.isKeyDown(Keyboard.KEY_Z);
        if (now && !zDown) {
            zDown = true;
            if (mc.currentScreen == null) {
                boolean sneak = mc.player.isSneaking();
                FanRenXiuXianMengZhi.NETWORK.sendToServer(new ShenShiPressureTriggerPacket(sneak));
            }
        } else if (!now) {
            zDown = false;
        }
    }
}