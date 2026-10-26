package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import com.hiroto99.windowslib.util.StringTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UICheckbox implements IUIComponent {
    private int x, y;
    private final int size = 12;
    private final String label;
    private boolean checked;
    private final Consumer<Boolean> onToggled;
    private Supplier<StringTranslator> onHover; // ツールチップ用の処理

    public UICheckbox(String label, boolean initialChecked, Consumer<Boolean> onToggled) {
        this.label = label;
        this.checked = initialChecked;
        this.onToggled = onToggled;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) {}
    public void setLabel(Supplier<StringTranslator> onHover) { this.onHover = onHover; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 四角いチェックボックスの枠を描画
        graphics.fill(x, y, x + size, y + size, 0xFF333333);
        if (checked) {
            // チェックが入っている場合の塗りつぶしやマーク
            graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF55FF55);
        }
        // ラベルの描画
        var font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.text(font, net.minecraft.network.chat.Component.literal(label), x + size + 4, y + 2, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        // チェックボックス周辺がクリックされたか
        if (mx >= x && mx <= x + size + 50 && my >= y && my <= y + size) {
            checked = !checked;
            onToggled.accept(checked);
            return true;
        }
        return false;
    }

    @Override
    public boolean tooltipRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (onHover == null) {
            return false;
        }
        if (mouseX >= x && mouseX <= x + size + 50 && mouseY >= y && mouseY <= y + size) {
            var font = Minecraft.getInstance().font;
            guiGraphics.setComponentTooltipForNextFrame(font, onHover.get().toLiteralList(), mouseX, mouseY);
            return true;
        }
        return false;
    }
}
