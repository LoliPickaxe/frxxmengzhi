package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.RepulsionTogglePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 排斥力场（客户端）：小键盘 8 开/关 */
@SideOnly(Side.CLIENT)
public class RepulsionClientHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        if (ClientHooks.keyRepulsion.isPressed()) {
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new RepulsionTogglePacket());
        }
    }
}