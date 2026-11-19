package com.hiroto99.windowslib.util.list;

import java.util.List;

public record Index2D(int x, int y) {
    // 任意の移動
    public Index2D offset(int dx, int dy) {
        return new Index2D(this.x + dx, this.y + dy);
    }

    public Index2D add(Index2D other) {
        return new Index2D(this.x + other.x, this.y + other.y);
    }

    public Index2D subtract(Index2D other) {
        return new Index2D(this.x - other.x, this.y - other.y);
    }

    // 上下左右（1マス移動のショートカット）
    public Index2D up()         { return up(1); }
    public Index2D down()       { return down(1); }
    public Index2D left()       { return left(1); }
    public Index2D right()      { return right(1); }
    public Index2D up(int d)    { return new Index2D(x, y - 1); }
    public Index2D down(int d)  { return new Index2D(x, y + 1); }
    public Index2D left(int d)  { return new Index2D(x - 1, y); }
    public Index2D right(int d) { return new Index2D(x + 1, y); }

    // 周囲4方向・8方向を一括取得（塗りつぶしや輪郭抽出、隣接判定に便利）
    public List<Index2D> neighbors4() {
        return List.of(up(), down(), left(), right());
    }

    public List<Index2D> neighbors8() {
        return List.of(
                up(), down(), left(), right(),
                offset(-1, -1), offset(1, -1), offset(-1, 1), offset(1, 1)
        );
    }

    // 指定された幅・高さの範囲内に収まっているか
    public boolean isWithin(int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isWithin(List2D<?> list) {
        return isWithin(list.width, list.height);
    }

    // 範囲外にはみ出た座標を無理やり範囲内に収める（ドラッグ操作やカーソル移動に便利）
    public Index2D clamp(int width, int height) {
        int clampedX = Math.clamp(x, 0, width - 1);
        int clampedY = Math.clamp(y, 0, height - 1);
        return new Index2D(clampedX, clampedY);
    }

    // XとYを入れ替える（転置）
    @SuppressWarnings("SuspiciousNameCombination")
    public Index2D swap() {
        return new Index2D(y, x);
    }

    // 90度時計回り回転（width, height の枠内での座標変換）
    public Index2D rotate90(int height) {
        return new Index2D(height - 1 - this.y, this.x);
    }

    // 左右反転（ミラーリング）
    public Index2D flipHorizontal(int width) {
        return new Index2D(width - 1 - this.x, this.y);
    }

    // 上下反転
    public Index2D flipVertical(int height) {
        return new Index2D(this.x, height - 1 - this.y);
    }

    // マンハッタン距離（グリッド上の移動マス数）
    public int manhattanDistance(Index2D other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    // 1次元インデックス（フラットな配列の通し番号）との相互変換
    public int toFlatIndex(int width) {
        return y * width + x;
    }

    public static Index2D fromFlatIndex(int flatIndex, int width) {
        return new Index2D(flatIndex % width, flatIndex / width);
    }

    // GUIのピクセル座標への変換（例：スロットサイズ18pxのグリッドから画面上の描画座標へ）
    public Index2D scale(int stepX, int stepY) {
        return new Index2D(this.x * stepX, this.y * stepY);
    }
}
