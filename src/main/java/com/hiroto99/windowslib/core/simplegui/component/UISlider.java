package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class UISlider implements IUIComponent {
    private int x, y, width, height;
    private final String label;
    private double value; // 0.0 ～ 1.0
    private boolean isDragging = false;
    private final java.util.function.Consumer<Double> onValueChanged;

    public UISlider(String label, double initialValue, java.util.function.Consumer<Double> onValueChanged) {
        this.label = label;
        this.value = Math.clamp(initialValue, 0.0, 1.0);
        this.onValueChanged = onValueChanged;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 背景のバー
        graphics.fill(x, y, x + width, y + height, 0xFF555555);
        // つまみの位置を計算して描画
        int knobX = x + (int) (value * (width - 8));
        graphics.fill(knobX, y, knobX + 8, y + height, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (mx >= x && mx <= x + width && my >= y && my <= y + height) {
            isDragging = true;
            updateValue(mx);
            return true;
        }
        return false;
    }

    // ドラッグ中の処理なども組み合わせると完璧になります
    private void updateValue(double mouseX) {
        this.value = Math.clamp((mouseX - x) / (double) width, 0.0, 1.0);
        onValueChanged.accept(this.value);
    }
}
