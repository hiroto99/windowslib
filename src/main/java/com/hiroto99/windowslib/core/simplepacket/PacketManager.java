package com.hiroto99.windowslib.core.simplepacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.reflections.Reflections;

import java.util.*;

public class PacketManager {

    // ユーザーが提示してくれた登録用メソッドで使われるセット
    private static final Set<String> packagesToScan = new HashSet<>();

    /**
     * スキャン対象のパッケージパスを動的に追加する
     */
    public static void register(String modPackagePath) {
        packagesToScan.add(modPackagePath);
    }

    /**
     * 登録されたすべてのパッケージを一括スキャンしてNeoForgeに登録する
     */
    public static void registerAll(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        for (String packagePath : packagesToScan) {
            try {
                // 登録されたパッケージパスごとにReflectionsを生成してスキャン
                Reflections reflections = new Reflections(packagePath);
                var packetClasses = reflections.getTypesAnnotatedWith(AutoPacket.class);

                for (Class<?> clazz : packetClasses) {
                    if (SimplePacket.class.isAssignableFrom(clazz)) {
                        registerSinglePacket(registrar, (Class<? extends SimplePacket>) clazz);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to scan package for packets: " + packagePath);
                e.printStackTrace();
            }
        }
    }

    private static <T extends SimplePacket> void registerSinglePacket(PayloadRegistrar registrar, Class<T> clazz) {
        try {
            var typeField = clazz.getField("TYPE");
            var codecField = clazz.getField("STREAM_CODEC");
            var annotation = clazz.getAnnotation(AutoPacket.class);

            var type = (CustomPacketPayload.Type<T>) typeField.get(null);
            var codec = (StreamCodec<FriendlyByteBuf, T>) codecField.get(null);

            if (annotation.value() == TargetSide.SERVER) {
                registrar.playToServer(type, codec, (packet, context) -> packet.handle(context));
            } else {
                registrar.playToClient(type, codec, (packet, context) -> packet.handle(context));
            }
        } catch (Exception e) {
            System.err.println("Failed to register packet: " + clazz.getName());
            e.printStackTrace();
        }
    }
}
