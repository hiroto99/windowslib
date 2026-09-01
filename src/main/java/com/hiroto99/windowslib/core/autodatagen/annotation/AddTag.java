package com.hiroto99.windowslib.core.autodatagen.annotation;

import com.hiroto99.windowslib.core.autodatagen.generatortypes.TagType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AddTag {
    TagType tagtype() default TagType.NONE; // タグ生成カテゴリー
    String[] tagKeyPath() default {}; // 生成してもらうタグ
}
