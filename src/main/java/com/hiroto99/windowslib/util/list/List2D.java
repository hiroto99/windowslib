package com.hiroto99.windowslib.util.list;

import java.util.*;
import java.util.function.Predicate;

public class List2D<V> implements Iterable<List2D.Entry<V>> {
    // 1. イテレータが返す要素の型（座標と値のペア）
    public record Entry<V>(Index2D index, V value) {
        // x(), y() へのショートカットがあるとさらに便利
        public int x() { return index.x(); }
        public int y() { return index.y(); }
    }

    private int width;
    private int height;
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

    // --- get 系（値の取得） ---
    /**
     * 指定された2次元インデックスに対応する要素を取得します。
     *
     * <p>指定された座標がグリッドの範囲外である場合は {@link IndexOutOfBoundsException} をスローします。
     * 指定された座標に要素が登録されていない（空である）場合は {@code null} を返します。</p>
     *
     * null安全な取得が必要な場合は {@link #getOptional(Index2D)} または
     * {@link #getOrDefault(Index2D, Object)} の使用を検討してください。</p>
     *
     * @param index 取得対象の位置を表す {@link Index2D}（{@code null} 不可）
     * @return 指定された座標に格納されている要素。要素が存在しない場合は {@code null}
     * @throws NullPointerException {@code index} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code index} がグリッドの範囲外の場合
     */
    private V get(Index2D index) {
        Objects.requireNonNull(index, "index cannot be null");
        checkBounds(index); // 範囲外なら分かりやすい例外を投げる
        return data.get(index); // マップに未登録（空きマス）なら null を返す
    }

    /**
     * 指定された X, Y 座標に対応する要素を取得します。
     *
     * <p>内部的には {@code get(new Index2D(x, y))} と同等です。
     * 指定された座標に要素が登録されていない場合は {@code null} を返します。</p>
     *
     * @param x X座標（列インデックス）
     * @param y Y座標（行インデックス）
     * @return 指定された座標に格納されている要素。要素が存在しない場合は {@code null}
     */
    private V get(int x, int y) {
        return get(new Index2D(x, y));
    }

    /**
     * 指定された2次元インデックスに対応する要素を取得します。
     *
     * <p>指定された座標に要素が登録されていないか指定された座標がグリッドの範囲外の場合は {@code defaultValue} の値を返します。
     *
     * @param index 取得対象の位置を表す {@link Index2D}
     * @param defaultValue 指定された座標に格納されている要素が存在しないか指定された座標がグリッドの範囲外の場合に返す値
     * @return 指定された座標に格納されている要素。要素が存在しないか指定された座標がグリッドの範囲外の場合は {@code defaultValue}
     * @throws NullPointerException {@code index} が {@code null} の場合
     */
    public V getOrDefault(Index2D index, V defaultValue) {
        Objects.requireNonNull(index, "index cannot be null");
        switch (index) {
            case Index2D(_, _) when !isInBounds(index) -> {
                return defaultValue;
            }
            case Index2D(_, _) when !data.containsKey(index) -> {
                return defaultValue;
            }
            default -> {
                return get(index);
            }
        }
    }

    public V getOrDefault(int x, int y, V defaultValue) {
        return getOrDefault(new Index2D(x, y), defaultValue);
    }

