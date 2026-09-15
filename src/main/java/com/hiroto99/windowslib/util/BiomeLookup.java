package com.hiroto99.windowslib.util;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.HolderLookup;

public class BiomeLookup {

    /**
     * 文字列（例: "minecraft:plains" または "#minecraft:is_overworld"）から HolderSet<Biome> を取得
     */
    public static HolderSet<Biome> getHolderSet(HolderLookup.Provider provider, String biomeInput) {
        HolderGetter<Biome> biomeGetter = provider.lookupOrThrow(Registries.BIOME);

        // 先頭が '#' の場合はタグとして処理
        if (biomeInput.startsWith("#")) {
            String tagId = biomeInput.substring(1);
            TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, Identifier.parse(tagId));

            // タグに対応する HolderSet を取得
            return biomeGetter.getOrThrow(tagKey);
        } else {
            // 単一のバイオームIDとして処理
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, Identifier.parse(biomeInput));

            // 単一の Holder から要素数1の HolderSet (HolderSet.direct) を作成
            return HolderSet.direct(biomeGetter.getOrThrow(biomeKey));
        }
    }
}
