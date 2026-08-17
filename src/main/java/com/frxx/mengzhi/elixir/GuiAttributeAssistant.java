package com.frxx.mengzhi.elixir;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiAttributeAssistant extends GuiScreen {

    private final ItemStack stack;
    private GuiTextField fieldShieldMax;
    private GuiTextField fieldShieldCurrent;    // 新增
    private GuiTextField fieldShieldRegen;
    private GuiTextField fieldQiToShield;
    private GuiTextField fieldAbsorptionRatio;
    private GuiButton btnApply;
    private GuiButton btnReset;
    private GuiButton btnClose;

    public GuiAttributeAssistant(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;
        int fieldWidth = 150;
        int fieldHeight = 20;
        int startY = centerY - 100;

        net.minecraft.nbt.NBTTagCompound config = ItemAttributeAssistant.getOrCreateConfig(stack);

        fieldShieldMax = new GuiTextField(0, fontRenderer, centerX - fieldWidth / 2, startY, fieldWidth, fieldHeight);
        fieldShieldMax.setText(String.valueOf(config.getInteger(ItemAttributeAssistant.KEY_SHIELD_MAX)));
        fieldShieldMax.setMaxStringLength(10);
        fieldShieldMax.setEnableBackgroundDrawing(true);

        fieldShieldCurrent = new GuiTextField(1, fontRenderer, centerX - fieldWidth / 2, startY + 30, fieldWidth, fieldHeight);
        fieldShieldCurrent.setText(String.valueOf(config.getInteger(ItemAttributeAssistant.KEY_SHIELD_CURRENT)));
        fieldShieldCurrent.setMaxStringLength(10);
        fieldShieldCurrent.setEnableBackgroundDrawing(true);

        fieldShieldRegen = new GuiTextField(2, fontRenderer, centerX - fieldWidth / 2, startY + 60, fieldWidth, fieldHeight);
        fieldShieldRegen.setText(String.valueOf(config.getInteger(ItemAttributeAssistant.KEY_SHIELD_REGEN)));
        fieldShieldRegen.setMaxStringLength(10);
        fieldShieldRegen.setEnableBackgroundDrawing(true);

        fieldQiToShield = new GuiTextField(3, fontRenderer, centerX - fieldWidth / 2, startY + 90, fieldWidth, fieldHeight);
        fieldQiToShield.setText(String.valueOf(config.getInteger(ItemAttributeAssistant.KEY_QI_TO_SHIELD)));
        fieldQiToShield.setMaxStringLength(10);
        fieldQiToShield.setEnableBackgroundDrawing(true);

        fieldAbsorptionRatio = new GuiTextField(4, fontRenderer, centerX - fieldWidth / 2, startY + 120, fieldWidth, fieldHeight);
        fieldAbsorptionRatio.setText(String.valueOf(config.getInteger(ItemAttributeAssistant.KEY_ABSORPTION_RATIO)));
        fieldAbsorptionRatio.setMaxStringLength(10);
        fieldAbsorptionRatio.setEnableBackgroundDrawing(true);

        btnApply = new GuiButton(0, centerX - 100, startY + 160, 90, 20, "\u5e94\u7528\u5e76\u4fdd\u5b58");
        btnReset = new GuiButton(1, centerX + 10, startY + 160, 90, 20, "\u91cd\u7f6e\u9ed8\u8ba4");
        btnClose = new GuiButton(2, centerX - 45, startY + 190, 90, 20, "\u5173\u95ed");

        buttonList.add(btnApply);
        buttonList.add(btnReset);
        buttonList.add(btnClose);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            saveConfig();
            mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.GREEN + "[\u5c5e\u6027\u5c0f\u52a9\u624b] \u914d\u7f6e\u5df2\u4fdd\u5b58\u5230\u7269\u54c1"));
        } else if (button.id == 1) {
            resetToDefaults();
        } else if (button.id == 2) {
            mc.player.closeScreen();
        }
    }

    private void saveConfig() {
        net.minecraft.nbt.NBTTagCompound nbt = ItemAttributeAssistant.getOrCreateConfig(stack);
        nbt.setInteger(ItemAttributeAssistant.KEY_SHIELD_MAX, parseBounded(fieldShieldMax.getText(), ItemAttributeAssistant.DEFAULT_SHIELD_MAX));
        nbt.setInteger(ItemAttributeAssistant.KEY_SHIELD_CURRENT, parseBounded(fieldShieldCurrent.getText(), ItemAttributeAssistant.DEFAULT_SHIELD_CURRENT));
        nbt.setInteger(ItemAttributeAssistant.KEY_SHIELD_REGEN, parseBounded(fieldShieldRegen.getText(), ItemAttributeAssistant.DEFAULT_SHIELD_REGEN));
        nbt.setInteger(ItemAttributeAssistant.KEY_QI_TO_SHIELD, parseBounded(fieldQiToShield.getText(), ItemAttributeAssistant.DEFAULT_QI_TO_SHIELD));
        nbt.setInteger(ItemAttributeAssistant.KEY_ABSORPTION_RATIO, parseBounded(fieldAbsorptionRatio.getText(), ItemAttributeAssistant.DEFAULT_ABSORPTION_RATIO));

        // 同时发送到服务端，写入玩家数据并立即应用（解决客户端物品 NBT 不同步问题）
        com.frxx.mengzhi.FanRenXiuXianMengZhi.NETWORK.sendToServer(
            new com.frxx.mengzhi.network.AssistantConfigPacket(
                nbt.getInteger(ItemAttributeAssistant.KEY_SHIELD_MAX),
                nbt.getInteger(ItemAttributeAssistant.KEY_SHIELD_CURRENT),
                nbt.getInteger(ItemAttributeAssistant.KEY_SHIELD_REGEN),
                nbt.getInteger(ItemAttributeAssistant.KEY_QI_TO_SHIELD),
                nbt.getInteger(ItemAttributeAssistant.KEY_ABSORPTION_RATIO)
            )
        );
    }

    /** 剥离千位分隔逗号后解析；非法输入回退默认值；钳制到小助手数值上限 */
    private int parseBounded(String text, int fallback) {
        try {
            String cleaned = text.replace(",", "").replace("\uFF0C", "").trim();
            if (cleaned.isEmpty()) {
                return 0;
            }
            long value = Long.parseLong(cleaned);
            return (int) Math.min(Math.max(0, value), ItemAttributeAssistant.MAX_SHIELD_VALUE);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void resetToDefaults() {
        fieldShieldMax.setText(String.valueOf(ItemAttributeAssistant.DEFAULT_SHIELD_MAX));
        fieldShieldCurrent.setText(String.valueOf(ItemAttributeAssistant.DEFAULT_SHIELD_CURRENT));
        fieldShieldRegen.setText(String.valueOf(ItemAttributeAssistant.DEFAULT_SHIELD_REGEN));
        fieldQiToShield.setText(String.valueOf(ItemAttributeAssistant.DEFAULT_QI_TO_SHIELD));
        fieldAbsorptionRatio.setText(String.valueOf(ItemAttributeAssistant.DEFAULT_ABSORPTION_RATIO));
        saveConfig();
    }

    @Override
    public void updateScreen() {
        fieldShieldMax.updateCursorCounter();
        fieldShieldCurrent.updateCursorCounter();
        fieldShieldRegen.updateCursorCounter();
        fieldQiToShield.updateCursorCounter();
        fieldAbsorptionRatio.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        fieldShieldMax.textboxKeyTyped(typedChar, keyCode);
        fieldShieldCurrent.textboxKeyTyped(typedChar, keyCode);
        fieldShieldRegen.textboxKeyTyped(typedChar, keyCode);
        fieldQiToShield.textboxKeyTyped(typedChar, keyCode);
        fieldAbsorptionRatio.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        fieldShieldMax.mouseClicked(mouseX, mouseY, mouseButton);
        fieldShieldCurrent.mouseClicked(mouseX, mouseY, mouseButton);
        fieldShieldRegen.mouseClicked(mouseX, mouseY, mouseButton);
        fieldQiToShield.mouseClicked(mouseX, mouseY, mouseButton);
        fieldAbsorptionRatio.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        int centerX = width / 2;
        int centerY = height / 2;

        drawCenteredString(fontRenderer, TextFormatting.GOLD + "\u5c5e\u6027\u5c0f\u52a9\u624b - \u914d\u7f6e\u9762\u677f", centerX, centerY - 130, 0xFFFFFF);

        drawString(fontRenderer, TextFormatting.WHITE + "\u62a4\u76d6\u6700\u5927\u503c:", centerX - 120, centerY - 95, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.WHITE + "\u5f53\u524d\u62a4\u76d6\u503c:", centerX - 120, centerY - 65, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.WHITE + "\u62a4\u76d6\u56de\u590d/\u79d2:", centerX - 120, centerY - 35, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.WHITE + "\u7075\u6c14\u8f6c\u62a4\u76d6\u6bd4 (1:x):", centerX - 120, centerY - 5, 0xFFFFFF);
        drawString(fontRenderer, TextFormatting.WHITE + "\u4f24\u5bb3\u5438\u6536\u6bd4 (1\u70b9\u62a4\u76d6=x\u4f24\u5bb3):", centerX - 120, centerY + 25, 0xFFFFFF);

        fieldShieldMax.drawTextBox();
        fieldShieldCurrent.drawTextBox();
        fieldShieldRegen.drawTextBox();
        fieldQiToShield.drawTextBox();
        fieldAbsorptionRatio.drawTextBox();

        drawCenteredString(fontRenderer, TextFormatting.GRAY + "\u63d0\u793a: Shift+\u53f3\u952e\u53ef\u76f4\u63a5\u5e94\u7528\u5f53\u524d\u914d\u7f6e\u5230\u73a9\u5bb6", centerX, centerY + 220, 0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}