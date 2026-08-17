package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.CaoKongActionPacket;
import com.frxx.mengzhi.network.CaoKongStatePacket;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 灵力操控系统（服务端）：
 *  - \ 键开关；只操控生物（实体），不操控方块
 *  - 拿起：右键点击生物 = 拿起；点击空气 = 群控视线内生物
 *  - 投掷：右键丢出；蹲下右键蓄力 = 暴气投掷（速度/嵌入深度随蓄力提升）
 *  - 撞击：撞到障碍物时，被扔生物受到「玩家面板伤害 x 80%」的撞击伤害；
 *          不爆炸，只沿碰撞箱方向凿开最多 depth 格方块（无蓄力 2 格，满蓄力 10 格）
 */
public class CaoKongHandler {

    public static final String TAG_ON = "CaoKongOn";

    public static final double COST_PER_COUNT_PER_MIN = 1.8; // x1.8 灵力/分钟/数量
    public static final int MAX_CHARGE_TICKS = 200;          // 10s 满蓄力
    public static final int MAX_DIST = 128;
    public static final double THROW_MAX = 384.0;
    public static final float IMPACT_RATIO = 0.8F;           // 撞击伤害 = 面板 x 80%
    public static final int PENETRATION_MIN = 2;             // 无蓄力嵌入格数
    public static final int PENETRATION_MAX = 10;            // 满蓄力嵌入格数

    public static final DamageSource CAOKONG_DAMAGE = new DamageSource("caokong");

    private static final Map<UUID, HoldData> HOLD = new HashMap<>();
    private static final List<ThrowFlight> FLIGHTS = new ArrayList<>();

    private static class HoldData {
        double distance = 4.0;
        final List<EntityLivingBase> entities = new ArrayList<>();
    }

    private static class ThrowFlight {
        Vec3d pos;
        Vec3d dir;            // 单位方向
        int remainingTicks;   // 剩余生存 tick
        int totalTicks;       // 总寿命
        double traveled;      // 已飞行距离
        double maxDist;       // 极限距离
        double v0;            // 起始速度 m/s
        double vMax;          // 终点速度 m/s
        EntityLivingBase entity;
        float impactDmg;      // 撞击伤害（面板 x 80%）
        int depth;            // 可嵌入格数（2~10）
    }

    // ==================== 入口 ====================

