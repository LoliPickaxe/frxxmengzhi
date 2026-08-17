package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.gui.GuiTiandaoPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 天道（客户端）：小键盘 . 打开天道面板；拦截死亡 GUI 保证不死不灭。 */
@SideOnly(Side.CLIENT)
public class TiandaoClientHandler {

    public static int currentTargetId = -1;
    public static String currentTargetName = "";

    /** 以准星锁定目标（自身或看向的生物） */
    public static void resolveTarget() {
        Minecraft mc = Minecraft.getMinecraft();
        currentTargetId = -1;
        currentTargetName = "\u81ea\u8eab";
        if (mc == null || mc.player == null) {
            return;
        }
        RayTraceResult hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
            && hit.entityHit instanceof EntityLivingBase && hit.entityHit.isEntityAlive()
            && hit.entityHit != mc.player) {
            currentTargetId = hit.entityHit.getEntityId();
            currentTargetName = hit.entityHit.getName();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (ClientHooks.keyTiandaoPanel.isPressed()) {
            resolveTarget();
            mc.displayGuiScreen(new GuiTiandaoPanel());
        }
    }

    /** 绝对拦截死亡 GUI：服务端已免死，此处兜底关闭死亡界面；天道免被击抖动 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.player != null) {
            if (mc.currentScreen instanceof GuiGameOver) {
                // 仅天道：跳过死亡界面直接重生（服务端免死兜底，真正死亡时立即复活）
                // 非天道玩家完全不动死亡界面，保留原版重生流程
                if (TiandaoHandler.isTiandao(mc.player)) {
                    mc.player.respawnPlayer();
                    mc.displayGuiScreen(null);
                }
            }
            // 天道：取消被攻击抖动（身体红色闪动/后仰、视角镜头摇晃）
            if (TiandaoHandler.isTiandao(mc.player)) {
                mc.player.hurtTime = 0;
                mc.player.hurtResistantTime = 0;
                mc.player.attackedAtYaw = 0.0F;
            }
        }
    }
}