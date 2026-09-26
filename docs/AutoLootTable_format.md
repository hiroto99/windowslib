# Conditional DSL

## Example

```text
@AutoLootTable(
    name = "test_block",
    type = LootType.BLOCK,
    blockType = LootBlockType.CUSTOM,
    pool = {
        @LootTablePool(
            name = "main",
            rollsMin = 1,
            rollsMax = 1,
            bonusRolls = 0,
            condition =
                "weather_check/raining:true",

            dropItemData = {
                "minecraft:dirt/3/1/1/0",
                "minecraft:diamond/1/1/1/0/random_chance:0.1"
            }
        )
    }
)
```

## Grammar

```ebnf
AutoLootTable
    = "AutoLootTable"
      ,
      "("
      ,
      { AutoLootTableProperty }
      ,
      ")"
      ;

AutoLootTableProperty
    = NameProperty
    | TypeProperty
    | BlockTypeProperty
    | OreDropsProperty
    | PoolProperty
    ;

NameProperty
    = "name"
      ,
      "="
      ,
      String
      ;

TypeProperty
    = "type"
      ,
      "="
      ,
      LootType
      ;

BlockTypeProperty
    = "blockType"
      ,
      "="
      ,
      LootBlockType
      ;

OreDropsProperty
    = "oreDrops"
      ,
      "="
      ,
      ResourceLocation
      ;

PoolProperty
    = "pool"
      ,
      "="
      ,
      "{"
      ,
      LootTablePool
      ,
      { "," , LootTablePool }
      ,
      "}"
      ;

LootTablePool
    = "@LootTablePool"
      ,
      "("
      ,
      { LootTablePoolProperty }
      ,
      ")"
      ;

LootTablePoolProperty
    = PoolNameProperty
    | RollsMinProperty
    | RollsMaxProperty
    | BonusRollsProperty
    | ConditionProperty
    | DropItemDataProperty
    ;

PoolNameProperty
    = "name"
      ,
      "="
      ,
      String
      ;

RollsMinProperty
    = "rollsMin"
      ,
      "="
      ,
      Float
      ;

RollsMaxProperty
    = "rollsMax"
      ,
      "="
      ,
      Float
      ;

BonusRollsProperty
    = "bonusRolls"
      ,
      "="
      ,
      Float
      ;

ConditionProperty
    = "condition"
      ,
      "="
```