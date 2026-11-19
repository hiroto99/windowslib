package com.hiroto99.windowslib.util.list;

import java.util.*;

public class List2D<V> implements Iterable<List2D.Entry<V>> {
    // 1. イテレータが返す要素の型（座標と値のペア）
    public record Entry<V>(Index2D index, V value) {
        // x(), y() へのショートカットがあるとさらに便利
        public int x() { return index.x(); }
        public int y() { return index.y(); }
    }

    int width;
    int height;
    private final Map<Index2D, V> data = new HashMap<>();

    // 2. Iterable の実装（拡張for文が使えるようになる）
    @Override
    public Iterator<Entry<V>> iterator() {
        return new Iterator<>() {
            private int flatIndex = 0;
            private final int total = width * height;

            @Override
            public boolean hasNext() {
                return flatIndex < total;
            }

            @Override
            public Entry<V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                // 先ほど紹介した fromFlatIndex で (x, y) を一発計算
                Index2D pos = Index2D.fromFlatIndex(flatIndex++, width);
                return new Entry<>(pos, get(pos));
            }
        };
    }

    // 1. 基本の get(Index2D)：これをすべての基準にする
    private V get(Index2D index) {
        Objects.requireNonNull(index, "index cannot be null");
        checkBounds(index); // 範囲外なら分かりやすい例外を投げる
        return data.get(index); // マップに未登録（空きマス）なら null を返す
    }

    // 2. 座標直指定の get(int x, int y)：Index2D 版に委譲する
    private V get(int x, int y) {
        return get(new Index2D(x, y));
    }

    // 3. 未登録（null）のときにデフォルト値を返す版（テクスチャの透明色や空スロットに超便利）
    public V getOrDefault(Index2D index, V defaultValue) {
        var value = get(index);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public V getOrDefault(int x, int y, V defaultValue) {
        return getOrDefault(new Index2D(x, y), defaultValue);
    }

    // 4. 範囲外アクセスでも例外を投げず安全に Optional で返す版（隣接マスの探索に便利）
    public Optional<V> getOptional(Index2D index) {
        if (index != null || !index.isWithin(width, height)) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(index));
    }

    public Optional<V> getOptional(int x, int y) {
        return getOptional(new Index2D(x, y));
    }

    // 境界チェック用ヘルパー
    private void checkBounds(Index2D index) {
        if (!index.isWithin(width, height)) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " is out of bounds for size (" + width + ", " + height + ")"
            );
        }
    }
}