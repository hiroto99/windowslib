package com.hiroto99.windowslib.modify.datasaver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AutoCodecBuilder {
    private final List<FieldEntry<?>> entries = new ArrayList<>();

    /**
     * ビルダーに変数の定義（キー名、Codec、Getter、Setter）を登録する
     */
    public <T> AutoCodecBuilder add(String key, Codec<T> codec, Supplier<T> getter, Consumer<T> setter) {
        this.entries.add(new FieldEntry<>(key, codec, getter, setter));
        return this;
    }

    /**
     * デフォルト値付きの登録ヘルパー（セーブデータにキーが無い場合も安全に復元できる）
     */
    public <T> AutoCodecBuilder addOptional(String key, Codec<T> codec, T defaultValue, Supplier<T> getter, Consumer<T> setter) {
        this.entries.add(new FieldEntry<>(key, codec, getter, setter, true, defaultValue));
        return this;
    }

    /**
     * 登録された全変数を1つのNBTオブジェクトにまとめて読み書きするCodecを自動生成する
     */
    public Codec<ModifySaver> buildSavedDataCodec(Supplier<ModifySaver> factory) {
        MapCodec<ModifySaver> mapCodec = new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return entries.stream().map(e -> ops.createString(e.key));
            }

            @Override
            public <T> DataResult<ModifySaver> decode(DynamicOps<T> ops, MapLike<T> input) {
                ModifySaver data = factory.get();
                for (FieldEntry<?> entry : entries) {
                    DataResult<?> result = entry.decodeAndApply(ops, input);
                    if (result.isError()) {
                        return DataResult.error(() -> "Failed to decode field [" + entry.key + "]: " + result.error().get().message());
                    }
                }
                return DataResult.success(data);
            }

            @Override
            public <T> RecordBuilder<T> encode(ModifySaver input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                RecordBuilder<T> builder = prefix;
                for (FieldEntry<?> entry : entries) {
                    builder = entry.encodeFromGetter(ops, builder);
                }
                return builder;
            }
        };

        return mapCodec.codec();
    }

    public AutoCodecBuilder withPrefix(String prefix) {
        AutoCodecBuilder parent = this;
        return new AutoCodecBuilder() {
            @Override
            public <T> AutoCodecBuilder add(String key, Codec<T> codec, Supplier<T> getter, Consumer<T> setter) {
                parent.add(prefix + "/" + key, codec, getter, setter);
                return this;
            }

            @Override
            public <T> AutoCodecBuilder addOptional(String key, Codec<T> codec, T defaultValue, Supplier<T> getter, Consumer<T> setter) {
                parent.addOptional(prefix + "/" + key, codec, defaultValue, getter, setter);
                return this;
            }
        };
    }

    public boolean hasAnyChanged() {
        boolean changed = false;
        for (FieldEntry<?> entry : this.entries) {
            if (entry.checkAndUpdateDirty()) {
                changed = true;
            }
        }
        return changed;
    }

    // 個別の変数定義を保持する内部レコード
    private class FieldEntry<V> {
        final String key;
        final Codec<V> codec;
        final Supplier<V> getter;
        final Consumer<V> setter;
        final boolean optional;
        final V defaultValue;
        Object lastSavedSnapshot; // ★ 前回保存・確認した値のスナップショット

        FieldEntry(String key, Codec<V> codec, Supplier<V> getter, Consumer<V> setter) {
            this(key, codec, getter, setter, false, null);
        }

        FieldEntry(String key, Codec<V> codec, Supplier<V> getter, Consumer<V> setter, boolean optional, V defaultValue) {
            this.key = key;
            this.codec = codec;
            this.getter = getter;
            this.setter = setter;
            this.optional = optional;
            this.defaultValue = defaultValue;
            lastSavedSnapshot = createSnapshot(getter.get());
        }

        private Object createSnapshot(V value) {
            if (value instanceof List<?> list) return List.copyOf(list);
            if (value instanceof Map<?, ?> map) return Map.copyOf(map);
            if (value instanceof Set<?> set) return Set.copyOf(set);
            return value;
        }

        <T> DataResult<V> decodeAndApply(DynamicOps<T> ops, MapLike<T> input) {
            T rawValue = input.get(this.key);
            if (rawValue == null) {
                if (this.optional) {
                    this.setter.accept(wrapMutableIfCollection(this.defaultValue));
                    return DataResult.success(this.defaultValue);
                }
                return DataResult.error(() -> "Missing required key: " + this.key);
            }
            DataResult<V> parsed = this.codec.parse(ops, rawValue);
            parsed.ifSuccess(val -> this.setter.accept(wrapMutableIfCollection(val)));
            return parsed;
        }

        // ★ 不変の List / Map / Set を自動で可変コレクションに変換するヘルパー
        @SuppressWarnings("unchecked")
        private V wrapMutableIfCollection(V value) {
            if (value instanceof List<?> list && !(list instanceof ArrayList<?>)) {
                return (V) new ArrayList<>(list);
            }
            if (value instanceof Map<?, ?> map && !(map instanceof HashMap<?, ?>)) {
                return (V) new HashMap<>(map);
            }
            if (value instanceof Set<?> set && !(set instanceof HashSet<?>)) {
                return (V) new HashSet<>(set);
            }
            return value;
        }

        <T> RecordBuilder<T> encodeFromGetter(DynamicOps<T> ops, RecordBuilder<T> builder) {
            V currentValue = this.getter.get();
            if (currentValue != null) {
                return builder.add(this.key, this.codec.encodeStart(ops, currentValue));
            }
            return builder;
        }

        // 変化があったか判定し、あればスナップショットを更新して true を返す
        boolean checkAndUpdateDirty() {
            V current = this.getter.get();
            if (!java.util.Objects.equals(this.lastSavedSnapshot, current)) {
                this.lastSavedSnapshot = createSnapshot(current);
                return true;
            }
            return false;
        }
    }
}
