package com.hiroto99.windowslib.core.autodatagen;

import com.hiroto99.windowslib.WindowsLib;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoLootTable;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoTag;
import com.hiroto99.windowslib.util.ParseLanguageException;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class AutoDataGenEngine {
    // スキャンしたデータを一時的に蓄えておくプール（DataGenプロバイダがここを参照する）
    public static final List<DataEntryTag> COLLECTED_DATA_TAG = new ArrayList<>();
    public static final List<DataEntryLoot> COLLECTED_DATA_LOOT = new ArrayList<>();

    // データの持ち運び用（Java record で簡潔に定義）
    public record DataEntryTag(AutoTag annotation, Object value) {}
    public record DataEntryLoot(AutoLootTable annotation, Object value, String MODID) {}

    public static void scanPackage(String packageName) {
        // タグ生成アノテーションのスキャン
        COLLECTED_DATA_TAG.clear(); // 念のため初期化
        COLLECTED_DATA_LOOT.clear(); // 念のため初期化

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackage(packageName)
                        .addScanners(Scanners.FieldsAnnotated)
        );

        Set<Class<?>> types = reflections.getTypesAnnotatedWith(Mod.class);
        Optional<String> MODID = Optional.empty();
        for (Class<?> clazz : types) {
            Mod ann = clazz.getAnnotation(Mod.class);
            if (ann != null && !Objects.equals(ann.value(), packageName.replace(packageName.substring(0, packageName.lastIndexOf('.')), ""))) {
                MODID = Optional.of(ann.value());
            }
        }

        // @AutoTag が付与されたフィールドを全自動検出
        Set<Field> fields = reflections.getFieldsAnnotatedWith(AutoTag.class);

        for (Field field : fields) {
            try {
                // static フィールドからオブジェクト（ItemやBlockのインスタンス）を取得
                Object value = field.get(null);
                AutoTag ann = field.getAnnotation(AutoTag.class);

                if (value != null) {
                    if (value instanceof DeferredHolder<?, ?> holder) {
                        value = holder.get();
                    }
                    if (value instanceof DeferredItem<?> holder) {
                        value = holder.get();
                    }
                    if (value instanceof DeferredBlock<?> holder) {
                        value = holder.get();
                    }
                    COLLECTED_DATA_TAG.add(new DataEntryTag(ann, value));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // @AutoLootTable が付与されたフィールドを全自動検出
        fields = reflections.getFieldsAnnotatedWith(AutoLootTable.class);

        for (Field field : fields) {
            try {
                // static フィールドからオブジェクト（ItemやBlockのインスタンス）を取得
                Object value = field.get(null);
                AutoLootTable ann = field.getAnnotation(AutoLootTable.class);

                if (value != null) {
                    if (value instanceof DeferredHolder<?, ?> holder) {
                        value = holder.get();
                    }
                    if (value instanceof DeferredItem<?> holder) {
                        value = holder.get();
                    }
                    if (value instanceof DeferredBlock<?> holder) {
                        value = holder.get();
                    }
                    COLLECTED_DATA_LOOT.add(new DataEntryLoot(ann, value, MODID.orElse(WindowsLib.MODID)));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public record LootTableData(Item item, int weight, UniformGenerator count, float fortuneMultiplier, String conditions) {}

    public static LootTableData dropItemDataDecode(String dropItemData) throws ParseLanguageException {
        String[] decodeData = dropItemData.split("/");
        if (decodeData.length < 5) {
            throw new ParseLanguageException("Invalid drop item data format:\n" +
                    "Input: \"" + Arrays.toString(decodeData) + "\"\n" +
                    "Correct format: \"item/weight/min/max/fortune multiplier(can 0)/conditions(nullable)\" (Example:[\"minecraft:dirt/3/1/1/0\"])");
        }
        Identifier location = Identifier.parse(decodeData[0]);
        Optional<Holder.Reference<Item>> rawItem = BuiltInRegistries.ITEM.get(location);
        Item item = rawItem.map(Holder.Reference::value).orElse(null);
        StringBuilder decodeConditions = new StringBuilder();
        for (int i = 5; i < decodeData.length; i++) {
            decodeConditions.append(decodeData[i]).append("/");
        }
        decodeConditions.delete(decodeConditions.length() - 1, decodeConditions.length());
        return new LootTableData(item, Integer.parseInt(decodeData[1]), UniformGenerator.between(Float.parseFloat(decodeData[2]), Float.parseFloat(decodeData[3])), Float.parseFloat(decodeData[4]), decodeData.length > 5 ? decodeConditions.toString() : "");
    }

    enum ConditionsMemoryMethodEnum {
        NONE,
        INVERTED,
        ALL_OF,
        ANY_OF
    }
}
