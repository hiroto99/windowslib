package com.hiroto99.windowslib.datagen;

import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryLoot;
import com.hiroto99.windowslib.core.autodatagen.ConditionsBuilder;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoLootTable;
import com.hiroto99.windowslib.core.autodatagen.annotation.LootTablePool;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.LootType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.hiroto99.windowslib.datagen.AutoDataGenProvider.DATA_ENTRY_LOOT;

public class AutoEntityLootProvider extends EntityLootSubProvider {
    HolderLookup.Provider registries;

    // The constructor can be private if this class is an inner class of your loot table provider.
    // The parameter is provided by the lambda in the LootTableProvider's constructor.
    public AutoEntityLootProvider(HolderLookup.Provider lookupProvider) {
        // The first parameter is a set of blocks we are creating loot tables for. Instead of hardcoding,
        // we use our block registry and just pass an empty set here.
        // The second parameter is the feature flag set, this will be the default flags
        // unless you are adding custom flags (which is beyond the scope of this article).
        super(FeatureFlags.DEFAULT_FLAGS, lookupProvider);
        registries = lookupProvider;
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        List<EntityType<?>> entities = new ArrayList<>();

        // 💡 あなたのエンジンが持つ全エントリーをループ
        for (DataEntryLoot dataEntry : DATA_ENTRY_LOOT) {
            // もしアノテーションがついている対象が「Block」のインスタンスであれば
            if (dataEntry.value() instanceof EntityType<?> entityType) {
                // 追加するルートテーブルタイプがブロックではなかったらスキップ
                if (dataEntry.annotation().type() != LootType.ENTITY) {
                    continue;
                }
                // 他のModのブロックであっても、ここにすべて詰め込む
                entities.add(entityType);
            }
        }

        return entities.stream();
    }

    // Actually add our loot tables.
    @Override
    public void generate() {
        for (DataEntryLoot entryLoot : DATA_ENTRY_LOOT) {
            AutoLootTable entryLootAnnotation = entryLoot.annotation();
            if (entryLootAnnotation.type() != LootType.ENTITY) {
                continue;
            }
            if (!(entryLoot.value() instanceof EntityType<?> entityType)) {
                continue;
            }
            List<AutoDataGenEngine.LootTableData> lootTableData = new ArrayList<>();
            LootTable.Builder lootTable = LootTable.lootTable();
            for (LootTablePool lootPoolData : entryLootAnnotation.pool()) {
                LootPool.Builder lootPool = LootPool.lootPool();
                for (String dropItemDataEntry : lootPoolData.dropItemData()) {
                    lootTableData.add(AutoDataGenEngine.dropItemDataDecode(dropItemDataEntry));
                }
                for (AutoDataGenEngine.LootTableData lootTableDataEntry : lootTableData) {
                    LootPoolEntryContainer.Builder lootItem = LootItem.lootTableItem(lootTableDataEntry.item())
                            .setWeight(lootTableDataEntry.weight())
                            .apply(SetItemCountFunction.setCount(lootTableDataEntry.count()))
                            .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0.0F, lootTableDataEntry.fortuneMultiplier())));

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
            this.add(entityType, lootTable);
        }
    }
}
