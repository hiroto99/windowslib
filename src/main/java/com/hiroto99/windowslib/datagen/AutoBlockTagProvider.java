package com.hiroto99.windowslib.datagen;

import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryTag;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoTag;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.AutoTagSelector;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.hiroto99.windowslib.datagen.AutoDataGenProvider.DATA_ENTRY_TAG;

public class AutoBlockTagProvider extends BlockTagsProvider {
    public AutoBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        super(output, lookupProvider, modId);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        List<DataEntryTag> dataGenEntries = DATA_ENTRY_TAG;
        for (DataEntryTag dataEntry : dataGenEntries) {
            AutoTag AddTagData = dataEntry.annotation();
            List<TagKey<?>> tagKeyList = new AutoTagSelector(AddTagData.tagtype(), AddTagData.tagKeyPath()).getKeyTags();
            tagKeyList.stream().filter(tagKey -> tagKey.registry() == Registries.BLOCK).forEach(tagKey -> {
                TagKey<Block> tagkey = (TagKey<Block>) tagKey;
                if (dataEntry.value() instanceof Block v) {
                    this.tag(tagkey)
                            .add(v);
                }
            });
        }
    }
}
