package com.frxx.mengzhi;

import com.frxx.mengzhi.client.render.RenderSpiritBolt;
import com.frxx.mengzhi.command.LingJieCommand;
import com.frxx.mengzhi.command.ShieldOverflowCommand;
import com.frxx.mengzhi.command.TiandaoRealmCommand;
import com.frxx.mengzhi.elixir.CreativeTabElixir;
import com.frxx.mengzhi.elixir.ElixirRegistry;
import com.frxx.mengzhi.elixir.ElixirTickHandler;
import com.frxx.mengzhi.entity.EntitySpiritBolt;
import com.frxx.mengzhi.handler.ClientHooks;
import com.frxx.mengzhi.handler.CaoKongClientHandler;
import com.frxx.mengzhi.handler.CaoKongHandler;
import com.frxx.mengzhi.handler.DunGuangClientHandler;
import com.frxx.mengzhi.handler.BreathingHandler;
import com.frxx.mengzhi.handler.DunGuangHandler;
import com.frxx.mengzhi.handler.GuardClientHandler;
import com.frxx.mengzhi.handler.GuardHandler;
import com.frxx.mengzhi.handler.GangQiClientHandler;
import com.frxx.mengzhi.handler.GangQiHandler;
import com.frxx.mengzhi.handler.HandMiningClientHandler;
import com.frxx.mengzhi.handler.HandMiningHandler;
import com.frxx.mengzhi.handler.ProjectilePanelClientHandler;
import com.frxx.mengzhi.handler.ProjectilePanelHandler;
import com.frxx.mengzhi.handler.RepulsionClientHandler;
import com.frxx.mengzhi.handler.RepulsionFieldHandler;
import com.frxx.mengzhi.handler.TiandaoClientHandler;
import com.frxx.mengzhi.handler.TiandaoHandler;
import com.frxx.mengzhi.handler.SelfDestructClientHandler;
import com.frxx.mengzhi.handler.SelfDestructHandler;
import com.frxx.mengzhi.handler.LingLiSkillClientHandler;
import com.frxx.mengzhi.handler.LingLiSkillHandler;
import com.frxx.mengzhi.handler.SRPGuardHandler;
import com.frxx.mengzhi.handler.ShenShiClientHandler;
import com.frxx.mengzhi.handler.ShenShiPressureHandler;
import com.frxx.mengzhi.handler.SpiritBoltClientHandler;
import com.frxx.mengzhi.handler.SpiritBoltHandler;
import com.frxx.mengzhi.handler.SpiritBombClientHandler;
import com.frxx.mengzhi.handler.SpiritBombHandler;
import com.frxx.mengzhi.lingli.LingLiRegistry;
import com.frxx.mengzhi.lingjie.LingJieDimension;
import com.frxx.mengzhi.lingjie.LingJieHeightDamageHandler;
import com.frxx.mengzhi.lingjie.LingJieLingYuHandler;
import com.frxx.mengzhi.lingjie.LingJieMobSpawnHandler;
import com.frxx.mengzhi.lingjie.LingJieOreGenerator;
import com.frxx.mengzhi.lingjie.LingJieShouYuanHandler;
import com.frxx.mengzhi.lingjie.ItemChuanSuoQi;
import com.frxx.mengzhi.lingli.network.LingLiWirelessTogglePacket;
import com.frxx.mengzhi.network.DunGuangActionPacket;
import com.frxx.mengzhi.network.DunGuangStatePacket;
import com.frxx.mengzhi.network.GuardActionPacket;
import com.frxx.mengzhi.network.GuardStatePacket;
import com.frxx.mengzhi.network.GangQiActionPacket;
import com.frxx.mengzhi.network.GangQiStatePacket;
import com.frxx.mengzhi.network.HandMiningSyncPacket;
import com.frxx.mengzhi.network.HandMiningTogglePacket;
import com.frxx.mengzhi.network.SelfDestructShakePacket;
import com.frxx.mengzhi.network.SelfDestructTriggerPacket;
import com.frxx.mengzhi.network.ShenShiPressureTriggerPacket;
import com.frxx.mengzhi.network.SpiritBoltFirePacket;
import com.frxx.mengzhi.network.ProjectilePanelTogglePacket;
import com.frxx.mengzhi.network.TiandaoPanelPacket;
import com.frxx.mengzhi.network.TiandaoNbtDocPacket;
import com.frxx.mengzhi.network.RepulsionTogglePacket;
import com.frxx.mengzhi.network.LingLiSkillPacket;
import com.frxx.mengzhi.network.SpiritBombDetonatePacket;
import com.frxx.mengzhi.network.SpiritBombPlacePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = FanRenXiuXianMengZhi.MODID,
    name = FanRenXiuXianMengZhi.NAME,
    version = FanRenXiuXianMengZhi.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:fanrenxiuxian;required-after:yvanchuxiuzhen;after:mixinbooter;"
)
public class FanRenXiuXianMengZhi {
    public static final String MODID = "frxxmengzhi";
    public static final String NAME = "FanRenXiuXian MengZhi";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MODID)
    public static FanRenXiuXianMengZhi INSTANCE;

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("frxxmz");

    public static Logger logger;

    // Creative tab for elixirs
    public static final CreativeTabElixir TAB_ELIXIR = CreativeTabElixir.TAB_ELIXIR;

    // Placeholder for elixir icon item (set after registration)
    public static Item ELIXIR_ICON_ITEM = null;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        int id = 0;
        NETWORK.registerMessage(GuardActionPacket.Handler.class, GuardActionPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(GuardStatePacket.Handler.class, GuardStatePacket.class, id++, Side.CLIENT);
        NETWORK.registerMessage(com.frxx.mengzhi.network.AssistantConfigPacket.Handler.class, com.frxx.mengzhi.network.AssistantConfigPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(SelfDestructTriggerPacket.Handler.class, SelfDestructTriggerPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(SelfDestructShakePacket.Handler.class, SelfDestructShakePacket.class, id++, Side.CLIENT);
        NETWORK.registerMessage(ShenShiPressureTriggerPacket.Handler.class, ShenShiPressureTriggerPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(HandMiningTogglePacket.Handler.class, HandMiningTogglePacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(HandMiningSyncPacket.Handler.class, HandMiningSyncPacket.class, id++, Side.CLIENT);
        NETWORK.registerMessage(SpiritBombPlacePacket.Handler.class, SpiritBombPlacePacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(SpiritBombDetonatePacket.Handler.class, SpiritBombDetonatePacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(SpiritBoltFirePacket.Handler.class, SpiritBoltFirePacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(GangQiActionPacket.Handler.class, GangQiActionPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(GangQiStatePacket.Handler.class, GangQiStatePacket.class, id++, Side.CLIENT);
        NETWORK.registerMessage(com.frxx.mengzhi.network.CaoKongActionPacket.Handler.class,
            com.frxx.mengzhi.network.CaoKongActionPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(com.frxx.mengzhi.network.CaoKongStatePacket.Handler.class,
            com.frxx.mengzhi.network.CaoKongStatePacket.class, id++, Side.CLIENT);
        NETWORK.registerMessage(DunGuangActionPacket.Handler.class, DunGuangActionPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(DunGuangStatePacket.Handler.class, DunGuangStatePacket.class, id++, Side.CLIENT);
        NETWORK.registerMessage(ProjectilePanelTogglePacket.Handler.class, ProjectilePanelTogglePacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(TiandaoPanelPacket.Handler.class, TiandaoPanelPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(TiandaoNbtDocPacket.Handler.class, TiandaoNbtDocPacket.class, id++, Side.CLIENT);
        NETWORK.registerMessage(RepulsionTogglePacket.Handler.class, RepulsionTogglePacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(LingLiWirelessTogglePacket.Handler.class, LingLiWirelessTogglePacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(LingLiSkillPacket.Handler.class, LingLiSkillPacket.class, id++, Side.SERVER);

        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "spirit_bolt"),
            EntitySpiritBolt.class, "SpiritBolt", 0, INSTANCE, 64, 4, true);

        MinecraftForge.EVENT_BUS.register(GuardHandler.class);
        FMLCommonHandler.instance().bus().register(GuardHandler.class);

        MinecraftForge.EVENT_BUS.register(SelfDestructHandler.class);
        FMLCommonHandler.instance().bus().register(SelfDestructHandler.class);

        MinecraftForge.EVENT_BUS.register(ShenShiPressureHandler.class);
        FMLCommonHandler.instance().bus().register(ShenShiPressureHandler.class);

        MinecraftForge.EVENT_BUS.register(HandMiningHandler.class);
        FMLCommonHandler.instance().bus().register(HandMiningHandler.class);

        MinecraftForge.EVENT_BUS.register(SpiritBombHandler.class);
        FMLCommonHandler.instance().bus().register(SpiritBombHandler.class);

        MinecraftForge.EVENT_BUS.register(SpiritBoltHandler.class);
        FMLCommonHandler.instance().bus().register(SpiritBoltHandler.class);

        MinecraftForge.EVENT_BUS.register(GangQiHandler.class);
        FMLCommonHandler.instance().bus().register(GangQiHandler.class);

        MinecraftForge.EVENT_BUS.register(CaoKongHandler.class);
        FMLCommonHandler.instance().bus().register(CaoKongHandler.class);

        MinecraftForge.EVENT_BUS.register(DunGuangHandler.class);
        FMLCommonHandler.instance().bus().register(DunGuangHandler.class);

        MinecraftForge.EVENT_BUS.register(ProjectilePanelHandler.class);
        FMLCommonHandler.instance().bus().register(ProjectilePanelHandler.class);

        MinecraftForge.EVENT_BUS.register(TiandaoHandler.class);
        FMLCommonHandler.instance().bus().register(TiandaoHandler.class);

        MinecraftForge.EVENT_BUS.register(RepulsionFieldHandler.class);
        FMLCommonHandler.instance().bus().register(RepulsionFieldHandler.class);

        MinecraftForge.EVENT_BUS.register(BreathingHandler.class);
        FMLCommonHandler.instance().bus().register(BreathingHandler.class);

        MinecraftForge.EVENT_BUS.register(LingLiSkillHandler.class);
        FMLCommonHandler.instance().bus().register(LingLiSkillHandler.class);

        MinecraftForge.EVENT_BUS.register(SRPGuardHandler.class);
        FMLCommonHandler.instance().bus().register(SRPGuardHandler.class);

        // Register elixir items (triggers ElixirRegistry.registerItems via EventBus)
        MinecraftForge.EVENT_BUS.register(ElixirRegistry.class);

        // Register elixir tick handler for temporary buffs
        MinecraftForge.EVENT_BUS.register(ElixirTickHandler.class);

        // Register 灵力电力系统 (generators / storage / batteries)
        LingLiRegistry.init(event);

        // Register 灵界 dimension + high-altitude damage
        LingJieDimension.register();
        MinecraftForge.EVENT_BUS.register(LingJieHeightDamageHandler.class);
        MinecraftForge.EVENT_BUS.register(ItemChuanSuoQi.class);

        // Register 灵界灵石矿脉生成器（仅 lingjie 维度生效）
        GameRegistry.registerWorldGenerator(new LingJieOreGenerator(), 0);

        // Register 灵界玩法：灵裕值 / 寿元倍率 / 灵界刷怪
        MinecraftForge.EVENT_BUS.register(LingJieLingYuHandler.class);
        MinecraftForge.EVENT_BUS.register(LingJieShouYuanHandler.class);
        MinecraftForge.EVENT_BUS.register(LingJieMobSpawnHandler.class);

        if (event.getSide().isClient()) {
            ClientHooks.registerKeyBindings();
            MinecraftForge.EVENT_BUS.register(GuardClientHandler.class);
            FMLCommonHandler.instance().bus().register(GuardClientHandler.class);
            MinecraftForge.EVENT_BUS.register(SelfDestructClientHandler.class);
            FMLCommonHandler.instance().bus().register(SelfDestructClientHandler.class);
            MinecraftForge.EVENT_BUS.register(ShenShiClientHandler.class);
            FMLCommonHandler.instance().bus().register(ShenShiClientHandler.class);
            MinecraftForge.EVENT_BUS.register(HandMiningClientHandler.class);
            FMLCommonHandler.instance().bus().register(HandMiningClientHandler.class);
            MinecraftForge.EVENT_BUS.register(SpiritBombClientHandler.class);
            FMLCommonHandler.instance().bus().register(SpiritBombClientHandler.class);
            MinecraftForge.EVENT_BUS.register(SpiritBoltClientHandler.class);
            FMLCommonHandler.instance().bus().register(SpiritBoltClientHandler.class);
            MinecraftForge.EVENT_BUS.register(GangQiClientHandler.class);
            FMLCommonHandler.instance().bus().register(GangQiClientHandler.class);
            MinecraftForge.EVENT_BUS.register(CaoKongClientHandler.class);
            FMLCommonHandler.instance().bus().register(CaoKongClientHandler.class);
            MinecraftForge.EVENT_BUS.register(DunGuangClientHandler.class);
            FMLCommonHandler.instance().bus().register(DunGuangClientHandler.class);
            MinecraftForge.EVENT_BUS.register(ProjectilePanelClientHandler.class);
            FMLCommonHandler.instance().bus().register(ProjectilePanelClientHandler.class);
            MinecraftForge.EVENT_BUS.register(TiandaoClientHandler.class);
            FMLCommonHandler.instance().bus().register(TiandaoClientHandler.class);
            MinecraftForge.EVENT_BUS.register(RepulsionClientHandler.class);
            FMLCommonHandler.instance().bus().register(RepulsionClientHandler.class);
            MinecraftForge.EVENT_BUS.register(LingLiSkillClientHandler.class);
            FMLCommonHandler.instance().bus().register(LingLiSkillClientHandler.class);
            RenderingRegistry.registerEntityRenderingHandler(EntitySpiritBolt.class,
                manager -> new RenderSpiritBolt(manager));
        }
        logger.info("凡人修仙梦制 loaded!");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 物品模型注册已移至 ModelRegistryEvent（见 RenderDebugHooks.onModelRegistry）：
        // 1.12.2 的 ModelLoader.onRegisterItems 在 RenderItem 构造时一次性消费静态 customModels，
        // 而 RenderItem 构造先于 FMLInitializationEvent，故在 init 注册必然失效（紫黑方块）。

        // Set elixir icon item after registration
        if (!ElixirRegistry.PERMANENT_ELIXIRS.isEmpty()) {
            ELIXIR_ICON_ITEM = ElixirRegistry.PERMANENT_ELIXIRS.get(0);
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new TiandaoRealmCommand());
        event.registerServerCommand(new ShieldOverflowCommand());
        event.registerServerCommand(new LingJieCommand());
    }

    @Mod.EventBusSubscriber(modid = FanRenXiuXianMengZhi.MODID, value = Side.CLIENT)
    public static class RenderDebugHooks {

        private static final java.util.Map<String, IBakedModel> BAKE_SNAPSHOT = new java.util.HashMap<>();

        @SubscribeEvent
        public static void onModelRegistry(ModelRegistryEvent event) {
            for (Item item : ElixirRegistry.ALL_ELIXIRS) {
                ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
            if (logger != null) {
                logger.info("Registered {} custom item models via ModelRegistryEvent", ElixirRegistry.ALL_ELIXIRS.size());
            }
        }

        @SubscribeEvent
        public static void onModelBake(ModelBakeEvent event) {
            if (logger == null) return;
            BAKE_SNAPSHOT.clear();
            for (Item item : ElixirRegistry.ALL_ELIXIRS) {
                dumpBaked(event, item.getRegistryName());
            }
            dumpBaked(event, new ResourceLocation("frxxmengzhi", "test_item"));
            dumpBaked(event, new ResourceLocation("minecraft", "apple"));
            dumpBaked(event, new ResourceLocation("minecraft", "item/apple"));
        }

        private static void dumpBaked(ModelBakeEvent event, ResourceLocation rl) {
            ModelResourceLocation mrl = new ModelResourceLocation(rl, "inventory");
            IBakedModel baked = event.getModelRegistry().getObject(mrl);
            String info;
            if (baked == null) {
                info = "NULL_NOT_REGISTERED";
            } else if (baked == ModelBakery.MODEL_MISSING) {
                info = "MISSING_MODEL_FALLBACK";
            } else {
                TextureAtlasSprite sprite = baked.getParticleTexture();
                info = "OK particle=" + (sprite == null ? "null" : sprite.getIconName());
            }
            logger.info("[RENDERDEBUG] {} => {}", mrl, info);
            BAKE_SNAPSHOT.put(mrl.toString(), baked);
        }

        @SubscribeEvent
        public static void onStitchPost(TextureStitchEvent.Post event) {
            if (logger == null) return;
            for (Item item : ElixirRegistry.ALL_ELIXIRS) {
                dumpSprite(event.getMap(), item.getRegistryName().getResourcePath());
            }
            dumpSprite(event.getMap(), "test_item");
        }

        private static void dumpSprite(TextureMap map, String path) {
            String iconName = "frxxmengzhi:items/" + path;
            TextureAtlasSprite sprite = map.getAtlasSprite(iconName);
            int[][] data = sprite.getFrameTextureData(0);
            int w = sprite.getIconWidth();
            int h = sprite.getIconHeight();
            int nonzero = 0;
            int total = 0;
            if (data != null && data[0] != null) {
                int[] px = data[0];
                total = px.length;
                for (int v : px) {
                    if (v != 0) nonzero++;
                }
            }
            logger.info("[STITCHDEBUG] {} => w={} h={} pixels={}/{} name={}", iconName, w, h, nonzero, total, sprite.getIconName());
        }

        private static boolean mesherDone = false;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (logger == null || mesherDone) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getRenderItem() == null) return;
            mesherDone = true;
            for (Item item : ElixirRegistry.ALL_ELIXIRS) {
                dumpMesher(mc, new ItemStack(item));
            }
            dumpMesher(mc, new ItemStack(Items.APPLE));
        }

        private static void dumpMesher(Minecraft mc, ItemStack stack) {
            IBakedModel baked = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
            IBakedModel eventBaked = BAKE_SNAPSHOT.get(new ModelResourceLocation(stack.getItem().getRegistryName(), "inventory").toString());
            TextureAtlasSprite sp = mc.getTextureMapBlocks().getAtlasSprite("frxxmengzhi:items/" + stack.getItem().getRegistryName().getResourcePath());
            logger.info("[TICKDEBUG] {} sameObject={} mesherParticle={} eventParticle={} spriteNow={} spriteSize={}x{}",
                stack.getItem().getRegistryName(),
                baked == eventBaked,
                particleOf(baked),
                particleOf(eventBaked),
                sp == null ? "null" : sp.getIconName(),
                sp == null ? -1 : sp.getIconWidth(),
                sp == null ? -1 : sp.getIconHeight());
        }

        private static String particleOf(IBakedModel model) {
            if (model == null) return "null";
            TextureAtlasSprite sprite = model.getParticleTexture();
            return sprite == null ? "null" : sprite.getIconName();
        }
    }
}
