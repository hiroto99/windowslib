package com.hiroto99.windowslib.datagen;

import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryLoot;
import com.hiroto99.windowslib.core.autodatagen.ConditionsBuilder;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoLootTable;
import com.hiroto99.windowslib.core.autodatagen.annotation.LootTablePool;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.LootBlockType;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.LootType;
import com.hiroto99.windowslib.util.lookups.ItemLookup;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.hiroto99.windowslib.datagen.AutoDataGenProvider.DATA_ENTRY_LOOT;

public class AutoBlockLootProvider extends BlockLootSubProvider {
    HolderLookup.Provider registries;

    // The constructor can be private if this class is an inner class of your loot table provider.
    // The parameter is provided by the lambda in the LootTableProvider's constructor.
    public AutoBlockLootProvider(HolderLookup.Provider lookupProvider) {
        // The first parameter is a set of blocks we are creating loot tables for. Instead of hardcoding,
        // we use our block registry and just pass an empty set here.
        // The second parameter is the feature flag set, this will be the default flags
        // unless you are adding custom flags (which is beyond the scope of this article).
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
        registries = lookupProvider;
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> blocks = new ArrayList<>();

        // 💡 あなたのエンジンが持つ全エントリーをループ
        for (DataEntryLoot dataEntry : DATA_ENTRY_LOOT) {
            // もしアノテーションがついている対象が「Block」のインスタンスであれば
            if (dataEntry.value() instanceof Block block) {
                // 追加するルートテーブルタイプがブロックではなかったらスキップ
                if (dataEntry.annotation().type() != LootType.BLOCK) {
                    continue;
                }
                // 他のModのブロックであっても、ここにすべて詰め込む
                blocks.add(block);
            }
        }

        return blocks;
    }

    // Actually add our loot tables.
    @Override
    protected void generate() {
        for (DataEntryLoot entryLoot : DATA_ENTRY_LOOT) {
            AutoLootTable entryLootAnnotation = entryLoot.annotation();
            if (entryLootAnnotation.type() != LootType.BLOCK) {
                continue;
            }
            if (entryLoot.value() instanceof Block block) {
                if (entryLootAnnotation.blockType() == LootBlockType.DROP_SELF) {
                    dropSelf(block);
                }
                if (entryLootAnnotation.blockType() == LootBlockType.DROP_OTHER) {
                    add(block, getLootTable(entryLootAnnotation));
                }
                if (entryLootAnnotation.blockType() == LootBlockType.SILK_TOUCH_ONLY) {
                    add(block, createSilkTouchOnlyTable(block));
                }
                if (entryLootAnnotation.blockType() == LootBlockType.SILK_TOUCH_DISPATCH) {
                    add(block, createSilkTouchDispatchTable(block, getLootItem(entryLootAnnotation)));
                }
                if (entryLootAnnotation.blockType() == LootBlockType.SILK_TOUCH_OR_SHEARS_ONLY) {
                    add(block, createShearsOrSilkTouchOnlyDrop(block));
                }
                if (entryLootAnnotation.blockType() == LootBlockType.SILK_TOUCH_OR_SHEARS_DISPATCH) {
                    add(block, createSilkTouchOrShearsDispatchTable(block, getLootItem(entryLootAnnotation)));
                }
                if (entryLootAnnotation.blockType() == LootBlockType.SHEARS_ONLY) {
                    add(block, createShearsOnlyDrop(block));
                }
                if (entryLootAnnotation.blockType() == LootBlockType.SHEARS_DISPATCH) {
                    add(block, createShearsDispatchTable(block, getLootItem(entryLootAnnotation)));
                }
                if (entryLootAnnotation.blockType() == LootBlockType.ORE) {
                    add(block, createOreDrop(block, ItemLookup.getItem(registries, entryLootAnnotation.oreDrops()).orElse(Items.AIR)));
                }
                if (entryLootAnnotation.blockType() == LootBlockType.NO_DROPS) {
                    add(block, noDrop());
                }
            }
        }
    }

    private LootTable.Builder getLootTable(AutoLootTable entryLootAnnotation) {
        List<AutoDataGenEngine.LootTableData> lootTableData = new ArrayList<>();
        LootTable.Builder lootTable = LootTable.lootTable();
        for (LootTablePool lootPoolData : entryLootAnnotation.pool()) {
            LootPool.Builder lootPool = LootPool.lootPool();
            for (String dropItemDataEntry : lootPoolData.dropItemData()) {
                lootTableData.add(AutoDataGenEngine.dropItemDataDecode(dropItemDataEntry));
            }
            for (AutoDataGenEngine.LootTableData lootTableDataEntry : lootTableData) {
                LootPoolEntryContainer.Builder<?> lootItem = LootItem.lootTableItem(lootTableDataEntry.item())
                        .setWeight(lootTableDataEntry.weight())
                        .setQuality(lootTableDataEntry.quality());

                for (LootItemCondition.Builder conditionsBuilder : new ConditionsBuilder().get(lootTableDataEntry.conditions(), registries)) {
                    lootItem = lootItem.when(conditionsBuilder);
                }
                lootPool = lootPool.add(lootItem);
            }
            lootPool = lootPool.name(entryLootAnnotation.name())
                    .setRolls(UniformGenerator.between(lootPoolData.rollsMin(), lootPoolData.rollsMax()))
                    .setBonusRolls(ConstantValue.exactly(lootPoolData.bonusRolls()));
            lootTable = lootTable.withPool(lootPool);
        }
        return lootTable;
    }

    private LootPoolEntryContainer.Builder<?> getLootItem(AutoLootTable entryLootAnnotation) {
        AutoDataGenEngine.LootTableData lootTableData = null;
        LootTablePool lootTablePool = entryLootAnnotation.pool()[0];
        for (String dropItemDataEntry : lootTablePool.dropItemData()) {
            lootTableData = AutoDataGenEngine.dropItemDataDecode(dropItemDataEntry);
        }
        if (lootTableData == null) {
            throw new IllegalArgumentException("lootTablePool.pool is null");
        }
        LootPoolEntryContainer.Builder<?> lootItem = LootItem.lootTableItem(lootTableData.item())
                .setWeight(lootTableData.weight())
                .setQuality(lootTableData.quality());

        for (LootItemCondition.Builder conditionsBuilder : new ConditionsBuilder().get(lootTableData.conditions(), registries)) {
            lootItem = lootItem.when(conditionsBuilder);
        }
        return lootItem;
    }
}
