package com.hiroto99.windowslib.core.simplegui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TabItem {
    private final Component title;
    private final Identifier iconTexture; // タブに表示するアイコンなど（オプション）
    private final Runnable onSelected;    // タブが切り替わったときの処理

    public TabItem(Component title, Identifier iconTexture, Runnable onSelected) {
        this.title = title;
        this.iconTexture = iconTexture;
        this.onSelected = onSelected;
    }

    public Component getTitle() { return title; }
    public Identifier getIconTexture() { return iconTexture; }
    public void select() { onSelected.run(); }
}