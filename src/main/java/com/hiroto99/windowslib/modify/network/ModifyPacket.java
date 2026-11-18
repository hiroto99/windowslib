package com.hiroto99.windowslib.modify.network;

import com.hiroto99.windowslib.WindowsLib;
import com.hiroto99.windowslib.core.simplepacket.AutoPacket;
import com.hiroto99.windowslib.core.simplepacket.SimplePacket;
import com.hiroto99.windowslib.core.simplepacket.TargetSide;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

@AutoPacket(value = TargetSide.CLIENT, generatecodec = false)
public record ModifyPacket(List<SyncStreamBuilder.PendingUpdate<?>> updates) implements SimplePacket {
    public static final Type<ModifyPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WindowsLib.MODID, "modify_sync"));

    // 全Modifyの変数を集約する専用ビルダー
    public static final SyncStreamBuilder BUILDER = new SyncStreamBuilder();

    public ModifyPacket() {
        this(List.of());
    }

    // ★ ビルダーから自動生成された差分専用 STREAM_CODEC
    public static final StreamCodec<RegistryFriendlyByteBuf, ModifyPacket> STREAM_CODEC = new StreamCodec<>() {
        private StreamCodec<RegistryFriendlyByteBuf, List<SyncStreamBuilder.PendingUpdate<?>>> generatedCodec;

        private StreamCodec<RegistryFriendlyByteBuf, List<SyncStreamBuilder.PendingUpdate<?>>> getOrBuild() {
            if (this.generatedCodec == null) {
                this.generatedCodec = BUILDER.buildDiffStreamCodec();
            }
            return this.generatedCodec;
        }

        @Override
        public ModifyPacket decode(RegistryFriendlyByteBuf buf) {
            List<SyncStreamBuilder.PendingUpdate<?>> decodedUpdates = getOrBuild().decode(buf);
            return new ModifyPacket(decodedUpdates);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ModifyPacket packet) {
            getOrBuild().encode(buf, List.of());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StreamCodec<RegistryFriendlyByteBuf, ModifyPacket> streamCodec() {
        return STREAM_CODEC;
    }

    // ★ メインスレッド上で安全に値を反映し、コールバックを発火させる
    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            for (SyncStreamBuilder.PendingUpdate<?> update : updates()) {
                update.apply();
            }
        });
    }

    // --- 送信トリガー ---

    /**
     * 変化した変数がある場合のみ、差分を全プレイヤーへ送信する
     */
    public static void syncChangesToAll() {
        if (BUILDER.detectChanges()) {
            PacketDistributor.sendToAllPlayers(new ModifyPacket());
        }
    }

    /**
     * ログイン時など、特定プレイヤーへ全変数を一括送信する
     */
    public static void syncAllToPlayer(ServerPlayer player) {
        BUILDER.markAllDirty();
        PacketDistributor.sendToPlayer(player, new ModifyPacket());
    }
}
