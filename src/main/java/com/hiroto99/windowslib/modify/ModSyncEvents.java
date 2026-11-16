package com.hiroto99.windowslib.modify;

import com.hiroto99.windowslib.modify.network.ModifyPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "mymod")
public class ModSyncEvents {
    // 毎Tick終了時に変化をチェックし、変わった変数がある時だけ自動送信
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ModifyPacket.syncChangesToAll();
    }

    // ログイン時に全変数を同期
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModifyPacket.syncAllToPlayer(player);
        }
    }
}
