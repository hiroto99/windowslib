package com.hiroto99.windowslib.util.lookups;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DimensionLookup {
    public static ResourceKey<Level> getKey(String dimensionId) {
        // ResourceLocation.parse(id) でIDを解析 (間違ったフォーマットの場合は例外が飛ぶため Fail-Fast に最適)
        Identifier location = Identifier.parse(dimensionId);

        // DIMENSION レジストリを指定して ResourceKey を作成
        return ResourceKey.create(Registries.DIMENSION, location);
    }
}
