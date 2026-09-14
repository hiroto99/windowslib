package com.hiroto99.windowslib.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.HolderGetter;
import java.util.Optional;

public class EnchantmentLookup {

    /**
     * 文字列のIDから ResourceKey<Enchantment> を生成する
     */
    public static ResourceKey<Enchantment> getKey(String enchantmentId) {
        Identifier location = Identifier.parse(enchantmentId);
        return ResourceKey.create(Registries.ENCHANTMENT, location);
    }

    /**
     * Datagen環境などで HolderGetter がある場合、Holder<Enchantment> を取得する
     */
    public static Holder<Enchantment> getHolder(HolderGetter<Enchantment> lookup, String enchantmentId) {
        ResourceKey<Enchantment> key = getKey(enchantmentId);
        return Holder.direct(lookup.getOrThrow(key).value());
    }
}
