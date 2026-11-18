package com.hiroto99.windowslib.modify;

import com.hiroto99.windowslib.WindowsLib;
import com.hiroto99.windowslib.modify.datasaver.ModifySaver;
import com.hiroto99.windowslib.modify.network.ModifyPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = WindowsLib.MODID)
public class ModifyEvents {
    private static int tickCounter = 0;

    // 毎Tick終了時に変化をチェックし、変わった変数がある時だけ自動送信
    // datasaver側の完全保存機能の実行も行う
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ModifyPacket.syncChangesToAll();

        // ここからはdatasaver側の処理
        // 毎Tickではなく20Tick（1秒）ごとにチェックすれば負荷はほぼゼロ
        if (++tickCounter % 20 != 0) return;

        if (ModifySaver.BUILDER.hasAnyChanged()) {
            ServerLevel overworld = event.getServer().overworld();
            ModifySaver.get(overworld).setDirty();
        }
    }

    // ログイン時に全変数を同期
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModifyPacket.syncAllToPlayer(player);
        }
    }
}
