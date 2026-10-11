package com.hiroto99.windowslib;

import com.hiroto99.windowslib.core.lifecycle.EntityLifecycleHooks;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

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
}
