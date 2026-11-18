package com.hiroto99.windowslib.modify;

import com.hiroto99.windowslib.modify.datasaver.ModifySaver;
import com.hiroto99.windowslib.modify.datasaver.SaveableModify;
import com.hiroto99.windowslib.modify.network.ClientModify;
import com.hiroto99.windowslib.modify.network.ModifyPacket;
import com.hiroto99.windowslib.modify.network.ServerSyncModify;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModifySystemManager {
    private static final Map<Identifier, Object> MODIFIES = new LinkedHashMap<>();
    private static final Map<Identifier, Object> CLIENT_MODIFIES = new LinkedHashMap<>();

    /**
     * システムにModifyを登録するメソッド
     * （システム側で自動的にセーブ対象か判定し、ビルダーへ登録する）
     */
    public static <T extends Modify> T registerModify(Identifier id, T modifyInstance) {
        MODIFIES.put(id, modifyInstance);

        if (modifyInstance instanceof SaveableModify saveable) {
            saveable.defineSaveFields(ModifySaver.BUILDER.withPrefix(id.toString()));
        }
        if (modifyInstance instanceof ServerSyncModify serverSync) {
            serverSync.defineServerSync(ModifyPacket.BUILDER.withPrefix(id.toString()));
        }
        return modifyInstance;
    }

    // クライアント初期化時に呼ぶ登録メソッド
    public static <T extends ClientModify> T registerModifyClient(Identifier id, T clientModifyInstance) {
        CLIENT_MODIFIES.put(id, clientModifyInstance);

        if (clientModifyInstance instanceof ClientModify clientSync) {
            clientSync.defineClientSync(ModifyPacket.BUILDER.withPrefix(id.toString()));
        }
        return clientModifyInstance;
    }
    public static Map<Identifier, Object> getAll() {
        return Map.copyOf(MODIFIES);
    }
    public static Map<Identifier, Object> getAllClient() {
        return Map.copyOf(CLIENT_MODIFIES);
    }
}