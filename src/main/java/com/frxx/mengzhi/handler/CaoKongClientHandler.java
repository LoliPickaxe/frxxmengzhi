package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.CaoKongActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class CaoKongClientHandler {

    private static volatile boolean enabled;
    private static volatile double distance = 4.0;
    private static volatile int heldEntities;

    // 蓄力状态（本地计时）
    private static boolean charging;
    private static long chargeStart;
    private static boolean prevRight;

    public static void update(boolean newEnabled, double newDistance, int entities) {
        enabled = newEnabled;
        distance = newDistance;
        heldEntities = entities;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isHolding() {
        return heldEntities > 0;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (ClientHooks.keyCaoKongToggle.isPressed()) {
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new CaoKongActionPacket(CaoKongActionPacket.Action.TOGGLE));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            charging = false;
            prevRight = false;
            return;
        }
        // 滚轮调距
        int wheel = Mouse.getDWheel();
        if (wheel != 0 && mc.currentScreen == null && enabled) {
            int delta = wheel > 0 ? 1 : -1;
            FanRenXiuXianMengZhi.NETWORK.sendToServer(
                new CaoKongActionPacket(CaoKongActionPacket.Action.SCROLL_DIST, delta));
        }
        if (!enabled) {
            charging = false;
            prevRight = false;
            return;
        }
        EntityPlayer player = mc.player;
        boolean right = Mouse.isButtonDown(1);
        boolean pressed = right && !prevRight;
        boolean released = !right && prevRight;
        prevRight = right;

        if (player.isSneaking() && isHolding()) {
            // 蹲下+右键：蓄力投掷
            if (released && charging) {
                long heldMs = System.currentTimeMillis() - chargeStart;
                int ticks = (int) (heldMs / 50L);
                FanRenXiuXianMengZhi.NETWORK.sendToServer(
                    new CaoKongActionPacket(CaoKongActionPacket.Action.DROP, ticks));
                charging = false;
            } else if (right && !charging) {
                charging = true;
                chargeStart = System.currentTimeMillis();
            } else if (!right) {
                charging = false;
            }
        } else {
            charging = false;
            if (pressed) {
                // 右键：已持有则丢弃，否则拿起
                if (isHolding()) {
                    FanRenXiuXianMengZhi.NETWORK.sendToServer(
                        new CaoKongActionPacket(CaoKongActionPacket.Action.DROP, 0));
                } else {
                    FanRenXiuXianMengZhi.NETWORK.sendToServer(
                        new CaoKongActionPacket(CaoKongActionPacket.Action.CLICK));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Text event) {
        if (event.getType() != net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        if (!enabled) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc);
        String text = "\u3010\u7075\u529b\u64cd\u63a7\u3011\u8ddd\u79bb:" + (int) distance;
        if (heldEntities > 0) {
            text += " | \u64cd\u63a7:" + heldEntities;
        }
        if (charging) {
            int ticks = (int) ((System.currentTimeMillis() - chargeStart) / 50L);
            float pct = Math.min(1.0F, ticks / (float) CaoKongHandler.MAX_CHARGE_TICKS);
            text += " | \u84c4\u529b:" + (int) (pct * 100) + "%";
        }
        mc.fontRenderer.drawStringWithShadow(text, 4, res.getScaledHeight() - 60, 0xFFFFFF);
        if (isHolding()) {
            mc.fontRenderer.drawStringWithShadow(
                "\u53f3\u952e\uff1a\u4e22\u5f03 | \u51cc\u4e0b\u53f3\u952e\uff1a\u66b4\u6c14\u6295\u63b7\uff08\u78b0\u5899\u53d7\u78b0\u64ca\u4f24\u5bb3\u5e76\u5d4c\u5165\u5899\u4f53\uff09",
                4, res.getScaledHeight() - 48, 0xFFFF55);
        }
    }
}