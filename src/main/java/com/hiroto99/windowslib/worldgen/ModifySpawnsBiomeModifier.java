package com.hiroto99.windowslib.worldgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import java.util.function.Function;

/**
 * <p>A biome modifier that changes mob spawning within biomes. Uses the following JSON format:</p>
 *
 * <pre>
 * {
 *   "type": "windowslib:modify_spawns", // Required
 *   "biomes": "#namespace:biome_tag", // Can specify a biome ID, a list of biome IDs, or #namespace:biome_tag
 *   "spawners":
 *   {
 *     "type": "namespace:entity_type", // Type of mob to spawn
 *     "weight": 100, // int; spawn weight (frequency)
 *     "minCount": 1, // int; minimum group size
 *     "maxCount": 4, // int; maximum group size
 *   }
 * }
 * </pre>
 *
 * <p>You can also specify a list of spawner objects instead of a single spawner object:</p>
 *
 * <pre>
 * {
 *   "type": "windowslib:modify_spawns", // Required
 *   "biomes": "#namespace:biome_tag", // Can specify a biome ID, a list of biome IDs, or #namespace:biome_tag
 *   "spawners":
 *   [
 *     {
 *       "type": "namespace:entity_type", // Type of mob to spawn
 *       "weight": 100, // int; spawn weight (frequency)
 *       "minCount": 1, // int; minimum group size
 *       "maxCount": 4, // int; maximum group size
 *     },
 *     {
 *       // Additional spawner object
 *     }
 *   ]
 * }
 * </pre>
 *
 * @param biomes   The biomes where mob spawning is to be added.
 * @param spawners A list of weighted SpawnerData specifying the EntityType, weight, and flock size.
*/
public record ModifySpawnsBiomeModifier(HolderSet<Biome> biomes, WeightedList<SpawnerData> spawners) implements BiomeModifier {

    // 2. MapCodec の定義（後述のレジストリ登録に使用）
    public static final MapCodec<ModifySpawnsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(ModifySpawnsBiomeModifier::biomes),
                    Codec.either(WeightedList.codec(SpawnerData.CODEC), Weighted.codec(SpawnerData.CODEC)).xmap(
                            either -> either.map(Function.identity(), WeightedList::<SpawnerData>of), // convert list/singleton to list when decoding
                            list -> list.unwrap().size() == 1 ? Either.right(list.unwrap().get(0)) : Either.left(list) // convert list to singleton/list when encoding
                    ).fieldOf("spawners").forGetter(ModifySpawnsBiomeModifier::spawners)
            ).apply(instance, ModifySpawnsBiomeModifier::new)
    );

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && this.biomes.contains(biome)) {
            MobSpawnSettingsBuilder spawnBuilder = builder.getMobSpawnSettings();
            WeightedList.Builder<SpawnerData> spawns = null;
            for (MobCategory category : MobCategory.values()) {
                spawns = spawnBuilder.getSpawner(category);
            }
            for (Weighted<SpawnerData> spawner : this.spawners.unwrap()) {
                EntityType<?> type = spawner.value().type();
                if (spawns != null) {
                    HolderSet<EntityType<?>> entityTypes = HolderSet.direct(Holder.direct(type));
                    spawns.removeIf(spawnerData -> entityTypes.contains(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(spawnerData.value().type())));
                }
                spawnBuilder.addSpawn(type.getCategory(), spawner.weight(), spawner.value());
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
