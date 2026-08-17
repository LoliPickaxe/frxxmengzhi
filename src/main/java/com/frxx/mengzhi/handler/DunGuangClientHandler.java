package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.DunGuangActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

/** 遁光飞行（客户端）：H 键切换、滚轮调速、白色光球/拖尾/菱形粒子、HUD。 */
@SideOnly(Side.CLIENT)
public class DunGuangClientHandler {

    private static volatile boolean active;
    private static volatile boolean passive;
    private static volatile double speed = 2.0;
    private static volatile int realm = 1;
    private static boolean wasActive; // 上一 tick 状态（用于切换本地光球碰撞箱）
    private static int lastHotbarSlot; // 上一 tick 物品栏槽位（滚轮调速时恢复，防止切物品栏）

    public static void update(boolean newActive, boolean newPassive, double newSpeed, int newRealm) {
        active = newActive;
        passive = newPassive;
        speed = newSpeed;
        realm = newRealm;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isPassive() {
        return passive;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (ClientHooks.keyDunGuang.isPressed()) {
            boolean sneak = mc.player.isSneaking();
            FanRenXiuXianMengZhi.NETWORK.sendToServer(
                new DunGuangActionPacket(sneak
                    ? DunGuangActionPacket.Action.TOGGLE_PASSIVE
                    : DunGuangActionPacket.Action.TOGGLE_ACTIVE));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        EntityPlayer player = mc.player;

        // PRE 阶段：在本 tick 本地移动/碰撞判定之前把 noClip 与碰撞箱设好
        // （原版客户端 move() 只看 noClip 决定是否碰撞，迟一个 tick 就会表现为撞墙）
        if (event.phase == TickEvent.Phase.START) {
            if (active != wasActive) {
                wasActive = active;
                if (active) {
                    DunGuangHandler.setPlayerSize(player, (float) DunGuangHandler.SIZE, (float) DunGuangHandler.SIZE);
                } else {
                    DunGuangHandler.setPlayerSize(player, 0.6F, 1.8F);
                }
            }
            player.noClip = active;
            return;
        }

        // END：记录上一槽位（供滚轮调速时恢复），发射粒子
        lastHotbarSlot = player.inventory.currentItem;
        if (!active) {
            return;
        }
        spawnParticles(player);
    }

    /** 滚轮调速。原版在 InputEvent 之前的同一事件循环里已把滚轮消费掉用于切换物品栏，
     *  此处读出本次事件滚轮值发送调速，并把物品栏恢复到原来的槽位。 */
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null || !active) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int delta = wheel > 0 ? 1 : -1;
        FanRenXiuXianMengZhi.NETWORK.sendToServer(new DunGuangActionPacket(DunGuangActionPacket.Action.SCROLL, delta));
        int prev = lastHotbarSlot;
        if (mc.player.inventory.currentItem != prev) {
            mc.player.inventory.currentItem = prev;
            mc.player.connection.sendPacket(new CPacketHeldItemChange(prev));
        }
    }

    /** 主动遁光期间隐藏手持物品与手臂（类似旁观者模式隐藏手的做法）。 */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (active) {
            event.setCanceled(true);
        }
    }

    /** 他人视角：隐形玩家（遁光光球）整体不再渲染，包括手持物品层——
     *  否则身体隐形后手里物品仍悬浮半空，非常突兀。 */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre event) {
        if (event.getEntity() instanceof EntityPlayer && event.getEntity().isInvisible()) {
            event.setCanceled(true);
        }
    }

    /** 白色光球 + 拖尾（纯原版白色粒子）；元婴及以上额外绘制菱形轮廓。 */
    private static void spawnParticles(EntityPlayer player) {
        double px = player.posX;
        double py = player.posY + DunGuangHandler.SIZE / 2.0;
        double pz = player.posZ;
        boolean jewel = realm >= 4; // 元婴及以上：尖锐菱形

        // 中心光球（白色竖光，静止不飘）
        player.world.spawnParticle(EnumParticleTypes.END_ROD, px, py, pz, 0.0, 0.0, 0.0);
        // 随机散光（白色星火）
        player.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, px + (player.world.rand.nextDouble() - 0.5) * 0.3,
            py + (player.world.rand.nextDouble() - 0.5) * 0.3,
            pz + (player.world.rand.nextDouble() - 0.5) * 0.3, 0.0, 0.0, 0.0);

        // 拖尾（沿运动反方向的白色烟/星火）
        double mx = player.motionX;
        double my = player.motionY;
        double mz = player.motionZ;
        for (int i = 1; i <= 2; i++) {
            player.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px - mx * i * 1.2, py - my * i * 1.2 - 0.1, pz - mz * i * 1.2, 0.0, 0.0, 0.0);
            player.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, px - mx * i * 0.6, py - my * i * 0.6 - 0.1, pz - mz * i * 0.6, 0.0, 0.0, 0.0);
        }

        if (jewel) {
            // 菱形轮廓（上下左右四点）
            double r = 0.35;
            player.world.spawnParticle(EnumParticleTypes.END_ROD, px, py + r, pz, 0.0, 0.0, 0.0);
            player.world.spawnParticle(EnumParticleTypes.END_ROD, px, py - r, pz, 0.0, 0.0, 0.0);
            player.world.spawnParticle(EnumParticleTypes.END_ROD, px + r, py, pz, 0.0, 0.0, 0.0);
            player.world.spawnParticle(EnumParticleTypes.END_ROD, px - r, py, pz, 0.0, 0.0, 0.0);
            // 菱形尖角（上下）
            player.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, px, py + r * 1.6, pz, 0.0, 0.06, 0.0);
            player.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, px, py - r * 1.6, pz, 0.0, -0.06, 0.0);
        } else {
            // 结丹期圆形光球轮廓
            double r = 0.22;
            double a = player.world.rand.nextDouble() * Math.PI * 2.0;
            player.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, px + Math.cos(a) * r, py + Math.sin(a) * r * 0.6, pz, 0.0, 0.0, 0.0);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Text event) {
        if (event.getType() != net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        if (!active && !passive) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc);
        String text;
        if (active) {
            text = "【遁光】×" + (int) speed + " 灵力" + (int) DunGuangHandler.costPerSec(realm, speed) + "/s";
        } else {
            text = "【遁光】被动护体（致死时随机逃遁）";
        }
        mc.fontRenderer.drawStringWithShadow(text, 4, res.getScaledHeight() - 20, 0xFFFFFF);
    }
}