    public static void handleAction(EntityPlayerMP player, CaoKongActionPacket.Action action, int arg) {
        NBTTagCompound data = player.getEntityData();
        boolean on = data.getBoolean(TAG_ON);
        HoldData hold = HOLD.get(player.getUniqueID());

        switch (action) {
            case TOGGLE: {
                if (!GuardHandler.hasRealm(data)) {
                    send(player, TextFormatting.RED + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u60a8\u672a\u4fee\u4ed9\uff0c\u4e0d\u53ef\u64cd\u63a7");
                    return;
                }
                boolean next = !on;
                data.setBoolean(TAG_ON, next);
                if (!next) {
                    releaseInPlace(player);
                }
                if (next) {
                    int cap = entityCap(player);
                    send(player, TextFormatting.GREEN + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u5df2\u5f00\u542f\uff08\u53f3\u952e\u53ef\u64cd\u63a7\u751f\u7269\uff1b\u89c6\u7a7a\u6c14\uff1a\u7fa4\u63a7\uff1b\u6eda\u8f6e\u8c03\u8ddd\u79bb\uff1b\u51cc\u4e0b\u53f3\u952e\u66b4\u6c14\u6295\u63b7\uff09");
                    send(player, TextFormatting.DARK_AQUA + "\u3010\u5883\u9650\u3011" + realmName(player)
                        + "\uff1a\u751f\u7269\u00d7" + cap);
                } else {
                    send(player, TextFormatting.GRAY + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u5df2\u5173\u95ed");
                }
                sync(player);
                break;
            }
            case CLICK: {
                if (!on) {
                    return;
                }
                if (hold != null && isHolding(hold)) {
                    drop(player, hold, 0);
                    return;
                }
                RayTraceResult hit = pickRay(player, MAX_DIST);
                if (hit == null) {
                    return;
                }
                if (hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit instanceof EntityLivingBase) {
                    pickEntity(player, (EntityLivingBase) hit.entityHit);
                } else if (hit.typeOfHit == RayTraceResult.Type.BLOCK) {
                    send(player, TextFormatting.GRAY + "\u3010\u7075\u5297\u64cd\u63a7\u3011\u65b9\u5757\u65e0\u6cd5\u64cd\u63a7\uff0c\u53ea\u80fd\u64cd\u63a7\u751f\u7269");
                } else {
                    // 视角空中：范围群控
                    List<EntityLivingBase> targets = entitiesInView(player, 24);
                    if (targets.isEmpty()) {
                        send(player, TextFormatting.RED + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u89c6\u91ce\u5185\u6ca1\u6709\u53ef\u64cd\u63a7\u751f\u7269");
                        return;
                    }
                    int cap = entityCap(player);
                    if (targets.size() > cap) {
                        targets = new ArrayList<>(targets.subList(0, cap));
                    }
                    HoldData h = getOrCreate(player);
                    h.entities.addAll(targets);
                    send(player, TextFormatting.GREEN + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u7fa4\u63a7 " + targets.size() + " \u53ea\u751f\u7269");
                    sync(player);
                }
                break;
            }
            case SCROLL_DIST: {
                if (!on) {
                    return;
                }
                HoldData h = hold != null ? hold : getOrCreate(player);
                h.distance = MathHelper.clamp(h.distance + arg * 0.5, 1, MAX_DIST);
                sync(player);
                break;
            }
            case DROP: {
                if (on && hold != null && isHolding(hold)) {
                    drop(player, hold, MathHelper.clamp(arg, 0, MAX_CHARGE_TICKS));
                }
                break;
            }
            default:
                break;
        }
    }

    // ==================== 拿起 ====================

    private static void pickEntity(EntityPlayerMP player, EntityLivingBase target) {
        if (target == player || !target.isEntityAlive()) {
            return;
        }
        if (target.getMaxHealth() * 2.0 > player.getMaxHealth()) {
            send(player, TextFormatting.RED + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u76ee\u6807\u592a\u5f3a\uff0c\u65e0\u6cd5\u64cd\u63a7");
            return;
        }
        int cap = entityCap(player);
        HoldData h = getOrCreate(player);
        if (h.entities.size() >= cap) {
            send(player, TextFormatting.RED + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u5df2\u8fbe\u751f\u7269\u64cd\u63a7\u4e0a\u9650 \u00d7" + cap);
            return;
        }
        h.entities.add(target);
        send(player, TextFormatting.GREEN + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u5df2\u64cd\u63a7 " + target.getName()
            + " (" + h.entities.size() + "/" + cap + ")\uff1b\u53f3\u952e\u4e22\u5f03\uff0c\u51cc\u4e0b\u53f3\u952e\u66b4\u6c14\u6295\u63b7");
        sync(player);
    }

    // ==================== 判定 ====================

    private static RayTraceResult pickRay(EntityPlayer player, double dist) {
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        Vec3d end = eye.addVector(look.x * dist, look.y * dist, look.z * dist);
        RayTraceResult blockHit = player.world.rayTraceBlocks(eye, end);
        double blockDist = blockHit != null ? blockHit.hitVec.distanceTo(eye) : Double.MAX_VALUE;
        EntityLivingBase best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : player.world.loadedEntityList) {
            if (!(e instanceof EntityLivingBase) || e == player || !e.isEntityAlive()) {
                continue;
            }
            AxisAlignedBB aabb = e.getEntityBoundingBox().grow(0.3);
            RayTraceResult r = aabb.calculateIntercept(eye, end);
            if (r != null) {
                double d = r.hitVec.distanceTo(eye);
                if (d < bestDist && d < blockDist) {
                    bestDist = d;
                    best = (EntityLivingBase) e;
                }
            }
        }
        if (best != null) {
            return new RayTraceResult(best);
        }
        return blockHit;
    }

    /** 视角内生物（与视线夹角 < 15 度，血量符合判定）。 */
    private static List<EntityLivingBase> entitiesInView(EntityPlayer player, double range) {
        List<EntityLivingBase> out = new ArrayList<>();
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        if (look.lengthSquared() < 0.0001) {
            look = new Vec3d(0, -1, 0);
        }
        double maxHp = player.getMaxHealth() / 2.0;
        for (Entity e : new ArrayList<>(player.world.loadedEntityList)) {
            if (!(e instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase t = (EntityLivingBase) e;
            if (t == player || !t.isEntityAlive() || t.getMaxHealth() > maxHp) {
                continue;
            }
            Vec3d to = t.getPositionVector().addVector(0, t.height / 2, 0).subtract(eye);
            double d = to.lengthVector();
            if (d > range) {
                continue;
            }
            if (to.normalize().dotProduct(look) > 0.96) {
                out.add(t);
            }
        }
        return out;
    }

    // ==================== 每 tick：跟随与消耗 ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = event.player;
        if (player.world.isRemote || player.isDead) {
            return;
        }
        HoldData h = HOLD.get(player.getUniqueID());
        if (h == null || h.entities.isEmpty()) {
            return;
        }
        // 跟随视线移动
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        Vec3d target = eye.addVector(look.x * h.distance, look.y * h.distance * 0.85, look.z * h.distance);
        Iterator<EntityLivingBase> it = h.entities.iterator();
        while (it.hasNext()) {
            EntityLivingBase e = it.next();
            if (!e.isEntityAlive()) {
                it.remove();
                continue;
            }
            e.setPositionAndUpdate(target.x, target.y - e.height / 2.0, target.z);
            e.motionX = 0;
            e.motionY = 0;
            e.motionZ = 0;
        }
        if (h.entities.isEmpty()) {
            HOLD.remove(player.getUniqueID());
            return;
        }
        // 消耗灵力：数量 x 1.8 / 分钟
        int count = h.entities.size();
        double cost = count * COST_PER_COUNT_PER_MIN / 60.0 / 20.0;
        NBTTagCompound data = player.getEntityData();
        double power = data.getDouble("Power");
        if (power < cost) {
            send(player, TextFormatting.RED + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u7075\u529b\u4e0d\u8db3\uff0c\u81ea\u52a8\u89e3\u9664");
            releaseInPlace(player);
            return;
        }
        data.setDouble("Power", power - cost);
    }

    // ==================== 丢弃 / 投掷 ====================

    private static void drop(EntityPlayerMP player, HoldData h, int chargeTicks) {
        if (h.entities.isEmpty()) {
            HOLD.remove(player.getUniqueID());
            return;
        }
        boolean charged = chargeTicks > 0;
        float charge = charged ? Math.min(1.0F, chargeTicks / (float) MAX_CHARGE_TICKS) : 0.0F;
        // 非线性速度：无蓄力 5→10 m/s；蓄力 8→20 m/s
        double v0 = 5.0 + 3.0 * charge;
        double vMax = 10.0 + 10.0 * charge;
        double maxDist = charged ? THROW_MAX : 32.0;
        Vec3d dir = player.getLookVec().normalize();
        float impactDmg = panelDmg(player) * IMPACT_RATIO;
        // 嵌入格数：无蓄力 2 格，满蓄力 10 格，随蓄力线性提升
        int depth = PENETRATION_MIN + (int) Math.round((PENETRATION_MAX - PENETRATION_MIN) * charge);
        throwEntities(player, new ArrayList<>(h.entities), dir, v0, vMax, maxDist, impactDmg, depth);
send(player, charged
            ? TextFormatting.GOLD + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u66b4\u6c14\u6295\u63b7\uff01\u78b0\u5899\u65f6\u53ef\u5d4c\u5165\u6700\u591a " + depth + " \u683c"
            : TextFormatting.GRAY + "\u3010\u7075\u529b\u64cd\u63a7\u3011\u5df2\u4e22\u5f03");
        HOLD.remove(player.getUniqueID());
        sync(player);
    }

    /** 原地放下（不造成伤害）。 */
    private static void releaseInPlace(EntityPlayer player) {
        HoldData h = HOLD.remove(player.getUniqueID());
        if (h == null) {
            return;
        }
        for (EntityLivingBase e : h.entities) {
            if (e.isEntityAlive()) {
                e.motionX = 0;
                e.motionY = 0;
                e.motionZ = 0;
            }
        }
    }

    private static void throwEntities(EntityPlayerMP player, List<EntityLivingBase> entities,
                                      Vec3d dir, double v0, double vMax, double maxDist,
                                      float impactDmg, int depth) {
        for (EntityLivingBase e : entities) {
            if (!e.isEntityAlive()) {
                continue;
            }
            Vec3d pos = e.getPositionVector();
            ThrowFlight f = new ThrowFlight();
            f.pos = pos;
            f.dir = dir;
            f.entity = e;
            f.impactDmg = impactDmg;
            f.depth = depth;
            f.v0 = v0;
            f.vMax = vMax;
            f.maxDist = maxDist;
            f.totalTicks = (int) Math.ceil(maxDist / Math.max(0.1, (v0 + vMax) / 2.0));
            f.remainingTicks = f.totalTicks;
            FLIGHTS.add(f);
        }
    }

    // ==================== 飞行模拟 ====================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || FLIGHTS.isEmpty()) {
            return;
        }
        WorldServer world = null;
        if (FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
            world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        }
        if (world == null) {
            return;
        }
        Iterator<ThrowFlight> it = FLIGHTS.iterator();
        while (it.hasNext()) {
            ThrowFlight f = it.next();
            if (f.entity == null || !f.entity.isEntityAlive()) {
                it.remove();
                continue;
            }
            double progress = 1.0 - f.remainingTicks / (double) f.totalTicks;
            double speed = f.v0 + (f.vMax - f.v0) * Math.sqrt(progress); // 先快后缓
            double step = speed / 20.0;
            double nx = f.pos.x + f.dir.x * step;
            double ny = f.pos.y + f.dir.y * step;
            double nz = f.pos.z + f.dir.z * step;
            f.pos = new Vec3d(nx, ny, nz);
            f.traveled += step;
            f.remainingTicks--;
            boolean hitObstacle = false;
            boolean impact = false;
            if (f.traveled >= f.maxDist || f.remainingTicks <= 0) {
                impact = true;
            } else if (f.pos.y < 0 || f.pos.y > 255) {
                impact = true;
            } else {
                BlockPos bp = new BlockPos(f.pos);
                IBlockState bs = world.getBlockState(bp);
                if (bs.getBlock() != Blocks.AIR && bs.isFullBlock()) {
                    impact = true;
                    hitObstacle = true;
                }
            }
            if (!impact) {
                f.entity.setPositionAndUpdate(nx, ny, nz);
                f.entity.motionX = 0;
                f.entity.motionY = 0;
                f.entity.motionZ = 0;
                continue;
            }
            // 撞击：
            //  - 撞到障碍物：受撞击伤害 = 面板 x 80%；按碰撞体钻入 depth 格（蓄力越高钻得越深）
            //  - 没撞到东西（飞完/出界）：直接放下
            if (f.entity.isEntityAlive()) {
                if (hitObstacle) {
                    f.entity.attackEntityFrom(CAOKONG_DAMAGE, f.impactDmg);
                    if (f.entity.isEntityAlive() && f.depth > 0) {
                        Vec3d p = f.pos;
                        int n = 0;
                        while (n < f.depth) {
                            Vec3d next = p.addVector(f.dir.x, f.dir.y, f.dir.z);
                            if (next.y < 0 || next.y > 255) {
                                break;
                            }
                            if (!carveBox(world, f.entity, next)) {
                                break;
                            }
                            p = next;
                            n++;
                        }
                        f.pos = p;
                    }
                }
                f.entity.setPositionAndUpdate(f.pos.x, f.pos.y, f.pos.z);
                f.entity.motionX = 0;
                f.entity.motionY = 0;
                f.entity.motionZ = 0;
            }
            it.remove();
        }
    }

    /** 凿开碰撞箱覆盖的所有方块（基岩/不可破坏除外），逐个播方块碎裂粒子。 */
    private static boolean carveBox(World world, EntityLivingBase e, Vec3d center) {
        double w = e.width / 2.0;
        double h = e.height;
        int minX = MathHelper.floor(center.x - w);
        int minY = MathHelper.floor(center.y);
        int minZ = MathHelper.floor(center.z - w);
        int maxX = MathHelper.floor(center.x + w);
        int maxY = MathHelper.floor(center.y + h);
        int maxZ = MathHelper.floor(center.z + w);
        boolean touched = false;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (y < 0 || y > 255) {
                        continue;
                    }
                    BlockPos p = new BlockPos(x, y, z);
                    IBlockState s = world.getBlockState(p);
                    if (s.getBlock() == Blocks.AIR || s.getBlock() == Blocks.BEDROCK
                        || s.getBlockHardness(world, p) < 0) {
                        continue;
                    }
                    world.destroyBlock(p, true);
                    if (world instanceof WorldServer) {
                        ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_DUST,
                            p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                            5, 0.3, 0.3, 0.3, 0.1, net.minecraft.block.Block.getStateId(s));
                    }
                    touched = true;
                }
            }
        }
        return touched;
    }

    // ==================== 辅助 ====================

    private static HoldData getOrCreate(EntityPlayer player) {
        UUID id = player.getUniqueID();
        HoldData h = HOLD.get(id);
        if (h == null) {
            h = new HoldData();
            HOLD.put(id, h);
        }
        return h;
    }

    private static boolean isHolding(HoldData h) {
        return h != null && !h.entities.isEmpty();
    }

    /** 境界上限：可控生物数量。 */
    public static int entityCap(EntityPlayer player) {
        int jj = (int) Math.floor(player.getEntityData().getDouble("JingJieNum"));
        int mult = MathHelper.clamp(jj, 1, 5);
        return 4 * mult;
    }

    public static String realmName(EntityPlayer player) {
        String[] names = {"练气", "筑基", "结丹", "元婴", "化神"};
        int jj = (int) Math.floor(player.getEntityData().getDouble("JingJieNum"));
        return names[MathHelper.clamp(jj - 1, 0, 4)] + "期";
    }

    /** 玩家面板伤害（物理 + 法术）。 */
    public static float panelDmg(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        return (float) (data.getDouble("Attack") + data.getDouble("MagicAttack"));
    }

    private static void send(EntityPlayer player, String text) {
        player.sendMessage(new TextComponentString(text));
    }

    private static void sync(EntityPlayerMP player) {
        HoldData h = HOLD.get(player.getUniqueID());
        int entities = 0;
        if (h != null) {
            for (EntityLivingBase e : h.entities) {
                if (e.isEntityAlive()) {
                    entities++;
                }
            }
        }
        double dist = h == null ? 0 : h.distance;
        FanRenXiuXianMengZhi.NETWORK.sendTo(new CaoKongStatePacket(
            player.getEntityData().getBoolean(TAG_ON), dist, entities), player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerLoggedOutEvent event) {
        if (event.player.world.isRemote) {
            return;
        }
        HOLD.remove(event.player.getUniqueID());
    }
}