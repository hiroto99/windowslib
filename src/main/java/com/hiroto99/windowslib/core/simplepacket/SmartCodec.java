package com.hiroto99.windowslib.core.simplepacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SmartCodec {
    StreamCodec<ByteBuf, Integer> SMART_INT = new StreamCodec<>() {
        // boolがtrueの場合はVAR_INTで通信、falseの場合はINTで通信
        public Integer decode(ByteBuf input) {
            boolean flag = input.readBoolean();
            if (flag) {
                return VarInt.read(input);
            } else {
                return input.readInt();
            }
        }

        public void encode(ByteBuf output, Integer value) {
            if (value >= 0 && value <= 268435455) {
                output.writeBoolean(true);
                VarInt.write(output, value);
            } else {
                output.writeBoolean(false);
                output.writeInt(value);
            }
        }
    };

    StreamCodec<ByteBuf, Long> SMART_LONG = new StreamCodec<>() {
        // boolがtrueの場合はVAR_LONGで通信、falseの場合はLONGで通信
        public Long decode(ByteBuf input) {
            boolean flag = input.readBoolean();
            if (flag) {
                return VarLong.read(input);
            } else {
                return input.readLong();
            }
        }

        public void encode(ByteBuf output, Long value) {
            if (value >= 0L && value <= 72057594037927935L) {
                output.writeBoolean(true);
                VarLong.write(output, value);
            } else {
                output.writeBoolean(false);
                output.writeLong(value);
            }
        }
    };

    static <B extends ByteBuf, V> StreamCodec<B, List<V>> makeCodecList(StreamCodec<B, V> streamCodec) {
        return makeCodecList(streamCodec, 256); // デフォルトの上限値を設定しておくのも便利です
    }

    static <B extends ByteBuf, V> StreamCodec<B, List<V>> makeCodecList(final StreamCodec<B, V> streamCodec, final int maxSize) {
        return ByteBufCodecs.collection(
                size -> new ArrayList<>(size), // ラムダ式で初期容量を受け取るコンストラクタを明示
                streamCodec,
                maxSize
        );
    }

    static <B extends ByteBuf, K, V> StreamCodec<B, Map<K, V>> makeMapCodec(StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec) {
        return makeMapCodec(keyCodec, valueCodec, 256); // デフォルトの上限値を設定しておくのも便利です
    }

    static <B extends ByteBuf, K, V> StreamCodec<B, Map<K, V>> makeMapCodec(final StreamCodec<? super B, K> keyCodec, final StreamCodec<? super B, V> valueCodec, final int maxSize) {
        return ByteBufCodecs.map(
                size -> new HashMap<>(size), // ラムダ式で初期容量を受け取るコンストラクタを明示
                keyCodec,
                valueCodec,
                maxSize
        );
    }
}
