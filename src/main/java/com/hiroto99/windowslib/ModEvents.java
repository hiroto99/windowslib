package com.hiroto99.windowslib;

import com.hiroto99.windowslib.core.lifecycle.EntityLifecycleHooks;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

public class ModEvents {
    @SubscribeEvent
    private static void onEntityLoaded(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            Entity entity = event.getEntity();
            EntityLifecycleHooks.onEntityLoaded(entity);
        }
    }

    @SubscribeEvent
    private static void onEntityLoaded(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            Entity entity = event.getEntity();
            EntityLifecycleHooks.onEntityLoaded(entity);
        }
    }
}
