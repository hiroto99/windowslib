package com.hiroto99.windowslib.core.simplegui.component;

import com.hiroto99.windowslib.core.simplegui.IUIComponent;
import com.hiroto99.windowslib.util.StringTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.CommonColors;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Supplier;

public class UITankBar implements IUIComponent {
    private int x, y, width, height;
    private final float capacity;
    private final Supplier<FluidStack> fluidSupplier; // 液体処理の関数
    private Supplier<StringTranslator> onHover; // ツールチップ用の処理

    public UITankBar(float capacity, Supplier<FluidStack> fluidSupplier) {
        this.capacity = capacity;
        this.fluidSupplier = fluidSupplier;
    }

    @Override public void setPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public void setSize(int width, int height) { this.width = width; this.height = height; }
    public void setLabel(Supplier<StringTranslator> onHover) { this.onHover = onHover; }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1. タンクの背景（空の部分の枠や色）
        graphics.fill(x, y, x + width, y + height, 0xFF333333);

        // 2. 液体部分（下から上に向かって溜まるように計算）
        float ratio = Math.clamp(getFillRatio(), 0.0f, 1.0f);
        int filledHeight = (int) (height * ratio);
        int renderY = y + height - filledHeight; // 下から描画するためYを調整

        // 例: 水色（液体っぽく）で描画
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getFluidTexture(fluidSupplier.get()), x, renderY, width, filledHeight, getColorTint(fluidSupplier.get()));
    }

    public float getFillRatio() {
        return fluidSupplier.get().amount() / capacity;
    }

    public FluidStack getFluid() {
        return fluidSupplier.get();
    }

    public float getCapacity() {
        return capacity;
    }

    @Override
    public boolean tooltipRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (onHover == null) {
            return false;
        }
        var font = Minecraft.getInstance().font;
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            guiGraphics.setComponentTooltipForNextFrame(font, onHover.get().toLiteralList(), mouseX, mouseY);
            return true;
        }
        return false;
    }

    private TextureAtlasSprite getFluidTexture(FluidStack fluidStack) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidStack.getFluid().defaultFluidState()).stillMaterial().sprite();
    }

    private int getColorTint(FluidStack fluidStack) {
        FluidTintSource tintSource = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidStack.getFluid().defaultFluidState()).fluidTintSource();
        if(tintSource == null) {
            return CommonColors.WHITE;
        }

        return tintSource.colorAsStack(fluidStack);
    }
}