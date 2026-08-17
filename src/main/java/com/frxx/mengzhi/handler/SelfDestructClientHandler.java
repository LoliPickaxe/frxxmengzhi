package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.gui.GuiSelfDestruct;
import com.frxx.mengzhi.network.SelfDestructTriggerPacket;
import com.frxx.mengzhi.network.SpiritBombDetonatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class SelfDestructClientHandler {

    public static final long CONFIRM_WINDOW_MS = 1000L;

    public static boolean physicalEnabled = true;
    public static boolean soulEnabled = false;
    public static boolean confirmState = false;
    private static long confirmTime = 0L;

    private static int shakeTicks = 0;
    private static int shakeTotal = 0;
    private static float shakeAmplitude = 0F;
    private static int shakeColor = 0xFFFF3C3C;
    private static final Random SHAKE_RAND = new Random();
    private static boolean gDown = false;

    public static void onShake(int ticks, float amplitude, int color) {
        shakeTicks = Math.max(shakeTicks, ticks);
        shakeTotal = Math.max(shakeTotal, ticks);
        shakeAmplitude = Math.max(shakeAmplitude, amplitude);
        shakeColor = color;
    }

    public static void resetConfirm() {
        confirmState = false;
        confirmTime = 0L;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) return;
        if (ClientHooks.keySelfDestructPanel.isPressed()) {
            mc.displayGuiScreen(new GuiSelfDestruct());
        }
    }

    /** 面板内按 G 的确认流程 */
    public static void handleExecuteKey() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        if (!physicalEnabled && !soulEnabled) {
            mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "\u8bf7\u81f3\u5c11\u9009\u62e9\u4e00\u79cd\u7206\u70b8\u65b9\u5f0f"));
            return;
        }
        long now = System.currentTimeMillis();
        if (!confirmState) {
            confirmState = true;
            confirmTime = now;
        } else if (now - confirmTime <= CONFIRM_WINDOW_MS) {
            confirmState = false;
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new SelfDestructTriggerPacket(physicalEnabled, soulEnabled));
            mc.player.closeScreen();
        } else {
            confirmTime = now;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // G 键用原始键盘轮询，避免 GUI/KeyBinding 事件路由导致按下的 G 进不到面板
        Minecraft tickMc = Minecraft.getMinecraft();
        if (tickMc == null || tickMc.player == null) {
            gDown = false;
        } else {
            boolean now = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_G);
            if (now && !gDown) {
                gDown = true;
                if (tickMc.currentScreen instanceof GuiSelfDestruct) {
                    handleExecuteKey();
                } else if (tickMc.currentScreen == null) {
                    if (tickMc.player.isSneaking()) {
                        FanRenXiuXianMengZhi.NETWORK.sendToServer(new SpiritBombDetonatePacket());
                        tickMc.player.sendMessage(new TextComponentString(TextFormatting.GOLD + "\u5f15\u7206\u7075\u6c14\u70b8\u5f39\uff01"));
                    } else {
                        tickMc.player.sendMessage(new TextComponentString(TextFormatting.GRAY + "\u6211\uff0c\u4e0d\u60f3\u6b7b"));
                    }
                }
            } else if (!now) {
                gDown = false;
            }
        }

        if (shakeTicks <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            shakeTicks = 0;
            return;
        }
        float ratio = (float) shakeTicks / (float) Math.max(1, shakeTotal);
        float amp = shakeAmplitude * ratio;
        float yawDelta = (SHAKE_RAND.nextFloat() - 0.5F) * 2F * amp;
        float pitchDelta = (SHAKE_RAND.nextFloat() - 0.5F) * 2F * amp * 0.7F;
        mc.player.rotationYaw += yawDelta;
        mc.player.rotationPitch += pitchDelta;
        mc.player.prevRotationYaw += yawDelta;
        mc.player.prevRotationPitch += pitchDelta;
        shakeTicks--;
    }

    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (shakeTicks <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        float ratio = (float) shakeTicks / (float) Math.max(1, shakeTotal);
        float alpha = Math.min(1.0F, shakeAmplitude * ratio);
        ScaledResolution res = new ScaledResolution(mc);
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();
        int edge = 26;
        int color = ((int) (alpha * 255F / 2F)) << 24 | (shakeColor & 0xFFFFFF);
        GlStateManager.pushMatrix();
        GuiIngame.drawRect(0, 0, w, edge, color);
        GuiIngame.drawRect(0, h - edge, w, h, color);
        GuiIngame.drawRect(0, edge, edge, h - edge, color);
        GuiIngame.drawRect(w - edge, edge, w, h - edge, color);
        GlStateManager.popMatrix();
    }
}