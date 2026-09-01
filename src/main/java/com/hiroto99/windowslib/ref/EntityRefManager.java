package com.hiroto99.windowslib.ref;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A centralized manager for resolving and caching {@link Entity}
 * instances by their {@link UUID}.
 *
 * <p>
 * This manager exists to safely handle entity references across world reloads,
 * chunk unloads, and save/load cycles, especially when dealing with:
 * </p>
 *
 * <ul>
 *   <li>Custom mobs storing owner / leader / target references</li>
 *   <li>Vanilla or third-party entities accessed via Mixin</li>
 *   <li>Entities that may not be loaded at the time of NBT deserialization</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 *
 * <ul>
 *   <li><b>UUID is the source of truth</b> — entity instances are transient.</li>
 *   <li><b>No entity resolution during NBT read/write</b>.</li>
 *   <li><b>No mandatory per-tick lookups</b>.</li>
 *   <li><b>WeakReference-based caching</b> to avoid memory leaks and chunk pinning.</li>
 * </ul>
 *
 * <p>
 * Entity resolution is intentionally <b>best-effort</b>:
 * if an entity is not loaded or does not exist, this manager will return {@code null}.
 * Callers must treat {@code null} as a valid and expected result.
 * </p>
 *
 * <h2>Correct Usage</h2>
 *
 * <ul>
 *   <li>AI Goal {@code canUse()}, {@code start()}, or conditional {@code tick()}</li>
 *   <li>Gameplay events (commands, attack hooks, state transitions)</li>
 *   <li>Entity load callbacks (best performance)</li>
 * </ul>
 *
 * <h2>Incorrect Usage</h2>
 *
 * <ul>
 *   <li>Calling resolve unconditionally every tick</li>
 *   <li>Resolving during {@code readAdditionalSaveData}</li>
 *   <li>Storing resolved entities as long-lived strong references</li>
 * </ul>
 *
 * <p>
 * This class is safe to use with vanilla entities and entities from other mods,
 * including those accessed via Mixin. It does not assume ownership of any entity
 * lifecycle.
 * </p>
 */
public final class EntityRefManager {
    private static final Map<UUID, WeakReference<Entity>> CACHE = new HashMap<>();

    public static void onEntityLoaded(Entity entity) {
        CACHE.put(entity.getUUID(), new WeakReference<>(entity));
    }

    public static void onEntityUnloaded(Entity entity) {
        CACHE.remove(entity.getUUID());
    }

    /**
     * Resolves an {@link Entity} from the given UUID
     * using a best-effort, side-effect-free lookup.
     *
     * <p>
     * This method is intentionally designed to be <b>lightweight and safe</b>,
     * as it may be invoked indirectly by vanilla logic such as:
     * </p>
     *
     * <ul>
     *   <li>Target copying between mobs</li>
     *   <li>AI state transitions</li>
     *   <li>Ownership or relationship checks</li>
     * </ul>
     *
     * <p>
     * No guarantees are provided:
     * returning {@code null} is considered a <b>normal and expected outcome</b>,
     * not an error condition.
     * </p>
     *
     * <h3>Resolution Behavior</h3>
     * <ul>
     *   <li>If a valid cached instance exists, it is returned.</li>
     *   <li>If the entity is currently loaded in the given world, it is returned.</li>
     *   <li>If the entity is not loaded or no longer exists, {@code null} is returned.</li>
     * </ul>
     *
     * <h3>Important Guarantees</h3>
     * <ul>
     *   <li>This method never loads chunks or entities.</li>
     *   <li>This method has no observable side effects.</li>
     *   <li>This method never throws exceptions.</li>
     * </ul>
     *
     * <p>
     * The returned entity must be treated as a <b>temporary handle</b>.
     * It may become invalid at any time due to chunk unloading or entity removal.
     * </p>
     *
     * <h3>Recommended Usage</h3>
     *
     * <p><b>AI Goal (canUse / start):</b></p>
     *
     * <pre>{@code
     * public boolean canUse() {
     *     LivingEntity owner =
     *         (LivingEntity) EntityRefManager.resolve(ownerUuid, level);
     *     return owner != null && owner.isAlive();
     * }
     * }</pre>
     *
     * <p><b>Event or command handling:</b></p>
     *
     * <pre>{@code
     * Entity target = EntityRefManager.resolve(targetUuid, level);
     * if (target != null) {
     *     doSomethingWith(target);
     * }
     * }</pre>
     *
     * <h3>Incorrect Usage</h3>
     *
     * <p><b>❌ Do NOT call unconditionally every tick:</b></p>
     *
     * <pre>{@code
     * // Bad: causes unnecessary repeated lookups
     * public void tick() {
     *     Entity e = EntityRefManager.resolve(uuid, level);
     * }
     * }</pre>
     *
     * <p><b>❌ Do NOT store the resolved entity as a long-lived field:</b></p>
     *
     * <pre>{@code
     * // Bad: prevents proper entity unloading
     * this.cachedEntity = EntityRefManager.resolve(uuid, level);
     * }</pre>
     *
     * <h3>null Handling</h3>
     *
     * <p>
     * A {@code null} return value indicates that the entity is currently unavailable.
     * This is a valid state and should be handled by skipping the action,
     * waiting until a later time, or falling back to alternative behavior.
     * </p>
     *
     * @param uuid  the UUID of the entity to resolve
     * @param level the server-level context used for lookup
     * @return the resolved entity, or {@code null} if unavailable
     */
    @Nullable
    public static Entity resolve(UUID uuid, ServerLevel level) {
        if (uuid == null) return null;

        // WeakReference キャッシュ
        WeakReference<Entity> ref = CACHE.get(uuid);
        Entity e = ref != null ? ref.get() : null;

        if (e != null && !e.isRemoved()) {
            return e;
        }

        // バニラAPI：ロードはしない、マップ参照のみ
        e = level.getEntity(uuid);
        if (e != null) {
            CACHE.put(uuid, new WeakReference<>(e));
            return e;
        }

        return null;
    }
}
