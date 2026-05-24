package dev.falaris.client.module;

import dev.falaris.client.FalarisClient;

import java.util.ArrayList;
import java.util.List;

public final class FriendsManager {
    private final List<String> friends = new ArrayList<>();

    public void add(String name) {
        String lower = name.toLowerCase();
        if (!friends.contains(lower)) {
            friends.add(lower);
            save();
        }
    }

    public void remove(String name) {
        friends.remove(name.toLowerCase());
        save();
    }

    public boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    public List<String> getAll() {
        return List.copyOf(friends);
    }

    public void setFriends(List<String> list) {
        friends.clear();
        for (String s : list) {
            friends.add(s.toLowerCase());
        }
    }

    private void save() {
        FalarisClient.getInstance().getConfigManager().saveFriends(this);
    }
}
