package com.frxx.mengzhi.lingjie;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 灵界灵石矿脉生成器。
 *
 * 基准（人界，来自凡人修仙本体 BlockFangkuailingshi03/05）：
 *  - 低阶灵石矿 fangkuailingshi03：每区块 20 次尝试，矿脉 6 格
 *  - 高阶灵石矿 fangkuailingshi05：每区块 1 次尝试，矿脉 4 格
 *
 * 灵界规则（仅 lingjie 维度生效；维度地形复用主世界生成器，地表约 y63~128，
 * 因此矿脉全部生成在地表之下，并在地表下方留避让带，避免矿石悬空或裸露地面）：
 *  - 中层 y31~55：低阶灵石矿 03 密集（40 次/24 格）+ 灵石块 08 / 灵石砖 09 缀饰 + 高阶 ×2
 *  - 深层 y3~30：低阶灵石矿 03 主量（频率 ×5=100 次 ÷1.5 ≈ 67 次、矿脉 ×5=30 格）+ 高阶 ×4
 *  - 洞穴壁面 y24~55：低阶灵石矿只补在洞壁空气邻面的石头上（挖洞可见，不裸露地表）
 *  - 熔岩带 y0~3：中阶灵石块 08 少量（×3）
 *  - 地表避让带 y56 以上：不生成矿脉（防止裸露地面）
 *  - 巨型矿脉：极小概率（每区块约 1%）触发，矿脉 = 人界 ×20（120 格），低阶为主并夹带高阶
 */
public class LingJieOreGenerator implements IWorldGenerator {

    public static final String MOD_ID = "yvanchuxiuzhen";

    // 人界基线
    private static final int BASE_LOW_VEIN = 6;
    private static final int BASE_HIGH_VEIN = 4;

    // 灵界倍率
    private static final int LOW_VEIN_MULT = 5;
    private static final int HIGH_VEIN_MULT = 3;
    private static final int GIANT_VEIN_MULT = 20;
    private static final double GIANT_CHANCE = 0.01D;
    // 2026-08-16 调参（仅频率，矿脉大小不变）：低阶 ÷1.5、中阶 08 ×3、高阶 05 ×2
    private static final int LOW_DEEP_ATTEMPTS = 67; // 人界 20 ×5 = 100，÷1.5 ≈ 67

    // 垂直分层高度区间（设计文档 §1.2，按主世界地形修正：全部低于地表）
    private static final int MID_Y_MIN = 31;
    private static final int MID_Y_MAX = 55;
    private static final int DEEP_Y_MIN = 3;
    private static final int DEEP_Y_MAX = 30;
    private static final int LAVA_Y_MIN = 0;
    private static final int LAVA_Y_MAX = 3;

    private final Map<String, Block> cache = new HashMap<>();

    private static volatile boolean loggedFirstRun = false;

    private Block stone() {
        return Blocks.STONE;
    }

    private Block resolveBlock(String name) {
        Block b = Block.REGISTRY.getObject(new ResourceLocation(MOD_ID, name));
        if (b == null || b == Blocks.AIR) {
            b = Block.getBlockFromName(MOD_ID + ":" + name);
        }
        return (b == null || b == Blocks.AIR) ? null : b;
    }

    private Block spiritBlock(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        Block b = resolveBlock(name);
        cache.put(name, b);
        if (FanRenXiuXianMengZhi.logger != null) {
            FanRenXiuXianMengZhi.logger.info("灵界矿脉：方块 {}:{} 解析结果 = {}",
                MOD_ID, name, b == null ? "NULL(未找到)" : b.getRegistryName());
        }
        return b;
    }

    private void makeVeins(Random random, int chunkX, int chunkZ, World world,
                           Block ore, int attempts, int veinSize, int minY, int maxY) {
        if (ore == null) {
            return;
        }
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        for (int i = 0; i < attempts; i++) {
            int cx = baseX + 1 + random.nextInt(14);
            int cy = minY + random.nextInt(maxY - minY + 1);
            int cz = baseZ + 1 + random.nextInt(14);
            for (int t = 0; t < veinSize; t++) {
                int x = cx + random.nextInt(6) - 3;
                int y = cy + random.nextInt(4) - 2;
                int z = cz + random.nextInt(6) - 3;
                if (x >= baseX + 1 && x < baseX + 15 && z >= baseZ + 1 && z < baseZ + 15
                    && world.getBlockState(new BlockPos(x, y, z)).getBlock() == stone()) {
                    world.setBlockState(new BlockPos(x, y, z), ore.getDefaultState(), 2);
                }
            }
        }
    }

    private boolean exposedToAir(World world, BlockPos pos) {
        return world.isAirBlock(pos.up()) || world.isAirBlock(pos.down())
            || world.isAirBlock(pos.north()) || world.isAirBlock(pos.south())
            || world.isAirBlock(pos.east()) || world.isAirBlock(pos.west());
    }

