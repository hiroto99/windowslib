package com.hiroto99.windowslib.modify.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SyncStreamBuilder {
    private final List<SyncEntry<?>> entries = new ArrayList<>();
    private final BitSet dirtyFlags = new BitSet();

    public record PendingUpdate<T>(SyncEntry<T> entry, T newValue) {
        public void apply() {
            this.entry.setter.accept(this.newValue);
            if (this.entry.onSyncCallback != null) {
                this.entry.onSyncCallback.accept(this.newValue);
            }
        }
    }

    /**
     * ビルダーに同期対象の変数を登録する
     */
    public <T> SyncStreamBuilder add(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Supplier<T> getter,
            Consumer<T> setter
    ) {
        return add(codec, getter, setter, null);
    }

    public <T> SyncStreamBuilder add(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Supplier<T> getter,
            Consumer<T> setter,
            Consumer<T> onClientSyncCallback
    ) {
        int index = this.entries.size();
        this.entries.add(new SyncEntry<>(index, codec, getter, setter, onClientSyncCallback));
        return this;
    }


    /**
     * 前回送信時から値が変わった変数を自動検知する
     */
    public boolean detectChanges() {
        for (SyncEntry<?> entry : this.entries) {
            if (entry.hasChanged()) {
                this.dirtyFlags.set(entry.index);
            }
        }
        return !this.dirtyFlags.isEmpty();
    }

    /**
     * すべての変数を強制的に送信対象にする（プレイヤーログイン時のフル同期用）
     */
    public void markAllDirty() {
        this.dirtyFlags.set(0, this.entries.size());
    }

    /**
     * ★ ビルダーに登録された全変数から「変化したものだけを送受信するStreamCodec」を自動生成する
     */
    public StreamCodec<RegistryFriendlyByteBuf, List<PendingUpdate<?>>> buildDiffStreamCodec() {
        return new StreamCodec<>() {
            @Override
            public List<PendingUpdate<?>> decode(RegistryFriendlyByteBuf buf) {
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                List<PendingUpdate<?>> updates = new ArrayList<>(count);

                for (int i = 0; i < count; i++) {
                    int index = ByteBufCodecs.VAR_INT.decode(buf);
                    if (index >= 0 && index < entries.size()) {
                        // Nettyスレッドではバイト列の読み取りだけを行う
                        updates.add(entries.get(index).decodePending(buf));
                    }
                }
                return updates;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, List<PendingUpdate<?>> unused) {
                int count = dirtyFlags.cardinality();
                ByteBufCodecs.VAR_INT.encode(buf, count);

                for (int i = dirtyFlags.nextSetBit(0); i >= 0; i = dirtyFlags.nextSetBit(i + 1)) {
                    ByteBufCodecs.VAR_INT.encode(buf, i);
                    entries.get(i).writeAndUpdateSnapshot(buf);
                }
                dirtyFlags.clear();
            }
        };
    }

    // 個別の変数と前回送信値のスナップショットを保持する内部クラス
    private static class SyncEntry<T> {
        final int index;
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;
        final Supplier<T> getter;
        final Consumer<T> setter;
        final Consumer<T> onSyncCallback;
        T lastSentSnapshot;

        SyncEntry(
                int index,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                Supplier<T> getter,
                Consumer<T> setter,
                Consumer<T> onSyncCallback
        ) {
            this.index = index;
            this.codec = codec;
            this.getter = getter;
            this.setter = setter;
            this.onSyncCallback = onSyncCallback;
            this.lastSentSnapshot = getter.get();
        }

        /**
         * 前回送信時の値と現在の値を比較し、変化があれば true を返す
         */
        boolean hasChanged() {
            return !Objects.equals(this.lastSentSnapshot, this.getter.get());
        }

        /**
         * サーバー側：現在の値をバッファへ書き込み、スナップショットを更新する
         */
        void writeAndUpdateSnapshot(RegistryFriendlyByteBuf buf) {
            T current = this.getter.get();
            this.codec.encode(buf, current);
            this.lastSentSnapshot = current;
        }

        /**
         * クライアント側（Nettyスレッド）：バッファから値を読み取り、メインスレッド適用用のPendingUpdateを返す
         */
        PendingUpdate<T> decodePending(RegistryFriendlyByteBuf buf) {
            T received = this.codec.decode(buf);
            return new PendingUpdate<>(this, received);
        }
    }
}
