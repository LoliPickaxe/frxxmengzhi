package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.GuardActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuardClientHandler {

    private static volatile boolean guardOn;
    private static volatile double guard;
    private static volatile double guardMax;

    public static void update(boolean on, double g, double max) {
        guardOn = on;
        guard = g;
        guardMax = max;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        if (ClientHooks.keyGuardToggle.isPressed()) {
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new GuardActionPacket(GuardActionPacket.Action.TOGGLE));
        }
        if (ClientHooks.keyGuardCharge.isPressed()) {
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new GuardActionPacket(GuardActionPacket.Action.CHARGE));
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (!guardOn || guardMax <= 0) return;

        double ratio = Math.min(1.0, guard / guardMax);

        int color;
        if (ratio > 0.6) {
            color = 0xFF00FF00;
        } else if (ratio > 0.3) {
            color = 0xFFFFAA00;
        } else {
            color = 0xFFFF4444;
        }

        ScaledResolution res = new ScaledResolution(mc);
        int barWidth = 100;
        int barHeight = 8;
        int x = 4;
        int y = res.getScaledHeight() - 30;

        GlStateManager.pushMatrix();
        GuiIngame.drawRect(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);
        GuiIngame.drawRect(x, y, x + (int) (barWidth * ratio), y + barHeight, color);

        String text = String.format("%.0f/%.0f", guard, guardMax);
        mc.fontRenderer.drawStringWithShadow(text, x + barWidth / 2 - mc.fontRenderer.getStringWidth(text) / 2, y + barHeight + 2, 0xFFFFFFFF);
        GlStateManager.popMatrix();
    }
}
