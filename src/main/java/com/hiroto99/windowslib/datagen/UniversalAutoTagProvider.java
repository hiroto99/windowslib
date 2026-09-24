package com.hiroto99.windowslib.datagen;

import com.hiroto99.windowslib.core.autodatagen.AutoDataGenEngine.DataEntryTag;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoTag;
import com.hiroto99.windowslib.core.autodatagen.generatortypes.TagType;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.hiroto99.windowslib.datagen.AutoDataGenProvider.DATA_ENTRY_TAG;

public class UniversalAutoTagProvider implements DataProvider {
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;
    private final String modId;

    public UniversalAutoTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        this.output = output;
        this.lookupProvider = lookupProvider;
        this.modId = modId;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.lookupProvider.thenCompose(provider -> {
            // 💡 修正：TagLookup をやめ、[レジストリキー -> [タグキー -> TagBuilder]] の2重マップで保持する
            Map<ResourceKey<? extends Registry<?>>, Map<TagKey<?>, TagBuilder>> builders = new HashMap<>();

            // DATA_ENTRY_TAG から全ての不規則なデータをループ処理
            for (DataEntryTag dataEntry : DATA_ENTRY_TAG) {
                AutoTag addTagData = dataEntry.annotation();
                Class<? extends TagType<?>> tagTypeClass = addTagData.tagtype();
                TagType<?>[] constants = tagTypeClass.getEnumConstants();

                if (constants != null && constants.length > 0) {
                    TagType<?> tagType = constants[0];
                    processDynamicEntry(provider, builders, tagType, dataEntry.value(), addTagData.tagKeyPath());
                }
            }

            // 溜め込んだタグデータを一括でJSONファイルとして書き出す
            List<CompletableFuture<?>> futures = new ArrayList<>();

            builders.forEach((registryKey, tagsMap) -> {
                tagsMap.forEach((tagKey, tagBuilder) -> {
                    // 1. パスプロバイダの生成 (Target.DATA_PACK)
                    PackOutput.PathProvider pathProvider = this.output.createPathProvider(
                            PackOutput.Target.DATA_PACK,
                            "tags/" + registryKey.identifier().getPath()
                    );
                    Path path = pathProvider.json(tagKey.location());

                    // 2. 💡 修正：serializeToJson() ではなく build() を使用する
                    // build() メソッドは List<TagEntry> を返します。
                    // これを Minecraft の標準仕様に従って JSON オブジェクトに変換します。
                    com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
                    com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();

                    // tagBuilder.build() で溜め込んだ要素をループしてJSON配列に流し込む
                    tagBuilder.build().forEach(entry -> jsonArray.add(entry.toString()));

                    jsonObject.addProperty("replace", false); // 上書き設定 (デフォルトはfalse)
                    jsonObject.add("values", jsonArray);

                    // 3. ファイル書き出しタスクを追加
                    futures.add(DataProvider.saveStable(cache, jsonObject, path));
                });
            });

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    /**
     * 💡 ジェネリクス（<T>）を使って、実行時に Block や Item などの不規則な型を自動で適合させる
     */
    @SuppressWarnings("unchecked")
    private <T> void processDynamicEntry(
            HolderLookup.Provider provider,
            Map<ResourceKey<? extends Registry<?>>, Map<TagKey<?>, TagBuilder>> builders,
            TagType<?> rawTagType,
            Object rawValue,
            String[] paths) {

        // 1. 安全に型をキャストして固定する
        TagType<T> tagType = (TagType<T>) rawTagType;
        ResourceKey<Registry<T>> registryKey = tagType.getRegistry();

        // 2. 💡 追加：不規則なオブジェクトから安全に ResourceLocation (ID) を逆引きする
        Identifier id = getRegistryId(provider, registryKey, rawValue);

        // 3. 💡 修正：このレジストリ用のタグマップを無ければ作る
        Map<TagKey<?>, TagBuilder> tagsMap = builders.computeIfAbsent(registryKey, k -> new HashMap<>());

        // 4. アノテーションに指定された全てのパスに対してタグを登録する
        for (TagKey<T> tagKey : tagType.getTagKeys(paths)) {
            // 💡 修正：TagBuilder.create() で新しく実体を生成し、そこに直接IDを追加する
            TagBuilder builder = tagsMap.computeIfAbsent(tagKey, k -> TagBuilder.create());

            // 登録対象を要素として追加（型に依存せず ResourceLocation で一元管理されるため100%安全）
            builder.addElement(id);
        }
    }

    /**
     * 💡 NeoForge 21.x 対応：渡された不規則なオブジェクトから動的に ResourceLocation (ID) を取得する
     */
    @SuppressWarnings("unchecked")
    private <T> Identifier getRegistryId(HolderLookup.Provider provider, ResourceKey<Registry<T>> registryKey, Object value) {
        // 1. もし値がすでに DeferredHolder や Holder だった場合のアンラップ処理
        if (value instanceof Holder<?> holder) {
            return holder.unwrapKey().map(ResourceKey::identifier).orElse(Identifier.parse(""));
        }

        // 2. BuiltInRegistries.REGISTRY から Holder<Registry<?>> を安全に取得する
        // (.get() の戻り値は Holder.Reference<Registry<?>> なので、.value() で生レジストリを取り出す)
        var registryHolder = net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(registryKey.identifier());

        if (registryHolder.isPresent()) {
            // value() を呼び出して、Holder の内部から本当の Registry クラスを取り出す
            Registry<T> registry = (Registry<T>) registryHolder.get().value();

            // 値(T)をキャストして、レジストリからIDを逆引きする
            T typedValue = (T) value;
            Identifier id = registry.getKey(typedValue);

            if (id != null) {
                return id;
            }
        }

        // 3. 上記で取れなかった場合の確実なフォールバック（プロバイダのルックアップ名簿から解決を試みる）
        var lookup = provider.lookup(registryKey).orElse(null);
        if (lookup != null) {
            T typedValue = (T) value;
            // ルックアップ名簿に直接登録されている Holder から、登録名をブチ抜く
            return lookup.get((ResourceKey<T>) value)
                    .map(Holder.Reference::key)
                    .map(ResourceKey::identifier)
                    .orElseThrow(() -> new IllegalArgumentException("レジストリからオブジェクトのIDを逆引きできませんでした: " + value));
        }

        throw new IllegalArgumentException("指定されたレジストリが見つかりません: " + registryKey);
    }

    @Override
    public String getName() {
        return "Universal Auto Tag Provider";
    }
}
