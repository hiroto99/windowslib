package com.hiroto99.windowslib.core.simplepacket;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.reflections.Reflections;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class PacketManager {
    // ユーザーが提示してくれた登録用メソッドで使われるセット
    private static final Set<String> packagesToScan = new HashSet<>();

    // ResolveCodecForClassで使うマップを作成
    private static final Map<Class<?>, StreamCodec<? extends ByteBuf, ?>> StreamCodecKey = new HashMap<>();

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
                StreamCodec<? extends ByteBuf, ?> fieldCodec = resolveCodec(component);
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
    private static StreamCodec<? extends ByteBuf, ?> resolveCodec(RecordComponent component) {
        Class<?> type = component.getType();

        // 1. フィールドの型が java.util.Listかどうかを判定
        if (type == List.class) {
            // 2. ジェネリクス情報（例: List<BlockPos> の <BlockPos> 部分）を取得する
            Type genericType = component.getGenericType();

            if (genericType instanceof ParameterizedType parameterizedType) {
                // 実際の型引数（<> の中身）を配列で取得（Listは引数が1つなので [0] を見る）
                Type[] typeArguments = parameterizedType.getActualTypeArguments();

                if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?> innerClass) {
                    // 3. 中身の型の単体コーデックを再帰的に解決する
                    // 例: innerClass が BlockPos.class なら、BlockPos.STREAM_CODEC が返る
                    StreamCodec<? extends ByteBuf, ?> innerCodec = resolveCodecForClass(innerClass);

                    // 4. 💡 API側の「SmartCodec.makeCodecList」に流し込んで、List用のコーデックに変換する
                    // 引数に単体コーデックを渡すと、自動的に List<T> 対応のコーデックになります
                    return SmartCodec.makeCodecList(innerCodec);
                }
            }
        }

        // 1. そのフィールドが配列（Array）かどうかをチェック
        if (type.isArray()) {
            // 2. 配列の中身の型（要素の型）を取得
            Class<?> elementType = type.getComponentType();

            // 3. 中身の型に応じて、適切な配列用のコーデックを生成して返す
            if (elementType == byte.class) {
                return ByteBufCodecs.BYTE_ARRAY;
            } else if (elementType == long.class) {
                return ByteBufCodecs.LONG_ARRAY;
            } else {
                // 4. 再帰呼び出しで、中身単体のコーデックを解決（例: BlockPos.STREAM_CODEC）
                StreamCodec<? extends ByteBuf, ?> elementCodec = resolveCodecForClass(elementType);

                // 5. 💡 API側の「SmartCodec.makeCodecList」に流し込んで、List用のコーデックに変換する
                // 引数に単体コーデックを渡すと、自動的に List<T> 対応のコーデックになります
                StreamCodec<? extends ByteBuf, ?> listCodec = SmartCodec.makeCodecList(elementCodec);

                // 🌟 最後の出口で xmap を使い、List から配列へ、配列から List へと相互変換をかける
                // 生の型 (StreamCodec) を経由させて型変換エラーを強引に黙らせます
                StreamCodec rawListCodec = (StreamCodec) listCodec;

                // 6. 【修正点】xmap ではなく map メソッドを呼び出す
                StreamCodec rawArrayCodec = rawListCodec.map(
                        (Object obj) -> {
                            // デコード時 (読み込み): List<Object> -> Object[]（配列）
                            List<Object> list = (List<Object>) obj;
                            Object[] array = (Object[]) Array.newInstance(elementType, list.size());
                            return list.toArray(array);
                        },
                        (Object arrayObj) -> {
                            // エンコード時 (書き込み): 配列（Object[]） -> List<Object>
                            return List.of((Object[]) arrayObj);
                        }
                );
            }
        }

        // 1. フィールドの型が java.util.Mapかどうかを判定
        if (Map.class.isAssignableFrom(type)) {
            // 2. ジェネリクス情報（例: Map<String, BlockPos> の <String, BlockPos> 部分）を取得する
            Type genericType = component.getGenericType();

            if (genericType instanceof ParameterizedType parameterizedType) {
                // 実際の型引数（<> の中身）を配列で取得（Optionalは引数が1つなので [0] を見る）
                Type[] typeArguments = parameterizedType.getActualTypeArguments();

                // Mapは必ずキーと値の2つの型パラメーターを持ちます (<K, V>)
                if (typeArguments.length == 2 && typeArguments[0] instanceof Class<?> keyClass && typeArguments[1] instanceof Class<?> valueClass) {
                    // 3. キーと値の単体コーデックをそれぞれ再帰的に解決
                    StreamCodec<? extends ByteBuf, ?> keyCodec = resolveCodecForClass(keyClass);
                    StreamCodec<? extends ByteBuf, ?> valueCodec = resolveCodecForClass(valueClass);

                    StreamCodec rawKeyCodec = keyCodec;
                    StreamCodec rawValueCodec = valueCodec;

                    // 4. 💡 Minecraftバニラの「ByteBufCodecs.map」に流し込んで Map 用コーデックに変換
                    // シグネチャ: map(IntFunction, StreamCodec, StreamCodec)
                    // ※第1引数には、読み込み時にどのMap実装を使うか（例: HashMap::new）を指定するヘルパーが必要です。
                    // バニラの標準オーバーロードに合わせたリフレクション呼び出しを行います。
                    
                    // キャストを挟んで呼び出す（警告が出ますがコンパイルは通ります）
                    return SmartCodec.makeMapCodec(rawKeyCodec, rawValueCodec);
                }
            }
        }

        // 1. フィールドの型が java.util.Optional かどうかを判定
        if (type == Optional.class) {
            // 2. ジェネリクス情報（例: Optional<BlockPos> の <BlockPos> 部分）を取得する
            Type genericType = component.getGenericType();

            if (genericType instanceof ParameterizedType parameterizedType) {
                // 実際の型引数（<> の中身）を配列で取得（Optionalは引数が1つなので [0] を見る）
                Type[] typeArguments = parameterizedType.getActualTypeArguments();

                if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?> innerClass) {
                    // 3. 中身の型の単体コーデックを再帰的に解決する
                    // 例: innerClass が BlockPos.class なら、BlockPos.STREAM_CODEC が返る
                    StreamCodec<? extends ByteBuf, ?> innerCodec = resolveCodecForClass(innerClass);

                    // 4. 💡 Minecraftバニラの「ByteBufCodecs.optional」に流し込んで、Optional用のコーデックに変換する
                    // 引数に単体コーデックを渡すと、自動的に Optional<T> 対応のコーデックになります
                    return ByteBufCodecs.optional(innerCodec);
                }
            }
            throw new UnsupportedOperationException("Optional のジェネリクス型を特定できませんでした。");
        }

        return resolveCodecForClass(type);
    }

    private static StreamCodec<? extends ByteBuf, ?> resolveCodecForClass(Class<?> type) {
        if (StreamCodecKey.containsKey(type)) {
            return StreamCodecKey.get(type);
        } else {
            if (type.isEnum()) {
                // Enumの ordinal (インデックス) を使って自動シリアライズするコーデックを動的生成
                // 引数型: (IntFunction<E>, ToIntFunction<E>)
                IntFunction<Object> fromId = id -> type.getEnumConstants()[id];
                ToIntFunction<Object> toId = obj -> ((Enum<?>) obj).ordinal();

                return ByteBufCodecs.idMapper(fromId, toId);
            }
            throw new UnsupportedOperationException("未対応の型です: " + type.getName());
        }
    }

    public static void createStreamCodecKey() {
        // プリミティブ型（および対応するラッパー型）の登録
        addKey(boolean.class, ByteBufCodecs.BOOL);
        addKey(Boolean.class, ByteBufCodecs.BOOL);
        addKey(byte.class, ByteBufCodecs.BYTE);
        addKey(Byte.class, ByteBufCodecs.BYTE);
        addKey(short.class, ByteBufCodecs.SHORT);
        addKey(Short.class, ByteBufCodecs.SHORT);

        // スマートコーデック（独自実装）
        addKey(int.class, SmartCodec.SMART_INT);
        addKey(Integer.class, SmartCodec.SMART_INT);
        addKey(long.class, SmartCodec.SMART_LONG);
        addKey(Long.class, SmartCodec.SMART_LONG);

        // 小数の型
        addKey(float.class, ByteBufCodecs.FLOAT);
        addKey(Float.class, ByteBufCodecs.FLOAT);
        addKey(double.class, ByteBufCodecs.DOUBLE);
        addKey(Double.class, ByteBufCodecs.DOUBLE);

        // オブジェクト・データの塊
        addKey(String.class, ByteBufCodecs.STRING_UTF8);
        addKey(Tag.class, ByteBufCodecs.TAG);
        addKey(CompoundTag.class, ByteBufCodecs.COMPOUND_TAG);
        addKey(Vector3fc.class, ByteBufCodecs.VECTOR3F);
        addKey(Quaternionfc.class, ByteBufCodecs.QUATERNIONF);
        addKey(GameProfile.class, ByteBufCodecs.GAME_PROFILE);

        // バニラオブジェクト
        addKey(BlockPos.class, BlockPos.STREAM_CODEC);
        addKey(ItemStack.class, ItemStack.STREAM_CODEC);
        addKey(Identifier.class, Identifier.STREAM_CODEC);
        addKey(Component.class, ComponentSerialization.STREAM_CODEC);

        // 動的レジストリ解決型（RegistryFriendlyByteBufが必要なもの）
        addKey(Block.class, ByteBufCodecs.registry(Registries.BLOCK));
        addKey(Item.class, ByteBufCodecs.registry(Registries.ITEM));
        addKey(Fluid.class, ByteBufCodecs.registry(Registries.FLUID));
        addKey(Biome.class, ByteBufCodecs.registry(Registries.BIOME));
        addKey(DimensionType.class, ByteBufCodecs.registry(Registries.DIMENSION_TYPE));
        addKey(MobEffect.class, ByteBufCodecs.registry(Registries.MOB_EFFECT));
    }

    public static <T> void addKey(Class<T> key, StreamCodec<? extends ByteBuf, T> value) {
        StreamCodecKey.put(key, value);
    }
}
