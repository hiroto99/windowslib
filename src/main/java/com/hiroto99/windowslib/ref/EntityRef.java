package com.hiroto99.windowslib.ref;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A typed, lightweight reference to an {@link Entity} identified by UUID.
 *
 * <p>
 * This class does not store or own the actual entity instance.
 * It merely provides a convenient, type-safe way to resolve an entity
 * when needed.
 * </p>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>The UUID is the only persistent state.</li>
 *   <li>The resolved entity may be {@code null} at any time.</li>
 *   <li>The resolved entity must be treated as a temporary handle.</li>
 * </ul>
 *
 * <p>
 * This class is safe to use with vanilla entities and entities from other mods.
 * </p>
 *
 * @param <T> the expected entity type
 */
public final class EntityRef<T extends Entity> {

    private final UUID uuid;

    public EntityRef(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    /**
     * Attempts to resolve the referenced entity in the given level.
     *
     * <p>
     * This method is best-effort and may return {@code null} if the entity
     * is not currently loaded or no longer exists.
     * </p>
     *
     * @param level the server level to resolve in
     * @return the resolved entity, or {@code null} if unavailable
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T resolve(ServerLevel level) {
        return (T) EntityRefManager.resolve(uuid, level);
    }

    /**
     * @return the UUID backing this reference
     */
    public UUID uuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "EntityRef[" + uuid + "]";
    }
}