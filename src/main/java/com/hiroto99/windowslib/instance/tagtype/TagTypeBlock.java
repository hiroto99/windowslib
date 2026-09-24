package com.hiroto99.windowslib.instance.tagtype;

import com.hiroto99.windowslib.core.autodatagen.generatortypes.TagType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public enum TagTypeBlock implements TagType<Block> {
    // 💡 シングルトン（要素1つ）として定義。これが constants[0] で取得されます
    INSTANCE;

    @Override
    public ResourceKey<Registry<Block>> getRegistry() {
        // Minecraftに「このenumはBlockを扱うよ」と教える
        return Registries.BLOCK;
    }
}
