package com.hiroto99.windowslib.core.autodatagen;

import com.hiroto99.windowslib.util.lookups.*;
import net.minecraft.advancements.criterion.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConditionsBuilder {
    List<LootItemCondition.Builder> ConditionBuilders = new ArrayList<>();
    StringBuilder ConditionsMemory = new StringBuilder();
    AutoDataGenEngine.ConditionsMemoryMethodEnum ConditionsMemoryMethod = AutoDataGenEngine.ConditionsMemoryMethodEnum.NONE;
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
                    ConditionsMemory.delete(ConditionsMemory.length(), ConditionsMemory.length());
                    if (ConditionsMemoryMethod == AutoDataGenEngine.ConditionsMemoryMethodEnum.INVERTED) {
                        ConditionBuilders.add(InvertedLootItemCondition.invert(new ConditionsBuilder().get(ConditionsMemory.toString(), provider).stream().findFirst().orElse(LootItemRandomChanceCondition.randomChance(1))));
                    }
                    if (ConditionsMemoryMethod == AutoDataGenEngine.ConditionsMemoryMethodEnum.ALL_OF) {
                        ConditionBuilders.add(AllOfCondition.allOf(new ConditionsBuilder().get(ConditionsMemory.toString(), provider).toArray(new LootItemCondition.Builder[0])));
                    }
                    if (ConditionsMemoryMethod == AutoDataGenEngine.ConditionsMemoryMethodEnum.ANY_OF) {
                        ConditionBuilders.add(AnyOfCondition.anyOf(new ConditionsBuilder().get(ConditionsMemory.toString(), provider).toArray(new LootItemCondition.Builder[0])));
                    }
                    ConditionsMemory.delete(0, ConditionsMemory.length());
                    ConditionsMemoryMethod = AutoDataGenEngine.ConditionsMemoryMethodEnum.NONE;
                } else {
                    ConditionsMemory.append(";");
                }
            } else {
                detectorConditionAndSendBracketDetector(condition, "inverted", AutoDataGenEngine.ConditionsMemoryMethodEnum.INVERTED);
                detectorConditionAndSendBracketDetector(condition, "all_of", AutoDataGenEngine.ConditionsMemoryMethodEnum.ALL_OF);
                detectorConditionAndSendBracketDetector(condition, "any_of", AutoDataGenEngine.ConditionsMemoryMethodEnum.ANY_OF);
                if (condition.startsWith("random_chance:")) {
                    float randomChance = Float.parseFloat((clearMethodPart(condition, "random_chance:").split(":")[1]));
                    ConditionBuilders.add(LootItemRandomChanceCondition.randomChance(randomChance));
                }
                if (condition.startsWith("random_chance_with_looting_enchant/")) {
                    float chance = 0f;
                    float perLevelAboveFirst = 0f;
                    for (String conditionPart : clearMethodPart(condition, "random_chance_with_looting_enchant/").split(",")) {
                        String[] conditionDict = conditionPart.split(":");
                        if (conditionDict[0].equals("chance")) {
                            chance = Float.parseFloat(conditionDict[1]);
                        }
                        if (conditionDict[0].equals("per_level_above_first")) {
                            perLevelAboveFirst = Float.parseFloat(conditionDict[1]);
                        }
                    }
                    ConditionBuilders.add(LootItemRandomChanceWithEnchantedBonusCondition.
                            randomChanceAndLootingBoost(provider, chance, perLevelAboveFirst == 0f ? chance : perLevelAboveFirst));
                }
                if (condition.startsWith("time_check/")) {
                    String clock = "minecraft:overworld";
                    long period = 24000;
                    int min = 0;
                    int max = 24000;
                    for (String conditionPart : clearMethodPart(condition, "time_check/").split(",")) {
                        String[] conditionDict = conditionPart.split(":");
                        if (conditionDict[0].equals("clock")) {
                            clock = joinValueIfExist(conditionDict);
                        }
                        if (conditionDict[0].equals("period")) {
                            period = Long.parseLong(conditionDict[1]);
                        }
                        if (conditionDict[0].equals("min")) {
                            min = Integer.parseInt(conditionDict[1]);
                        }
                        if (conditionDict[0].equals("max")) {
                            max = Integer.parseInt(conditionDict[1]);
                        }
                    }
                    if (clock.endsWith("overworld")) {
                        ConditionBuilders.add(TimeCheck.time(provider.getOrThrow(WorldClocks.OVERWORLD), IntRange.range(min, max)).setPeriod(period));
                    } else if (clock.endsWith("the_end")) {
                        ConditionBuilders.add(TimeCheck.time(provider.getOrThrow(WorldClocks.THE_END), IntRange.range(min, max)).setPeriod(period));
                    }
                }
                if (condition.startsWith("weather_check/")) {
                    boolean checkRaining = false;
                    boolean raining = false;
                    boolean checkThundering = false;
                    boolean thundering = false;
                    for (String conditionPart : clearMethodPart(condition, "weather_check/").split(",")) {
                        String[] conditionDict = conditionPart.split(":");
                        if (conditionDict[0].equals("raining")) {
                            raining = Boolean.parseBoolean(conditionDict[1]);
                            checkRaining = true;
                        }
                        if (conditionDict[0].equals("thundering")) {
                            thundering = Boolean.parseBoolean(conditionDict[1]);
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
                if (condition.startsWith("dimension_check:")) {
                    ResourceKey<Level> checkDimension = DimensionLookup.getKey(joinValueIfExist(condition.split(":")));
                    ConditionBuilders.add(LocationCheck.checkLocation(new LocationPredicate.Builder().setDimension(checkDimension)));
                }
                if (condition.startsWith("biome_check:")) {
                    String conditionValue = joinValueIfExist(condition.split(":"));
                    ConditionBuilders.add(LocationCheck.checkLocation(new LocationPredicate.Builder().setBiomes(BiomeLookup.getHolderSet(provider, conditionValue))));
                }
                if (condition.startsWith("survives_explosion")) {
                    ConditionBuilders.add(ExplosionCondition.survivesExplosion());
                }
                if (condition.startsWith("match_tool/")) {
                    String[] conditionDict = clearMethodPart(condition, "table_bonus/").split(",");
                    ItemPredicate.Builder matchToolBuilder = ItemPredicate.Builder.item();
                    for (String conditionPart : conditionDict) {
                        if (conditionPart.startsWith("#")) {
                            matchToolBuilder = matchToolBuilder.of(ItemLookup.itemHolderGetter(provider), ItemLookup.getTagkey(conditionPart));
                        } else {
                            matchToolBuilder = matchToolBuilder.of(ItemLookup.itemHolderGetter(provider), ItemLookup.getItem(provider, conditionPart).orElse(Items.AIR));
                        }
                    }
                    ConditionBuilders.add(MatchTool.toolMatches(matchToolBuilder));
                }
                if (condition.startsWith("enchantment_active:")) {
                    boolean active = Boolean.parseBoolean(clearMethodPart(condition, "enchantment_active:"));
                    if (active) {
                        ConditionBuilders.add(EnchantmentActiveCheck.enchantmentActiveCheck());
                    } else {
                        ConditionBuilders.add(EnchantmentActiveCheck.enchantmentInactiveCheck());
                    }
                }
                if (condition.startsWith("table_bonus/")) {
                    Holder<Enchantment> enchantment = null;
                    float[] chances = new float[0];
                    for (String conditionPart : clearMethodPart(condition, "table_bonus/").split(",")) {
                        String[] conditionDict = conditionPart.split(":");
                        if (conditionDict[0].equals("enchantment")) {
                            enchantment = EnchantmentLookup.getHolder(provider.lookupOrThrow(Registries.ENCHANTMENT), joinValueIfExist(conditionDict));
                        }
                        if (conditionDict[0].equals("chances")) {
                            chances = new float[conditionDict.length - 1];
                            for (int i = 0; i < chances.length; i++) {
                                chances[i] = Float.parseFloat(conditionDict[i + 1]);
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
                MobEffectsPredicate.Builder mobEffectsPredicate = MobEffectsPredicate.Builder.effects();
                for (String conditionPart : clearMethodPart(condition, "entity_properties/").split(",")) {
                    String[] conditionDict = conditionPart.split(":");
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
                        entityType = EntityTypeLookup.getHolderSet(provider, joinValueIfExist(conditionDict));
                    }
                    if (conditionDict[0].equals("effects")) {
                        for (int i = 1; i < conditionDict.length - 1; i++) {
                            mobEffectsPredicate = mobEffectsPredicate.and(MobEffectLookup.getHolderSet(provider, conditionDict[i]));
                        }
                    }
                }
                if (entityTarget == null) {
                    throw new NullPointerException("entity_properties:entity is null or invalid");
                }
                if (entityType == null) {
                    throw new NullPointerException("entity_properties:entity_type is null or invalid");
                }
                ConditionBuilders.add(LootItemEntityPropertyCondition.hasProperties(entityTarget,
                        new EntityPredicate.Builder().entityType(new EntityTypePredicate(entityType))
                                .effects(mobEffectsPredicate).build()
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

    public void detectorConditionAndSendBracketDetector(String condition, String detectCondition, AutoDataGenEngine.ConditionsMemoryMethodEnum setConditionsMemoryMethod) {
        if (condition.startsWith(detectCondition + "[")) {
            ConditionsMemory.append(condition.substring((detectCondition + "[").length()));
            ConditionsMemoryMethod = setConditionsMemoryMethod;
            conditionsMemoryLength += 1;
        }
    }

    String clearMethodPart(String condition, String methodString) {
        return condition.replace(methodString, "");
    }

    String joinValueIfExist(String[] conditionDict) {
        if (conditionDict.length == 3) return conditionDict[1] + ":" + conditionDict[2];
        else return conditionDict[1];
    }
}
