package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import com.hiroto99.windowslib.util.StringTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.function.Supplier;

public class UIEnergyBar implements IUIComponent {
    private int x, y, width, height;
    private final EnergyHandler energy; // エネルギー処理の関数
    private Supplier<StringTranslator> onHover; // ツールチップ用の処理

    public UIEnergyBar(EnergyHandler energy) {
        this.energy = energy;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }
    public void setLabel(Supplier<StringTranslator> onHover) { this.onHover = onHover; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1. タンクの背景（空の部分の枠や色）
        graphics.fill(x, y, x + width, y + height, 0xFF333333);

        float ratio = Math.clamp(getFillRatio(), 0.0f, 1.0f);
        int stored = (int) ((height - 2) * ratio);

        // 例: 黄色とオレンジ色で描画
        graphics.fillGradient(x + 1, y - 1 + (height - stored), x + width - 1,
                y + height - 1, 0xffb515FF, 0xff600bFF);
    }

    public float getFillRatio() {
        float amount = energy.getAmountAsInt();
        float capacity = energy.getCapacityAsInt();
        if (capacity <= 0) {
            return 0.0f;
        }
        return amount / capacity;
    }

    public EnergyHandler getEnergy() {
        return energy;
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