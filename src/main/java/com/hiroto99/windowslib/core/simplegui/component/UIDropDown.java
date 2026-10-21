package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Consumer;

public class UIDropDown implements IUIComponent {
    private int x, y, width, height;
    private final List<String> items;
    private int selectedIndex = 0;
    private boolean isOpen = false;
    private final Consumer<String> onSelected;

    public UIDropDown(List<String> items, Consumer<String> onSelected) {
        this.items = items;
        this.onSelected = onSelected;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;

        // 1. メインのボタン部分を描画
        graphics.fill(x, y, x + width, y + height, 0xFF444444);
        String currentText = items.isEmpty() ? "Select..." : items.get(selectedIndex);
        graphics.text(font, Component.literal(currentText), x + 4, y + (height - 8) / 2, 0xFFFFFF, false);
        // 矢印マークの代わりの文字
        graphics.text(font, Component.literal(isOpen ? "▲" : "▼"), x + width - 12, y + (height - 8) / 2, 0xAAAAAA, false);

        // 2. 開いている場合は選択肢のリストを重ねて描画（一番手前に来るようにする）
        if (isOpen) {
            int itemHeight = 14;
            for (int i = 0; i < items.size(); i++) {
                int itemY = y + height + (i * itemHeight);
                boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY <= itemY + itemHeight;

                // リスト項目の背景（ホバー時は明るく）
                graphics.fill(x, itemY, x + width, itemY + itemHeight, isHovered ? 0xFF666666 : 0xFF333333);
                graphics.text(font, Component.literal(items.get(i)), x + 4, itemY + 3, 0xFFFFFF, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        // リストが開いているときのクリック判定
        if (isOpen) {
            int itemHeight = 14;
            for (int i = 0; i < items.size(); i++) {
                int itemY = y + height + (i * itemHeight);
                if (mx >= x && mx <= x + width && my >= itemY && my <= itemY + itemHeight) {
                    selectedIndex = i;
                    isOpen = false; // 選択したら閉じる
                    onSelected.accept(items.get(i));
                    return true;
                }
            }
            // リストの外側をクリックしたら閉じる
            isOpen = false;
        }

        // メイン部分をクリックした場合の開閉切り替え
        if (mx >= x && mx <= x + width && my >= y && my <= y + height) {
            isOpen = !isOpen;
            return true;
        }

        return false;
    }
}
