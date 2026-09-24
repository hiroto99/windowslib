package com.hiroto99.windowslib.core.autodatagen.annotation;

import com.hiroto99.windowslib.core.autodatagen.generatortypes.LootBlockType;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.LootType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Repeatable(AutoLootTableRules.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoLootTable {
    // ルートテーブルの名前
    String name() default "";
    // ルートテーブルの種類
    LootType type() default LootType.NONE;
    // ルートテーブルの種類がブロックの時のルートテーブルのテンプレート
    LootBlockType blockType() default LootBlockType.NO_DROPS;
    // プール情報
    @NotNull LootTablePool[] pool();
    // ルートテーブルのブロックテンプレートがOREの時、落とす原石
    String oreDrops() default "";
}
