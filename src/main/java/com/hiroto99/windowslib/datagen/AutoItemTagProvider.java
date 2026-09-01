package com.hiroto99.windowslib.datagen;

import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine;
import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryTag;
import com.hiroto99.windowslib.core.autodatagen.annotation.AddTag;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.AutoTagSelector;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AutoItemTagProvider extends ItemTagsProvider {
    public AutoItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        super(output, lookupProvider, modId);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        List<DataEntryTag> dataGenEntries = AutoDataGenEngine.COLLECTED_DATA_TAG;
        for (DataEntryTag dataEntry : dataGenEntries) {
            AddTag AddTagData = dataEntry.annotation();
            List<TagKey<?>> tagKeyList = new AutoTagSelector(AddTagData.tagtype(), AddTagData.tagKeyPath()).getKeyTags();
            tagKeyList.stream().filter(tagKey -> tagKey.registry() == Registries.ITEM).forEach(tagKey -> {
                TagKey<Item> tagkey = (TagKey<Item>) tagKey;
                if (dataEntry.value() instanceof Item v) {
                    this.tag(tagkey)
                            .add(v);
                }
            });
        }
    }
}
