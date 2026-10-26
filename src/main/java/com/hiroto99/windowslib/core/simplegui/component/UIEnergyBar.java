package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import com.hiroto99.windowslib.util.StringTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.function.Supplier;

public class UIEnergyBar implements IUIComponent {
    private int x, y, width, height;
    private final Supplier<EnergyHandler> energySupplier; // 液体処理の関数
    private Supplier<StringTranslator> onHover; // ツールチップ用の処理

    public UIEnergyBar(Supplier<EnergyHandler> fluidSupplier) {
        this.energySupplier = fluidSupplier;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }
    public void setLabel(Supplier<StringTranslator> onHover) { this.onHover = onHover; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int stored = (int)(height * getFillRatio());

        // 例: 水色（液体っぽく）で描画
        graphics.fillGradient(x, y + (height - stored), x + width,
                y + height, 0xffb51500, 0xff600b00);
    }

    public int getFillRatio() {
        return energySupplier.get().getAmountAsInt() / energySupplier.get().getCapacityAsInt();
    }

    public EnergyHandler getEnergy() {
        return energySupplier.get();
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