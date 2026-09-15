package com.hiroto99.windowslib.core.autodatagen;

import com.hiroto99.windowslib.core.autodatagen.annotation.AutoLootTable;
import com.hiroto99.windowslib.core.autodatagen.annotation.AutoTag;
import com.hiroto99.windowslib.util.BiomeLookup;
import com.hiroto99.windowslib.util.DimensionLookup;
import com.hiroto99.windowslib.util.EnchantmentLookup;
import com.hiroto99.windowslib.util.EntityTypeLookup;
import com.hiroto99.windowslib.util.exception.ParseLanguageException;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.EntityTypePredicate;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.*;
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
    public record DataEntryLoot(AutoLootTable annotation, Object value) {}

    public static void scanPackage(String packageName) {
        // タグ生成アノテーションのスキャン
        COLLECTED_DATA_TAG.clear(); // 念のため初期化
        COLLECTED_DATA_LOOT.clear(); // 念のため初期化

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackage(packageName)
                        .addScanners(Scanners.FieldsAnnotated)
        );

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
                    COLLECTED_DATA_LOOT.add(new DataEntryLoot(ann, value));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public record LootTableData(Item item, int weight, int quality, String conditions) {}

    public static LootTableData dropItemDataDecode(String dropItemData) throws ParseLanguageException {
        String[] decodeData = dropItemData.split("/");
        if (decodeData.length < 2) {
            throw new ParseLanguageException("Invalid drop item data format:\n" +
                    "Input: \"" + decodeData + "\"\n" +
                    "Correct format: \"item/weight/quality/conditions(nullable)\" (Example:[\"minecraft:dirt/3/1\"])");
        }
        Identifier location = Identifier.parse(decodeData[0]);
        Optional<Holder.Reference<Item>> rawItem = BuiltInRegistries.ITEM.get(location);
        Item item = rawItem.map(Holder.Reference::value).orElse(null);
        return new LootTableData(item, Integer.parseInt(decodeData[1]), Integer.parseInt(decodeData[2]), decodeData.length > 3 ? decodeData[3] : "");
    }

    enum ConditionsMemoryMethodEnum {
        NONE,
        INVERTED,
        ALL_OF,
        ANY_OF
    }

    public class ConditionsBuilder {
        List<LootItemCondition.Builder> ConditionBuilders = new ArrayList<>();
        StringBuilder ConditionsMemory = new StringBuilder();
        ConditionsMemoryMethodEnum ConditionsMemoryMethod = ConditionsMemoryMethodEnum.NONE;
        int conditionsMemoryLength = 0;
        public List<LootItemCondition.Builder> get(String conditions, HolderLookup.Provider provider) {
            for (String condition : conditions.toLowerCase(Locale.ROOT).split(";")) {
                if (conditionsMemoryLength > 0) {
                    if (condition.startsWith("[")) {
                        conditionsMemoryLength += 1;
                    }
                    if (condition.contains("]")) {
                        conditionsMemoryLength -= getCountOfCloseBrackets(condition);
                    }
                    ConditionsMemory.append(condition);
                    if (conditionsMemoryLength == 0) {
                        ConditionsMemory.delete(ConditionsMemory.length() - 1, ConditionsMemory.length() - 1);
                        if (ConditionsMemoryMethod == ConditionsMemoryMethodEnum.INVERTED) {
                            ConditionBuilders.add(InvertedLootItemCondition.invert(new ConditionsBuilder().get(ConditionsMemory.toString(), provider).stream().findFirst().get()));
                        }
                        if (ConditionsMemoryMethod == ConditionsMemoryMethodEnum.ALL_OF) {
                            ConditionBuilders.add(AllOfCondition.allOf((LootItemCondition.Builder) new ConditionsBuilder().get(ConditionsMemory.toString(), provider)));
                        }
                        if (ConditionsMemoryMethod == ConditionsMemoryMethodEnum.ANY_OF) {
                            ConditionBuilders.add(AnyOfCondition.anyOf((LootItemCondition.Builder) new ConditionsBuilder().get(ConditionsMemory.toString(), provider)));
                        }
                        ConditionsMemory.delete(0, ConditionsMemory.length() - 1);
                        ConditionsMemoryMethod = ConditionsMemoryMethodEnum.NONE;
                    } else {
                        ConditionsMemory.append(";");
                    }
                } else {
                    detectorConditionAndSendBracketDetector(condition, "inverted", ConditionsMemoryMethodEnum.INVERTED);
                    detectorConditionAndSendBracketDetector(condition, "all_of", ConditionsMemoryMethodEnum.ALL_OF);
                    detectorConditionAndSendBracketDetector(condition, "any_of", ConditionsMemoryMethodEnum.ANY_OF);
                    if (condition.startsWith("random_chance/")) {
                        float randomChance = Float.parseFloat((condition.split("/")[1]));
                        ConditionBuilders.add(LootItemRandomChanceCondition.randomChance(randomChance));
                    }
                    if (condition.startsWith("random_chance_with_looting_enchant/")) {
                        float chance = 0f;
                        float perLevelAboveFirst = 0f;
                        for (String conditionPart : condition.split(",")) {
                            String[] conditionDict = conditionPart.split("/");
                            if (conditionDict[0].equals("chance")) {
                                chance = Float.parseFloat((conditionPart.split("/")[1]));
                            }
                            if (conditionDict[0].equals("per_level_above_first")) {
                                perLevelAboveFirst = Float.parseFloat((conditionPart.split("/")[1]));
                            }
                        }
                        ConditionBuilders.add(LootItemRandomChanceWithEnchantedBonusCondition.
                                randomChanceAndLootingBoost(provider, chance, perLevelAboveFirst == 0f ? chance : perLevelAboveFirst));
                    }
                    if (condition.startsWith("time_check/")) {
                        String clock = "minecraft:overworld";
                        int period = 24000;
                        int min = 0;
                        int max = 24000;
                        for (String conditionPart : condition.split(",")) {
                            String[] conditionDict = conditionPart.split("/");
                            if (conditionDict[0].equals("clock")) {
                                clock = conditionDict[1];
                            }
                            if (conditionDict[0].equals("period")) {
                                period = Integer.parseInt((conditionPart.split("/")[1]));
                            }
                            if (conditionDict[0].equals("min")) {
                                min = Integer.parseInt((conditionPart.split("/")[1]));
                            }
                            if (conditionDict[0].equals("max")) {
                                max = Integer.parseInt((conditionPart.split("/")[1]));
                            }
                        }
                        ConditionBuilders.add(TimeCheck.time(Holder.direct(provider.getOrThrow(WorldClocks.OVERWORLD).value()), IntRange.range(min, max)));
                    }
                    if (condition.startsWith("weather_check/")) {
                        boolean checkRaining = false;
                        boolean raining = false;
                        boolean checkThundering = false;
                        boolean thundering = false;
                        for (String conditionPart : condition.split(",")) {
                            String[] conditionDict = conditionPart.split("/");
                            if (conditionDict[0].equals("raining")) {
                                raining = Boolean.parseBoolean((conditionPart.split("/")[1]));
                                checkRaining = true;
                            }
                            if (conditionDict[0].equals("thundering")) {
                                thundering = Boolean.parseBoolean((conditionPart.split("/")[1]));
                                checkThundering = true;
                            }
                        }
                        WeatherCheck.Builder weatherCheckCondition = WeatherCheck.weather();
                        if (checkRaining) {
                            weatherCheckCondition = weatherCheckCondition.setRaining(raining);
                        }
                        if (checkThundering) {
                            weatherCheckCondition = weatherCheckCondition.setThundering(thundering);
                        }
                        ConditionBuilders.add(weatherCheckCondition);
                    }
                    if (condition.startsWith("dimension_check/")) {
                        ResourceKey<Level> checkDimension = DimensionLookup.getKey(condition.split("/")[1]);
                        ConditionBuilders.add(LocationCheck.checkLocation(new LocationPredicate.Builder().setDimension(checkDimension)));
                    }
                    if (condition.startsWith("biome_check/")) {
                        HolderSet<Biome> checkBiome = BiomeLookup.getHolderSet(provider, condition.split("/")[1]);
                        ConditionBuilders.add(LocationCheck.checkLocation(new LocationPredicate.Builder().setBiomes(checkBiome)));
                    }
                    if (condition.startsWith("survives_explosion/")) {
                        ConditionBuilders.add(ExplosionCondition.survivesExplosion());
                    }
                    if (condition.startsWith("enchantment_active/")) {
                        boolean active = Boolean.parseBoolean(condition.split("/")[1]);
                        if (active) {
                            ConditionBuilders.add(EnchantmentActiveCheck.enchantmentActiveCheck());
                        } else {
                            ConditionBuilders.add(EnchantmentActiveCheck.enchantmentInactiveCheck());
                        }
                    }
                    if (condition.startsWith("table_bonus/")) {
                        Holder<Enchantment> enchantment = null;
                        float[] chances = new float[0];
                        for (String conditionPart : condition.split(",")) {
                            String[] conditionDict = conditionPart.split("/");
                            if (conditionDict[0].equals("enchantment")) {
                                enchantment = EnchantmentLookup.getHolder(provider.lookupOrThrow(Registries.ENCHANTMENT), conditionPart.split("/")[1]);
                            }
                            if (conditionDict[0].equals("chances")) {
                                chances = new float[conditionPart.split("/").length];
                                int i = 0;
                                for (String chanceString : conditionPart.split("/")) {
                                    chances[i] = Float.parseFloat(chanceString);
                                    i++;
                                }
                            }
                        }
                        if (enchantment == null) {
                            throw new NullPointerException("table_bonus/enchantment is null");
                        }
                        ConditionBuilders.add(BonusLevelTableCondition.bonusLevelFlatChance(enchantment, chances));
                    }
                }
                if (condition.startsWith("entity_properties/")) {
                    LootContext.EntityTarget entityTarget = null;
                    HolderSet<EntityType<?>> entityType = null;
                    for (String conditionPart : condition.split(",")) {
                        String[] conditionDict = conditionPart.split("/");
                        if (conditionDict[0].equals("entity")) {
                            if (conditionDict[1].equals("this")) {
                                entityTarget = LootContext.EntityTarget.THIS;
                            }
                            if (conditionDict[1].equals("attacker")) {
                                entityTarget = LootContext.EntityTarget.ATTACKER;
                            }
                            if (conditionDict[1].equals("direct_attacker")) {
                                entityTarget = LootContext.EntityTarget.DIRECT_ATTACKER;
                            }
                            if (conditionDict[1].equals("attacking_player")) {
                                entityTarget = LootContext.EntityTarget.ATTACKING_PLAYER;
                            }
                            if (conditionDict[1].equals("target_entity")) {
                                entityTarget = LootContext.EntityTarget.TARGET_ENTITY;
                            }
                            if (conditionDict[1].equals("interacting_entity")) {
                                entityTarget = LootContext.EntityTarget.INTERACTING_ENTITY;
                            }
                        }
                        if (conditionDict[0].equals("entity_type")) {
                            entityType = EntityTypeLookup.getHolderSet(provider, conditionDict[1]);
                        }
                    }
                    if (entityTarget == null) {
                        throw new NullPointerException("entity_properties/entity is null or invalid");
                    }
                    if (entityType == null) {
                        throw new NullPointerException("entity_properties/entity_type is null or invalid");
                    }
                    ConditionBuilders.add(LootItemEntityPropertyCondition.hasProperties(entityTarget,
                            new EntityPredicate.Builder().entityType(new EntityTypePredicate(entityType)).build()
                    ));
                }
            }
            return ConditionBuilders;
        }

        int getCountOfCloseBrackets(String condition) {
            int count = 0;
            for (int i = 0; i < condition.length(); i++) {
                if (condition.charAt(i) == ']') {
                    count++;
                }
            }
            return count;
        }

        public void detectorConditionAndSendBracketDetector(String condition, String detectCondition, ConditionsMemoryMethodEnum setConditionsMemoryMethod) {
            if (condition.startsWith(detectCondition + "[")) {
                ConditionsMemory.append(condition.substring((detectCondition + "[").length()));
                ConditionsMemoryMethod = setConditionsMemoryMethod;
                conditionsMemoryLength += 1;
            }
        }
    }
}
