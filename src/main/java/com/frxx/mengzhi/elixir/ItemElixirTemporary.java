package com.frxx.mengzhi.elixir;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemElixirTemporary extends ItemElixirBase {

    private static final int DURATION_TICKS = 600 * 20;

    public ItemElixirTemporary(ElixirType type, ElixirRealm realm, int baseValue, int zhenYuanCost) {
        super(type, realm, true, baseValue, zhenYuanCost);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        NBTTagCompound data = player.getEntityData();
        double zhenYuan = data.getDouble("Base");
        int cost = toleranceCost;

        String reject = ElixirConsumeRules.checkRealm(player, realm);
        if (reject != null) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        if (zhenYuan < cost) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u771f\u5143\u4e0d\u8db3\uff01\u9700\u8981 " + cost + " \u771f\u5143\uff0c\u5f53\u524d: " + (int)zhenYuan));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        data.setDouble("Base", zhenYuan - cost);

        String buffKey = "TempElixir_" + elixirType.getId() + "_" + realm.getShortName();
        NBTTagCompound buffData = new NBTTagCompound();
        buffData.setInteger("Value", baseValue);
        buffData.setInteger("Duration", DURATION_TICKS);
        buffData.setInteger("Type", elixirType.ordinal());
        data.setTag(buffKey, buffData);

        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }

        player.sendMessage(new TextComponentString(TextFormatting.AQUA + "\u670d\u7528" + realm.getFullName() + elixirType.getDisplayName() + 
            TextFormatting.AQUA + " (\u4e00\u65f6)\u3001\u83b7\u5f97\u5f3a\u529f\u589e\u76ca +" + baseValue + "\u3001\u6301\u7eed 10 \u5206\u949f"));
        player.sendMessage(new TextComponentString(TextFormatting.GRAY + "\u6d88\u8017\u771f\u5143: " + cost + "\u3001\u5269\u4f59\u771f\u5143: " + (int)(zhenYuan - cost)));

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}