package com.frxx.mengzhi.elixir;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FanRenXiuXianMengZhi.MODID)
public class ElixirRegistry {

    public static final List<Item> PERMANENT_ELIXIRS = new ArrayList<>();
    public static final List<Item> TEMPORARY_ELIXIRS = new ArrayList<>();
    public static final List<Item> ALL_ELIXIRS = new ArrayList<>();
    public static Item TEST_ITEM = null;

    // Base values per realm (index 0=练气, 1=筑基, 2=金丹, 3=元婴, 4=化神)
    private static final int[] SHIELD_MAX_BASE = { 50, 120, 300, 600, 1200 };
    private static final int[] SHIELD_REGEN_BASE = { 2, 5, 12, 25, 50 };
    // Absorption ratio per realm: how many damage points 1 shield point absorbs
    // Stored as int * 100 (e.g., 150 = 1.5x, 200 = 2.0x)
    private static final int[] SHIELD_ABSORPTION_BASE = { 150, 180, 220, 280, 350 }; // 1.5x, 1.8x, 2.2x, 2.8x, 3.5x

    private static final int[] TOLERANCE_COST = { 5, 10, 20, 40, 80 };

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();

        // Register permanent elixirs (5 realms × 3 types = 15)
        for (ElixirRealm realm : ElixirRealm.values()) {
            int idx = realm.getLevel() - 1;

            // Shield Max
            Item shieldMax = new ItemElixirPermanent(ElixirType.SHIELD_MAX, realm, SHIELD_MAX_BASE[idx], TOLERANCE_COST[idx]);
            registry.register(shieldMax);
            PERMANENT_ELIXIRS.add(shieldMax);

            // Shield Regen
            Item shieldRegen = new ItemElixirPermanent(ElixirType.SHIELD_REGEN, realm, SHIELD_REGEN_BASE[idx], TOLERANCE_COST[idx]);
            registry.register(shieldRegen);
            PERMANENT_ELIXIRS.add(shieldRegen);

            // Shield Absorption (was reduction)
            Item shieldAbsorption = new ItemElixirPermanent(ElixirType.SHIELD_ABSORPTION, realm, SHIELD_ABSORPTION_BASE[idx], TOLERANCE_COST[idx]);
            registry.register(shieldAbsorption);
            PERMANENT_ELIXIRS.add(shieldAbsorption);
        }

        // Register temporary elixirs (5 realms × 3 types = 15)
        for (ElixirRealm realm : ElixirRealm.values()) {
            int idx = realm.getLevel() - 1;

            // Temporary Shield Max (cost: 50/100/200/400/800 ZhenYuan)
            Item tempShieldMax = new ItemElixirTemporary(ElixirType.SHIELD_MAX, realm, 
                SHIELD_MAX_BASE[idx] * 3, TOLERANCE_COST[idx] * 10);
            registry.register(tempShieldMax);
            TEMPORARY_ELIXIRS.add(tempShieldMax);

            // Temporary Shield Regen
            Item tempShieldRegen = new ItemElixirTemporary(ElixirType.SHIELD_REGEN, realm, 
                SHIELD_REGEN_BASE[idx] * 3, TOLERANCE_COST[idx] * 10);
            registry.register(tempShieldRegen);
            TEMPORARY_ELIXIRS.add(tempShieldRegen);

            // Temporary Shield Absorption
            Item tempShieldAbsorption = new ItemElixirTemporary(ElixirType.SHIELD_ABSORPTION, realm, 
                SHIELD_ABSORPTION_BASE[idx] * 3, TOLERANCE_COST[idx] * 10);
            registry.register(tempShieldAbsorption);
            TEMPORARY_ELIXIRS.add(tempShieldAbsorption);
        }

        // Register Attribute Assistant (creative only)
        Item attributeAssistant = new ItemAttributeAssistant();
        registry.register(attributeAssistant);

        // TEMP: rendering diagnostic test item (plain Item, vanilla apple texture)
        Item testItem = new Item()
            .setRegistryName("frxxmengzhi:test_item")
            .setUnlocalizedName("frxxmengzhi.test_item")
            .setCreativeTab(net.minecraft.creativetab.CreativeTabs.MISC);
        registry.register(testItem);
        TEST_ITEM = testItem;

        // Collect all
        ALL_ELIXIRS.addAll(PERMANENT_ELIXIRS);
        ALL_ELIXIRS.addAll(TEMPORARY_ELIXIRS);
        ALL_ELIXIRS.add(attributeAssistant);
        ALL_ELIXIRS.add(testItem);

        FanRenXiuXianMengZhi.logger.info("Registered {} permanent elixirs, {} temporary elixirs, and 1 attribute assistant", 
            PERMANENT_ELIXIRS.size(), TEMPORARY_ELIXIRS.size());
    }
}