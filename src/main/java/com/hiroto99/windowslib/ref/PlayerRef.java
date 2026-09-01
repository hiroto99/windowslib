package com.hiroto99.windowslib.ref;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A lightweight reference to a {@link ServerPlayer} identified by UUID.
 *
 * <p>
 * Unlike {@link EntityRef}, players are resolved via the server player list
 * and may be unavailable when offline.
 * </p>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Offline players resolve to {@code null}.</li>
 *   <li>This class never forces player loading.</li>
 *   <li>The returned player must be treated as a temporary handle.</li>
 * </ul>
 */
public final class PlayerRef {

    private final UUID uuid;

    public PlayerRef(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    /**
     * Attempts to resolve the referenced player from the server.
     *
     * @param server the active minecraft server
     * @return the server player, or {@code null} if the player is offline
     */
    @Nullable
    public ServerPlayer resolve(MinecraftServer server) {
        return server.getPlayerList().getPlayer(uuid);
    }

    /**
     * @return the UUID backing this reference
     */
    public UUID uuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "PlayerRef[" + uuid + "]";
    }
}