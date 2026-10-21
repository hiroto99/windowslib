package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class UITextButton implements IUIComponent {
    private int x, y;
    private final Component text;
    private final int normalColor;
    private final int hoverColor;
    private final Runnable onPress;

    // 通常時の色と、マウスホバー時の色を指定できるようにする
    public UITextButton(Component text, int normalColor, int hoverColor, Runnable onPress) {
        this.text = text;
        this.normalColor = normalColor;
        this.hoverColor = hoverColor;
        this.onPress = onPress;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) {} // サイズはフォント幅から自動計算

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int width = font.width(text);
        int height = 9; // 標準的なフォントの高さ

        // マウスが文字の上に載っているか判定
        boolean isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int currentColor = isHovered ? hoverColor : normalColor;

        // 文字を描画（ホバー時は色を変える）
        graphics.text(font, text, x, y, currentColor, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        var font = Minecraft.getInstance().font;
        int width = font.width(text);
        int height = 9;

        double mouseX = event.x();
        double mouseY = event.y();

        // クリック位置がテキストの範囲内ならアクションを実行
        if (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height) {
            onPress.run();
            return true;
        }
        return false;
    }
}