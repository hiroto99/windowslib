package com.hiroto99.windowslib.util.lookups;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Optional;

public class ItemLookup {

    /**
     * 文字列（例: "minecraft:grass_block" または "#minecraft:planks"）から HolderSet<Biome> を取得
     */
    public static TagKey<Item> getTagkey(String biomeInput) {
        // 先頭が '#' の場合はタグとして処理
        if (biomeInput.startsWith("#")) {
            String tagId = biomeInput.substring(1);

            // タグに対応する HolderSet を取得
            return TagKey.create(Registries.ITEM, Identifier.parse(tagId));
        } else {
            // 単一のバイオームIDとして処理
            ResourceKey<Registry<Item>> biomeKey = ResourceKey.createRegistryKey(Identifier.parse(biomeInput));

            // 単一の Holder から要素数1の HolderSet (HolderSet.direct) を作成
            return TagKey.create(biomeKey, Identifier.parse(biomeInput));
        }
    }

    public static Optional<Item> getItem(HolderLookup.Provider provider, String biomeInput) {
        HolderGetter<Item> itemGetter = provider.lookupOrThrow(Registries.ITEM);

        if (biomeInput.startsWith("#")) {
            return Optional.empty();
        } else {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.parse(biomeInput));

            // HolderGetter から安全に Holder<Item> を取得して Item を返す
            return itemGetter.get(key).map(Holder::value);
        }
    }

    public static HolderGetter<Item> itemHolderGetter(HolderLookup.Provider provider) {
        return provider.lookupOrThrow(Registries.ITEM);
    }
}
