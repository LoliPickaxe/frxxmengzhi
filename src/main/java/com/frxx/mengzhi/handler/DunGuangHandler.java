package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.DunGuangActionPacket;
import com.frxx.mengzhi.network.DunGuangStatePacket;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * 遁光飞行系统（服务端）：
 *  - 结丹期(境界>=3)及以上可开启
 *  - H 键：主动遁光开关；潜行+H：被动护体开关（两种模式互斥）
 *  - 主动：碰撞箱 0.2 光球、noClip 穿透一切、速度 2x~境界上限、滚轮调速
 *  - 被动：致死伤害拦截 + 随机方位 128~256 格逃遁，触发后自动关闭
 *  - 灵力消耗线性、化神高速档额外消耗真元
 */
public class DunGuangHandler {

    public static final String TAG_ACTIVE = "DunGuangActive";
    public static final String TAG_PASSIVE = "DunGuangPassive";
    public static final String TAG_SPEED = "DunGuangSpeed";

    public static final double MIN_SPEED = 2.0;                    // 最低速 = 2 倍创造飞行
    public static final double BASE_FLY_SPEED = 0.05F;             // 原版创造飞行速度
    public static final double SIZE = 0.2;                         // 光球碰撞箱
    public static final double UNDERGROUND_FACTOR = 0.5;           // 入地减速
    public static final double COST_MIN_PER_SEC = 100.0;           // 最低速灵力消耗/秒
    public static final double ZHENYUAN_COST_PER_SEC = 50.0;       // 化神高速真元消耗/秒
    public static final double ZHENYUAN_SPEED_THRESHOLD = 30.0;    // 超过该倍速才耗真元

    public static final DamageSource DUNGUANG_ESCAPE = new DamageSource("dunguang_escape")
        .setDamageBypassesArmor();

    private static final String TAG_WAS_ALLOW_FLY = "DunGuangWasAllowFly";
    private static final String TAG_WAS_FLYING = "DunGuangWasFlying";
    private static final String TAG_WAS_FLY_SPEED = "DunGuangWasFlySpeed";

    /** sendPlayerAbilities 节流：每 10 tick 同步一次能力（原先每 tick 都发，造成卡顿） */
    private static final int ABILITY_SYNC_INTERVAL = 10;

    // ==================== 境界参数 ====================

    /** 境界速度上限：结丹 10x / 元婴 20x / 化神 50x */
    public static double maxSpeed(int realm) {
        if (realm >= 5) return 50.0;
        if (realm >= 4) return 20.0;
        return 10.0;
    }

    /** 境界档位最高灵力消耗/秒：结丹 500 / 元婴 1000 / 化神 2000 */
    public static double maxCost(int realm) {
        if (realm >= 5) return 2000.0;
        if (realm >= 4) return 1000.0;
        return 500.0;
    }

    public static int realm(EntityPlayer player) {
        return (int) Math.floor(player.getEntityData().getDouble("JingJieNum"));
    }

    public static String realmName(EntityPlayer player) {
        String[] names = {"练气", "筑基", "结丹", "元婴", "化神"};
        int jj = realm(player);
        return names[MathHelper.clamp(jj - 1, 0, 4)] + "期";
    }

    /** 当前倍速下的灵力消耗/秒（线性：最低速 100/s → 境界最高速 最高消耗/s） */
    public static double costPerSec(int realm, double speed) {
        double max = maxSpeed(realm);
        double topCost = maxCost(realm);
        return COST_MIN_PER_SEC + (speed - MIN_SPEED) * (topCost - COST_MIN_PER_SEC) / (max - MIN_SPEED);
    }

    // ==================== 入口 ====================

