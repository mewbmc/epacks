package io.starseed.epacks.managers;

import io.starseed.epacks.Epacks;
import io.starseed.epacks.backPack.Backpack;
import org.bukkit.entity.Player;

public class LevelManager {
    private final Epacks plugin;

    public LevelManager(Epacks plugin) {
        this.plugin = plugin;
    }

    public boolean canUpgradeBackpack(Player player, Backpack backpack) {
        String currentType = backpack.getType();
        String nextType = getNextBackpackType(currentType);
        if (nextType == null) {
            return false;
        }

        int requiredLevel = plugin.getConfig().getInt("backpack_types." + nextType + ".level_required", 0);
        return getPlayerLevel(player) >= requiredLevel;
    }

    private String getNextBackpackType(String currentType) {
        String[] types = plugin.getConfig().getConfigurationSection("backpack_types").getKeys(false).toArray(new String[0]);
        for (int i = 0; i < types.length - 1; i++) {
            if (types[i].equals(currentType)) {
                return types[i + 1];
            }
        }
        return null;
    }

    private int getPlayerLevel(Player player) {
        // This is a placeholder. You should implement this method to get the player's level
        // from your server's level system or economy plugin.
        // For now, we'll return a dummy value.
        return 10;
    }
}