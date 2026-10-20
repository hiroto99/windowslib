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
    private final int ALL_INVENTORY_SLOT_INDEX;

    private final List<Integer> RESULT_INVENTORY_SLOT_LIST = new java.util.ArrayList<>();

    public BaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()), new ItemStacksResourceHandler(1));
    }

    public BaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity blockEntity, ItemStacksResourceHandler handler) {
        super(menuType, containerId);
        this.blockEntity = (T) blockEntity;
        this.level = inventory.player.level();
        this.block = level.getBlockState(blockEntity.getBlockPos()).getBlock();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        addContainers(handler);
        ALL_INVENTORY_SLOT_INDEX = VANILLA_SLOT_COUNT + BLOCK_INVENTORY_SLOT_COUNT;
    }

    public abstract void addContainers(ItemStacksResourceHandler handler);

    @Override
    protected Slot addSlot(Slot slot) {
        ++BLOCK_INVENTORY_SLOT_COUNT;
        return super.addSlot(slot);
    }

    public Slot addNormalSlot(ItemStacksResourceHandler handler, int xPosition, int yPosition) {
        return this.addSlot(new ResourceHandlerSlot(handler, handler::set, VANILLA_SLOT_COUNT + BLOCK_INVENTORY_SLOT_COUNT, xPosition, yPosition));
    }

    public Slot addResultSlot(ItemStacksResourceHandler handler, int xPosition, int yPosition) {
        RESULT_INVENTORY_SLOT_LIST.add(VANILLA_SLOT_COUNT + BLOCK_INVENTORY_SLOT_COUNT);
        return this.addSlot(new ResourceHandlerSlot(handler, handler::set, VANILLA_SLOT_COUNT + BLOCK_INVENTORY_SLOT_COUNT, xPosition, yPosition) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
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
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, block);
    }
}
