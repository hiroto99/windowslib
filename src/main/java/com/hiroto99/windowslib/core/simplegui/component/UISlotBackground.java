package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class UISlotBackground implements IUIComponent {
    private int x, y;
    private final int width = 18;
    private final int height = 18;
    private final Identifier slotTexture; // スロット用のテクスチャを指定

    public UISlotBackground(Identifier slotTexture) {
        this.slotTexture = slotTexture;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) {} // サイズはスロット標準の18x18固定

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 例: 指定したスロット用テクスチャを描画
        graphics.blit(RenderPipelines.GUI_TEXTURED, slotTexture, x, y, 0, 0, width, height, 18, 18);
    }
}