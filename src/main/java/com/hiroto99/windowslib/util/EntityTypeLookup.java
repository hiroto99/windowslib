package com.hiroto99.windowslib.util;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class EntityTypeLookup {

    /**
     * 文字列（例: "minecraft:zombie" または "#minecraft:skeletons"）から HolderSet<EntityType<?>> を取得
     */
    public static HolderSet<EntityType<?>> getHolderSet(HolderLookup.Provider provider, String input) {
        HolderGetter<EntityType<?>> entityGetter = provider.lookupOrThrow(Registries.ENTITY_TYPE);

        // 先頭が '#' の場合はタグとして処理
        if (input.startsWith("#")) {
            String tagId = input.substring(1);
            TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(tagId));

            // タグに対応する HolderSet を取得
            return entityGetter.getOrThrow(tagKey);
        } else {
            // 単一のエンティティタイプIDとして処理
            ResourceKey<EntityType<?>> entityKey = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse(input));

            // 単一の Holder から要素数1の HolderSet (HolderSet.direct) を作成
            return HolderSet.direct(entityGetter.getOrThrow(entityKey));
        }
    }
}
