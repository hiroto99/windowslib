package com.hiroto99.windowslib.modify.network;

import com.hiroto99.windowslib.modify.Modify;

public interface ServerSyncModify extends Modify {
    /**
     * 接続先のClientModifyに送る変数をビルダーへ定義する
     */
    void defineServerSync(SyncStreamBuilder builder);
}
