package com.hiroto99.windowslib.core.autodatagen.generatortypes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AutoTagSelector {
    private final TagType tagType;
    private final String[] tagKeyPath;

    public AutoTagSelector(TagType tagType, String... tagKeyPath) {
        this.tagType = tagType;
        this.tagKeyPath = tagKeyPath;
    }

    public List<TagKey<?>> getKeyTags() {
        List<TagKey<?>> tagKeyPaths;
        if (tagType == TagType.BLOCK) {
            tagKeyPaths = Arrays.stream(tagKeyPaths())
                    .map(x -> TagKey.create(Registries.BLOCK, Identifier.parse(x)))
                    .collect(Collectors.toList());
        } else if (tagType == TagType.ITEM) {
            tagKeyPaths = Arrays.stream(tagKeyPaths())
                    .map(x -> TagKey.create(Registries.ITEM, Identifier.parse(x)))
                    .collect(Collectors.toList());
        } else if (tagType == TagType.ENTITY_TYPE) {
            tagKeyPaths = Arrays.stream(tagKeyPaths())
                    .map(x -> TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(x)))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
        return tagKeyPaths;
    }

    String[] tagKeyPaths() {
        return tagKeyPath;
    }
}
