package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.WindowsLib;
import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import com.hiroto99.windowslib.util.StringTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class UISlotBackground implements IUIComponent {
    private int x, y;
    private final int width = 18;
    private final int height = 18;
    private final Identifier slotTexture; // スロット用のテクスチャを指定
    private static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(WindowsLib.MODID, "textures/gui/slot.png");
    private Supplier<StringTranslator> onHover; // ツールチップ用の処理

    public UISlotBackground() {
        this(DEFAULT_TEXTURE);
    }

    public UISlotBackground(Identifier slotTexture) {
        this.slotTexture = slotTexture;
    }

    @Override public void setPosition(int x, int y) { this.x = x - 1; this.y = y - 1; }
    @Override public void setSize(int width, int height) {} // サイズはスロット標準の18x18固定
    public void setLabel(Supplier<StringTranslator> onHover) { this.onHover = onHover; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 例: 指定したスロット用テクスチャを描画
        graphics.blit(RenderPipelines.GUI_TEXTURED, slotTexture, x, y, 0, 0, width, height, width, height);
    }

    @Override
    public boolean tooltipRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (onHover == null) {
            return false;
        }
        var font = Minecraft.getInstance().font;
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            guiGraphics.setComponentTooltipForNextFrame(font, onHover.get().toLiteralList(), mouseX, mouseY);
            return true;
        }
        return false;
    }
}