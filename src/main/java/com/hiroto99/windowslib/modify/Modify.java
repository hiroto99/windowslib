package com.hiroto99.windowslib.modify;

import net.minecraft.resources.Identifier;

public interface Modify {
    /**
     * このModifyの保存・通信用のキーを定義する
     * ※通信するためにはこのキーと接続先のClientModifyのキーを一致させる必要があります。
     */
    Identifier type();
}
