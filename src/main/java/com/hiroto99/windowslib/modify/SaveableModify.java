package com.hiroto99.windowslib.modify;

import com.hiroto99.windowslib.modify.datasaver.AutoCodecBuilder;

public interface SaveableModify extends FeaturedBaseModify {
    /**
     * このModifyが持つ保存対象変数をビルダーへ定義する
     */
    void defineSaveFields(AutoCodecBuilder builder);
}

