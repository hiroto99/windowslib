package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import com.hiroto99.windowslib.util.StringTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public class UIButton implements IUIComponent {
    private int x, y, width, height;
    private final Component label;
    private final Runnable onPress; // ボタンが押された時の処理
    private Supplier<StringTranslator> onHover; // ツールチップ用の処理

    public UIButton(Component label, Runnable onPress) {
        this.label = label;
        this.onPress = onPress;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }
    public void setLabel(Supplier<StringTranslator> onHover) { this.onHover = onHover; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // マウスがホバーしているかどうかで色を変える簡易的な判定
        boolean isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int bgColor = isHovered ? 0xFF888888 : 0xFF555555;

        // ボタンの背景を描画
        graphics.fill(x, y, x + width, y + height, bgColor);

        // ボタンの文字を中央付近に描画
        var font = Minecraft.getInstance().font;
        int textWidth = font.width(label);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        graphics.text(font, label, textX, textY, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // クリック位置がボタンの範囲内か判定
        // ※ event からマウス座標を取得するメソッド名に合わせて調整してください（例: event.x(), event.y() など）
        double mouseX = event.x();
        double mouseY = event.y();

        if (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height) {
            onPress.run(); // コールバックを実行
            return true;   // イベントを消費
        }
        return false;
    }

    @Override
    public boolean tooltipRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (onHover == null) {
            return false;
        }
        if (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height) {
            var font = Minecraft.getInstance().font;
            guiGraphics.setComponentTooltipForNextFrame(font, onHover.get().toLiteralList(), mouseX, mouseY);
            return true;
        }
        return false;
    }
}