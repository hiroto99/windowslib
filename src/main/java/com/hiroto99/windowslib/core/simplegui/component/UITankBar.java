package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.function.Supplier;

public class UITankBar implements IUIComponent {
    private int x, y, width, height;
    private final Supplier<Float> fillRatioSupplier; // 0.0 (空) ～ 1.0 (満タン)

    public UITankBar(Supplier<Float> fillRatioSupplier) {
        this.fillRatioSupplier = fillRatioSupplier;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1. タンクの背景（空の部分の枠や色）
        graphics.fill(x, y, x + width, y + height, 0xFF333333);

        // 2. 液体部分（下から上に向かって溜まるように計算）
        float ratio = Math.clamp(fillRatioSupplier.get(), 0.0f, 1.0f);
        int filledHeight = (int) (height * ratio);
        int renderY = y + (height - filledHeight); // 下から描画するためYを調整

        // 例: 水色（液体っぽく）で描画
        graphics.fill(x, renderY, x + width, y + height, 0xFF4287f5);
    }
}