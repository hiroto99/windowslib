package com.hiroto99.windowslib.util.list;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

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

            @Override
            public boolean hasNext() {
                return flatIndex < (width * height);
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

    private List2D() {
        this.width = 0;
        this.height = 0;
    }

    public List2D(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // --- new 系（リストの作成・コピー） ---
    /**
     * 指定された {@link Map} のエントリから新しい {@link List2D} を生成します。
     *
     * <p>渡されたマップの内容は防御的にコピーされるため、生成後に元のマップを変更しても
     * この {@code List2D} には影響しません。値が {@code null} のエントリは無視されます。</p>
     *
     * <p>グリッドの境界（幅・高さ）は、登録されたすべての {@link Index2D} を包含する
     * 最小の矩形範囲として自動的に決定されます。</p>
     *
     * @param <T> 格納する要素の型
     * @param map 座標と要素のマッピング（{@code null} 不可）
     * @return 生成された新しい {@link List2D} インスタンス
     * @throws NullPointerException {@code map} が {@code null} の場合、またはキーに {@code null} が含まれる場合
     * @throws IllegalArgumentException 負の座標など、無効な {@link Index2D} が含まれている場合
     */
    public static <T> List2D<T> of(Map<Index2D, T> map) {
        Objects.requireNonNull(map, "map cannot be null");
        List2D<T> grid = new List2D<>();
        map.forEach((index, value) -> {
            Objects.requireNonNull(index, "index cannot be null");
            if (!index.isValid()) {
                throw new IllegalArgumentException("index cannot use negative coordinates in Index2D: " + index);
            }
            if (value != null) {
                grid.setWidthOfCreatingRealtime(index.x());
                grid.setHeightOfCreatingRealtime(index.y());
                grid.set(index, value);
            }
        });
        return grid;
    }

    /**
     * 指定された複数の {@link Entry}（座標と値のペア）から新しい {@link List2D} を生成します。
     *
     * <p>同じ座標を持つエントリが複数指定された場合は、後ろに指定された値で上書きされます。
     * 値が {@code null} のエントリは未登録（空マス）として扱われます。</p>
     *
     * @param <T> 格納する要素の型
     * @param entries 初期配置する座標と要素のペア（配列および各要素は {@code null} 不可）
     * @return 生成された新しい {@link List2D} インスタンス
     * @throws NullPointerException {@code entries} またはその要素に {@code null} が含まれる場合
     * @throws IllegalArgumentException 負の座標など、無効な {@link Index2D} が含まれている場合
     */
    @SafeVarargs
    public static <T> List2D<T> ofEntries(Entry<T>... entries) {
        Objects.requireNonNull(entries, "entries cannot be null");
        List2D<T> grid = new List2D<>();
        for (Entry<T> entry : entries) {
            Objects.requireNonNull(entry, "entry cannot be null");
            Objects.requireNonNull(entry.index(), "index cannot be null");
            if (!entry.index().isValid()) {
                throw new IllegalArgumentException("index cannot use negative coordinates in Index2D: " + entry.index());
            }
            if (entry.value() != null) {
                grid.setWidthOfCreatingRealtime(entry.index().x());
                grid.setHeightOfCreatingRealtime(entry.index().y());
                grid.set(entry.index(), entry.value());
            }
        }
        return grid;
    }

    /**
     * 文字列の配列（アスキーアート形式のグリッド定義）と {@link EncodeData} を組み合わせて、
     * 新しい {@link List2D} を生成します。
     *
     * <p>配列の各要素が Y 座標（行）、文字列内の各文字の位置が X 座標（列）に対応します。
     * {@code encode} に未登録の文字（空白スペース {@code ' '} など）は空マスとしてスキップされます。</p>
     *
     * <p>グリッドの高さは {@code array.length}、幅は {@code array} 内の最長文字列の長さになります。</p>
     *
     * <pre>{@code
     * // 使用例
     * List2D<Block> grid = List2D.withEncode(
     *     new String[]{
     *         "###",
     *         "#O#",
     *         "###"
     *     },
     *     EncodeData.of('#', Blocks.STONE, 'O', Blocks.DIAMOND_BLOCK)
     * );
     * }</pre>
     *
     * @param <T> 格納する要素の型
     * @param array グリッドの配置を表す文字列配列（{@code null} 不可）
     * @param encode 文字と要素の対応表を保持する {@link EncodeData}（{@code null} 不可）
     * @return デコードされた要素が配置された新しい {@link List2D} インスタンス
     * @throws NullPointerException {@code array}、{@code array} 内の要素、または {@code encode} が {@code null} の場合
     */
    public static <T> List2D<T> withEncode(String[] array, EncodeData<T> encode) {
        Objects.requireNonNull(array, "array cannot be null");
        Objects.requireNonNull(encode, "encode cannot be null");

        List2D<T> grid = new List2D<>(array[0].length(), array.length);
        for (int y = 0; y < array.length; y++) {
            String low = array[y];
            Objects.requireNonNull(low, "low cannot be null");

            for (int x = 0; x < low.length(); x++) {
                T value;
                char column = low.charAt(x);
                if (column == ' ') {
                    value = null;
                } else {
                    value = encode.decode(column);
                }
                grid.set(x, y, value);
            }
        }
        return grid;
    }

    /**
     * 新しい2次元リストのインスタンスをこの2次元リストと同じ内容で生成します。
     *
     * <p>この2次元リストのインスタンスとは独立した状態で作成されるため、</p>
     * <pre>{@code
     * newGrid = grid;
     * }</pre>
     * <p>のコードと異なり、</p>
     * <pre>{@code
     * newGrid = grid.copy();
     * }</pre>
     * <p>のコードは {@code newGrid} のリストを書き換えても {@code grid} 側のリストが変更されることはありません。</p>
     *
     * @return この2次元リストと同じデータが入った新しい {@link List2D} インスタンス
     */
    public List2D<V> copy() {
        return List2D.of(this.data);
    }

    // --- add 系（列・行の作成） ---
    /**
     * グリッドの右端（末尾）に新しい空の列を1列追加します。
     *
     * <p>この操作により、グリッドの幅（{@code width}）が 1 増加します。
     * 既存の要素の座標は変更されず、新しく追加された列の各マスは未登録（{@code null}）状態になります。</p>
     */
    public void addColumn() {
        width += 1;
    }

    /**
     * グリッドの下端（末尾）に新しい空の行を1行追加します。
     *
     * <p>この操作により、グリッドの高さ（{@code height}）が 1 増加します。
     * 既存の要素の座標は変更されず、新しく追加された行の各マスは未登録（{@code null}）状態になります。</p>
     */
    public void addRow() {
        height += 1;
    }

    // --- insert 系（列・行の挿入） ---
    /**
     * 指定された X 座標の位置に新しい空の列を1列挿入します。
     *
     * <p>{@code targetX} 以降（{@code x >= targetX}）に存在していた既存の要素は、
     * すべて右方向（X座標が {@code +1}）へ自動的にシフトします。
     * 新しく挿入された列の各マスは未登録（{@code null}）状態になり、グリッドの幅（{@code width}）が 1 増加します。</p>
     *
     * <p>{@code targetX} に現在の幅（{@code width}）を指定した場合は、{@link #addColumn()} と同様に右端へ追加されます。</p>
     *
     * @param targetX 列を挿入する X 座標（{@code 0 <= targetX <= width}）
     * @throws IndexOutOfBoundsException {@code targetX} が範囲外（{@code targetX < 0 || targetX > width}）の場合
     */
    public void insertColumn(int targetX) {
        if (targetX < 0 || targetX > this.width) {
            throw new IndexOutOfBoundsException("Column index out of bounds: " + targetX);
        }

        Map<Index2D, V> nextMap = HashMap.newHashMap(this.data.size());
        for (Map.Entry<Index2D, V> entry : this.data.entrySet()) {
            Index2D pos = entry.getKey();
            if (pos.x() >= targetX) {
                // 挿入位置より右にある要素を右にずらす
                nextMap.put(pos.right(), entry.getValue());
            } else {
                // 挿入位置より左にある要素はそのまま
                nextMap.put(pos, entry.getValue());
            }
        }

        this.data.clear();
        this.data.putAll(nextMap);
        this.width++;
    }

    /**
     * 指定された Y 座標の位置に新しい空の行を1行挿入します。
     *
     * <p>{@code targetY} 以降（{@code y >= targetY}）に存在していた既存の要素は、
     * すべて下方向（Y座標が {@code +1}）へ自動的にシフトします。
     * 新しく挿入された行の各マスは未登録（{@code null}）状態になり、グリッドの高さ（{@code height}）が 1 増加します。</p>
     *
     * <p>{@code targetY} に現在の高さ（{@code height}）を指定した場合は、{@link #addRow()} と同様に下端へ追加されます。</p>
     *
     * @param targetY 行を挿入する Y 座標（{@code 0 <= targetY <= height}）
     * @throws IndexOutOfBoundsException {@code targetY} が範囲外（{@code targetY < 0 || targetY > height}）の場合
     */
    public void insertRow(int targetY) {
        if (targetY < 0 || targetY > this.height) {
            throw new IndexOutOfBoundsException("Row index out of bounds: " + targetY);
        }

        Map<Index2D, V> nextMap = HashMap.newHashMap(this.data.size());
        for (Map.Entry<Index2D, V> entry : this.data.entrySet()) {
            Index2D pos = entry.getKey();
            if (pos.y() >= targetY) {
                // 挿入位置より下にある要素を下にずらす
                nextMap.put(pos.down(), entry.getValue());
            } else {
                // 挿入位置より上にある要素はそのまま
                nextMap.put(pos, entry.getValue());
            }
        }

        this.data.clear();
        this.data.putAll(nextMap);
        this.height++;
    }

    // --- remove 系（列・行の削除） ---
    /**
     * 指定された X 座標の列を削除し、それより右側の要素を左へ詰めます。
     *
     * <p>{@code x == targetX} に登録されていた要素はすべて破棄されます。
     * {@code targetX} より右側（{@code x > targetX}）に存在していた既存の要素は、
     * すべて左方向（X座標が {@code -1}）へ自動的にシフトし、グリッドの幅（{@code width}）が 1 減少します。</p>
     *
     * @param targetX 削除する列の X 座標（{@code 0 <= targetX < width}）
     * @throws IndexOutOfBoundsException {@code targetX} が範囲外（{@code targetX < 0 || targetX >= width}）の場合
     */
    public void removeColumn(int targetX) {
        if (targetX < 0 || targetX > this.width) {
            throw new IndexOutOfBoundsException("Column index out of bounds: " + targetX);
        }

        Map<Index2D, V> nextMap = HashMap.newHashMap(this.data.size());
        for (Map.Entry<Index2D, V> entry : this.data.entrySet()) {
            Index2D pos = entry.getKey();
            if (pos.x() > targetX) {
                // 挿入位置より右にある要素を左にずらす
                nextMap.put(pos.left(), entry.getValue());
            } else if (pos.x() < targetX) {
                // 挿入位置より左にある要素はそのまま
                nextMap.put(pos, entry.getValue());
            }
        }

        this.data.clear();
        this.data.putAll(nextMap);
        this.width--;
    }

    /**
     * 指定された Y 座標の行を削除し、それより下側の要素を上へ詰めます。
     *
     * <p>{@code y == targetY} に登録されていた要素はすべて破棄されます。
     * {@code targetY} より下側（{@code y > targetY}）に存在していた既存の要素は、
     * すべて上方向（Y座標が {@code -1}）へ自動的にシフトし、グリッドの高さ（{@code height}）が 1 減少します。</p>
     *
     * @param targetY 削除する行の Y 座標（{@code 0 <= targetY < height}）
     * @throws IndexOutOfBoundsException {@code targetY} が範囲外（{@code targetY < 0 || targetY >= height}）の場合
     */
    public void removeRow(int targetY) {
        if (targetY < 0 || targetY > this.height) {
            throw new IndexOutOfBoundsException("Row index out of bounds: " + targetY);
        }

        Map<Index2D, V> nextMap = HashMap.newHashMap(this.data.size());
        for (Map.Entry<Index2D, V> entry : this.data.entrySet()) {
            Index2D pos = entry.getKey();
            if (pos.y() > targetY) {
                // 挿入位置より下にある要素を上にずらす
                nextMap.put(pos.up(), entry.getValue());
            } else if (pos.y() < targetY) {
                // 挿入位置より上にある要素はそのまま
                nextMap.put(pos, entry.getValue());
            }
        }

        this.data.clear();
        this.data.putAll(nextMap);
        this.height--;
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

    // --- fill 系（領域操作）
    /**
     * グリッド上の全ての座標を指定された要素で埋めます。
     *
     * <p>{@code null} でも指定された要素で置き換えます。</p>
     * @param value 全ての座標に埋める値
     */
    public void fill(V value) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                set(x, y, value);
            }
        }
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
     * 指定された要素がこのグリッド内に少なくとも1つ含まれているかを判定します。
     *
     * <p>要素の比較には {@link Objects#equals(Object, Object)} が使用されます。
     * {@code null} を指定した場合は、グリッド内に未登録（空）のマスが存在するかを判定します。</p>
     *
     * @param value 検索する要素（{@code null} 可）
     * @return 指定された要素がグリッド内に存在する場合は {@code true}、それ以外は {@code false}
     */
    public boolean contains(V value) {
        return value != null && data.containsValue(value);
    }

    // --- Stream / Collection系（Streamに変換・他のリストに変換・操作） ---
    /**
     * このグリッドに含まれるすべての要素を順番に処理する {@link Stream} を返します。
     *
     * <p>走査は左上 {@code (0, 0)} から右方向へ進み、行末に達すると次の行へ移動する順序（行優先順）で行われます。
     * 要素が未登録のマスについては {@code null} がストリームに流れます。</p>
     *
     * <p>空マス（{@code null}）を除外して登録済みの要素だけを処理したい場合は、
     * {@code stream().filter(Objects::nonNull)} を使用してください。</p>
     *
     * @return グリッド内の全マスを走査する {@link Stream}
     */
    public Stream<V> stream() {
        Stream.Builder<V> builder = Stream.builder();
        for (var entry : this) {
            builder.add(entry.value());
        }
        return builder.build();
    }

    /**
     * このグリッド内のすべての座標と要素のペア（{@link Entry}）を順番に処理する {@link Stream} を返します。
     *
     * <p>走査は左上 {@code (0, 0)} から右下 {@code (width - 1, height - 1)} まで行優先順で行われます。
     * 未登録のマスについては、値に {@code null} を持つ {@link Entry} が生成されます。</p>
     *
     * @return 全マスの {@link Entry} を要素とする順次 {@link Stream}
     */
    public Stream<Entry<V>> entryStream() {
        Stream.Builder<Entry<V>> builder = Stream.builder();
        for (var entry : this) {
            builder.add(entry);
        }
        return builder.build();
    }

    /**
     * ストリームの要素を指定された幅と高さの新しい {@link List2D} に集約する {@link Collector} を返します。
     *
     * <p>ストリームから流れる要素は、左上 {@code (0, 0)} から行優先順（X座標が先、次にY座標）に配置されます。
     * 要素数が {@code width * height} に満たない場合、残りのマスは未登録（{@code null}）状態になります。</p>
     *
     * <pre>{@code
     * // 使用例：要素を変換して同じサイズの List2D に戻す
     * List2D<String> result = grid.stream()
     *     .map(Object::toString)
     *     .collect(List2D.toList2D(grid.width(), grid.height()));
     * }</pre>
     *
     * @param <V> 格納する要素の型
     * @param width 生成する {@link List2D} の幅（0以上）
     * @param height 生成する {@link List2D} の高さ（0以上）
     * @return 要素を {@link List2D} に収集する {@link Collector}
     * @throws IllegalArgumentException {@code width} または {@code height} が負の値の場合
     * @throws IndexOutOfBoundsException ストリームの要素数が {@code width * height} を超えた場合
     */
    public static <V> Collector<V, ?, List2D<V>> toList2D(int width, int height) {
        return Collector.of(
                () -> new ArrayList<V>(width * height),
                List::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                list -> List2D.fromFlatList(list, width) // ここでList2Dに変換！
        );
    }

    /**
     * 1次元の {@link List} とグリッドの幅を指定して、新しい {@link List2D} を生成します。
     *
     * <p>リスト内の要素は左上 {@code (0, 0)} から行優先順（インデックス {@code y * width + x}）に配置されます。
     * グリッドの高さはリストの要素数と幅から {@code (list.size() + width - 1) / width}（切り上げ）で自動的に算出されます。</p>
     *
     * <p>リストの要素数が {@code width} の倍数に満たない場合、最終行の余ったマスは未登録（{@code null}）状態になります。
     * また、リスト内の {@code null} 要素も未登録マスとして扱われます。</p>
     *
     * @param <V> 格納する要素の型
     * @param list 変換元の1次元リスト（{@code null} 不可、要素に {@code null} を含めることは可能）
     * @param width 生成するグリッドの横幅（1以上）
     * @return 1次元リストから構築された新しい {@link List2D} インスタンス
     * @throws NullPointerException {@code list} が {@code null} の場合
     * @throws IllegalArgumentException {@code width} が 0 以下の場合（ただし {@code list} が空かつ {@code width == 0} の場合は空のグリッドを返します）
     */
    public static <V> List2D<V> fromFlatList(List<V> list, int width) {
        Objects.requireNonNull(list, "list must not be null");
        if (list.isEmpty() && width == 0) {
            return new List2D<>(0, 0);
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive when list is non-empty: " + width);
        }

        // 要素数が幅で割り切れない場合も最後の行が欠けないよう切り上げで高さを算出
        int height = (list.size() + width - 1) / width;
        List2D<V> grid = new List2D<>(width, height);

        for (int i = 0; i < list.size(); i++) {
            V value = list.get(i);
            if (value != null) {
                Index2D pos = Index2D.fromFlatIndex(i, width);
                grid.set(pos, value);
            }
        }
        return grid;
    }

    /**
     * このグリッドの全マス（{@code width * height} 個）を1次元の {@link List} に変換して返します。
     *
     * <p>要素は左上 {@code (0, 0)} から行優先順（インデックス {@code y * width + x}）で格納されます。
     * 要素が未登録のマスには {@code null} が格納されます。</p>
     *
     * <p>返されるリストは新しい可変の {@link ArrayList} であり、元の {@link List2D} とは独立しています。
     * リストを変更してもこのグリッドには影響しません。</p>
     *
     * @return 全マスの要素を行優先順で格納した新しい {@link List}（要素数は常に {@code width * height}）
     */
    public List<V> toFlatList() {
        int total = this.width * this.height;
        List<V> result = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            Index2D pos = Index2D.fromFlatIndex(i, this.width);
            result.add(this.data.get(pos)); // 未登録マスは null が追加される
        }
        return result;
    }

    /**
     * このグリッドの全要素を1次元の {@link List} に変換して返します。
     *
     * <p>要素は左上 {@code (0, 0)} から行優先順（インデックス {@code y * width + x}）で格納されます。
     * 返されるリストは新しいコピーであるため、リストを変更しても元の {@link List2D} には影響しません。</p>
     *
     * @return 全マスの要素を行優先順で格納した新しい {@link List}（未登録マスは {@code null}）
     */
    public List<V> toList() {
        return toFlatList();
    }

    /**
     * このグリッドに登録されている座標と要素のマッピングを新しい {@link Map} として返します。
     *
     * <p>値が未登録（{@code null}）の座標は返されるマップには含まれません。
     * 返されるマップは防御的にコピーされた独立したインスタンスであるため、
     * 変更を加えても元の {@link List2D} には影響しません。</p>
     *
     * @return 登録済みの {@link Index2D} と要素のペアを保持する新しい {@link Map}
     */
    public Map<Index2D, V> toMap() {
        return data;
    }

    // --- ヘルパーメソッド ---
    private void setWidthOfCreatingRealtime(int width) {
        if (this.width < width) {
            this.width = width;
        }
    }

    private void setHeightOfCreatingRealtime(int height) {
        if (this.height < height) {
            this.height = height;
        }
    }

    private void checkBounds(Index2D index) {
        if (!index.isWithin(width, height)) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " is out of bounds for size (" + width + ", " + height + ")"
            );
        }
    }
}