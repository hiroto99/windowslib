package com.hiroto99.windowslib.core.autodatagen.annotation;

import com.hiroto99.windowslib.core.autodatagen.generatortypes.TagType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoTag {
    Class<? extends TagType<?>> tagtype(); // タグ生成カテゴリー
    String[] tagKeyPath() default {}; // 生成してもらうタグ
}
