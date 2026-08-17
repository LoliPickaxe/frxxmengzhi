package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.LingLiSkillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 轻功/多段跳（客户端）：双击移动键触发冲刺，双击空格触发多段跳。
 * 双击判定：同一按键两次"按下沿"间隔 < 350ms。境界/饥饿/飞行限制由服务端保证。
 */
@SideOnly(Side.CLIENT)
public class LingLiSkillClientHandler {

    private static final long DOUBLE_TAP_MS = 350;

    private static final int KEY_FORWARD = 0;
    private static final int KEY_BACK = 1;
    private static final int KEY_LEFT = 2;
    private static final int KEY_RIGHT = 3;
    private static final int KEY_JUMP = 4;

    private static final long[] lastPress = new long[5];
    private static final boolean[] wasDown = new boolean[5];
    private static final KeyBinding[] keys = new KeyBinding[5];
    private static boolean initialized;

    private static void init() {
        GameSettings gs = Minecraft.getMinecraft().gameSettings;
        keys[KEY_FORWARD] = gs.keyBindForward;
        keys[KEY_BACK] = gs.keyBindBack;
        keys[KEY_LEFT] = gs.keyBindLeft;
        keys[KEY_RIGHT] = gs.keyBindRight;
        keys[KEY_JUMP] = gs.keyBindJump;
        initialized = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!initialized) {
            init();
        }
        if (mc.currentScreen != null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            KeyBinding key = keys[i];
            boolean down = key != null && key.isKeyDown();
            if (down && !wasDown[i]) {
                long last = lastPress[i];
                lastPress[i] = now;
                if (now - last < DOUBLE_TAP_MS) {
                    if (i == KEY_JUMP) {
                        FanRenXiuXianMengZhi.NETWORK.sendToServer(new LingLiSkillPacket(LingLiSkillPacket.TYPE_JUMP, 0));
                    } else {
                        FanRenXiuXianMengZhi.NETWORK.sendToServer(new LingLiSkillPacket(LingLiSkillPacket.TYPE_DASH, i));
                    }
                }
            }
            wasDown[i] = down;
        }
    }
}