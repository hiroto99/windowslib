package com.hiroto99.windowslib.datagen;

import com.hiroto99.windowslib.WindowsLib;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class ExampleAutoDataGenProvider {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // 【Mod本体側で自分のパッケージを明示的にスキャン】
        AutoDataGenEngine.scanPackage("com.example.mymod");

        var generator = event.getGenerator();
        var packOutput = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();

        // 共通API側で用意した自動プロバイダ、またはMod側のプロバイダを追加
        event.addProvider(new AutoItemTagProvider(packOutput, lookupProvider, WindowsLib.MODID));
        event.addProvider(new AutoBlockTagProvider(packOutput, lookupProvider, WindowsLib.MODID));
        event.addProvider(new AutoEntityTypeTagProvider(packOutput, lookupProvider, WindowsLib.MODID));
    }
}
