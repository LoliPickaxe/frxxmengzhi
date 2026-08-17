package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.SpiritBombPlacePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;

@SideOnly(Side.CLIENT)
public class SpiritBombClientHandler {

    private static final double REACH = 10.0;

    private static boolean nDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) {
            nDown = false;
            return;
        }
        boolean now = Keyboard.isKeyDown(Keyboard.KEY_N);
        if (now && !nDown) {
            nDown = true;
            if (mc.currentScreen == null) {
                tryPlace(mc);
            }
        } else if (!now) {
            nDown = false;
        }
    }

    private static void tryPlace(Minecraft mc) {
        EntityPlayer player = mc.player;
        if (player.isSneaking()) {
            sendPlace(SpiritBombPlacePacket.TYPE_ITEM, -1, 0, 0, 0);
            return;
        }
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = start.addVector(look.x * REACH, look.y * REACH, look.z * REACH);

        Entity best = null;
        double bestDist = REACH * REACH + 1.0;
        AxisAlignedBB search = player.getEntityBoundingBox()
            .expand(look.x * REACH, look.y * REACH, look.z * REACH).expand(1.0, 1.0, 1.0);
        List<Entity> entities = player.world.getEntitiesWithinAABBExcludingEntity(player, search);
        for (Entity e : entities) {
            AxisAlignedBB bb = e.getEntityBoundingBox().grow(e.getCollisionBorderSize());
            RayTraceResult hit = bb.calculateIntercept(start, end);
            if (hit != null) {
                double d = hit.hitVec.squareDistanceTo(start);
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                }
            }
        }

        RayTraceResult blockHit = player.world.rayTraceBlocks(start, end, false, true, false);
        double blockDist = Double.MAX_VALUE;
        if (blockHit != null && blockHit.typeOfHit == RayTraceResult.Type.BLOCK) {
            blockDist = blockHit.hitVec.squareDistanceTo(start);
        }

        if (best != null && bestDist <= blockDist) {
            if (best instanceof EntityItem) {
                sendPlace(SpiritBombPlacePacket.TYPE_ITEM, best.getEntityId(), 0, 0, 0);
            } else {
                sendPlace(SpiritBombPlacePacket.TYPE_ENTITY, best.getEntityId(), 0, 0, 0);
            }
        } else if (blockHit != null && blockHit.typeOfHit == RayTraceResult.Type.BLOCK) {
            sendPlace(SpiritBombPlacePacket.TYPE_BLOCK, 0,
                blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
        } else {
            player.sendMessage(new TextComponentString(TextFormatting.GRAY
                + "\u8bf7\u7784\u51c6\u751f\u7269\u3001\u65b9\u5757\u6216\u7269\u54c1\u540e\u518d\u6309 N"));
        }
    }

    private static void sendPlace(byte type, int targetId, int x, int y, int z) {
        FanRenXiuXianMengZhi.NETWORK.sendToServer(new SpiritBombPlacePacket(type, targetId, x, y, z));
    }
}
