package com.hiroto99.windowslib.modify;

import com.hiroto99.windowslib.WindowsLib;
import com.hiroto99.windowslib.modify.datasaver.AutoCodecBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.function.Consumer;

public class ModifySaver extends SavedData {
    private static final Identifier FILE_NAME = Identifier.fromNamespaceAndPath(WindowsLib.MODID, "modify_data");

    // 全Modifyから変数を集めるための専用ビルダー
    public static final AutoCodecBuilder BUILDER = new AutoCodecBuilder();

    // ビルダーの中身すべてを含む自動生成Codec（遅延初期化で全Modify登録後に確定）
    public static final Codec<ModifySaver> CODEC = Codec.lazyInitialized(
            () -> BUILDER.buildSavedDataCodec(ModifySaver::new)
    );

    public static final SavedDataType<ModifySaver> TYPE = new SavedDataType<>(
            FILE_NAME,
            ModifySaver::new,
            CODEC,
            null
    );

    public static ModifySaver get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Modの初期化時などに各Modifyから変数を登録する窓口
     */
    public static void registerVariables(Consumer<AutoCodecBuilder> registrar) {
        registrar.accept(BUILDER);
    }
}
