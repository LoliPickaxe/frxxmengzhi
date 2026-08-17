package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.SelfDestructShakePacket;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelfDestructHandler {

    public static final int PRE_CAST_TICKS = 60;

    private static final DamageSource SELF_DEATH = new DamageSource("selfDestruct").setDamageBypassesArmor().setDamageIsAbsolute();
    private static final DamageSource BODY_EXPLOSION = new DamageSource("bodyExplosion").setExplosion();
    private static final DamageSource SOUL_EXPLOSION = new DamageSource("soulExplosion").setDamageBypassesArmor().setDamageIsAbsolute();

    private static final Map<UUID, PreCast> countdowns = new ConcurrentHashMap<UUID, PreCast>();
    private static final Map<UUID, PendingBlast> blasts = new ConcurrentHashMap<UUID, PendingBlast>();

    private static class PreCast {
        int ticks;
        final boolean physical;
        final boolean soul;

        PreCast(int ticks, boolean physical, boolean soul) {
            this.ticks = ticks;
            this.physical = physical;
            this.soul = soul;
        }
    }

    private static class PendingBlast {
        final WorldServer world;
        final long[] positions;
        int index;

        PendingBlast(WorldServer world, long[] positions) {
            this.world = world;
            this.positions = positions;
        }
    }

    /** 境界 -> 爆炸半径: 练气6 筑基8 结丹10 元婴16 化神32 道祖及以上 128 */
    public static int getBlastRadius(NBTTagCompound data) {
        int jingJie = (int) Math.floor(data.getDouble("JingJieNum"));
        switch (jingJie) {
            case 1: return 6;
            case 2: return 8;
            case 3: return 10;
            case 4: return 16;
            case 5: return 32;
            default:
                return jingJie >= 6 ? 128 : 6;
        }
    }

    public static void startSelfDestruct(EntityPlayerMP player, boolean physical, boolean soul) {
        NBTTagCompound data = player.getEntityData();
        if (!GuardHandler.hasRealm(data)) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u60a8\u672a\u4fee\u4ed9\u4e0d\u53ef\u4f7f\u7528\u81ea\u7206"));
            return;
        }
        if (countdowns.containsKey(player.getUniqueID())) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u81ea\u7206\u51c6\u5907\u4e2d..."));
            return;
        }
        countdowns.put(player.getUniqueID(), new PreCast(PRE_CAST_TICKS, physical, soul));
        int color = physical && soul ? 0xFFCC44FF : (soul ? 0xFFB43CFF : 0xFFFF3C3C);
        FanRenXiuXianMengZhi.NETWORK.sendTo(new SelfDestructShakePacket(PRE_CAST_TICKS, 0.8F, color), player);
        player.getServerWorld().getMinecraftServer().getPlayerList().sendMessage(
            new TextComponentString(TextFormatting.RED + "[\u81ea\u7206] " + player.getName()
                + " \u6b63\u5728\u79ef\u84c4\u80fd\u91cf\uff0c3\u79d2\u540e\u81ea\u7206\uff01"));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (countdowns.isEmpty() && blasts.isEmpty()) return;
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return;

        if (!countdowns.isEmpty()) {
            Iterator<Map.Entry<UUID, PreCast>> it = countdowns.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, PreCast> entry = it.next();
                EntityPlayerMP player = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(entry.getKey());
                if (player == null || !player.isEntityAlive()) {
                    it.remove();
                    continue;
                }
                PreCast cast = entry.getValue();
                if (cast.ticks <= 0) {
                    it.remove();
                    detonate(player, cast);
                } else {
                    spawnPreCastParticles(player, cast);
                    cast.ticks--;
                }
            }
        }

        processBlasts();
    }

    private static void spawnPreCastParticles(EntityPlayerMP player, PreCast cast) {
        WorldServer world = player.getServerWorld();
        Random rand = world.rand;
        double dist = 0.5 + cast.ticks * 0.25;
        for (int i = 0; i < 24; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * rand.nextDouble() - 1);
            double dx = Math.sin(phi) * Math.cos(theta);
            double dy = Math.cos(phi);
            double dz = Math.sin(phi) * Math.sin(theta);
            boolean red = !cast.soul || (cast.physical && rand.nextBoolean());
            world.spawnParticle(EnumParticleTypes.REDSTONE,
                player.posX + dx * dist, player.posY + 1.0 + dy * dist, player.posZ + dz * dist,
                1, 0.0D, 0.0D, 0.0D, 0.0D,
                red ? 255 : 180, red ? 60 : 60, red ? 60 : 255);
        }
    }

    private static void detonate(EntityPlayerMP player, PreCast cast) {
        WorldServer world = player.getServerWorld();
        NBTTagCompound data = player.getEntityData();
        Random rand = world.rand;
        int radius = getBlastRadius(data);
        double cx = player.posX;
        double cy = player.posY;
        double cz = player.posZ;

        world.playSound(null, cx, cy, cz, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, 0.9F);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, cx, cy + 1, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, cx, cy + 1, cz, 4, 1.5D, 1.5D, 1.5D, 0.0D);

        int burst = Math.min(600, radius * 8);
        for (int i = 0; i < burst; i++) {
            double theta = rand.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * rand.nextDouble() - 1);
            double dx = Math.sin(phi) * Math.cos(theta);
            double dy = Math.cos(phi);
            double dz = Math.sin(phi) * Math.sin(theta);
            boolean red = !cast.soul || (cast.physical && rand.nextBoolean());
            world.spawnParticle(EnumParticleTypes.REDSTONE,
                cx + dx * radius, cy + 1 + dy * radius, cz + dz * radius,
                1, 0.0D, 0.0D, 0.0D, 0.0D,
                red ? 255 : 180, red ? 60 : 60, red ? 60 : 255);
        }

        double maxDamage = (data.getDouble("Attack") + data.getDouble("MagicAttack")) * 100.0;
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class,
            new AxisAlignedBB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius));
        for (EntityLivingBase target : targets) {
            if (target == player) continue;
            double dx = target.posX - cx;
            double dy = target.posY - cy;
            double dz = target.posZ - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > radius) continue;
            if (dist < 0.01) dist = 0.01;
            double factor = Math.max(0.6, 1.0 - 0.05 * dist);
            float dmg = (float) (maxDamage * factor);
            double kb = (1.0 - dist / radius) * 1.5;
            target.motionX += dx / dist * kb;
            target.motionY += 0.2 + 0.3 * (1.0 - dist / radius);
            target.motionZ += dz / dist * kb;
            target.velocityChanged = true;
            if (cast.physical && cast.soul) {
                target.attackEntityFrom(BODY_EXPLOSION, dmg / 2.0F);
                target.attackEntityFrom(SOUL_EXPLOSION, dmg / 2.0F);
            } else if (cast.physical) {
                target.attackEntityFrom(BODY_EXPLOSION, dmg);
            } else {
                target.attackEntityFrom(SOUL_EXPLOSION, dmg);
            }
        }

        double shouYuan = data.getDouble("ShouYuan");
        if (shouYuan > 0) {
            double factor = cast.physical && cast.soul ? 0.80 : (cast.soul ? 0.85 : 0.90);
            data.setDouble("ShouYuan", shouYuan * factor);
        }
        if (!player.attackEntityFrom(SELF_DEATH, Float.MAX_VALUE)) {
            player.setHealth(0.0F);
            player.setDead();
        }

        long[] positions = collectSphere(world, cx, cy, cz, radius);
        blasts.put(player.getUniqueID(), new PendingBlast(world, positions));

        world.getMinecraftServer().getPlayerList().sendMessage(
            new TextComponentString(TextFormatting.RED + "[\u81ea\u7206] " + player.getName() + " \u81ea\u7206\u4e86\uff01"));
    }

    /** 收集球形范围内非空气/基岩/屏障的方块（打包为 long 数组省内存） */
    private static long[] collectSphere(WorldServer world, double cx, double cy, double cz, double radius) {
        int r = (int) Math.ceil(radius);
        int minX = MathHelper.floor(cx - r);
        int maxX = MathHelper.floor(cx + r);
        int minY = Math.max(0, MathHelper.floor(cy - r));
        int maxY = Math.min(255, MathHelper.floor(cy + r));
        int minZ = MathHelper.floor(cz - r);
        int maxZ = MathHelper.floor(cz + r);
        double r2 = radius * radius;
        List<Long> list = new java.util.ArrayList<Long>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dx = x + 0.5 - cx;
                    double dy = y + 0.5 - cy;
                    double dz = z + 0.5 - cz;
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    IBlockState state = world.getBlockState(pos.setPos(x, y, z));
                    Block block = state.getBlock();
                    if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.BARRIER) continue;
                    list.add(((long) (z & 0xFFFF)) << 24 | ((long) (y & 0xFF)) << 16 | ((long) (x & 0xFFFF)));
                }
            }
        }
        long[] arr = new long[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    /** 供灵气炸弹等复用：收集并异步破坏球体方块（无掉落，分批+区块刷新） */
    public static void destroySphereAsync(WorldServer world, double cx, double cy, double cz, double radius) {
        long[] positions = collectSphere(world, cx, cy, cz, radius);
        if (positions.length == 0) return;
        blasts.put(UUID.randomUUID(), new PendingBlast(world, positions));
    }

    /** 分批破坏方块（无掉落），完成一个区块就整体刷新给客户端 */
    private static void processBlasts() {
        if (blasts.isEmpty()) return;
        Iterator<Map.Entry<UUID, PendingBlast>> it = blasts.entrySet().iterator();
        while (it.hasNext()) {
            PendingBlast blast = it.next().getValue();
            WorldServer world = blast.world;
            if (world == null || world.getMinecraftServer() == null) {
                it.remove();
                continue;
            }
            int budget = Math.max(24000, Math.min(120000, blast.positions.length / 150));
            int end = Math.min(blast.positions.length, blast.index + budget);
            Set<Long> chunks = new HashSet<Long>();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = blast.index; i < end; i++) {
                long v = blast.positions[i];
                int x = (int) ((short) (v & 0xFFFF));
                int y = (int) ((v >> 16) & 0xFF);
                int z = (int) ((short) ((v >> 24) & 0xFFFF));
                world.setBlockState(pos.setPos(x, y, z), Blocks.AIR.getDefaultState(), 0);
                chunks.add((((long) (x >> 4) & 0xFFFFL) << 16) | ((long) (z >> 4) & 0xFFFFL));
            }
            blast.index = end;
            if (!chunks.isEmpty()) {
                for (long key : chunks) {
                    int chunkX = (int) ((short) ((key >> 16) & 0xFFFF));
                    int chunkZ = (int) ((short) (key & 0xFFFF));
                    if (world.getChunkProvider().getLoadedChunk(chunkX, chunkZ) == null) continue;
                    Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
                    SPacketChunkData packet = new SPacketChunkData(chunk, 65535);
                    for (Object p : world.playerEntities) {
                        ((EntityPlayerMP) p).connection.sendPacket(packet);
                    }
                }
            }
            if (blast.index >= blast.positions.length) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.entity.player.EntityPlayer)) return;
        UUID id = event.getEntity().getUniqueID();
        if (countdowns.remove(id) != null) {
            if (event.getEntity().world != null && !event.getEntity().world.isRemote
                && event.getEntity().world.getMinecraftServer() != null) {
                event.getEntity().world.getMinecraftServer().getPlayerList().sendMessage(
                    new TextComponentString(TextFormatting.GRAY + "[\u81ea\u7206] " + event.getEntity().getName()
                        + " \u7684\u81ea\u7206\u88ab\u6253\u65ad"));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        countdowns.remove(id);
        blasts.remove(id);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        UUID id = event.player.getUniqueID();
        if (countdowns.remove(id) != null) {
            event.player.sendMessage(new TextComponentString(TextFormatting.GRAY + "\u81ea\u7206\u88ab\u6253\u65ad"));
        }
        blasts.remove(id);
    }
}
