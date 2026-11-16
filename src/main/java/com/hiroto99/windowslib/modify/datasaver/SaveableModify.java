package com.hiroto99.windowslib.modify.datasaver;

import net.minecraft.resources.Identifier;

public interface SaveableModify {
    /**
     * このModifyの保存用のキーを定義する
     */
    Identifier type();

    /**
     * このModifyが持つ保存対象変数をビルダーへ定義する
     */
    void defineSaveFields(AutoCodecBuilder builder);
}

