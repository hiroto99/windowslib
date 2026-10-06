package com.hiroto99.windowslib.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryCustomTag;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoTagforCustomTag;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.TagType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static com.hiroto99.windowslib.datagen.AutoDataGenProvider.DATA_ENTRY_CUSTOM_TAG;

public class UniversalAutoTagForCustomTagProvider implements DataProvider {
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;

    public UniversalAutoTagForCustomTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider
    ) {
        this.output = output;
        this.lookupProvider = lookupProvider;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return lookupProvider.thenCompose(_ -> {
            Map<ResourceKey<? extends Registry<?>>,
                    Map<TagKey<?>, TagBuilder>> builders = new HashMap<>();

            for (DataEntryCustomTag dataEntry : DATA_ENTRY_CUSTOM_TAG) {
                AutoTagforCustomTag ann = dataEntry.annotation();
                Class<? extends TagType<?>> tagTypeClass = ann.tagtype();
                TagType<?>[] constants = tagTypeClass.getEnumConstants();

                if (constants == null || constants.length == 0) {
                    continue;
                }

                processCustomTag(
                        builders,
                        dataEntry
                );
            }

            List<CompletableFuture<?>> futures = new ArrayList<>();

            builders.forEach((registryKey, tagsMap) -> {
                tagsMap.forEach((tagKey, builder) -> {
                    PackOutput.PathProvider pathProvider =
                            output.createPathProvider(
                                    PackOutput.Target.DATA_PACK,
                                    "tags/" + registryKey.identifier().getPath()
                            );

                    Path path = pathProvider.json(tagKey.location());
                    JsonObject json = new JsonObject();
                    JsonArray values = new JsonArray();

                    builder.build().forEach(entry ->
                            values.add(entry.toString())
                    );

                    json.addProperty("replace", false);
                    json.add("values", values);
                    
                    futures.add(
                            DataProvider.saveStable(
                                    cache,
                                    json,
                                    path
                            )
                    );
                });
            });

            return CompletableFuture.allOf(
                    futures.toArray(CompletableFuture[]::new)
            );
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void processCustomTag(
            Map<ResourceKey<? extends Registry<?>>,
                    Map<TagKey<?>, TagBuilder>> builders,
            DataEntryCustomTag dataEntry
    ) {
        AutoTagforCustomTag ann = dataEntry.annotation();

        TagType<T> tagType = (TagType<T>) ann.tagtype().getEnumConstants()[0];
        ResourceKey<Registry<T>> registryKey = tagType.getRegistry();
        TagKey<T> targetTag = (TagKey<T>) dataEntry.value();

        Map<TagKey<?>, TagBuilder> tagsMap = builders.computeIfAbsent(
                        registryKey,
                        _ -> new HashMap<>()
                );
        TagBuilder builder = tagsMap.computeIfAbsent(
                        targetTag,
                        _ -> TagBuilder.create()
                );
        for (String value : ann.elementValue()) {
            if (value.startsWith("#")) {
                builder.addTag(
                        Identifier.parse(
                                value.substring(1)
                        )
                );
            } else {
                builder.addElement(
                        Identifier.parse(value)
                );
            }
        }
    }

    @Override
    public String getName() {
        return "Universal Auto Custom Tag Provider";
    }
}
