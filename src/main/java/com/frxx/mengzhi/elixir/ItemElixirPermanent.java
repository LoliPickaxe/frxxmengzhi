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

public class ItemElixirPermanent extends ItemElixirBase {

    public ItemElixirPermanent(ElixirType type, ElixirRealm realm, int baseValue, int toleranceCost) {
        super(type, realm, false, baseValue, toleranceCost);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

NBTTagCompound playerData = player.getEntityData();
        String reject = ElixirConsumeRules.checkRealm(player, realm);
        if (reject != null) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        reject = ElixirConsumeRules.checkConsumeQuota(player, realm);
        if (reject != null) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        String toleranceKey = "ElixirTolerance_" + elixirType.getId() + "_" + realm.getShortName();
        int currentTolerance = playerData.getInteger(toleranceKey);

        int effect = calculateEffect(currentTolerance);
        if (effect <= 0) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u8010\u836f\u6027\u8fc7\u9ad8\uff0c\u8be5\u4e38\u836f\u5df2\u65e0\u6548\u679c\uff0c\u9700\u51d1\u5316\u8010\u836f\u503c"));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        ElixirConsumeRules.markConsumed(player, realm);
        applyEffect(player, effect);

        playerData.setInteger(toleranceKey, currentTolerance + toleranceCost);

        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }

        player.sendMessage(new TextComponentString(TextFormatting.GREEN + "\u670d\u7528" + realm.getFullName() + elixirType.getDisplayName() + 
            TextFormatting.GREEN + "\u3001\u83b7\u5f97\u6c38\u4e45\u5c5e\u6027 +" + effect));
        player.sendMessage(new TextComponentString(TextFormatting.GRAY + "\u5f53\u524d\u8010\u836f\u503c: " + (currentTolerance + toleranceCost) + 
            " (\u4e0b\u6b21\u6548\u679c\u5c06\u9012\u51cf)"));
        player.sendMessage(new TextComponentString(TextFormatting.GRAY + "\u8be5\u4e39\u836f\u53ef\u670d\u7528: " +
            ElixirConsumeRules.getQuota(player, realm) + " \u6b21\u3001\u5df2\u7528 " + ElixirConsumeRules.getConsumedCount(player, realm) + " \u6b21"));

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void applyEffect(EntityPlayer player, int value) {
        NBTTagCompound data = player.getEntityData();
        switch (elixirType) {
            case SHIELD_MAX:
                data.setDouble("GuardMaxBonus", data.getDouble("GuardMaxBonus") + value);
                break;
            case SHIELD_REGEN:
                data.setDouble("GuardRegenBonus", data.getDouble("GuardRegenBonus") + value);
                break;
            case SHIELD_ABSORPTION:
                data.setDouble("GuardAbsorptionBonus", data.getDouble("GuardAbsorptionBonus") + value / 100.0);
                break;
        }
    }
}