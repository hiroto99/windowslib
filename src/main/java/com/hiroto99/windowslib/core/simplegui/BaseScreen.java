package com.hiroto99.windowslib.core.simplegui;

import com.hiroto99.windowslib.core.simplegui.component.UITextBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final Identifier backgroundTexture;
    protected final List<IUIComponent> components = new ArrayList<>();

    public BaseScreen(T menu, Inventory playerInventory, Component title, Identifier backgroundTexture) {
        super(menu, playerInventory, title);
        this.backgroundTexture = backgroundTexture;
    }

    protected void addComponent(IUIComponent component) {
        this.components.add(component);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        if (backgroundTexture != null) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, x, y, 0, 0,
                    imageWidth, imageHeight, 256, 256);
        }

        // もし render() が無い環境で、背景抽出のタイミングで一緒にパーツを描画したい場合はここで回すこともできます
        for (IUIComponent component : components) {
            component.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 各カスタムパーツにクリックイベントを伝播
        for (IUIComponent component : components) {
            if (component.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // BaseScreen 内に追加するイベント中継メソッドの例
    @Override
    public boolean charTyped(CharacterEvent event) {
        for (IUIComponent component : components) {
            if (component instanceof UITextBox textBox && textBox.charTyped(event)) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for (IUIComponent component : components) {
            if (component instanceof UITextBox textBox && textBox.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }
}