package com.hiroto99.windowslib;

import com.hiroto99.windowslib.core.lifecycle.EntityLifecycleHooks;
import com.hiroto99.windowslib.core.simplepacket.PacketManager;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber
public class ModEvents {
    @SubscribeEvent
    private static void onEntityLoaded(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        EntityLifecycleHooks.onEntityLoaded(entity);
    }

    @SubscribeEvent
    private static void onEntityUnLoaded(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        EntityLifecycleHooks.onEntityUnloaded(entity);
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // 💡ここで PacketManager.registerAll を呼び出して一括登録を実行する！
        // 第2引数にはご自身のModのチャンネルネームスペース（ModIDなど）を指定します
        PacketManager.registerAll(event);
    }
}
