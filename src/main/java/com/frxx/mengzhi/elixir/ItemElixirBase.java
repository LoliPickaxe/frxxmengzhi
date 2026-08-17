package com.frxx.mengzhi.elixir;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public abstract class ItemElixirBase extends Item {
    protected final ElixirType elixirType;
    protected final ElixirRealm realm;
    protected final boolean isTemporary;
    protected final int baseValue;
    protected final int toleranceCost;

    public ItemElixirBase(ElixirType type, ElixirRealm realm, boolean isTemporary, int baseValue, int toleranceCost) {
        this.elixirType = type;
        this.realm = realm;
        this.isTemporary = isTemporary;
        this.baseValue = baseValue;
        this.toleranceCost = toleranceCost;

        this.setMaxStackSize(64);
        this.setCreativeTab(FanRenXiuXianMengZhi.TAB_ELIXIR);
        this.setRegistryName("frxxmengzhi", getElixirRegistryName());
        this.setUnlocalizedName(getElixirUnlocalizedName());
    }

    public String getElixirRegistryName() {
        String prefix = isTemporary ? "temp_" : "perm_";
        return prefix + elixirType.getId() + "_" + realm.getShortName();
    }

    public String getElixirUnlocalizedName() {
        return "frxxmengzhi." + (isTemporary ? "temp_" : "perm_") + elixirType.getId() + "_" + realm.getShortName();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        net.minecraft.util.ResourceLocation rl = this.getRegistryName();
        return net.minecraft.util.text.translation.I18n.translateToLocal("item." + rl.getResourceDomain() + "." + rl.getResourcePath() + ".name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable net.minecraft.world.World world, java.util.List<String> tooltip, net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add("\u00a77" + elixirType.getDescription());
        tooltip.add("\u00a7e\u5883\u754c: \u00a7f" + realm.getFullName());
        tooltip.add("\u00a7e\u57fa\u7840\u6570\u503c: \u00a7f+" + baseValue);
        tooltip.add("\u00a78\u670d\u7528\u9650\u5236: \u7ec3\u6c14 6\u6b21 / \u7b51\u57fa 8\u6b21 / \u7ed3\u4e39 12\u6b21 / \u5143\u5a74 18\u6b21 / \u5316\u795e 24\u6b21");
        tooltip.add("\u00a78\u670d\u7528\u524d\u63d0: \u4fee\u4e3a\u5883\u754c\u4e0d\u4f4e\u4e8e\u5f53\u524d\u4e39\u836f\u5883\u754c");
        tooltip.add("\u00a78\u670d\u7528\u6b21\u6570: \u540c\u5883\u754c\u4e39\u836f\u6309\u5f53\u524d\u5883\u754c\u914d\u989d\u8ba1\u6570\u3001\u7a81\u7834\u540e\u91cd\u7f6e\u8ba1\u6570");
        tooltip.add("\u00a78\u4f4e\u5883\u754c\u4e39\u836f: \u9ad8\u5883\u754c\u670d\u7528\u4f4e\u5883\u754c\u4e39\u836f\u6b21\u6570 = \u4f4e\u5883\u754c\u6b21\u6570\u00d72\uff08\u72ec\u7acb\u8ba1\u6570\uff09");
        
        if (isTemporary) {
            tooltip.add("\u00a7c\u4e00\u65f6\u4e38\u836f: \u6548\u679c\u5f3a\u529b\u4f46\u6d88\u8017\u771f\u5143");
            tooltip.add("\u00a7c\u4ee3\u4ef7: \u00a7f-" + toleranceCost + " \u771f\u5143/\u6b21");
            tooltip.add("\u00a77\u6301\u7eed\u65f6\u95f4: 600 \u79d2 (10 \u5206\u949f)");
            tooltip.add("\u00a77\u4e00\u65f6\u4e38\u836f\u4e0d\u53d7\u670d\u7528\u6b21\u6570\u9650\u5236");
        } else {
            tooltip.add("\u00a7a\u6c38\u4e45\u4e38\u836f: \u6548\u679c\u6c38\u4e45\u751f\u6548");
            tooltip.add("\u00a7a\u8010\u836f\u6027: \u00a7f\u6bcf\u670f\u7528\u4e00\u6b21\u589e\u52a0 " + toleranceCost + " \u70b9\u8010\u836f\u503c");
            tooltip.add("\u00a77\u8010\u836f\u503c\u8d8a\u9ad8\uff0c\u540c\u7c7b\u4e38\u836f\u6548\u679c\u9012\u51cf");
        }
        
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey("Tolerance")) {
                tooltip.add("\u00a78\u5f53\u524d\u8010\u836f\u503c: " + nbt.getInteger("Tolerance"));
            }
        }
    }

    public ElixirType getElixirType() { return elixirType; }
    public ElixirRealm getRealm() { return realm; }
    public boolean isTemporary() { return isTemporary; }
    public int getBaseValue() { return baseValue; }
    public int getToleranceCost() { return toleranceCost; }

    public int calculateEffect(int currentTolerance) {
        if (isTemporary) {
            return baseValue;
        }
        double reduction = currentTolerance / (double)(currentTolerance + 100);
        return (int) Math.floor(baseValue * (1.0 - reduction));
    }
}