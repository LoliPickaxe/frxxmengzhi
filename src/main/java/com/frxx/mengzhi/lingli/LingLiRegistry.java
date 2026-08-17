package com.frxx.mengzhi.lingli;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.lingli.block.BlockChuDianQi;
import com.frxx.mengzhi.lingli.block.BlockLingLiGenerator;
import com.frxx.mengzhi.lingli.item.ItemDianChi;
import com.frxx.mengzhi.lingli.item.ItemLingLiBlock;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi1;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi2;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi3;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi4;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi5;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator1;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator2;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator3;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator4;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator5;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LingLiRegistry {

    public static final Block[] GENERATOR_BLOCKS = new Block[5];
    public static final Block[] STORAGE_BLOCKS = new Block[5];
    public static final Item[] BATTERY_ITEMS = new Item[5];

    public static final CreativeTabs TAB_LINGLI_POWER = new CreativeTabs("lingli_power") {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack getTabIconItem() {
            return new ItemStack(GENERATOR_BLOCKS[0]);
        }

        @Override
        public void displayAllRelevantItems(net.minecraft.util.NonNullList<ItemStack> items) {
            for (Block block : GENERATOR_BLOCKS) {
                items.add(new ItemStack(block));
            }
            for (Block block : STORAGE_BLOCKS) {
                items.add(new ItemStack(block));
            }
            for (Item battery : BATTERY_ITEMS) {
                items.add(new ItemStack(battery));
            }
        }
    };

    private static final Class<? extends net.minecraft.tileentity.TileEntity>[] GENERATOR_TILE_CLASSES = new Class[] {
        TileLingLiGenerator1.class, TileLingLiGenerator2.class, TileLingLiGenerator3.class,
        TileLingLiGenerator4.class, TileLingLiGenerator5.class
    };

    private static final Class<? extends net.minecraft.tileentity.TileEntity>[] STORAGE_TILE_CLASSES = new Class[] {
        TileChuDianQi1.class, TileChuDianQi2.class, TileChuDianQi3.class,
        TileChuDianQi4.class, TileChuDianQi5.class
    };

    public static void init(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(LingLiRegistry.class);
        MinecraftForge.EVENT_BUS.register(ItemDianChi.class);

        NetworkRegistry.INSTANCE.registerGuiHandler(FanRenXiuXianMengZhi.INSTANCE, new LingLiGuiHandler());
    }

    @SubscribeEvent
    public static void onBlocksRegister(RegistryEvent.Register<Block> event) {
        for (int i = 0; i < 5; i++) {
            int tier = i + 1;

            Block generator = new BlockLingLiGenerator(tier)
                .setCreativeTab(TAB_LINGLI_POWER);
            generator.setUnlocalizedName("lingli.lingli_Generator_" + tier);
            generator.setRegistryName(LingLiConstants.NAMESPACE, "lingli_generator_" + tier);
            event.getRegistry().register(generator);
            GENERATOR_BLOCKS[i] = generator;

            Block storage = new BlockChuDianQi(tier)
                .setCreativeTab(TAB_LINGLI_POWER);
            storage.setUnlocalizedName("lingli.chudianqi_Storage_" + tier);
            storage.setRegistryName(LingLiConstants.NAMESPACE, "chudianqi_storage_" + tier);
            event.getRegistry().register(storage);
            STORAGE_BLOCKS[i] = storage;

            GameRegistry.registerTileEntity(GENERATOR_TILE_CLASSES[i], LingLiConstants.NAMESPACE + ":tile_lingli_generator_" + tier);
            GameRegistry.registerTileEntity(STORAGE_TILE_CLASSES[i], LingLiConstants.NAMESPACE + ":tile_chudianqi_storage_" + tier);
        }
    }

    @SubscribeEvent
    public static void onItemsRegister(RegistryEvent.Register<Item> event) {
        for (Block block : GENERATOR_BLOCKS) {
            event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        }
        for (Block block : STORAGE_BLOCKS) {
            event.getRegistry().register(new ItemLingLiBlock(block).setRegistryName(block.getRegistryName()));
        }
        for (int i = 0; i < 5; i++) {
            int tier = i + 1;
            Item battery = new ItemDianChi(tier)
                .setCreativeTab(TAB_LINGLI_POWER);
            battery.setUnlocalizedName("lingli.dianchi_Battery_" + tier);
            battery.setRegistryName(LingLiConstants.NAMESPACE, "dianchi_battery_" + tier);
            event.getRegistry().register(battery);
            BATTERY_ITEMS[i] = battery;
        }
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        for (Block block : GENERATOR_BLOCKS) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
        for (Block block : STORAGE_BLOCKS) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
        for (Item item : BATTERY_ITEMS) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }

    @SubscribeEvent
    public static void onRecipesRegister(RegistryEvent.Register<IRecipe> event) {
        Item lowStone = yvanItem("cailiaolingshi03");
        Item midStone = yvanItem("cailiaolingshi05");
        Item lowBlock = yvanItem("ling_shi_kuai_1");
        Item midBlock = yvanItem("ling_shi_kuai_2");

        GameRegistry.addShapedRecipe(recipeKey("lingli_Generator_1"), null, new ItemStack(GENERATOR_BLOCKS[0]),
            "LIL", "IRI", "LIL",
            'L', new ItemStack(lowStone),
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

        for (int i = 1; i < 5; i++) {
            int tier = i + 1;
            GameRegistry.addShapedRecipe(recipeKey("lingli_Generator_" + tier), null, new ItemStack(GENERATOR_BLOCKS[i]),
                " S ", " G ", " S ",
                'S', new ItemStack(midStone),
                'G', new ItemStack(GENERATOR_BLOCKS[i - 1]));
        }

        GameRegistry.addShapedRecipe(recipeKey("chudianqi_Storage_1"), null, new ItemStack(STORAGE_BLOCKS[0]),
            "IRI", "IBI", " R ",
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE),
            'B', new ItemStack(lowBlock));

        for (int i = 1; i < 5; i++) {
            int tier = i + 1;
            GameRegistry.addShapedRecipe(recipeKey("chudianqi_Storage_" + tier), null, new ItemStack(STORAGE_BLOCKS[i]),
                " B ", " S ", "   ",
                'B', new ItemStack(midBlock),
                'S', new ItemStack(STORAGE_BLOCKS[i - 1]));
        }

        GameRegistry.addShapedRecipe(recipeKey("dianchi_Battery_1"), null, new ItemStack(BATTERY_ITEMS[0]),
            " L ", "IRI", " L ",
            'I', new ItemStack(Items.IRON_INGOT),
            'L', new ItemStack(lowStone),
            'R', new ItemStack(Items.REDSTONE));

        for (int i = 1; i < 5; i++) {
            int tier = i + 1;
            GameRegistry.addShapedRecipe(recipeKey("dianchi_Battery_" + tier), null, new ItemStack(BATTERY_ITEMS[i]),
                " S ", " B ", " S ",
                'S', new ItemStack(midStone),
                'B', new ItemStack(BATTERY_ITEMS[i - 1]));
        }
    }

    private static ResourceLocation recipeKey(String name) {
        return new ResourceLocation(LingLiConstants.NAMESPACE, name);
    }

    private static Item yvanItem(String name) {
        return Item.getByNameOrId("yvanchuxiuzhen:" + name);
    }
}