    public static void handleAction(EntityPlayerMP player, DunGuangActionPacket.Action action, int arg) {
        NBTTagCompound data = player.getEntityData();
        switch (action) {
            case TOGGLE_ACTIVE: {
                if (realm(player) < 3) {
                    send(player, TextFormatting.RED + "【遁光】尚未结丹，无法开启遁光飞行");
                    return;
                }
                boolean next = !data.getBoolean(TAG_ACTIVE);
                data.setBoolean(TAG_ACTIVE, next);
                if (next) {
                    data.setBoolean(TAG_PASSIVE, false);
                    if (!data.hasKey(TAG_SPEED)) {
                        data.setDouble(TAG_SPEED, MIN_SPEED);
                    }
                    data.setDouble(TAG_SPEED, MathHelper.clamp(data.getDouble(TAG_SPEED), MIN_SPEED, maxSpeed(realm(player))));
                    float flySpeed = (float) (BASE_FLY_SPEED * data.getDouble(TAG_SPEED));
                    data.setBoolean(TAG_WAS_ALLOW_FLY, player.capabilities.allowFlying);
                    data.setBoolean(TAG_WAS_FLYING, player.capabilities.isFlying);
                    data.setFloat(TAG_WAS_FLY_SPEED, player.capabilities.getFlySpeed());
                    player.capabilities.allowFlying = true;
                    player.capabilities.isFlying = true;
                    player.capabilities.setFlySpeed(flySpeed);
                    player.sendPlayerAbilities();
                    player.setNoGravity(true);
                    player.noClip = true;
                    setPlayerSize(player, (float) SIZE, (float) SIZE);
                    send(player, TextFormatting.GREEN + "【遁光】主动遁光已开启：可穿透任何物体，滚轮调速");
                } else {
                    disableActive(player);
                    send(player, TextFormatting.GRAY + "【遁光】主动遁光已关闭");
                }
                sync(player);
                break;
            }
            case TOGGLE_PASSIVE: {
                if (realm(player) < 3) {
                    send(player, TextFormatting.RED + "【遁光】尚未结丹，无法开启遁光护体");
                    return;
                }
                boolean next = !data.getBoolean(TAG_PASSIVE);
                data.setBoolean(TAG_PASSIVE, next);
                if (next) {
                    data.setBoolean(TAG_ACTIVE, false);
                    disableActive(player);
                    send(player, TextFormatting.GREEN + "【遁光】被动护体已开启：遭遇致死伤害时将随机逃遁 128~256 格");
                } else {
                    send(player, TextFormatting.GRAY + "【遁光】被动护体已关闭");
                }
                sync(player);
                break;
            }
            case SCROLL: {
                if (!data.getBoolean(TAG_ACTIVE)) {
                    return;
                }
                double max = maxSpeed(realm(player));
                double speed = MathHelper.clamp(data.getDouble(TAG_SPEED) + arg, MIN_SPEED, max);
                data.setDouble(TAG_SPEED, speed);
                player.capabilities.setFlySpeed((float) (BASE_FLY_SPEED * speed));
                player.sendPlayerAbilities();
                send(player, TextFormatting.AQUA + "【遁光】倍速 " + (int) speed + "x（消耗 " + (int) costPerSec(realm(player), speed) + " 灵力/秒）");
                sync(player);
                break;
            }
            default:
                break;
        }
    }

    private static void disableActive(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        setPlayerSize(player, 0.6F, 1.8F);
        player.noClip = false;
        player.setNoGravity(false);
        player.capabilities.isFlying = data.getBoolean(TAG_WAS_FLYING);
        player.capabilities.allowFlying = data.getBoolean(TAG_WAS_ALLOW_FLY);
        player.capabilities.setFlySpeed(data.hasKey(TAG_WAS_FLY_SPEED)
            ? data.getFloat(TAG_WAS_FLY_SPEED) : (float) BASE_FLY_SPEED);
        player.sendPlayerAbilities();
    }

    // ==================== 每 tick：状态维持与消耗 ====================

