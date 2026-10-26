package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import com.hiroto99.windowslib.util.StringTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class UILabel implements IUIComponent {
    private int x, y;
    private final Component text;
    private final int color;
    private Supplier<StringTranslator> onHover; // ツールチップ用の処理

    public UILabel(Component text, int color) {
        this.text = text;
        this.color = color;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) {}
    public void setLabel(Supplier<StringTranslator> onHover) { this.onHover = onHover; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, text, x, y, color, false);
    }

    @Override
    public boolean tooltipRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (onHover == null) {
            return false;
        }
        var font = Minecraft.getInstance().font;
        int textWidth = font.width(text);
        int textX = x + textWidth;
        int textY = y + 8;
        if (mouseX >= x && mouseX <= textX && mouseY >= y && mouseY <= textY) {
            guiGraphics.setComponentTooltipForNextFrame(font, onHover.get().toLiteralList(), mouseX, mouseY);
            return true;
        }
        return false;
    }
}