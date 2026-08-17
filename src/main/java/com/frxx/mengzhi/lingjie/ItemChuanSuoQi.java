package com.frxx.mengzhi.lingjie;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 穿梭器：
 *  - 手持（主手或副手）且飞行时，飞行速度 ×200；
 *  - 手持时免疫 80% 的灵界天威伤害（受 20%）。
 */
public class ItemChuanSuoQi extends Item {

    public static final String NAME = "chuansuoqi";
    public static final float FLY_SPEED_MULTIPLIER = 200.0F;
    public static final float VANILLA_FLY_SPEED = 0.05F;
    public static final float DAMAGE_TAKEN = 0.2F;

    public static ItemChuanSuoQi INSTANCE;

    public ItemChuanSuoQi() {
        this.setUnlocalizedName("chuansuoqi.ChuanSuoQi");
        this.setRegistryName(new ResourceLocation(FanRenXiuXianMengZhi.MODID, NAME));
        this.setMaxStackSize(1);
    }

    public static boolean isHolding(EntityPlayer player) {
        if (INSTANCE == null) {
            return false;
        }
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        return main.getItem() == INSTANCE || off.getItem() == INSTANCE;
    }

    @SubscribeEvent
    public static void onItemsRegister(RegistryEvent.Register<Item> event) {
        INSTANCE = new ItemChuanSuoQi();
        event.getRegistry().register(INSTANCE);
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        if (INSTANCE != null) {
            ModelLoader.setCustomModelResourceLocation(INSTANCE, 0,
                new ModelResourceLocation(INSTANCE.getRegistryName(), "inventory"));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        if (player.capabilities.isFlying && isHolding(player)) {
            player.capabilities.setFlySpeed(VANILLA_FLY_SPEED * FLY_SPEED_MULTIPLIER);
        } else if (player.capabilities.getFlySpeed() > VANILLA_FLY_SPEED) {
            player.capabilities.setFlySpeed(VANILLA_FLY_SPEED);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.translateToLocal("chuansuoqi.tip1"));
        tooltip.add(I18n.translateToLocal("chuansuoqi.tip2"));
    }
}