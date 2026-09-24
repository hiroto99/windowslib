package com.hiroto99.windowslib.util.lookups;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;

public class MobEffectLookup {

    /**
     * 文字列（例: "minecraft:poison"）から HolderSet<MobEffect> を取得
     */
    public static Holder<MobEffect> getHolderSet(HolderLookup.Provider provider, String input) {
        HolderGetter<MobEffect> effectGetter = provider.lookupOrThrow(Registries.MOB_EFFECT);

        // 単一のエンティティタイプIDとして処理
        ResourceKey<MobEffect> effectKey = ResourceKey.create(Registries.MOB_EFFECT, Identifier.parse(input));
        // 単一の Holder から要素数1の HolderSet (HolderSet.direct) を作成
        return effectGetter.getOrThrow(effectKey);
    }
}
