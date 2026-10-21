package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import com.hiroto99.windowslib.core.simplegui.TabItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

public class UITabBar implements IUIComponent {
    private int x, y;
    private final int tabWidth = 28;  // タブ1つの幅
    private final int tabHeight = 24; // タブ1つの高さ
    private final List<TabItem> tabs = new ArrayList<>();
    private int selectedIndex = 0;

    public void addTab(TabItem tab) {
        tabs.add(tab);
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) {}

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;

        for (int i = 0; i < tabs.size(); i++) {
            int tabX = x + (i * (tabWidth + 2)); // 少し隙間を空けて横並び
            int tabY = y;
            boolean isSelected = (i == selectedIndex);
            boolean isHovered = mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= tabY && mouseY <= tabY + tabHeight;

            // 選択中かホバー状態かで色や高さを変える
            int bgColor = isSelected ? 0xFF888888 : (isHovered ? 0xFF666666 : 0xFF444444);
            graphics.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, bgColor);

            // タブのタイトルやアイコンを描画
            TabItem tab = tabs.get(i);
            graphics.text(font, tab.getTitle(), tabX + 4, tabY + 8, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        for (int i = 0; i < tabs.size(); i++) {
            int tabX = x + (i * (tabWidth + 2));
            int tabY = y;

            if (mx >= tabX && mx <= tabX + tabWidth && my >= tabY && my <= tabY + tabHeight) {
                selectedIndex = i;
                tabs.get(i).select(); // 選択されたタブの処理を実行
                return true;
            }
        }
        return false;
    }
}