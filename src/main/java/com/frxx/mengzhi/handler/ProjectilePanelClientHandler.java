package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.ProjectilePanelTogglePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 投掷物面板伤害开关（客户端）：按 3 快速切换，状态由服务端保存。 */
@SideOnly(Side.CLIENT)
public class ProjectilePanelClientHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (ClientHooks.keyProjectilePanel.isPressed()) {
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new ProjectilePanelTogglePacket());
        }
    }
}