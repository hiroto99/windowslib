package com.hiroto99.windowslib.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public class CodecUtils {
    public static <B extends ByteBuf, V> StreamCodec<B, List<V>> makeCodecList(StreamCodec<B, V> streamCodec) {
        return makeCodecList(streamCodec, 256); // デフォルトの上限値を設定しておくのも便利です
    }

    public static <B extends ByteBuf, V> StreamCodec<B, List<V>> makeCodecList(final StreamCodec<B, V> streamCodec, final int maxSize) {
        return ByteBufCodecs.collection(
                size -> new ArrayList<>(size), // ラムダ式で初期容量を受け取るコンストラクタを明示
                streamCodec,
                maxSize
        );
    }
}
