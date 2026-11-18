package com.hiroto99.windowslib.modify.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.lang.String;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SyncStreamBuilder {
    private final Map<String, SyncEntry<?>> entryMap = new LinkedHashMap<>();
    private final List<SyncEntry<?>> indexedEntries = new ArrayList<>();
    private final BitSet dirtyFlags = new BitSet();

    public record PendingUpdate<T>(SyncEntry<T> entry, T newValue) {
        public void apply() {
            if (this.entry.setter != null) {
                this.entry.setter.accept(this.newValue);
            }
            if (this.entry.onSyncCallback != null) {
                this.entry.onSyncCallback.accept(this.newValue);
            }
        }
    }

    /**
     * 【サーバー側（Modify本体）用】同期したい変数の読み取り（Getter）だけを登録する
     */
    public <T> SyncStreamBuilder bindServer(
            String key,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Supplier<T> getter
    ) {
        SyncEntry<T> entry = getOrCreateEntry(key, codec);
        entry.getter = getter;
        entry.lastSentSnapshot = getter.get();
        return this;
    }
    /**
     * 【クライアント側（ClientModify）用】受信した値の反映先（Setter）だけを登録する
     */
    public <T> SyncStreamBuilder bindClient(
            String key,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Consumer<T> setter
    ) {
        return bindClient(key, codec, setter, null);
    }

    public <T> SyncStreamBuilder bindClient(
            String key,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Consumer<T> setter,
            Consumer<T> onSyncCallback
    ) {
        SyncEntry<T> entry = getOrCreateEntry(key, codec);
        entry.setter = setter;
        entry.onSyncCallback = onSyncCallback;
        return this;
    }

    @SuppressWarnings("unchecked")
    private <T> SyncEntry<T> getOrCreateEntry(String key, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return (SyncEntry<T>) this.entryMap.computeIfAbsent(key, k -> {
            int index = this.indexedEntries.size();
            SyncEntry<T> newEntry = new SyncEntry<>(index, k, codec);
            this.indexedEntries.add(newEntry);
            return newEntry;
        });
    }

    /**
     * 前回送信時から値が変わった変数を自動検知する
     */
    public boolean detectChanges() {
        for (SyncEntry<?> entry : this.indexedEntries) {
            if (entry.getter != null && entry.hasChanged()) {
                this.dirtyFlags.set(entry.index);
            }
        }
        return !this.dirtyFlags.isEmpty();
    }

    /**
     * すべての変数を強制的に送信対象にする（プレイヤーログイン時のフル同期用）
     */
    public void markAllDirty() {
        for (SyncEntry<?> entry : this.indexedEntries) {
            if (entry.getter != null) {
                this.dirtyFlags.set(entry.index);
            }
        }
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
                    String key = ByteBufCodecs.STRING_UTF8.decode(buf);
                    SyncEntry<?> entry = entryMap.get(key);
                    if (entry != null) {
                        updates.add(entry.decodePending(buf));
                    } else {
                        throw new IllegalStateException("Unknown sync key received on client: " + key);
                    }
                }
                return updates;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, List<PendingUpdate<?>> unused) {
                int count = dirtyFlags.cardinality();
                ByteBufCodecs.VAR_INT.encode(buf, count);

                for (int i = dirtyFlags.nextSetBit(0); i >= 0; i = dirtyFlags.nextSetBit(i + 1)) {
                    SyncEntry<?> entry = indexedEntries.get(i);
                    ByteBufCodecs.STRING_UTF8.encode(buf, entry.key);
                    entry.writeAndUpdateSnapshot(buf);
                }
                dirtyFlags.clear();
            }
        };
    }

    // プレフィックス自動付与ヘルパー（システム側でModifyIDを付ける用）
    public SyncStreamBuilder withPrefix(String prefix) {
        SyncStreamBuilder parent = this;
        return new SyncStreamBuilder() {
            @Override
            public <T> SyncStreamBuilder bindServer(String key, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Supplier<T> getter) {
                parent.bindServer(prefix + "/" + key, codec, getter);
                return this;
            }

            @Override
            public <T> SyncStreamBuilder bindClient(String key, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> setter, Consumer<T> onSyncCallback) {
                parent.bindClient(prefix + "/" + key, codec, setter, onSyncCallback);
                return this;
            }
        };
    }

    // 個別の変数と前回送信値のスナップショットを保持する内部クラス
    private static class SyncEntry<T> {
        final int index;
        final String key;
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;
        Supplier<T> getter;
        Consumer<T> setter;
        Consumer<T> onSyncCallback;
        T lastSentSnapshot;

        SyncEntry(
                int index,
                String key,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec
        ) {
            this.index = index;
            this.key = key;
            this.codec = codec;
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
