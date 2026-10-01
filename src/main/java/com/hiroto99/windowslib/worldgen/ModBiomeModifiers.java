package com.hiroto99.windowslib.worldgen;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import static com.hiroto99.windowslib.WindowsLib.MODID;

public class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MODID);

    // カスタムBiomeModifierのCodecを登録
    public static final java.util.function.Supplier<MapCodec<ModifySpawnsBiomeModifier>> EXAMPLE_CUSTOM =
            BIOME_MODIFIER_SERIALIZERS.register("modify_spawns", () -> ModifySpawnsBiomeModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}
