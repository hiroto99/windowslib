package com.hiroto99.windowslib.modify.datasaver;

import com.hiroto99.windowslib.modify.Modify;

public interface SaveableModify extends Modify {
    /**
     * このModifyが持つ保存対象変数をビルダーへ定義する
     */
    void defineSaveFields(AutoCodecBuilder builder);
}

