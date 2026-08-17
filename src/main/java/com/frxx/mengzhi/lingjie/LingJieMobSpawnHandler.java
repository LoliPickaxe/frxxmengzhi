package com.frxx.mengzhi.lingjie;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 灵界刷怪（设计文档 §3）：
 *  - 宿主实体全部来自妖兽模组（yvanchuyaoshou），通过反射实例化，妖兽模组缺失时自动禁用；
 *  - 生成后改写自定义名（如 "结丹中期散修"），由妖兽模组既有 Procedure*XinXi 按名称关键字
 *    自动套用结丹/元婴/化神档属性与掉落，不再新建属性公式；
 *  - 档位权重：结丹 65%（主力）> 元婴 25% > 化神 10%（首领向）；
 *  - NoClear + persistenceRequired 防止妖兽模组自身清除与主世界机制的远距 despawn；
 *  - 玩家周边 48 格内妖兽实体上限 18，每 3 秒尝试补刷 1 只。
 */
public class LingJieMobSpawnHandler {

    private static final String ENTITY_PREFIX = "net.mcreator.yvanchuyaoshou.entity.";
    private static final String ENTITY_INNER = "$EntityCustom";
    private static final String KEY_NAME = "\u540d\u79f0"; // 名称
    private static final String KEY_NOCLEAR = "NoClear";

    private static final int SPAWN_INTERVAL = 60;
    private static final int CAP_RADIUS = 48;
    private static final int CAP_COUNT = 18;

    private static final Random RANDOM = new Random();

    private static final Map<UUID, Integer> TICK_COUNTERS = new HashMap<>();
    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static volatile boolean mobsUnavailable = false;

    private static class Host {
        final String className;
        final String label;

        Host(String className, String label) {
            this.className = className;
            this.label = label;
        }
    }

    private static final Host[][] TIERS = {
        {
            new Host("EntitySanXiu01", "\u6563\u4fee"), new Host("EntitySanXiu0102", "\u6563\u4fee"),
            new Host("EntitySanXiu02", "\u6563\u4fee"), new Host("EntitySanXiu0202", "\u6563\u4fee"),
            new Host("EntitySanXiu03", "\u6563\u4fee"), new Host("EntitySanXiu0302", "\u6563\u4fee"),
            new Host("EntitySanXiu04", "\u6563\u4fee"), new Host("EntitySanXiu0402", "\u6563\u4fee"),
            new Host("EntitySanXiu05", "\u6563\u4fee"), new Host("EntitySanXiu0502", "\u6563\u4fee"),
            new Host("EntityMoXiu01", "\u9b54\u4fee"), new Host("EntityMoXiu0102", "\u9b54\u4fee"),
            new Host("EntityMoXiu02", "\u9b54\u4fee"), new Host("EntityMoXiu0202", "\u9b54\u4fee"),
            new Host("EntityMoXiu03", "\u9b54\u4fee"), new Host("EntityMoXiu0302", "\u9b54\u4fee"),
            new Host("EntityGuLang0201", "\u5996\u517d"), new Host("EntityGuLang0401", "\u5996\u517d"),
            new Host("EntityBaiYuZhiZhu", "\u5996\u517d"), new Host("EntityXueYuZhiZhu", "\u5996\u517d"),
            new Host("EntityYingXiao02", "\u5996\u517d"), new Host("EntityYingXiao03", "\u5996\u517d"),
            new Host("EntityJunEYi02", "\u5996\u517d"), new Host("EntityJunEYi03", "\u5996\u517d"),
            new Host("EntityHunTieShou01", "\u5996\u517d"), new Host("EntityHunTieShou02", "\u5996\u517d"),
            new Host("EntityHunTieShou03", "\u5996\u517d"), new Host("EntityHunTieShou04", "\u5996\u517d"),
            new Host("EntityJiHunXie01", "\u5996\u517d"), new Host("EntityJiHunXie02", "\u5996\u517d"),
            new Host("EntityHeiZhu", "\u5996\u517d"), new Host("EntityHeiZhu2", "\u5996\u517d"),
            new Host("EntityYuWa", "\u5996\u517d"), new Host("EntityYuWa2", "\u5996\u517d"),
            new Host("EntityShouGuanYinBing02", "\u5996\u517d"), new Host("EntityShouGuanYinBing03", "\u5996\u517d"),
            new Host("EntityShiFengTuo04", "\u5996\u517d"), new Host("EntityLeiBanLu03", "\u5996\u517d")
        },
        {
            new Host("EntitySanXiu04", "\u6563\u4fee"), new Host("EntitySanXiu0402", "\u6563\u4fee"),
            new Host("EntitySanXiu05", "\u6563\u4fee"), new Host("EntitySanXiu0502", "\u6563\u4fee"),
            new Host("EntityMoXiu04", "\u9b54\u4fee"), new Host("EntityMoXiu0402", "\u9b54\u4fee"),
            new Host("EntityMoXiu05", "\u9b54\u4fee"), new Host("EntityMoXiu0502", "\u9b54\u4fee"),
            new Host("EntityYingXiao04", "\u5996\u517d"), new Host("EntityYingXiao05", "\u5996\u517d"),
            new Host("EntityJunEYi04", "\u5996\u517d"), new Host("EntityJunEYi05", "\u5996\u517d"),
            new Host("EntityLeiBanLu04", "\u5996\u517d"), new Host("EntityLeiYuanJuE04", "\u5996\u517d"),
            new Host("EntityLinHaiShouHuang04", "\u5996\u517d"), new Host("EntityShiFengTuo05", "\u5996\u517d")
        },
        {
            new Host("EntityHeHuanZong2", "\u5408\u6b22\u5b97"), new Host("EntityHeHuanZong3", "\u5408\u6b22\u5b97"),
            new Host("EntityTianGong102", "\u5929\u5bab"), new Host("EntityTianGong2", "\u5929\u5bab"),
            new Host("EntityTianGong3", "\u5929\u5bab"),
            new Host("EntityLeiBanLu05", "\u5996\u517d"), new Host("EntityLeiYuanJuE05", "\u5996\u517d"),
            new Host("EntityLinHaiShouHuang05", "\u5996\u517d"), new Host("EntityGuLang0501", "\u5996\u517d")
        }
    };

