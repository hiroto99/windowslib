package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class UITextBox implements IUIComponent {
    private int x, y, width, height;
    private String text = "";
    private boolean isFocused = false;

    public UITextBox(String initialText) {
        this.text = initialText != null ? initialText : "";
    }

    public String getText() { return text; }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1. テキストボックスの背景（フォーカスされているかで枠の色を変える）
        int borderColor = isFocused ? 0xFFFF55FF : 0xFF888888;
        graphics.fill(x, y, x + width, y + height, 0xFF222222); // 黒背景
        // 簡易的な枠線
        graphics.fill(x, y, x + width, y + 1, borderColor);
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        graphics.fill(x, y, x + 1, y + height, borderColor);
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        // 2. 入力されたテキストの描画
        var font = Minecraft.getInstance().font;
        graphics.text(font, Component.literal(text + (isFocused && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : "")),
                x + 4, y + (height - 8) / 2, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        // ボックス内がクリックされたか判定
        isFocused = (mx >= x && mx <= x + width && my >= y && my <= y + height);
        return isFocused;
    }

    // キーボードで文字が入力されたときにScreen側から呼び出すメソッド
    public boolean charTyped(CharacterEvent event) {
        if (isFocused) {
            int codePoint = event.codepoint();
            // 入力可能な文字コードの簡易判定
            if (codePoint >= 32 && codePoint < 127) {
                // 💡 整数（コードポイント）を char にキャストして文字として結合する
                text += (char) codePoint;
                System.out.println(">>> Added char! Current text: " + text);
                return true;
            }
        }
        return false;
    }

    // バックスペースなどで文字を消すときの処理
    public boolean keyPressed(KeyEvent event) {
        if (isFocused) {
            if (event.key() == 259 /* Backspace */ && !text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
                return true;
            }
        }
        return false;
    }
}
