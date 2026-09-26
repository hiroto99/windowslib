package com.hiroto99.windowslib.core.autodatagen.annotation;

public @interface LootTablePool {
    // ルートテーブルの名前
    String name() default "";
    // ロール回数
    float rollsMin() default 1;
    float rollsMax() default 1;
    // ボーナスロール回数
    float bonusRolls() default 0;
    // プールワイドの条件
    String condition() default "";
    // 自身を落とさないときに落とすアイテム["アイテム名/weight/min/max/幸運エンチャントの受ける倍率(幸運エンチャントの影響を受けないなら0)/条件(省略可能)"] (例:["minecraft:dirt/3/1/1/0"])
    String[] dropItemData() default "";
}
