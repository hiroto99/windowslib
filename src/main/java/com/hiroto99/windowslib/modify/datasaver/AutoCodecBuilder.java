package com.hiroto99.windowslib.modify.datasaver;

import com.hiroto99.windowslib.modify.ModifySaver;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.ArrayList;
import java.util.List;
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
            public <T> AutoCodecBuilder add(String key, com.mojang.serialization.Codec<T> codec, java.util.function.Supplier<T> getter, java.util.function.Consumer<T> setter) {
                parent.add(prefix + "/" + key, codec, getter, setter);
                return this;
            }

            @Override
            public <T> AutoCodecBuilder addOptional(String key, com.mojang.serialization.Codec<T> codec, T defaultValue, java.util.function.Supplier<T> getter, java.util.function.Consumer<T> setter) {
                parent.addOptional(prefix + "/" + key, codec, defaultValue, getter, setter);
                return this;
            }
        };
    }

    // 個別の変数定義を保持する内部レコード
    private record FieldEntry<V>(
            String key,
            Codec<V> codec,
            Supplier<V> getter,
            Consumer<V> setter,
            boolean optional,
            V defaultValue
    ) {
        FieldEntry(String key, Codec<V> codec, Supplier<V> getter, Consumer<V> setter) {
            this(key, codec, getter, setter, false, null);
        }

        <T> DataResult<V> decodeAndApply(DynamicOps<T> ops, MapLike<T> input) {
            T rawValue = input.get(this.key);
            if (rawValue == null) {
                if (this.optional) {
                    this.setter.accept(this.defaultValue);
                    return DataResult.success(this.defaultValue);
                }
                return DataResult.error(() -> "Missing required key: " + this.key);
            }
            DataResult<V> parsed = this.codec.parse(ops, rawValue);
            parsed.ifSuccess(this.setter);
            return parsed;
        }

        <T> RecordBuilder<T> encodeFromGetter(DynamicOps<T> ops, RecordBuilder<T> builder) {
            V currentValue = this.getter.get();
            if (currentValue != null) {
                return builder.add(this.key, this.codec.encodeStart(ops, currentValue));
            }
            return builder;
        }
    }
}
