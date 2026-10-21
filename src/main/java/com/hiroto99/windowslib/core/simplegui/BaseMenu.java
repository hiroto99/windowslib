package com.hiroto99.windowslib.core.simplegui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public abstract class BaseMenu<T extends BlockEntity> extends AbstractContainerMenu {
    public final T blockEntity;
    private final Level level;
    private final Block block;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;

    private int BLOCK_INVENTORY_SLOT_COUNT = 0;
    private int containerSlotIndex = 0; // ハンドラー内のインデックス追跡用
    private final int ALL_INVENTORY_SLOT_INDEX;

    private final List<Integer> RESULT_INVENTORY_SLOT_LIST = new java.util.ArrayList<>();

    // --- コンストラクタのバリエーション ---

    // ① コマンド用（スロット数指定可能・デフォルト1）
    public BaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory) {
        this(menuType, containerId, inventory, 1);
    }

    public BaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, int containerSlotsSize) {
        this(menuType, containerId, inventory, (BlockEntity) null, containerSlotsSize, true);
    }

    public BaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, int containerSlotsSize, boolean hasPlayerInventory) {
        this(menuType, containerId, inventory, (BlockEntity) null, containerSlotsSize, hasPlayerInventory);
    }

    // ② FriendlyByteBuf用
    public BaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData, int containerSlotsSize) {
        this(
                menuType,
                containerId,
                inventory,
                (extraData != null && extraData.isReadable()) ? inventory.player.level().getBlockEntity(extraData.readBlockPos()) : null,
                containerSlotsSize,
                true
        );
    }

    public BaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData, int containerSlotsSize, boolean hasPlayerInventory) {
        this(
                menuType,
                containerId,
                inventory,
                (extraData != null && extraData.isReadable()) ? inventory.player.level().getBlockEntity(extraData.readBlockPos()) : null,
                containerSlotsSize,
                hasPlayerInventory
        );
    }

    // ③ メインのコンストラクタ（サイズ指定でItemStacksResourceHandlerを自動生成）
    public BaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, @Nullable BlockEntity blockEntity, int containerSlotsSize, boolean hasPlayerInventory) {
        super(menuType, containerId);
        this.blockEntity = (T) blockEntity;
        this.level = inventory.player.level();
        ItemStacksResourceHandler handler = new ItemStacksResourceHandler(containerSlotsSize);

        if (blockEntity != null) {
            this.block = level.getBlockState(blockEntity.getBlockPos()).getBlock();
        } else {
            this.block = null;
        }

        // プレイヤーインベントリが必要な場合のみ追加する
        if (hasPlayerInventory) {
            addPlayerInventory(inventory);
            addPlayerHotbar(inventory);
        }

        // 機械側（設定用など）のスロットがある場合のみ追加
        if (containerSlotsSize > 0) {
            addContainers(handler);
        }

        ALL_INVENTORY_SLOT_INDEX = VANILLA_SLOT_COUNT + BLOCK_INVENTORY_SLOT_COUNT;
    }

    public abstract void addContainers(ItemStacksResourceHandler handler);

    @Override
    protected Slot addSlot(Slot slot) {
        ++BLOCK_INVENTORY_SLOT_COUNT;
        return super.addSlot(slot);
    }

    public Slot addNormalSlot(ItemStacksResourceHandler handler, int xPosition, int yPosition) {
        Slot slot = new ResourceHandlerSlot(handler, handler::set, containerSlotIndex, xPosition, yPosition);
        containerSlotIndex++;
        return this.addSlot(slot);
    }

    public Slot addResultSlot(ItemStacksResourceHandler handler, int xPosition, int yPosition) {
        int menuIndex = VANILLA_SLOT_COUNT + BLOCK_INVENTORY_SLOT_COUNT;
        RESULT_INVENTORY_SLOT_LIST.add(menuIndex);

        Slot slot = new ResourceHandlerSlot(handler, handler::set, containerSlotIndex, xPosition, yPosition) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 出力スロットには手動で置けない
            }
        };
        containerSlotIndex++;
        return this.addSlot(slot);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int i = 0; i < PLAYER_INVENTORY_ROW_COUNT; ++i) {
            for (int l = 0; l < PLAYER_INVENTORY_COLUMN_COUNT; ++l) {
                super.addSlot(new Slot(inventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < PLAYER_INVENTORY_COLUMN_COUNT; ++i) {
            super.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        Slot quickMovedSlot = this.slots.get(quickMovedSlotIndex);

        if (quickMovedSlot != null && quickMovedSlot.hasItem()) {
            ItemStack rawStack = quickMovedSlot.getItem();
            quickMovedStack = rawStack.copy();

            if (RESULT_INVENTORY_SLOT_LIST.contains(quickMovedSlotIndex)) {
                if (!this.moveItemStackTo(rawStack, 0, VANILLA_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
                quickMovedSlot.onQuickCraft(rawStack, quickMovedStack);
            }
            else if (quickMovedSlotIndex < VANILLA_SLOT_COUNT) {
                if (!this.moveItemStackTo(rawStack, VANILLA_SLOT_COUNT, ALL_INVENTORY_SLOT_INDEX, false)) {
                    if (quickMovedSlotIndex < PLAYER_INVENTORY_SLOT_COUNT) {
                        if (!this.moveItemStackTo(rawStack, PLAYER_INVENTORY_SLOT_COUNT, VANILLA_SLOT_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                    else if (!this.moveItemStackTo(rawStack, 0, PLAYER_INVENTORY_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            else if (!this.moveItemStackTo(rawStack, 0, PLAYER_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (rawStack.isEmpty()) {
                quickMovedSlot.setByPlayer(ItemStack.EMPTY);
            } else {
                quickMovedSlot.setChanged();
            }
            quickMovedSlot.onTake(player, rawStack);
        }

        return quickMovedStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return true;
        }
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, block);
    }
}