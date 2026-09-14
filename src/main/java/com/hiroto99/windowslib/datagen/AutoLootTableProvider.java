package com.hiroto99.windowslib.datagen;

import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryLoot;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.LootTableData;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoLootTable;
import com.hiroto99.windowslib.core.autodatagen.annotation.LootTablePool;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.hiroto99.windowslib.WindowsLib.MODID;
import static com.hiroto99.windowslib.datagen.AutoDataGenProvider.DATA_ENTRY_LOOT;

public class AutoLootTableProvider implements LootTableSubProvider {
    // The parameter is provided by the lambda (see below). It can be stored and used to lookup other registry entries.
    public AutoLootTableProvider(HolderLookup.Provider lookupProvider) {
        // Store the lookupProvider in a field
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
        for (DataEntryLoot entryLoot : DATA_ENTRY_LOOT) {
            AutoLootTable entryLootAnnotation = entryLoot.annotation();
            List<LootTableData> lootTableData = new ArrayList<>();
            LootTable.Builder lootTable = LootTable.lootTable();
            for (LootTablePool lootPoolData : entryLootAnnotation.pool()) {
                LootPool.Builder lootPool = LootPool.lootPool();
                for (String dropItemDataEntry : lootPoolData.dropItemData()) {
                    lootTableData.add(AutoDataGenEngine.dropItemDataDecode(dropItemDataEntry));
                }
                for (LootTableData lootTableDataEntry : lootTableData) {
                    lootPool = lootPool.add(LootItem.lootTableItem(lootTableDataEntry.item())
                            .setWeight(lootTableDataEntry.weight())
                            .setQuality(lootTableDataEntry.quality())
                    );
                }
                lootPool = lootPool.name(entryLootAnnotation.name())
                        .setRolls(UniformGenerator.between(lootPoolData.rollsMin(), lootPoolData.rollsMax()))
                        .setBonusRolls(ConstantValue.exactly(lootPoolData.bonusRolls()));
                lootTable = lootTable.withPool(lootPool);
            }
            consumer.accept(ResourceKey.create(
                    Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath(MODID, entryLootAnnotation.name())),
                    lootTable
            );
        }
    }
}