    private void makeExposedVeins(Random random, int chunkX, int chunkZ, World world,
                                  Block ore, int attempts, int veinSize, int minY, int maxY) {
        if (ore == null) {
            return;
        }
        for (int i = 0; i < attempts; i++) {
            for (int t = 0; t < veinSize; t++) {
                int x = chunkX * 16 + 1 + random.nextInt(14);
                int y = minY + random.nextInt(maxY - minY + 1);
                int z = chunkZ * 16 + 1 + random.nextInt(14);
                BlockPos pos = new BlockPos(x, y, z);
                if (world.getBlockState(pos).getBlock() == stone() && exposedToAir(world, pos)) {
                    world.setBlockState(pos, ore.getDefaultState(), 2);
                }
            }
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        int dim = world.provider.getDimension();
        if (!loggedFirstRun) {
            loggedFirstRun = true;
            if (FanRenXiuXianMengZhi.logger != null) {
                FanRenXiuXianMengZhi.logger.info("灵界矿脉生成器首次运行：world.dim={} 期望lingjie={}",
                    dim, LingJieDimension.DIMENSION_ID);
            }
        }
        if (dim != LingJieDimension.DIMENSION_ID) {
            return;
        }

        Block low = spiritBlock("fangkuailingshi03");
        Block high = spiritBlock("fangkuailingshi05");
        Block deco8 = spiritBlock("fangkuailingshi08");
        Block deco9 = spiritBlock("fangkuailingshi09");

        // 中层 y31~55：低阶灵石矿 03 密集层（调参低阶 ÷1.5 = 40 次）
        makeVeins(random, chunkX, chunkZ, world, low, 40,
            BASE_LOW_VEIN * 4, MID_Y_MIN, MID_Y_MAX);
        // 高阶 ×2（原有 1）
        makeVeins(random, chunkX, chunkZ, world, high, 2,
            BASE_HIGH_VEIN * HIGH_VEIN_MULT, MID_Y_MIN, MID_Y_MAX);
        // 灵石块 08（×3，原有 6）/ 灵石砖 09 缀饰
        makeVeins(random, chunkX, chunkZ, world, deco8, 18, 4, MID_Y_MIN, MID_Y_MAX);
        makeVeins(random, chunkX, chunkZ, world, deco9, 2, 3, MID_Y_MIN, MID_Y_MAX);

        // 洞穴壁面矿脉 y24~55：只生成在有空气邻面的石头上（洞壁/坑道可见，不裸露地表）
        makeExposedVeins(random, chunkX, chunkZ, world, low, 24, 6, 24, 55);

        // 深层 y3~30：低阶灵石矿 03 主量（÷1.5 ≈ 67 次、矿脉 ×5=30 格），高阶 ×4（原有 2）
        makeVeins(random, chunkX, chunkZ, world, low,
            LOW_DEEP_ATTEMPTS,
            BASE_LOW_VEIN * LOW_VEIN_MULT, DEEP_Y_MIN, DEEP_Y_MAX);
        makeVeins(random, chunkX, chunkZ, world, high, 4,
            BASE_HIGH_VEIN * HIGH_VEIN_MULT, DEEP_Y_MIN, DEEP_Y_MAX);

        // 熔岩带 y0~3：中阶灵石块 08 少量（×3，原有 4）
        makeVeins(random, chunkX, chunkZ, world, deco8, 12, 4, LAVA_Y_MIN, LAVA_Y_MAX);

        // 巨型矿脉：极小概率触发；矿脉 = 人界 ×20，低阶为主并夹带高阶
        if (random.nextFloat() < GIANT_CHANCE) {
            generateGiantVein(random, chunkX, chunkZ, world, low, high);
        }
    }

    private void generateGiantVein(Random random, int chunkX, int chunkZ, World world, Block low, Block high) {
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        int cx = baseX + 1 + random.nextInt(14);
        int cy = DEEP_Y_MIN + random.nextInt(DEEP_Y_MAX - DEEP_Y_MIN);
        int cz = baseZ + 1 + random.nextInt(14);
        // 巨型低阶矿脉：人界 ×20 = 120 格
        if (low != null) {
            for (int i = 0; i < BASE_LOW_VEIN * GIANT_VEIN_MULT; i++) {
                int x = cx + random.nextInt(8) - 4;
                int y = cy + random.nextInt(6) - 3;
                int z = cz + random.nextInt(8) - 4;
                if (x >= baseX + 1 && x < baseX + 15 && z >= baseZ + 1 && z < baseZ + 15
                    && world.getBlockState(new BlockPos(x, y, z)).getBlock() == stone()) {
                    world.setBlockState(new BlockPos(x, y, z), low.getDefaultState(), 2);
                }
            }
        }
        // 巨型矿脉内夹带若干高阶灵石
        if (high != null) {
            for (int i = 0; i < 4; i++) {
                int x = cx + random.nextInt(8) - 4;
                int y = cy + random.nextInt(6) - 3;
                int z = cz + random.nextInt(8) - 4;
                if (x >= baseX + 1 && x < baseX + 15 && z >= baseZ + 1 && z < baseZ + 15
                    && world.getBlockState(new BlockPos(x, y, z)).getBlock() == stone()) {
                    world.setBlockState(new BlockPos(x, y, z), high.getDefaultState(), 2);
                }
            }
        }
    }
}