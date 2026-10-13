package com.hiroto99.windowslib.core.simplepacket;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface SimplePacket extends CustomPacketPayload {
    // 各パケットで共通のハンドリング処理を強制、またはオプション化
    void handle(IPayloadContext context);
}