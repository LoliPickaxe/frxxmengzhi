package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.GangQiActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GangQiClientHandler {

    private static volatile boolean enabled;
    private static volatile int mode;

    public static void update(boolean newEnabled, int newMode) {
        enabled = newEnabled;
        mode = newMode;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        if (ClientHooks.keyGangQiToggle.isPressed()) {
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new GangQiActionPacket(GangQiActionPacket.Action.TOGGLE));
        }
        if (ClientHooks.keyGangQiMode.isPressed()) {
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new GangQiActionPacket(GangQiActionPacket.Action.MODE));
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        if (!enabled) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc);
        boolean full = mode == GangQiHandler.MODE_FULL;
        String modeName = full ? "\u5168\u5f00" : "\u666e\u901a";
        String cost = full ? "100" : "10";
        String text = TextFormatting.GREEN + "[\u7f61\u6c14]\u6a21\u5f0f\uff1a" + modeName
            + "\u8017\u7075 " + cost + "/\u79d2";
        int x = 4;
        int y = res.getScaledHeight() - 45;
        mc.fontRenderer.drawStringWithShadow(text, x, y, 0xFFFFFF);
    }
}