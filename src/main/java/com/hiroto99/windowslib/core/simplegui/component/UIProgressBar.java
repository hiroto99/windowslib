package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.function.Supplier;

public class UIProgressBar implements IUIComponent {
    private int x, y, width, height;
    private final Supplier<Float> progressSupplier; // 0.0 ~ 1.0

    public UIProgressBar(Supplier<Float> progressSupplier) {
        this.progressSupplier = progressSupplier;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 背景（必要に応じて色や描画方法を調整してください）
        graphics.fill(x, y, x + width, y + height, 0xFF555555);

        float progress = Math.clamp(progressSupplier.get(), 0.0f, 1.0f);
        int filledWidth = (int) (width * progress);
        graphics.fill(x, y, x + filledWidth, y + height, 0xFF00FF00);
    }
}