    /**
     * 服务端每 tick 开场（SERVER START，先于玩家实体更新与移动包处理）：
     * 在 EntityPlayerMP 处理 CPacketPlayer 之前就把 noClip 置位，
     * 原版 processPlayer 的反作弊（"moved wrongly" 回滚）只有当 noClip=false 才会触发，
     * 因此此处必须保证任何时刻移动包到达时 player.noClip 已是 true。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        for (EntityPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.getEntityData().getBoolean(TAG_ACTIVE)) {
                continue;
            }
            p.noClip = true;
            p.setNoGravity(true);
            p.capabilities.allowFlying = true;
            p.capabilities.isFlying = true;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = event.player;
        if (player.world.isRemote || player.isDead) {
            return;
        }
        NBTTagCompound data = player.getEntityData();
        if (data.getBoolean(TAG_ACTIVE)) {
            tickActive((EntityPlayerMP) player);
        }
    }

    private static void tickActive(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        int realm = realm(player);
        double speed = MathHelper.clamp(data.getDouble(TAG_SPEED), MIN_SPEED, maxSpeed(realm));

        // 灵力消耗
        double costPerTick = costPerSec(realm, speed) / 20.0;
        double power = data.getDouble("Power");
        if (power < costPerTick) {
            data.setBoolean(TAG_ACTIVE, false);
            disableActive(player);
            sync(player);
            send(player, TextFormatting.RED + "【遁光】灵力不足，遁光已自动关闭");
            return;
        }
        data.setDouble("Power", power - costPerTick);

        // 化神高速档：30x 以上额外消耗真元 50/秒
        // 真元不足时不强制关闭（否则真元未回满前将无法再次开启），
        // 而是自动把倍速降到 30x 以下继续飞行，全部灵力仍可全额使用
        if (speed >= ZHENYUAN_SPEED_THRESHOLD) {
            double zyCost = ZHENYUAN_COST_PER_SEC / 20.0;
            double zy = data.getDouble("Base");
            if (zy >= zyCost) {
                data.setDouble("Base", zy - zyCost);
            } else {
                speed = ZHENYUAN_SPEED_THRESHOLD - 1.0;
                data.setDouble(TAG_SPEED, speed);
                send(player, TextFormatting.YELLOW + "【遁光】真元不足，倍速自动降至 " + (int) speed + "x（真元恢复后可再上调）");
            }
        }

        // 穿透 + 飞行速度
        player.noClip = true;
        player.setNoGravity(true);
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        double factor = isUnderground(player) ? UNDERGROUND_FACTOR : 1.0;
        player.capabilities.setFlySpeed((float) (BASE_FLY_SPEED * speed * factor));
        if ((player.ticksExisted % ABILITY_SYNC_INTERVAL) == 0) {
            player.sendPlayerAbilities();
        }
        player.setInvisible(true);
        player.fallDistance = 0.0F;
        // 尺寸仅在实际变化时才设置（反射开销大，避免每 tick 无谓调用）
        if (player.width != (float) SIZE || player.height != (float) SIZE) {
            setPlayerSize(player, (float) SIZE, (float) SIZE);
        }

        // 服务端粒子（供他人观察；隔 tick 发射，降低每 tick 网络开销导致的卡顿）
        if ((player.ticksExisted & 1) == 0) {
            if (player.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) player.world;
                double px = player.posX;
                double py = player.posY + SIZE / 2.0;
                double pz = player.posZ;
                // 中心光球（白色竖光）
                ws.spawnParticle(EnumParticleTypes.END_ROD, px, py, pz, 1, 0, 0, 0, 0.0, 0, 0, 0);
                // 拖尾（沿运动反方向，白色烟）
                ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    px - player.motionX * 0.5, py - player.motionY * 0.5, pz - player.motionZ * 0.5,
                    1, 0, 0, 0, 0.0, 0, 0, 0);
            }
        }
    }

    /** 玩家是否身处方块内部（潜入地下）。 */
    private static boolean isUnderground(EntityPlayer player) {
        BlockPos pos = new BlockPos(player.posX, player.posY + 0.1, player.posZ);
        IBlockState s = player.world.getBlockState(pos);
        if (s.getBlock() != Blocks.AIR && s.isFullBlock()) {
            return true;
        }
        IBlockState up = player.world.getBlockState(pos.up());
        return up.getBlock() != Blocks.AIR && up.isFullBlock();
    }

