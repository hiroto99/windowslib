package com.hiroto99.windowslib.core.autodatagen.annotation;

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
    LootType type() default LootType.SELF;
    // プール情報
    @NotNull LootTablePool[] pool();
}
