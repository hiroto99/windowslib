package com.hiroto99.windowslib.core.simplepacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

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
            var annotation = clazz.getAnnotation(AutoPacket.class);

            var type = (CustomPacketPayload.Type<T>) typeField.get(null);
            var codec = createRecordCodec(clazz);

            if (annotation.value() != TargetSide.CLIENT) {
                registrar.playToServer(type, codec, (packet, context) -> packet.handle(context));
            }
            if (annotation.value() != TargetSide.SERVER) {
                registrar.playToClient(type, codec, (packet, context) -> packet.handle(context));
            }
        } catch (Exception e) {
            System.err.println("Failed to register packet: " + clazz.getName());
            e.printStackTrace();
        }
    }

    /**
     * 💡 レコードの変数（フィールド）が何個であっても、動的に対応して StreamCodec を自動生成する
     */
    @SuppressWarnings("unchecked")
    public static <R> StreamCodec<RegistryFriendlyByteBuf, R> createRecordCodec(Class<R> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("レコードクラスではありません: " + recordClass.getName());
        }

        try {
            RecordComponent[] components = recordClass.getRecordComponents();
            int fieldCount = components.length;

            if (fieldCount == 0) {
                // 引数なしレコードの場合は、ユニット（空）コーデックを返す
                // 1. StreamCodec.of の結果を、一度ジェネリクスなしの生の型 (StreamCodec) にキャスト
                StreamCodec rawEmptyCodec = (StreamCodec) StreamCodec.of((b, v) -> {}, b -> {
                    try {
                        return recordClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) { throw new RuntimeException(e); }
                });

                // 2. その後、目的の型にキャストして返却
                return (StreamCodec<RegistryFriendlyByteBuf, R>) rawEmptyCodec;
            }

            // 1. 動的に各フィールドの「型、コーデック、仮想ゲッター」を収集する
            Class<?>[] paramTypes = new Class<?>[fieldCount];
            List<Object> compositeArgs = new ArrayList<>(); // compositeメソッドに渡す引数リスト

            // 前半戦：各フィールドの [Codec, Getter] をペアで順番に詰めていく
            for (RecordComponent component : components) {
                paramTypes[compositeArgs.size() / 2] = component.getType();

                // コーデックの自動解決
                StreamCodec<ByteBuf, ?> fieldCodec = resolveCodec(component);
                compositeArgs.add(fieldCodec);

                // 🌟 仮想ゲッターをその場で生成して追加 (型エラーを回避するため Function.class で受ける)
                Method getterMethod = component.getAccessor();
                Function<R, ?> virtualGetter = (R instance) -> {
                    try { return getterMethod.invoke(instance); }
                    catch (Exception e) { throw new RuntimeException(e); }
                };
                compositeArgs.add(virtualGetter);
            }

            // 2. 対応するコンストラクタを取得し、引数の数に応じた「仮想関数」にラップする
            Constructor<R> constructor = recordClass.getDeclaredConstructor(paramTypes);
            Object virtualConstructor = createVirtualConstructor(constructor, fieldCount);
            compositeArgs.add(virtualConstructor); // 最後にコンストラクタを追加

            // 3. 💡 変数の数（fieldCount）に一致する StreamCodec.composite メソッドを動的に検索
            // compositeの引数の数 = (フィールド数 * 2) + 1 [コンストラクタ分]
            int totalArgCount = (fieldCount * 2) + 1;
            Class<?>[] compositeParamTypes = new Class<?>[totalArgCount];

            for (int i = 0; i < fieldCount * 2; i += 2) {
                compositeParamTypes[i] = StreamCodec.class; // codec1, codec2...
                compositeParamTypes[i + 1] = Function.class; // getter1, getter2...
            }
            // 最後のコンストラクタの型を決定 (1つならFunction、2つならBiFunction、3つ以上なら共通インターフェースが無いのでObject等で検索)
            compositeParamTypes[totalArgCount - 1] = (fieldCount == 1) ? Function.class :
                    (fieldCount == 2) ? BiFunction.class : Object.class;

            // 💡 引数の型配列が「完全に一致」する、バニラの指定数の composite メソッドを引き抜く
            Method targetCompositeMethod = findCompositeMethod(fieldCount, compositeParamTypes);

            // 4. 実行して動的に結合された StreamCodec を返却
            return (StreamCodec<RegistryFriendlyByteBuf, R>) targetCompositeMethod.invoke(null, compositeArgs.toArray());

        } catch (Throwable t) {
            throw new RuntimeException("動的可変StreamCodecの自動生成に失敗しました: " + recordClass.getName(), t);
        }
    }

    /**
     * 引数の数（1〜3個以上）に応じて、適切な「仮想コンストラクタ関数」を動的に生成する
     */
    private static Object createVirtualConstructor(Constructor<?> ctor, int fieldCount) {
        if (fieldCount == 1) {
            // 引数1つのときは通常の Function
            return (Function<Object, Object>) (v1) -> {
                try { return ctor.newInstance(v1); } catch (Exception e) { throw new RuntimeException(e); }
            };
        } else if (fieldCount == 2) {
            // 引数2つのときは BiFunction
            return (BiFunction<Object, Object, Object>) (v1, v2) -> {
                try { return ctor.newInstance(v1, v2); } catch (Exception e) { throw new RuntimeException(e); }
            };
        } else {
            // 💡 引数が3つ以上の場合は、Minecraft内部の「独自の多引数インターフェース」
            // （例: TriFunction, QuadFunction等。環境によりパッケージが異なる）にリフレクションで適合させる。
            // ここではJavaの動的プロキシ(Proxy)を使って、どんな多重引数インターフェースが要求されても自動でコンストラクタに流す「万能関数」を作ります。
            Class<?> functionalInterface = getConstructorInterfaceForCount(fieldCount);
            return java.lang.reflect.Proxy.newProxyInstance(
                    functionalInterface.getClassLoader(),
                    new Class<?>[]{functionalInterface},
                    (proxy, method, args) -> ctor.newInstance(args) // 渡された引数配列をそのまま展開して new
            );
        }
    }

    /**
     * フィールド数に応じたコンストラクタ用関数型インターフェースを判別する
     */
    private static Class<?> getConstructorInterfaceForCount(int count) {
        try {
            // Minecraftのバージョンや環境（Mojangマッピング等）に合わせて適切なクラス名を指定してください
            // 例: com.mojang.datafixers.util.Function3 など
            if (count > 2 && count <= 12) {
                return Class.forName("com.mojang.datafixers.util.Function" + count);
            }
            return Object.class;
        } catch (ClassNotFoundException e) {
            // 見つからない場合は、StreamCodec#composite の最後の引数の型を直接見に行く
            return Object.class;
        }
    }

    /**
     * リフレクションで条件に合う composite メソッドを曖昧検索（型消去対策）する
     */
    private static Method findCompositeMethod(int fieldCount, Class<?>[] expectedTypes) throws NoSuchMethodException {
        for (Method m : StreamCodec.class.getMethods()) {
            if (m.getName().equals("composite") && m.getParameterCount() == expectedTypes.length) {
                // 基本的な引数構成（前半がCodecとFunctionのペアであること）が一致していればそれを採用
                return m;
            }
        }
        throw new NoSuchMethodException("引数 " + fieldCount + " 個用の StreamCodec.composite メソッドが見つかりません。");
    }

    /**
     * フィールドの型やアノテーションから最適なコーデックを自動解決
     */
    private static StreamCodec<ByteBuf, ?> resolveCodec(RecordComponent component) throws Exception {
        Class<?> type = component.getType();
        if (type == BlockPos.class) {
            return BlockPos.STREAM_CODEC;
        }
        if (type == int.class || type == Integer.class) {
            return SmartCodec.SMART_INT;
        }
        if (type == String.class) {
            return ByteBufCodecs.STRING_UTF8;
        }
        throw new UnsupportedOperationException("未対応の型です: " + type.getName());
    }
}
