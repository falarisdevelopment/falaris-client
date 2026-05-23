package dev.falaris.client.alt;

import dev.falaris.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public final class AltManager {
    private final ConfigManager configManager;
    private final List<AltAccount> alts = new ArrayList<>();
    private final Random random = new Random();

    public AltManager(ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        load();
    }

    public void load() {
        alts.clear();
        alts.addAll(configManager.loadAlts());
        sortAlts();
    }

    public void save() {
        sortAlts();
        configManager.saveAlts(alts);
    }

    public List<AltAccount> getAlts() {
        return List.copyOf(alts);
    }

    public List<AltAccount> search(String query) {
        String normalized = query == null ? "" : query.toLowerCase().trim();
        return alts.stream()
                .filter(alt -> normalized.isEmpty() || alt.getUsername().toLowerCase().contains(normalized) || alt.getUuid().toLowerCase().contains(normalized))
                .collect(Collectors.toList());
    }

    public Optional<AltAccount> findById(String id) {
        return alts.stream().filter(alt -> alt.getId().equals(id)).findFirst();
    }

    public void addAlt(AltAccount alt) {
        alts.add(Objects.requireNonNull(alt, "alt"));
        save();
    }

    public void removeAlt(AltAccount alt) {
        alts.removeIf(existing -> existing.getId().equals(alt.getId()));
        save();
    }

    public void updateAlt(AltAccount updated) {
        for (int i = 0; i < alts.size(); i++) {
            if (alts.get(i).getId().equals(updated.getId())) {
                alts.set(i, Objects.requireNonNull(updated, "updated"));
                save();
                return;
            }
        }
    }

    public void markAsUsed(AltAccount alt) {
        updateAlt(alt.withLastUsed(System.currentTimeMillis()));
    }

    public void toggleFavorite(AltAccount alt) {
        alt = alt.withFavorite(!alt.isFavorite());
        updateAlt(alt);
    }

    public AltAccount randomAlt() {
        if (alts.isEmpty()) {
            throw new IllegalStateException("No alts saved.");
        }
        return alts.get(random.nextInt(alts.size()));
    }

    private void sortAlts() {
        alts.sort(Comparator.comparing(AltAccount::isFavorite).reversed()
                .thenComparing(AltAccount::getLastUsed).reversed()
                .thenComparing(AltAccount::getUsername));
    }
}
