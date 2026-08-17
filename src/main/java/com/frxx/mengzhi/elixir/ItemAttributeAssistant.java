package com.frxx.mengzhi.elixir;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
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

import javax.annotation.Nullable;
import java.util.List;

public class ItemAttributeAssistant extends Item {

    public static final String KEY_SHIELD_MAX = "ShieldMax";
    public static final String KEY_SHIELD_CURRENT = "ShieldCurrent";
    public static final String KEY_SHIELD_REGEN = "ShieldRegen";
    public static final String KEY_QI_TO_SHIELD = "QiToShieldRatio";
    public static final String KEY_ABSORPTION_RATIO = "AbsorptionRatio";

    // 小助手数值上限：int 最大值
    public static final int MAX_SHIELD_VALUE = 2147483647;

    // 玩家数据中保存的配置键（服务端权威）
    public static final String PLAYER_CONFIG_KEY = "FrxxAssistantConfig";

    public static final int DEFAULT_SHIELD_MAX = 1000;
    public static final int DEFAULT_SHIELD_CURRENT = 1000;
    public static final int DEFAULT_SHIELD_REGEN = 50;
    public static final int DEFAULT_QI_TO_SHIELD = 10;
    public static final int DEFAULT_ABSORPTION_RATIO = 200;

    public ItemAttributeAssistant() {
        this.setMaxStackSize(1);
        this.setCreativeTab(FanRenXiuXianMengZhi.TAB_ELIXIR);
        this.setRegistryName("frxxmengzhi:attribute_assistant");
        this.setUnlocalizedName("frxxmengzhi.attribute_assistant");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        net.minecraft.util.ResourceLocation rl = this.getRegistryName();
        return net.minecraft.util.text.translation.I18n.translateToLocal("item." + rl.getResourceDomain() + "." + rl.getResourcePath() + ".name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable net.minecraft.world.World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GOLD + "=== \u5c5e\u6027\u5c0f\u52a9\u624b (\u521b\u9020\u6a21\u5f0f\u4e13\u7528) ===");
        tooltip.add(TextFormatting.WHITE + "\u53f3\u952e: \u6253\u5f00\u914d\u7f6e\u754c\u9762");
        tooltip.add(TextFormatting.WHITE + "Shift+\u53f3\u952e: \u5e94\u7528\u914d\u7f6e\u5230\u73a9\u5bb6");
        tooltip.add("");

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null) {
            tooltip.add(TextFormatting.YELLOW + "\u5f53\u524d\u914d\u7f6e:");
            tooltip.add(TextFormatting.WHITE + "  \u62a4\u76d6\u6700\u5927\u503c: " + TextFormatting.GREEN + nbt.getInteger(KEY_SHIELD_MAX));
            tooltip.add(TextFormatting.WHITE + "  \u5f53\u524d\u62a4\u76d6\u503c: " + TextFormatting.GREEN + nbt.getInteger(KEY_SHIELD_CURRENT));
            tooltip.add(TextFormatting.WHITE + "  \u62a4\u76d6\u56de\u590d/\u79d2: " + TextFormatting.GREEN + nbt.getInteger(KEY_SHIELD_REGEN));
            tooltip.add(TextFormatting.WHITE + "  \u7075\u6c14\u8f6c\u62a4\u76d6\u6bd4: " + TextFormatting.GREEN + "1:" + nbt.getInteger(KEY_QI_TO_SHIELD));
            tooltip.add(TextFormatting.WHITE + "  \u4f24\u5bb3\u5438\u6536\u6bd4 (1\u70b9\u62a4\u76d6=\u591a\u5c11\u4f24\u5bb3): " + TextFormatting.GREEN + (nbt.getInteger(KEY_ABSORPTION_RATIO) / 100.0) + "x");
        } else {
            tooltip.add(TextFormatting.GRAY + "\u672a\u914d\u7f6e\uff0c\u5c06\u4f7f\u7528\u9ed8\u8ba4\u503c");
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (player.isSneaking()) {
            if (!world.isRemote) {
                applyToPlayer(player, stack);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        } else {
            if (world.isRemote) {
                openGuiClient(stack);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
    }

    @SideOnly(Side.CLIENT)
    private void openGuiClient(ItemStack stack) {
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
            new GuiAttributeAssistant(stack)
        );
    }

    private void applyToPlayer(EntityPlayer player, ItemStack stack) {
        NBTTagCompound config = player.getEntityData().getCompoundTag(PLAYER_CONFIG_KEY);
        if (config.hasNoTags()) {
            config = stack.getTagCompound();
            if (config == null) {
                config = getDefaultConfig();
            }
        }
        applyConfigToPlayer(player, config);
    }

    /** 客户端 GUI 保存时经网络包调用：写入玩家数据并立即应用（服务端） */
    public static void saveConfigToPlayer(EntityPlayer player, int shieldMax, int shieldCurrent, int shieldRegen,
                                          int qiToShield, int absorptionRatio) {
        NBTTagCompound config = new NBTTagCompound();
        config.setInteger(KEY_SHIELD_MAX, shieldMax);
        config.setInteger(KEY_SHIELD_CURRENT, shieldCurrent);
        config.setInteger(KEY_SHIELD_REGEN, shieldRegen);
        config.setInteger(KEY_QI_TO_SHIELD, qiToShield);
        config.setInteger(KEY_ABSORPTION_RATIO, absorptionRatio);
        player.getEntityData().setTag(PLAYER_CONFIG_KEY, config);
        applyConfigToPlayer(player, config);
    }

    public static void applyConfigToPlayer(EntityPlayer player, NBTTagCompound config) {
        // 数值上限：不允许超过 int 最大值（2147483647）
        int shieldMax = Math.min(config.getInteger(KEY_SHIELD_MAX), MAX_SHIELD_VALUE);
        int shieldCurrent = Math.min(config.getInteger(KEY_SHIELD_CURRENT), MAX_SHIELD_VALUE);
        int shieldRegen = Math.min(config.getInteger(KEY_SHIELD_REGEN), MAX_SHIELD_VALUE);
        int qiToShield = Math.min(config.getInteger(KEY_QI_TO_SHIELD), MAX_SHIELD_VALUE);
        int absorptionRatio = Math.min(config.getInteger(KEY_ABSORPTION_RATIO), MAX_SHIELD_VALUE);

        NBTTagCompound data = player.getEntityData();

        // Apply all 5 values
        data.setInteger(KEY_SHIELD_MAX, shieldMax);
        data.setInteger(KEY_SHIELD_CURRENT, shieldCurrent);
        data.setInteger(KEY_SHIELD_REGEN, shieldRegen);
        data.setInteger(KEY_QI_TO_SHIELD, qiToShield);
        data.setInteger(KEY_ABSORPTION_RATIO, absorptionRatio);

        // 护盾上限覆盖：>0 生效（GuardHandler.computeGuardMax 优先读取），设 0 则恢复按境界计算
        if (shieldMax > 0) {
            data.setDouble("GuardMaxOverride", shieldMax);
        } else {
            data.removeTag("GuardMaxOverride");
        }
        // 护盾回复覆盖/秒：>0 生效（GuardHandler 按 值/20 每 tick 回复），设 0 则恢复按上限比例
        if (shieldRegen > 0) {
            data.setDouble("GuardRegenOverride", shieldRegen);
        } else {
            data.removeTag("GuardRegenOverride");
        }

        if (!data.getBoolean("GuardOn")) {
            data.setBoolean("GuardOn", true);
        }
        data.setDouble("GuardMax", shieldMax);
        data.setDouble("Guard", shieldCurrent);
        data.setDouble("GuardAbsorption", absorptionRatio / 100.0);

        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            FanRenXiuXianMengZhi.NETWORK.sendTo(
                new com.frxx.mengzhi.network.GuardStatePacket(
                    true,
                    shieldCurrent,
                    shieldMax
                ),
                (net.minecraft.entity.player.EntityPlayerMP) player
            );
        }

        player.sendMessage(new TextComponentString(TextFormatting.GREEN + "[\u5c5e\u6027\u5c0f\u52a9\u624b] \u5df2\u5e94\u7528\u914d\u7f6e\u5230\u73a9\u5bb6"));
        player.sendMessage(new TextComponentString(TextFormatting.WHITE + "  \u62a4\u76d6\u6700\u5927\u503c: " + TextFormatting.GREEN + shieldMax));
        player.sendMessage(new TextComponentString(TextFormatting.WHITE + "  \u5f53\u524d\u62a4\u76d6\u503c: " + TextFormatting.GREEN + shieldCurrent));
        player.sendMessage(new TextComponentString(TextFormatting.WHITE + "  \u62a4\u76d6\u56de\u590d: " + TextFormatting.GREEN + shieldRegen));
        player.sendMessage(new TextComponentString(TextFormatting.WHITE + "  \u7075\u6c14\u8f6c\u62a4\u76d6: " + TextFormatting.GREEN + "1:" + qiToShield));
        player.sendMessage(new TextComponentString(TextFormatting.WHITE + "  \u4f24\u5bb3\u5438\u6536\u6bd4: " + TextFormatting.GREEN + (absorptionRatio / 100.0) + "x (1\u70b9\u62a4\u76d6=" + (absorptionRatio / 100.0) + "\u4f24\u5bb3)"));
    }

    public static NBTTagCompound getDefaultConfig() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger(KEY_SHIELD_MAX, DEFAULT_SHIELD_MAX);
        nbt.setInteger(KEY_SHIELD_CURRENT, DEFAULT_SHIELD_CURRENT);
        nbt.setInteger(KEY_SHIELD_REGEN, DEFAULT_SHIELD_REGEN);
        nbt.setInteger(KEY_QI_TO_SHIELD, DEFAULT_QI_TO_SHIELD);
        nbt.setInteger(KEY_ABSORPTION_RATIO, DEFAULT_ABSORPTION_RATIO);
        return nbt;
    }

    public static NBTTagCompound getOrCreateConfig(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(getDefaultConfig());
        }
        return stack.getTagCompound();
    }
}