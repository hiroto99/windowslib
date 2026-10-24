package com.hiroto99.windowslib.core.simplegui;

import com.hiroto99.windowslib.WindowsLib;
import com.hiroto99.windowslib.core.simplepacket.AutoPacket;
import com.hiroto99.windowslib.core.simplepacket.SimplePacket;
import com.hiroto99.windowslib.core.simplepacket.TargetSide;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@AutoPacket(TargetSide.SERVER)
public record SwitchTabPacket(int tabID) implements SimplePacket {
    public static final Type<SwitchTabPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WindowsLib.MODID, "switch_tab_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchTabPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SwitchTabPacket::tabID,
            SwitchTabPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        // クライアントのメインスレッドで安全に処理を実行
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof BaseMenu menu) {
                menu.setActiveTab(this.tabID);
            }
        });
    }
}
