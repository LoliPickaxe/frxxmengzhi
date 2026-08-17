package com.frxx.mengzhi.gui;

import com.frxx.mengzhi.handler.SelfDestructClientHandler;
import com.frxx.mengzhi.handler.SelfDestructHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiSelfDestruct extends GuiScreen {

    private GuiButton btnPhysical;
    private GuiButton btnSoul;

    @Override
    public void initGui() {
        SelfDestructClientHandler.resetConfirm();
        int centerX = width / 2;
        int centerY = height / 2;
        btnPhysical = new GuiButton(0, centerX - 100, centerY - 15, 200, 20, "");
        btnSoul = new GuiButton(1, centerX - 100, centerY + 10, 200, 20, "");
        updateButtonLabels();
        buttonList.add(btnPhysical);
        buttonList.add(btnSoul);
    }

    private void updateButtonLabels() {
        btnPhysical.displayString = (SelfDestructClientHandler.physicalEnabled ? TextFormatting.GREEN : TextFormatting.GRAY)
            + "\u8089\u8eab\u7206\u70b8\uff08\u7269\u7406\uff09: "
            + (SelfDestructClientHandler.physicalEnabled ? "\u5f00\u542f" : "\u5173\u95ed");
        btnSoul.displayString = (SelfDestructClientHandler.soulEnabled ? TextFormatting.GREEN : TextFormatting.GRAY)
            + "\u7075\u9b42\u7206\u70b8\uff08\u975e\u7269\u7406\uff09: "
            + (SelfDestructClientHandler.soulEnabled ? "\u5f00\u542f" : "\u5173\u95ed");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            SelfDestructClientHandler.physicalEnabled = !SelfDestructClientHandler.physicalEnabled;
            updateButtonLabels();
        } else if (button.id == 1) {
            SelfDestructClientHandler.soulEnabled = !SelfDestructClientHandler.soulEnabled;
            updateButtonLabels();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_V) {
            mc.player.closeScreen();
            return;
        }
        // G 键由 SelfDestructClientHandler 的键盘轮询统一处理
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int centerX = width / 2;
        int centerY = height / 2;

        drawCenteredString(fontRenderer, TextFormatting.GOLD + "\u81ea\u7206\u51c6\u5907", centerX, centerY - 75, 0xFFFFFF);

        NBTTagCompound data = mc.player.getEntityData();
        int jingJie = (int) Math.floor(data.getDouble("JingJieNum"));
        String realmName = jingJie >= 1 ? data.getString("JingJie") : "\u672a\u4fee\u4ed9";
        int radius = SelfDestructHandler.getBlastRadius(data);
        drawCenteredString(fontRenderer, "\u5f53\u524d\u5883\u754c: " + realmName + "    \u7206\u70b8\u534a\u5f84: " + radius + " \u683c",
            centerX, centerY - 50, 0xFFFFFF);
        drawCenteredString(fontRenderer, "\u4f24\u5bb3 = (\u7269\u7406\u653b\u51fb + \u6cd5\u672f\u653b\u51fb) \u00d7 100\uff0c\u6bcf2\u683c\u8870\u51cf10%\uff0c\u6700\u4f4e60%",
            centerX, centerY + 35, 0xAAAAAA);
        drawCenteredString(fontRenderer, TextFormatting.RED + "\u4ee3\u4ef7: \u7acb\u523b\u6b7b\u4ea1 + \u6c38\u4e45\u635f\u5931\u5bff\u5143 (\u8089\u4f5310% / \u7075\u9b4215% / \u53cc\u4fee20%)",
            centerX, centerY + 48, 0xFFFFFF);

        if (SelfDestructClientHandler.confirmState) {
            drawCenteredString(fontRenderer, TextFormatting.RED + "\u60a8\u771f\u7684\u90a3\u4e48\u505a\uff1f",
                centerX, centerY - 90, 0xFFFFFF);
            drawCenteredString(fontRenderer, TextFormatting.DARK_RED + "\u518d\u6309\u4e00\u6b21 G \u786e\u8ba4\u7206\u70b8\uff08\u8d85\u8fc7 1 \u79d2\u9700\u91cd\u65b0\u786e\u8ba4\uff09",
                centerX, centerY - 78, 0xFFFFFF);
        }

        drawCenteredString(fontRenderer, TextFormatting.GRAY + "\u6309 G \u5f00\u59cb\u81ea\u7206\uff08\u4e24\u6b21\u786e\u8ba4\uff09 | V / Esc \u5173\u95ed",
            centerX, centerY + 70, 0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}