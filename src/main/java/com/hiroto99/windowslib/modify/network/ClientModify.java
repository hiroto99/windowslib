package com.hiroto99.windowslib.modify.network;

import net.minecraft.resources.Identifier;

public interface ClientModify {
    /**
     * このModifyの通信用のキーを定義する
     * ※通信するためには接続元のModifyのキーとこのキーを一致させる必要があります。
     */
    Identifier type();

    /**
     * 接続元のModifyから受け取る変数をビルダーへ定義する
     */
    void defineClientSync(SyncStreamBuilder builder);
}
