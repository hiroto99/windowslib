package com.hiroto99.windowslib.util.list;

import java.util.*;
import java.util.function.Predicate;

/**
 * 2次元リストの型で、 {@code List<List<V>>} より使いやすく2次元リストを扱えます。
 *
 * @param <V> この2次元リストで取り扱う値
 * @see Index2D
 * @see List
 */
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
     * 指定された座標が {@code null} である場合は {@link NullPointerException} をスローします。
     * 指定された座標に要素が登録されていない（空である）場合は {@code null} を返します。</p>
     *
     * <p>{@code null} 安全な取得が必要な場合は {@link #getOptional(Index2D)} または
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
     * 指定された {@code X, Y} 座標に対応する要素を取得します。
     *
     * <p>指定された座標がグリッドの範囲外である場合は {@link IndexOutOfBoundsException} をスローします。
     * 指定された座標が {@code null} である場合は {@link NullPointerException} をスローします。
     * 指定された座標に要素が登録されていない（空である）場合は {@code null} を返します。</p>
     *
     * <p>{@code null} 安全な取得が必要な場合は {@link #getOptional(Index2D)} または
     * {@link #getOrDefault(Index2D, V)} の使用を検討してください。</p>
     *
     * @param x 取得対象のX座標（列インデックス）
     * @param y 取得対象のY座標（行インデックス）
     * @return 指定された座標に格納されている要素。要素が存在しない場合は {@code null}
     * @throws NullPointerException {@code index} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code index} がグリッドの範囲外の場合
     */
    private V get(int x, int y) {
        return get(new Index2D(x, y));
    }

    /**
     * 指定された2次元インデックスに対応する要素を取得します。
     *
     * <p>指定された座標に要素が登録されていないか、指定された座標が {@code null} かグリッドの範囲外の場合は {@code defaultValue} を返します。
     * {@link get(Index2D)} よりも {@code null} 安全です。</p>
     *
     * @param index 取得対象の位置を表す {@link Index2D}
     * @param defaultValue 指定された座標に格納されている要素が存在しないか、指定された座標が {@code null} かグリッドの範囲外の場合に返す値
     * @return 指定された座標に格納されている要素。要素が存在しないか、指定された座標が {@code null} かグリッドの範囲外の場合は {@code defaultValue}
     */
    public V getOrDefault(Index2D index, V defaultValue) {
        switch (index) {
            case null -> {
                return defaultValue;
            }
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

    /**
     * 指定された {@code X, Y} 座標に対応する要素を取得します。
     *
     * <p>指定された座標に要素が登録されていないか、指定された座標が {@code null} かグリッドの範囲外の場合は {@code defaultValue} を返します。
     * {@link get(int, int)} よりも {@code null} 安全です。</p>
     *
     * @param x 取得対象のX座標（列インデックス）
     * @param y 取得対象のY座標（行インデックス）
     * @param defaultValue 指定された座標に格納されている要素が存在しないか、指定された座標が {@code null} かグリッドの範囲外の場合に返す値
     * @return 指定された座標に格納されている要素。要素が存在しないか、指定された座標が {@code null} かグリッドの範囲外の場合は {@code defaultValue}
     */
    public V getOrDefault(int x, int y, V defaultValue) {
        return getOrDefault(new Index2D(x, y), defaultValue);
    }

    /**
     * 指定された2次元インデックスに対応する要素を {@code Optional} として取得します。
     *
     * <p>指定された座標に要素が登録されていないか、指定された座標が {@code null} かグリッドの範囲外の場合は {@code Optional.empty()} を返します。
     * {@link get(Index2D)} よりも {@code null} 安全です。</p>
     *
     * @param index 取得対象の位置を表す {@link Index2D}
     * @see Optional
     * @return 指定された座標に格納されている要素の {@code Optional} 。要素が存在しないか、指定された座標が {@code null} かグリッドの範囲外の場合は {@code Optional.empty()}
     */
    public Optional<V> getOptional(Index2D index) {
        if (index == null || !index.isWithin(width, height)) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(index));
    }

    /**
     * 指定された {@code X, Y} 座標に対応する要素を {@code Optional} として取得します。
     *
     * <p>指定された座標に要素が登録されていないか、指定された座標が {@code null} かグリッドの範囲外の場合は {@code Optional.empty()} を返します。
     * {@link get(int, int)} よりも {@code null} 安全です。</p>
     *
     * @param x 取得対象のX座標（列インデックス）
     * @param y 取得対象のY座標（行インデックス）
     * @see Optional
     * @return 指定された座標に格納されている要素の {@code Optional} 。要素が存在しないか、指定された座標が {@code null} かグリッドの範囲外の場合は {@code Optional.empty()}
     */
    public Optional<V> getOptional(int x, int y) {
        return getOptional(new Index2D(x, y));
    }

    // --- set 系（値の設定・上書き） ---
    /**
     * 指定された2次元インデックスに指定された要素を設定します。
     *
     * <p>指定された座標がグリッドの範囲外である場合は {@link IndexOutOfBoundsException} をスローします。
     * 指定された座標が {@code null} である場合は {@link NullPointerException} をスローします。</p>
     *
     * <p>指定された要素が {@code null} の場合は {@link clear(Index2D)} と同じ動作になり、マップの {@link size} を正確に保ちます。
     * 指定された座標に既に要素が入っている場合はその要素が指定された要素に書き換えられます。</p>
     *
     * @param index 設定対象の位置を表す {@link Index2D}
     * @param value 設定対象の位置に設定する値
     * @throws NullPointerException {@code index} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code index} がグリッドの範囲外の場合
     */
    public void set(Index2D index, V value) {
        Objects.requireNonNull(index, "index cannot be null");
        checkBounds(index);
        if (value == null) {
            clear(index);
            return;
        }
        data.compute(index, (_, _) -> value);
    }

    /**
     * 指定された {@code X, Y} 座標に指定された要素を設定します。
     *
     * <p>指定された座標がグリッドの範囲外である場合は {@link IndexOutOfBoundsException} をスローします。
     * 指定された座標が {@code null} である場合は {@link NullPointerException} をスローします。</p>
     *
     * <p>指定された要素が {@code null} の場合は {@link clear(int, int)} と同じ動作になり、マップの {@link size} を正確に保ちます。
     * 指定された座標に既に要素が入っている場合はその要素が指定された要素に書き換えられます。</p>
     *
     * @param x 設定対象のX座標（列インデックス）
     * @param y 設定対象のY座標（行インデックス）
     * @param value 設定対象の位置に設定する値
     * @throws NullPointerException {@code index} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code index} がグリッドの範囲外の場合
     */
    public void set(int x, int y, V value) {
        set(new Index2D(x, y), value);
    }

    /**
     * 指定された2次元インデックスに指定された要素を設定し、
     * 成功した場合（指定された座標に既に指定された要素が入っている場合も含む）は {@code true} を、何らかのエラーがスローされて失敗した場合は {@code false} を返します。
     *
     * <p>指定された要素が {@code null} の場合は {@link clear(Index2D)} と同じ動作になり、マップの {@link size} を正確に保ちます。
     * 指定された座標に既に要素が入っている場合はその要素が指定された要素に書き換えられます。</p>
     *
     * @param index 設定対象の位置を表す {@link Index2D}
     * @param value 設定対象の位置に設定する値
     * @see Boolean
     * @return 処理が成功したかを表す {@code boolean} 。エラーがスローされた場合は {@code false} 、指定された座標に既に指定された要素が入っている場合も {@code true}
     */
    public boolean trySet(Index2D index, V value) {
        try {
            Objects.requireNonNull(index, "index cannot be null");
            checkBounds(index);
            if (value == null) {
                clear(index);
                return true;
            }
            data.compute(index, (_, _) -> value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 指定された {@code X, Y} 座標に指定された要素を設定し、
     * 成功した場合（指定された座標に既に指定された要素が入っている場合も含む）は {@code true} を、何らかのエラーがスローされて失敗した場合は {@code false} を返します。
     *
     * <p>指定された要素が {@code null} の場合は {@link clear(int, int)} と同じ動作になり、マップの {@link size} を正確に保ちます。
     * 指定された座標に既に要素が入っている場合はその要素が指定された要素に書き換えられます。</p>
     *
     * @param x 設定対象のX座標（列インデックス）
     * @param y 設定対象のY座標（行インデックス）
     * @param value 設定対象の位置に設定する値
     * @see Boolean
     * @return 処理が成功したかを表す {@code boolean} 。エラーがスローされた場合は {@code false} 、指定された座標に既に指定された要素が入っている場合も {@code true}
     */
    public boolean trySet(int x, int y, V value) {
        return trySet(new Index2D(x, y), value);
    }

    // --- 枠（キャンバス・グリッド）の寸法系 ---
    /** @return 横幅（列数） */
    public int width() {
        return this.width;
    }

    /** @return 高さ（行数） */
    public int height() {
        return this.height;
    }

    /** @return 枠全体の最大マス数（width × height） */
    public int area() { // または capacity() / gridSize()
        return this.width * this.height;
    }

    /** 枠のサイズを {@link Index2D} として取得します。
     * 境界判定やサイズ比較に使うと便利です。
     *
     * @return 枠全体の最大マス数の {@link Index2D}
     */
    public Index2D dimensions() {
        return new Index2D(this.width, this.height);
    }

    /**
     * 指定された2次元インデックスがグリッドの縦横サイズの範囲内に収まっているかを判定します。
     *
     * @param index 判定対象の位置を表す {@link Index2D}
     * @return グリッドの縦横サイズの範囲内に収まっているかの {@code boolean}
     */
    private boolean isInBounds(Index2D index) {
        return index.isWithin(width, height);
    }

    /**
     * 指定された {@code X, Y} 座標がグリッドの縦横サイズの範囲内に収まっているかを判定します。
     *
     * @param x 判定対象のX座標（列インデックス）
     * @param y 判定対象のY座標（行インデックス）
     * @return グリッドの縦横サイズの範囲内に収まっているかの {@code boolean}
     */
    private boolean isInBounds(int x, int y) {
        return isInBounds(new Index2D(x, y));
    }

    // --- 実際に値が入っている要素数（マップとしてのサイズ）系 ---
    /** @return 実際に値がセットされているセルの個数（clearされたマスや未設定マスは含まない） */
    public int size() {
        return this.data.size();
    }

    /** @return 値が1つもセットされていないか（すべて空きマスか） */
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    /** @return すべてのマスに値が埋まっているか（満杯か） */
    public boolean isFull() {
        return this.data.size() == area();
    }

    /**
     * 指定された2次元インデックスに要素が存在しているかを判定します。
     *
     * @param index 判定対象の位置を表す {@link Index2D}
     * @return 指定した座標に値が存在する（nullでない）か
     */
    public boolean hasValue(Index2D index) {
        return index != null && index.isWithin(width, height) && this.data.containsKey(index);
    }

    /**
     * 指定された {@code X, Y} 座標に要素が存在しているかを判定します。
     *
     * @param x 判定対象のX座標（列インデックス）
     * @param y 判定対象のY座標（行インデックス）
     * @return 指定した座標に値が存在する（nullでない）か
     */
    public boolean hasValue(int x, int y) {
        return hasValue(new Index2D(x, y));
    }

    // --- clear 系（特定の点または全体のクリア） ---
    /**
     * 指定された2次元インデックスに対応している要素をリセットして {@code null} にします。
     *
     * <p>指定された座標がグリッドの範囲外である場合は {@link IndexOutOfBoundsException} をスローします。
     * 指定された座標が {@code null} である場合は {@link NullPointerException} をスローします。</p>
     *
     * @param index リセットして {@code null} にする対象の位置を表す {@link Index2D}
     * @throws NullPointerException {@code index} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code index} がグリッドの範囲外の場合
     */
    public void clear(Index2D index) {
        Objects.requireNonNull(index, "index cannot be null");
        checkBounds(index);
        data.remove(index);
    }

    /**
     * 指定された {@code X, Y} 座標に対応している要素をリセットして {@code null} にします。
     *
     * <p>指定された座標がグリッドの範囲外である場合は {@link IndexOutOfBoundsException} をスローします。
     * 指定された座標が {@code null} である場合は {@link NullPointerException} をスローします。</p>
     *
     * @param x 判定対象のX座標（列インデックス）
     * @param y 判定対象のY座標（行インデックス）
     * @throws NullPointerException {@code index} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code index} がグリッドの範囲外の場合
     */
    public void clear(int x, int y) {
        clear(new Index2D(x, y));
    }

    /**
     * すべてのセルの値を空にします。
     *
     * <p>全ての値が {@code null} に変換されますが削除されることはなく枠の大きさは維持されます。</p>
     */
    public void clearAll() {
        data.clear();
    }

    // --- indexOf 系（値から Index2D 座標を逆引き） ---
    /**
     * 指定した値と一致する最初の座標（左上から右方向スキャン順）を {@link Index2D} で返します。
     * 見つからない場合は {@code null}（または {@link Optional} 版の {@link findIndex} を利用）を返します。
     *
     * @param target 取得対象として指定する値
     * @return {@code target} で指定した値と一致する最初の座標の {@link Index2D} 。見つからない場合は {@code null}
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
     * 指定した値と一致する最初の座標（左上から右方向スキャン順）を {@code Optional(Index2D)} で返します。
     * 見つからない可能性を安全に扱いたい場合におすすめです。
     *
     * @param target 取得対象として指定する値
     * @see Optional
     * @see Index2D
     * @return {@code target} で指定した値と一致する最初の座標の {@code Optional(Index2D)}
     */
    public Optional<Index2D> findIndex(V target) {
        return Optional.ofNullable(indexOf(target));
    }

    /**
     * 指定した条件と一致する最初の座標を探し、 {@link Index2D} で返します。
     * 特定の透明度や条件のアイテム検索の検索に使えます。
     *
     * @param condition 検索対象として指定する条件
     * @see Predicate
     * @return {@code condition} で指定した条件と一致する最初の座標の {@code Optional(Index2D)}
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
     * 指定した値がこの2次元リストに含まれているかを取得します。
     *
     * @implNote
     * <p>次のコードの代わりとして動作します：
     * <pre>{@code
     *     indexOf(target) != null
     * }</pre>
     * @param target 取得対象として指定する値
     * @see indexOf(V)
     * @return {@code target} で指定した値がこの2次元リストに含まれているか
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