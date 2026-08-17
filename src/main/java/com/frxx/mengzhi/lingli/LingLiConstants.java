package com.frxx.mengzhi.lingli;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

public final class LingLiConstants {

    public static final String NAMESPACE = "lingli";

    public static final int WIRELESS_RANGE = 4;

    public static final int[] GENERATOR_CAPACITY = { 1000, 4000, 16000, 64000, 256000 };
    public static final int[] GENERATOR_MAX_OUTPUT = { 100, 200, 400, 800, 1600 };

    public static final int[] GENERATOR_BURN_TICKS = { 200, 100, 40, 20, 1 };

    public static final int[] STORAGE_CAPACITY = { 50000, 200000, 800000, 3200000, 12800000 };
    public static final int[] STORAGE_RATE = { 200, 400, 800, 1600, 3200 };

    public static final int[] BATTERY_CAPACITY = { 10000, 40000, 160000, 640000, 2560000 };
    public static final int[] BATTERY_RATE = { 200, 400, 800, 1600, 3200 };
    public static final int[] BATTERY_REGEN_PER_SECOND = { 100, 200, 400, 800, 1600 };
    public static final int[] BATTERY_RATIO = { 1, 2, 4, 8, 16 };

    private static final Map<ResourceLocation, Integer> FUEL_VALUES = ImmutableMap.<ResourceLocation, Integer>builder()
        .put(new ResourceLocation("yvanchuxiuzhen", "cailiaolingshi03"), 500)
        .put(new ResourceLocation("yvanchuxiuzhen", "ling_shi_kuai_1"), 4500)
        .put(new ResourceLocation("yvanchuxiuzhen", "cailiaolingshi05"), 50000)
        .put(new ResourceLocation("yvanchuxiuzhen", "ling_shi_kuai_2"), 450000)
        .put(new ResourceLocation("minecraft", "coal"), 1600)
        .put(new ResourceLocation("minecraft", "coal_block"), 16000)
        .put(new ResourceLocation("minecraft", "log"), 300)
        .put(new ResourceLocation("minecraft", "log2"), 300)
        .put(new ResourceLocation("minecraft", "planks"), 300)
        .put(new ResourceLocation("minecraft", "stick"), 100)
        .put(new ResourceLocation("minecraft", "sapling"), 100)
        .put(new ResourceLocation("minecraft", "crafting_table"), 300)
        .put(new ResourceLocation("minecraft", "chest"), 300)
        .put(new ResourceLocation("minecraft", "bookshelf"), 300)
        .put(new ResourceLocation("minecraft", "fence"), 300)
        .put(new ResourceLocation("minecraft", "fence_gate"), 300)
        .put(new ResourceLocation("minecraft", "wooden_slab"), 150)
        .put(new ResourceLocation("minecraft", "wooden_door"), 200)
        .put(new ResourceLocation("minecraft", "trapdoor"), 300)
        .put(new ResourceLocation("minecraft", "wooden_button"), 100)
        .put(new ResourceLocation("minecraft", "wooden_pressure_plate"), 300)
        .put(new ResourceLocation("minecraft", "boat"), 1200)
        .put(new ResourceLocation("minecraft", "lava_bucket"), 20000)
        .put(new ResourceLocation("minecraft", "blaze_rod"), 2400)
        .build();

    private LingLiConstants() {
    }

    public static boolean isFuel(ItemStack stack) {
        return stack != null && !stack.isEmpty() && FUEL_VALUES.containsKey(stack.getItem().getRegistryName());
    }

    public static int fuelValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        Integer value = FUEL_VALUES.get(stack.getItem().getRegistryName());
        return value == null ? 0 : value;
    }

    public static int generatorTier(Block block) {
        return tierOf(block, "lingli_generator_");
    }

    public static int storageTier(Block block) {
        return tierOf(block, "chudianqi_storage_");
    }

    public static int batteryTier(Item item) {
        if (item == null) {
            return -1;
        }
        return tierOf(item, "dianchi_battery_");
    }

    private static int tierOf(Item item, String prefix) {
        if (item == null) {
            return -1;
        }
        return tierOf(item.getRegistryName(), prefix);
    }

    private static int tierOf(Block block, String prefix) {
        if (block == null) {
            return -1;
        }
        return tierOf(block.getRegistryName(), prefix);
    }

    private static int tierOf(ResourceLocation rl, String prefix) {
        if (rl == null || !NAMESPACE.equals(rl.getResourceDomain())) {
            return -1;
        }
        String path = rl.getResourcePath();
        if (!path.startsWith(prefix)) {
            return -1;
        }
        try {
            int tier = Integer.parseInt(path.substring(prefix.length()));
            return (tier >= 1 && tier <= 5) ? tier : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}