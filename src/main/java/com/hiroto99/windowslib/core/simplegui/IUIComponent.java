package com.hiroto99.windowslib.core.simplegui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public interface IUIComponent {
    void setPosition(int x, int y);
    void setSize(int width, int height);

    // 描画処理
    void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick);

    // マウス入力やクリックのハンドリング（必要に応じて）
    default boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }
}
