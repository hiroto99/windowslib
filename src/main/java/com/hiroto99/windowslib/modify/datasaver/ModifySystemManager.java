package com.hiroto99.windowslib.modify.datasaver;

import com.hiroto99.windowslib.modify.ModifySaver;
import com.hiroto99.windowslib.modify.SaveableModify;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModifySystemManager {
    private static final Map<Identifier, Object> MODIFIES = new LinkedHashMap<>();

    /**
     * システムにModifyを登録するメソッド
     * （システム側で自動的にセーブ対象か判定し、ビルダーへ登録する）
     */
    public static <T> T registerModify(Identifier id, T modifyInstance) {
        MODIFIES.put(id, modifyInstance);

        // ★ システム側が主体となってビルダーへ自動登録（名前空間プレフィックスも自動付与）
        if (modifyInstance instanceof SaveableModify saveable) {
            AutoCodecBuilder scopedBuilder = ModifySaver.BUILDER.withPrefix(id.toString());
            saveable.defineSaveFields(scopedBuilder);
        }

        return modifyInstance;
    }

    public static Map<Identifier, Object> getAll() {
        return Map.copyOf(MODIFIES);
    }
}