    /** 範囲外アクセスでも例外を投げず安全に {@link Optional} で返す版（隣接マスの探索に便利）*/
    public Optional<V> getOptional(Index2D index) {
        if (index == null || !index.isWithin(width, height)) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(index));
    }

    public Optional<V> getOptional(int x, int y) {
        return getOptional(new Index2D(x, y));
    }

    // --- set 系（値の設定・上書き） ---
    /**
     * 指定座標に値をセットする。
     * {@code null} を渡した場合は {@link clear} と同じ動作になり、マップの {@link size} を正確に保つ。
     */
    @SuppressWarnings("JavadocReference")
    public void set(Index2D index, V value) {
        Objects.requireNonNull(index, "index cannot be null");
        checkBounds(index);
        if (value == null) {
            clear(index);
            return;
        }
        data.compute(index, (_, _) -> value);
    }

    // 座標直指定の set(int x, int y, V value)：Index2D 版に委譲する
    public void set(int x, int y, V value) {
        set(new Index2D(x, y), value);
    }

    /** 範囲内のときだけ値をセットし、成功したかどうかを boolean で返す */
    public boolean trySet(Index2D index, V value) {
        try {
            Objects.requireNonNull(index, "index cannot be null");
            checkBounds(index);
            data.compute(index, (_, _) -> value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean trySet(int x, int y, V value) {
        return trySet(new Index2D(x, y), value);
    }

    // --- 枠（キャンバス・グリッド）の寸法系 ---
    /** 横幅（列数 / X軸の長さ） */
    public int width() {
        return this.width;
    }

    /** 高さ（行数 / Y軸の長さ） */
    public int height() {
        return this.height;
    }

    /** 枠全体の最大マス数（width × height） */
    public int area() { // または capacity() / gridSize()
        return this.width * this.height;
    }

    /** 枠のサイズを Index2D として取得する（境界判定やサイズ比較に便利） */
    public Index2D dimensions() {
        return new Index2D(this.width, this.height);
    }

    /** 指定した座標 (x, y) がグリッドの縦横サイズの範囲内に収まっているかを判定する */
    private boolean isInBounds(Index2D index) {
        return index.isWithin(width, height);
    }

    private boolean isInBounds(int x, int y) {
        return isInBounds(new Index2D(x, y));
    }

    // --- 実際に値が入っている要素数（マップとしてのサイズ）系 ---
    /** 実際に値がセットされているセルの個数（clearされたマスや未設定マスは含まない） */
    public int size() {
        return this.data.size();
    }

    /** 値が1つもセットされていないか（すべて空きマスか） */
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    /** すべてのマスに値が埋まっているか（満杯か） */
    public boolean isFull() {
        return this.data.size() == area();
    }

    /** 指定した座標に値が存在する（nullでない）か */
    public boolean hasValue(Index2D index) {
        return index != null && index.isWithin(width, height) && this.data.containsKey(index);
    }

    public boolean hasValue(int x, int y) {
        return hasValue(new Index2D(x, y));
    }

    // --- clear 系（特定の点または全体のクリア） ---
    public void clear(Index2D index) {
        Objects.requireNonNull(index, "index cannot be null");
        checkBounds(index);
        data.remove(index);
    }

    public void clear(int x, int y) {
        clear(new Index2D(x, y));
    }

    /** すべてのセルの値を空にする（枠の大きさは維持） */
    public void clearAll() {
        data.clear();
    }

    // --- indexOf 系（値から Index2D 座標を逆引き） ---
    /**
     * 指定した値と一致する最初の座標（左上から右方向スキャン順）を {@link Index2D} で返す。
     * 見つからない場合は {@code null}（または {@link Optional} 版の {@link findIndex} を利用）を返す。
     */
    public Index2D indexOf(V target) {
        if (target == null) {
            // null（空きマス）を探す場合は全マスを走査
            for (var entry : this) {
                if (entry instanceof Entry(Index2D pos, var val) &&
                        val == null) return pos;
            }
            return null;
        }

        // 値がある場合は、サイズが大きいほど高速な走査順検索
        if (data.isEmpty()) return null;

        for (var entry : this) {
            if (entry instanceof Entry(Index2D pos, var val) &&
                    Objects.equals(val, target)) return pos;
        }
        return null;
    }

    /**
     * 見つからない可能性を安全に扱いたい場合の {@link Optional} 版
     */
    public Optional<Index2D> findIndex(V target) {
        return Optional.ofNullable(indexOf(target));
    }

    /**
     * 条件（{@code Predicate}）に一致する最初の座標を探す（例：特定の透明度や条件のアイテム検索）
     *
     * @see Predicate
     */
    public Optional<Index2D> findIndexIf(Predicate<V> condition) {
        Objects.requireNonNull(condition);
        for (var entry : this) {
            if (entry instanceof Entry(Index2D pos, var val) && val != null && condition.test(val)) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    /**
     * 特定の値が含まれているか（{@code indexOf != null} のショートカット）
     */
    public boolean contains(V target) {
        return target != null && data.containsValue(target);
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