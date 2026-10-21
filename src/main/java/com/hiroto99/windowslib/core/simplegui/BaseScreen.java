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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final Identifier backgroundTexture;

    // 1. 現在アクティブ（画面に表示中）なコンポーネントのリスト
    protected final List<IUIComponent> activeComponents = new ArrayList<>();

    // 2. タブごとにコンポーネントを管理するマップ
    private final Map<Integer, List<IUIComponent>> tabComponentsMap = new HashMap<>();
    private int currentTabId = 0;

    public BaseScreen(T menu, Inventory playerInventory, Component title, Identifier backgroundTexture) {
        super(menu, playerInventory, title);
        this.backgroundTexture = backgroundTexture;
    }

    // 常に全タブで共通表示したいパーツ用（タブバーなど）
    protected void addComponent(IUIComponent component) {
        this.activeComponents.add(component);
    }

    // 特定のタブIDにパーツを追加する用
    protected void addComponentToTab(int tabId, IUIComponent component) {
        tabComponentsMap.computeIfAbsent(tabId, k -> new ArrayList<>()).add(component);
    }

    // タブを切り替えるメソッド
    public void switchTab(int tabId) {
        this.currentTabId = tabId;

        // 一旦タブ専用のリストをクリアして再構築する準備
        // （※共通パーツとタブパーツをキレイに分離するため、必要に応じて調整してください）
        this.activeComponents.clear(); // 現在のパーツをクリア

        // 切り替え先のタブに登録されているパーツをアクティブリストに戻す
        if (tabComponentsMap.containsKey(tabId)) {
            this.activeComponents.addAll(tabComponentsMap.get(tabId));
        }
    }

    @Override
    protected void init() {
        super.init();
        // ウィンドウリサイズ時などに全クリア
        this.activeComponents.clear();
        this.tabComponentsMap.clear();

        // 子クラス側でタブごとのパーツを再登録させるためのフック
        initTabsAndComponents();

        // 現在選ばれているタブのパーツをアクティブリストに追加
        if (tabComponentsMap.containsKey(currentTabId)) {
            this.activeComponents.addAll(tabComponentsMap.get(currentTabId));
        }
    }

    // 子クラスでタブやパーツの初期化を行うためのメソッド
    protected void initTabsAndComponents() {
        // 子クラス側で override して使います
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        if (backgroundTexture != null) {
            // 💡 演算子抜けを修正済み
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, x, y, 0, 0,
                    imageWidth, imageHeight, 256, 256);
        }

        // アクティブなパーツを描画
        for (IUIComponent component : activeComponents) {
            component.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (IUIComponent component : activeComponents) {
            if (component.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        for (IUIComponent component : activeComponents) {
            if (component instanceof UITextBox textBox && textBox.charTyped(event)) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for (IUIComponent component : activeComponents) { // activeComponents に統一するとより安全です
            if (component instanceof UITextBox textBox && textBox.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }
}