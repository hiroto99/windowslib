package com.hiroto99.windowslib.instance.tagtype;

import com.hiroto99.windowslib.core.autodatagen.generatortypes.TagType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

public enum TagTypeEntityType implements TagType<EntityType<?>> {
    // 💡 シングルトン（要素1つ）として定義。これが constants[0] で取得されます
    INSTANCE;

    @Override
    public ResourceKey<Registry<EntityType<?>>> getRegistry() {
        // Minecraftに「このenumはEntityTypeを扱うよ」と教える
        return Registries.ENTITY_TYPE;
    }
}
