package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

public class UIProgressBar implements IUIComponent {
    private int x, y;
    private final Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final Supplier<Integer> progressSupplier; // 現在の進捗ピクセル数やスケールを返す
    private final BooleanSupplier isCrafting;        // 稼働中かどうか
    private final Direction direction;

    public enum Direction {
        HORIZONTAL_GROW, // 横に伸びる（矢印など）
        VERTICAL_SHRINK  // 下から上へ削り出す（液体や炎、クリスタルなど）
    }

    public UIProgressBar(Identifier texture, int textureWidth, int textureHeight,
                         Supplier<Integer> progressSupplier, BooleanSupplier isCrafting, Direction direction) {
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.progressSupplier = progressSupplier;
        this.isCrafting = isCrafting;
        this.direction = direction;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) {}

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!isCrafting.getAsBoolean()) return; // 稼働中でなければ描画しない

        int progress = progressSupplier.get();

        if (direction == Direction.HORIZONTAL_GROW) {
            // 横に伸びるパターン（矢印など）
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x, y, 0, 0,
                    progress, textureHeight, textureWidth, textureHeight);
        }
        else if (direction == Direction.VERTICAL_SHRINK) {
            // 下から上へ削り出すパターン（クリスタルなど）
            int renderHeight = progress;
            int renderY = y + (textureHeight - renderHeight);

            graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x, renderY,
                    0, textureHeight - renderHeight,
                    textureWidth, renderHeight,
                    textureWidth, textureHeight);
        }
    }
}