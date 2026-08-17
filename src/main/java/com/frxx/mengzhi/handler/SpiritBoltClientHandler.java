package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.SpiritBoltFirePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SpiritBoltClientHandler {

    private static final long FIRE_INTERVAL_MS = 300L;

    private static boolean enabled = false;
    private static long lastFire = 0L;

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) return;

        if (ClientHooks.keySpiritBolt.isPressed()) {
            enabled = !enabled;
            if (enabled) {
                mc.player.sendMessage(new TextComponentString(TextFormatting.GOLD
                    + "\u7075\u6c14\u5f39\u5df2\u5f00\u542f\uff1a\u6309\u4f4f\u9f20\u6807\u53f3\u952e\u53d1\u5c04\uff08\u51b7\u5374 0.3 \u79d2\uff0c\u6d88\u8017 10 \u7075\u529b\uff09"));
            } else {
                mc.player.sendMessage(new TextComponentString(TextFormatting.GRAY + "\u7071\u6c14\u5f39\u5df2\u5173\u95ed"));
            }
        }

        if (enabled && mc.currentScreen == null && ClientHooks.keySpiritBoltFire.isKeyDown()) {
            long now = System.currentTimeMillis();
            if (now - lastFire >= FIRE_INTERVAL_MS) {
                lastFire = now;
                FanRenXiuXianMengZhi.NETWORK.sendToServer(new SpiritBoltFirePacket());
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelWhileEnabled(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelWhileEnabled(event);
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        cancelWhileEnabled(event);
    }

    private static void cancelWhileEnabled(PlayerEvent event) {
        if (enabled && event.isCancelable() && event.getEntityPlayer().world.isRemote) {
            event.setCanceled(true);
        }
    }
}
