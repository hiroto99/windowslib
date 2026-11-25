package com.hiroto99.windowslib.util.list;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EncodeData<T> {
    private final Map<Character, T> mapping;

    // ベースとなるプライベートコンストラクタ
    private EncodeData(Map<Character, T> mapping) {
        this.mapping = Map.copyOf(mapping); // 完全な不変マップとして保持
    }

    // Java 25 (JEP 513): this(...) の前にバリデーションと変換処理を記述！
    @SuppressWarnings("unchecked")
    public EncodeData(Object... pairs) {
        Objects.requireNonNull(pairs, "pairs must not be null");
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "キー(Character)と値は必ずペア（偶数個）で指定してください。現在の引数数: " + pairs.length
            );
        }

        Map<Character, T> temp = HashMap.newHashMap(pairs.length / 2);
        for (int i = 0; i < pairs.length; i += 2) {
            Object rawKey = pairs[i];
            Object rawValue = pairs[i + 1];

            // 1. キーの型チェック（Pattern Matching for instanceof）
            if (!(rawKey instanceof Character key)) {
                throw new IllegalArgumentException(
                        (i + 1) + "番目の引数は Character である必要があります: " + rawKey
                );
            }
            // 2. 重複キーの防止（Map.of と同じ安全設計）
            if (temp.containsKey(key)) {
                throw new IllegalArgumentException("キー '" + key + "' が重複して指定されています");
            }
            // 3. 値のnullチェック（不要なら消してもOKです）
            Objects.requireNonNull(rawValue, "キー '" + key + "' に対応する値が null です");

            temp.put(key, (T) rawValue);
        }

        // すべての検証と変換が終わった安全な状態でメインコンストラクタへ委譲！
        this(temp);
    }

    // 無限にペアを渡せるファクトリメソッド
    public static <T> EncodeData<T> of(Object... pairs) {
        return new EncodeData<>(pairs);
    }

    // 空のEncodeData用
    public static <T> EncodeData<T> of() {
        return new EncodeData<>(Map.of());
    }

    // 文字から値を取得（未登録なら null を返す：List2D.withEncode 用）
    public T decode(char symbol) {
        return this.mapping.get(symbol);
    }

    // Optional で安全に取得したい場合用
    public Optional<T> find(char symbol) {
        return Optional.ofNullable(this.mapping.get(symbol));
    }
}
