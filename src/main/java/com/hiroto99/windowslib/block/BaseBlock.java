package com.hiroto99.windowslib.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import oshi.util.tuples.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class BaseBlock<B extends BaseEntityBlock> extends BaseEntityBlock {
    public class PropertyPair<T extends Comparable<T>> extends Pair<Property<T>, T> {
        /**
         * Create a pair and store two objects.
         *
         * @param property the first object to store
         * @param t        the second object to store
         */
        public PropertyPair(Property<T> property, T t) {
            super(property, t);
        }
    }

    // 例: [BlockStateProperties.FACING, Direction.NORTH]
    private final List<PropertyPair<?>> propertyList;

    public BaseBlock(Properties properties, List<PropertyPair<?>> propertyList) {
        super(properties);
        this.propertyList = propertyList;

        BlockState blockState = stateDefinition.any();
        for (PropertyPair<?> propertyTuple : propertyList) {
            blockState = register(propertyTuple, blockState);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected MapCodec<B> codec() {
        Class<?> clazz = getGenericParamType(this.getClass());
        try {
            Object obj = Objects.requireNonNull(clazz).getDeclaredConstructor(Properties.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        try {
            // 1. BarrelBlock(Properties) コンストラクタをリフレクションで取得
            Function rawFunction = getRawFunction(clazz);
            return simpleCodec(rawFunction);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("対応するコンストラクタが見つかりません", e);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (PropertyPair<?> propertyTuple : propertyList) {
            builder.add(propertyTuple.getA());
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        animate(state, this.propertyList, level, pos, random);
    }

    abstract void animate(BlockState state, List<PropertyPair<?>> properties, Level level, BlockPos pos, RandomSource random);

    @Override
    abstract public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState);

    // Mapへの登録を呼び出す側
    private BlockState register(PropertyPair<?> propertytuple, BlockState blockState) {
        // 直接 put(propertytuple.getA(), propertytuple.getB()) と書くとエラーになるため
        // ヘルパーメソッドに丸ごと渡す
        return putHelper(propertytuple, blockState);
    }

    // このメソッドを通すことで、? が具体的な型 T に確定する
    private <T extends Comparable<T>> BlockState putHelper(PropertyPair<T> pair, BlockState blockState) {
        // ここでは pair.getA() が Property<T>、pair.getB() が T として完全に一致する！
        return blockState.setValue(pair.getA(), pair.getB());
    }

    // 必要なPropertyPairを見つけるために役立つメソッド
    @SuppressWarnings({"unchecked"})
    public @Nullable <T extends Comparable<T>> PropertyPair<T> find(Property<T> property) {
        for (PropertyPair<?> processPropertyPair : propertyList) {
            Property<?> processProp = processPropertyPair.getA();
            if (processProp == property) {
                return (PropertyPair<T>) processPropertyPair;
            }
        }
        return new PropertyPair<>(property, null);
    }

    private static @NonNull Function getRawFunction(Class<?> clazz) throws NoSuchMethodException {
        Constructor<?> constructor = clazz.getDeclaredConstructor(
                Properties.class
        );
        constructor.setAccessible(true);

        // 2. コンストラクタを simpleCodec が要求する Function の形にラップする
        Function<Properties, ?> virtualConstructor = (Properties props) -> {
            try {
                return constructor.newInstance(props);
            } catch (Exception e) {
                throw new RuntimeException("BarrelBlock のリフレクションインスタンス生成に失敗しました", e);
            }
        };

        // 3. ラップした関数を生の型経由で Block.simpleCodec() に渡す
        Function rawFunction = (Function) virtualConstructor;
        return rawFunction;
    }

    /**
     * 💡 継承元のジェネリッククラスから「B」の実際の型クラスを取得する
     *
     * @param clazz 調査対象の具象クラス（例: MyMachineBlock.class）
     * @return 割り当てられている具象クラス（例: MyMachineBlockEntity.class）、特定できない場合は null
     */
    private static Class<?> getGenericParamType(Class<?> clazz) {
        // 1. 直系の親クラス（スーパークラス）のジェネリクス情報を取得
        Type superclass = clazz.getGenericSuperclass();

        // 2. 親クラスがジェネリクス（<B>など）を持っているかチェック
        if (superclass instanceof ParameterizedType parameterizedType) {
            // 3. <> の中身（型引数の配列）を取得
            Type[] typeArgs = parameterizedType.getActualTypeArguments();

            // FacingBlock<B> は引数が1つなので、最初の要素（インデックス 0）を見る
            if (typeArgs.length > 0) {
                Type firstArg = typeArgs[0];

                // それが通常のクラス型であればキャストして返却
                if (firstArg instanceof Class<?> actualClass) {
                    return actualClass;
                }
            }
        }

        // もし直系の親がジェネリクスを持っておらず、さらに上の親が持っている可能性がある場合は
        // 再帰的に親クラスを掘り下げる
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null && superClazz != Object.class) {
            return getGenericParamType(superClazz);
        }

        return null;
    }
}