    // ==================== 被动护体：致死拦截 + 逃遁 ====================

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
        if (player.world.isRemote) {
            return;
        }
        NBTTagCompound data = player.getEntityData();
        if (!data.getBoolean(TAG_PASSIVE)) {
            return;
        }
        // 必死判定：本次伤害足以清空当前生命
        float damage = event.getAmount();
        if (damage < player.getHealth()) {
            return;
        }
        // 拦截致死伤害
        event.setCanceled(true);
        player.setHealth(Math.max(1.0F, player.getHealth() - damage * 0.01F));
        player.hurtResistantTime = 40;

        // 随机方位逃遁 128~256 格
        WorldServer ws = (WorldServer) player.world;
        Random rnd = player.getRNG();
        double px = player.posX;
        double pz = player.posZ;
        double tx = px;
        double tz = pz;
        double ty = player.posY;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            double dist = 128.0 + rnd.nextInt(129);
            int nx = MathHelper.floor(px + Math.cos(angle) * dist);
            int nz = MathHelper.floor(pz + Math.sin(angle) * dist);
            BlockPos top = ws.getTopSolidOrLiquidBlock(new BlockPos(nx, 0, nz));
            if (top.getY() <= 1 || top.getY() > 250) {
                continue;
            }
            tx = nx + 0.5;
            tz = nz + 0.5;
            ty = top.getY() + 1.0;
            break;
        }
        player.setPositionAndUpdate(tx, ty, tz);
        player.motionX = 0;
        player.motionY = 0;
        player.motionZ = 0;
        player.fallDistance = 0.0F;

        // 触发后护体自动关闭，恢复正常飞行
        data.setBoolean(TAG_PASSIVE, false);
        data.setBoolean(TAG_ACTIVE, false);
        disableActive(player);
        send(player, TextFormatting.GOLD + "【遁光】护体触发：已遁至 (" + MathHelper.floor(tx) + ", " + MathHelper.floor(ty)
            + ", " + MathHelper.floor(tz) + ")");

        // 逃遁点视觉
        ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, px, player.posY, pz, 1, 0, 0, 0, 0.0, 0, 0, 0);
        ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, tx, ty, tz, 5, 0, 0, 0, 0.0, 0, 0, 0);
        sync(player);
    }

    // ==================== 反射：修改玩家碰撞箱 ====================

    /** Entity#setSize 是 protected，使用反射修改尺寸字段并重建包围盒。
     *  dev 环境字段名 width/height，反混淆后是 field_70130_N/field_70131_O，需兜底。 */
    public static void setPlayerSize(EntityPlayer player, float width, float height) {
        try {
            Field fw = findEntityField("width", "field_70130_N");
            Field fh = findEntityField("height", "field_70131_O");
            if (fw != null) {
                fw.setFloat(player, width);
            }
            if (fh != null) {
                fh.setFloat(player, height);
            }
        } catch (Exception ignored) {
            return;
        }
        double cx = player.posX;
        double cy = player.posY;
        double cz = player.posZ;
        player.setEntityBoundingBox(new net.minecraft.util.math.AxisAlignedBB(
            cx - width / 2.0, cy, cz - width / 2.0,
            cx + width / 2.0, cy + height, cz + width / 2.0));
    }

    /** 依次尝试若干个字段名（dev 名 / SRG 混淆名），找到就返回。 */
    private static Field findEntityField(String... names) {
        for (String name : names) {
            try {
                Field f = Entity.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // ==================== 辅助 ====================

    private static void send(EntityPlayer player, String text) {
        player.sendMessage(new TextComponentString(text));
    }

    public static void sync(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        FanRenXiuXianMengZhi.NETWORK.sendTo(new DunGuangStatePacket(
            data.getBoolean(TAG_ACTIVE),
            data.getBoolean(TAG_PASSIVE),
            data.getDouble(TAG_SPEED),
            realm(player)), player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerLoggedOutEvent event) {
        if (event.player.world.isRemote) {
            return;
        }
        if (event.player instanceof EntityPlayerMP) {
            disableActive((EntityPlayerMP) event.player);
        }
    }
}