package com.hiroto99.windowslib.core.simplepacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;

public interface SmartCodec {
    StreamCodec<ByteBuf, Integer> SMART_INT = new StreamCodec<ByteBuf, Integer>() {
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
}
