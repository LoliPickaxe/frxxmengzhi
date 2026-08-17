package com.frxx.mengzhi.gui;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.handler.TiandaoClientHandler;
import com.frxx.mengzhi.network.TiandaoPanelPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/** 天道面板：切换模式 / 制造天劫 / 改属性 */
@SideOnly(Side.CLIENT)
public class GuiTiandaoPanel extends GuiScreen {

    @Override
    public void initGui() {
        buttonList.clear();
        int centerX = width / 2;
        int centerY = height / 2;
        buttonList.add(new GuiButton(0, centerX - 100, centerY - 75, 200, 20,
            TextFormatting.AQUA + "切换模式（创造/生存/冒险）"));
        buttonList.add(new GuiButton(1, centerX - 100, centerY - 50, 200, 20,
            TextFormatting.RED + "制造天劫（200%面板伤害，无视护甲）"));
        buttonList.add(new GuiButton(2, centerX - 100, centerY - 25, 200, 20,
            TextFormatting.GOLD + "天道修改属性（NBT修改器）"));
        buttonList.add(new GuiButton(3, centerX - 100, centerY, 200, 20,
            TextFormatting.GRAY + "重新锁定目标（当前: " + TiandaoClientHandler.currentTargetName + "）"));
        buttonList.add(new GuiButton(4, centerX - 100, centerY + 25, 200, 20,
            TextFormatting.LIGHT_PURPLE + "天道之力（开/关）：攻击与右键抹除"));
        buttonList.add(new GuiButton(5, centerX - 100, centerY + 50, 200, 20,
            TextFormatting.DARK_PURPLE + "天道抹除状态：净化自身所有效果"));
        buttonList.add(new GuiButton(6, centerX - 100, centerY + 75, 200, 20,
            TextFormatting.DARK_RED + "天道自动攻击（开/关）：每刻9×9×9清场"));
        buttonList.add(new GuiButton(7, centerX - 100, centerY + 100, 200, 20, "关闭 (Esc)"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                FanRenXiuXianMengZhi.NETWORK.sendToServer(
                    new TiandaoPanelPacket(TiandaoPanelPacket.ACTION_SWITCH_MODE, -1, 0, 0.0));
                break;
            case 1:
                FanRenXiuXianMengZhi.NETWORK.sendToServer(
                    new TiandaoPanelPacket(TiandaoPanelPacket.ACTION_CAST_TRIBULATION,
                        TiandaoClientHandler.currentTargetId, 0, 0.0));
                break;
            case 2:
                mc.displayGuiScreen(new GuiTiandaoNbtEditor());
                break;
            case 3:
                TiandaoClientHandler.resolveTarget();
                initGui();
                break;
            case 4:
                FanRenXiuXianMengZhi.NETWORK.sendToServer(
                    new TiandaoPanelPacket(TiandaoPanelPacket.ACTION_TOGGLE_FORCE, -1, 0, 0.0));
                break;
            case 5:
                FanRenXiuXianMengZhi.NETWORK.sendToServer(
                    new TiandaoPanelPacket(TiandaoPanelPacket.ACTION_CLEAR_EFFECTS, -1, 0, 0.0));
                break;
            case 6:
                FanRenXiuXianMengZhi.NETWORK.sendToServer(
                    new TiandaoPanelPacket(TiandaoPanelPacket.ACTION_TOGGLE_AUTO, -1, 0, 0.0));
                break;
            case 7:
                mc.displayGuiScreen(null);
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = width / 2;
        int centerY = height / 2;
        drawCenteredString(fontRenderer, TextFormatting.DARK_RED + "天道·绝对主宰",
            centerX, centerY - 98, 0xFFFFFF);
        drawCenteredString(fontRenderer,
            "血量锁定100·免伤免死·免疫负面·灵力真元恒满·全自动维修",
            centerX, centerY - 84, 0xAAAAAA);
        drawCenteredString(fontRenderer,
            "当前目标: " + TextFormatting.YELLOW + TiandaoClientHandler.currentTargetName
                + TextFormatting.WHITE + "（看向生物后按【重新锁定目标】）",
            centerX, centerY + 122, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}