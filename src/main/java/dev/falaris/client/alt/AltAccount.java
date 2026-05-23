package dev.falaris.client.alt;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AltAccount {
    private final String id;
    private final String username;
    private final String uuid;
    private final boolean favorite;
    private final long lastUsed;

    public AltAccount(String id, String username, String uuid, boolean favorite, long lastUsed) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username").trim();
        this.uuid = uuid == null ? "" : uuid.trim();
        this.favorite = favorite;
        this.lastUsed = lastUsed;
    }

    public static AltAccount create(String username, String uuid) {
        return new AltAccount(UUID.randomUUID().toString(), username.trim(), uuid == null ? "" : uuid.trim(), false, 0L);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getUuid() {
        return uuid;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public AltAccount withUsername(String username) {
        return new AltAccount(id, username, uuid, favorite, lastUsed);
    }

    public AltAccount withUuid(String uuid) {
        return new AltAccount(id, username, uuid, favorite, lastUsed);
    }

    public AltAccount withFavorite(boolean favorite) {
        return new AltAccount(id, username, uuid, favorite, lastUsed);
    }

    public AltAccount withLastUsed(long lastUsed) {
        return new AltAccount(id, username, uuid, favorite, lastUsed);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AltAccount account)) {
            return false;
        }
        return id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
