package com.hiroto99.windowslib.core.autodatagen.generatortypes;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface TagType<T> {
    ResourceKey<Registry<T>> getRegistry();

    default List<TagKey<T>> getTagKeys(String[] tagKeyPath) {
        return Arrays.stream(tagKeyPath)
                .map(x -> TagKey.create(getRegistry(), Identifier.parse(x)))
                .collect(Collectors.toList());
    }
}
