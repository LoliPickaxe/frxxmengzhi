package com.frxx.mengzhi.elixir;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CreativeTabElixir extends CreativeTabs {

    public static final CreativeTabElixir TAB_ELIXIR = new CreativeTabElixir();

    private CreativeTabElixir() {
        super("frxxmengzhi_elixir");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack getTabIconItem() {
        return new ItemStack(FanRenXiuXianMengZhi.ELIXIR_ICON_ITEM);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getTabLabel() {
        return "frxxmengzhi_elixir";
    }
}