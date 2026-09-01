package com.hiroto99.windowslib.core.lifecycle;

import com.hiroto99.windowslib.ref.EntityRefManager;
import net.minecraft.world.entity.Entity;

/**
 * Central hook point for entity lifecycle events.
 *
 * <p>
 * This class acts as a thin bridge between loader-/platform-specific
 * entity lifecycle detection and the reference system.
 * </p>
 *
 * <p>
 * No resolution, caching, or game logic is performed here.
 * </p>
 */
public final class EntityLifecycleHooks {

    private EntityLifecycleHooks() {
    }

    /**
     * Called when an entity is fully added to a server world.
     *
     * @param entity the loaded entity
     */
    public static void onEntityLoaded(Entity entity) {
        EntityRefManager.onEntityLoaded(entity);
    }

    /**
     * Called when an entity is removed from the world or unloaded.
     *
     * @param entity the entity being unloaded
     */
    public static void onEntityUnloaded(Entity entity) {
        EntityRefManager.onEntityUnloaded(entity);
    }
}