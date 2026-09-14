package com.hiroto99.windowslib.datagen;

import com.hiroto99.windowslib.WindowsLib;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryTag;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryLoot;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.COLLECTED_DATA_LOOT;
import static com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.COLLECTED_DATA_TAG;

public class AutoDataGenProvider {
    static List<String> ModIDs = new ArrayList<>();
    public static List<DataEntryTag> DATA_ENTRY_TAG = new ArrayList<>();
    public static List<DataEntryLoot> DATA_ENTRY_LOOT = new ArrayList<>();

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        /*【Mod本体側で自分のパッケージを明示的にスキャン】
           ModIDs.stream().forEach(ModID -> {
               AutoDataGenEngine.scanPackage(ModID).forEach(dataEntryTag -> {
                   DATA_ENTRY_TAG.add(dataEntryTag);
               });
           }); */
        ModIDs.forEach(ModID -> {
            AutoDataGenEngine.scanPackage(ModID);
            DATA_ENTRY_TAG.addAll(COLLECTED_DATA_TAG);
            DATA_ENTRY_LOOT.addAll(COLLECTED_DATA_LOOT);
        });

        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // 共通API側で用意した自動プロバイダ、またはMod側のプロバイダを追加
        generator.addProvider(true, new AutoItemTagProvider(packOutput, lookupProvider, WindowsLib.MODID));
        generator.addProvider(true, new AutoBlockTagProvider(packOutput, lookupProvider, WindowsLib.MODID));
        generator.addProvider(true, new AutoEntityTypeTagProvider(packOutput, lookupProvider, WindowsLib.MODID));
    }

    public static void register(String modPackagePath) {
        ModIDs.add(modPackagePath);
    }
}
