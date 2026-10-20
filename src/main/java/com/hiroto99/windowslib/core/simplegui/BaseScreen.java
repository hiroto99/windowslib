package com.hiroto99.windowslib.core.simplegui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class BaseScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    // 画面ごとに異なる背景テクスチャを保持
    private final Identifier backgroundTexture;

    public BaseScreen(T menu, Inventory playerInventory, Component title, Identifier backgroundTexture) {
        super(menu, playerInventory, title);
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        // テクスチャが設定されている場合のみ自動で背景を描画
        if (backgroundTexture != null) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, x, y, 0, 0,
                    imageWidth, imageHeight, 256, 256);
        }
    }
}