    private static final String[] REALM_NAMES = {"\u7ed3\u4e39", "\u5143\u5a74", "\u5316\u795e"}; // 结丹/元婴/化神
    private static final String[] PHASES = {"\u521d\u671f", "\u4e2d\u671f", "\u540e\u671f"}; // 初期/中期/后期
    private static final int[] TIER_WEIGHT = {65, 25, 10};

    private static Class<?> entityClass(String className) {
        Class<?> cached = CLASS_CACHE.get(className);
        if (cached != null) {
            return cached;
        }
        if (FAILED.contains(className)) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(ENTITY_PREFIX + className + ENTITY_INNER);
            CLASS_CACHE.put(className, clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            if (FanRenXiuXianMengZhi.logger != null) {
                FanRenXiuXianMengZhi.logger.warn("灵界刷怪：妖兽模组实体 {} 不存在，跳过该宿主", className);
            }
            FAILED.add(className);
            return null;
        }
    }

    private static int pickTier() {
        int roll = RANDOM.nextInt(100);
        if (roll < TIER_WEIGHT[0]) {
            return 0;
        }
        if (roll < TIER_WEIGHT[0] + TIER_WEIGHT[1]) {
            return 1;
        }
        return 2;
    }

    private static EntityLiving createEntity(World world, Host host) {
        Class<?> clazz = entityClass(host.className);
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<?> ctor = clazz.getConstructor(World.class);
            return (EntityLiving) ctor.newInstance(world);
        } catch (Exception e) {
            if (!FAILED.contains(host.className)) {
                FAILED.add(host.className);
                if (FanRenXiuXianMengZhi.logger != null) {
                    FanRenXiuXianMengZhi.logger.warn("灵界刷怪：妖兽实体 {} 构造失败，跳过该宿主：{}", host.className, e);
                }
            }
            return null;
        }
    }

    private static BlockPos findGround(World world, int x, int z, int startY) {
        for (int y = startY; y >= 3; y--) {
            BlockPos below = new BlockPos(x, y - 1, z);
            if (world.getBlockState(below).isFullBlock()) {
                BlockPos at = new BlockPos(x, y, z);
                BlockPos above = new BlockPos(x, y + 1, z);
                if (!world.getBlockState(at).isFullBlock() && !world.getBlockState(above).isFullBlock()) {
                    return at;
                }
            }
        }
        return null;
    }

    private static boolean spawnOne(EntityPlayer player) {
        World world = player.world;
        int tier = pickTier();
        Host[] tierHosts = TIERS[tier];
        Host host = tierHosts[RANDOM.nextInt(tierHosts.length)];
        EntityLiving entity = createEntity(world, host);
        if (entity == null) {
            return false;
        }
        double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
        double dist = 14.0D + RANDOM.nextDouble() * 8.0D;
        int tx = (int) (player.posX + Math.cos(angle) * dist);
        int tz = (int) (player.posZ + Math.sin(angle) * dist);
        BlockPos ground = findGround(world, tx, tz, (int) player.posY + 2);
        if (ground == null) {
            entity.setDead();
            return false;
        }
        String name = REALM_NAMES[tier] + PHASES[RANDOM.nextInt(3)] + host.label;
        entity.setCustomNameTag(name);
        NBTTagCompound data = entity.getEntityData();
        data.setString(KEY_NAME, name);
        data.setBoolean(KEY_NOCLEAR, true);
        entity.enablePersistence();
        entity.setLocationAndAngles(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D,
            RANDOM.nextFloat() * 360.0F, 0.0F);
        world.spawnEntity(entity);
        return true;
    }

    private static int countNearby(EntityPlayer player) {
        AxisAlignedBB box = new AxisAlignedBB(player.posX, player.posY, player.posZ,
            player.posX, player.posY, player.posZ).grow(CAP_RADIUS);
        return player.world.getEntitiesWithinAABB(Entity.class, box,
            e -> e != null && e.getClass().getName().startsWith(ENTITY_PREFIX)).size();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        if (player.dimension != LingJieDimension.DIMENSION_ID) {
            TICK_COUNTERS.remove(player.getUniqueID());
            return;
        }
        if (mobsUnavailable) {
            return;
        }
        UUID uuid = player.getUniqueID();
        int counter = TICK_COUNTERS.getOrDefault(uuid, 0) + 1;
        if (counter < SPAWN_INTERVAL) {
            TICK_COUNTERS.put(uuid, counter);
            return;
        }
        TICK_COUNTERS.put(uuid, 0);
        if (countNearby(player) >= CAP_COUNT) {
            return;
        }
        spawnOne(player);
    }
}