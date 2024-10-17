package io.starseed.epacks.backPack;

import io.starseed.epacks.Epacks;
import io.starseed.epacks.managers.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;
import io.starseed.epacks.managers.MessageManager;

import java.util.*;

public class Backpack {
    private final String backpackId;
    private UUID uuid;
    private String type;
    private int capacity;
    private boolean autoPickupEnabled;
    private HashMap<Material, Integer> contents;
    private List<Material> autoPickupFilter;
    private double autoPickupRange;
    private Map<String, Integer> upgradeLevels;
    private final Epacks plugin;


    public Backpack(Epacks plugin, String backpackId, String type, FileConfiguration config) {
        this.plugin = plugin;
        this.backpackId = backpackId;
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.capacity = config.getInt("backpack_types." + type + ".capacity");
        this.autoPickupEnabled = config.getBoolean("auto_pickup.default_enabled");
        this.contents = new HashMap<>();
        this.autoPickupFilter = new ArrayList<>();
        this.autoPickupRange = config.getDouble("auto_pickup.pickup_range");
        this.upgradeLevels = new HashMap<>();
        upgradeLevels.put("capacity", 0);
        upgradeLevels.put("sell_multiplier", 0);
        loadAutoPickupFilter(config);
    }

    private void loadAutoPickupFilter(FileConfiguration config) {
        List<String> filterList = config.getStringList("auto_pickup.filter");
        for (String item : filterList) {
            try {
                Material material = Material.valueOf(item.toUpperCase());
                autoPickupFilter.add(material);
            } catch (IllegalArgumentException e) {
                // Invalid material name, skip it
            }
        }
    }

    public boolean addItem(ItemStack item) {
        if (getTotalItems() + item.getAmount() > capacity) {
            return false;
        }
        Material material = item.getType();
        int amount = contents.getOrDefault(material, 0) + item.getAmount();
        contents.put(material, amount);
        return true;
    }

    public boolean removeItem(ItemStack item) {
        Material material = item.getType();
        int currentAmount = contents.getOrDefault(material, 0);
        if (currentAmount < item.getAmount()) {
            return false;
        }
        int newAmount = currentAmount - item.getAmount();
        if (newAmount == 0) {
            contents.remove(material);
        } else {
            contents.put(material, newAmount);
        }
        return true;
    }

    public int getTotalItems() {
        return contents.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean upgrade(FileConfiguration config) {
        String nextType = getNextBackpackType(config);
        if (nextType == null) {
            return false;
        }
        this.type = nextType;
        this.capacity = config.getInt("backpack_types." + nextType + ".capacity");
        return true;
    }

    private String getNextBackpackType(FileConfiguration config) {
        String[] types = config.getConfigurationSection("backpack_types").getKeys(false).toArray(new String[0]);
        for (int i = 0; i < types.length - 1; i++) {
            if (types[i].equals(this.type)) {
                return types[i + 1];
            }
        }
        return null;
    }

    public int getUpgradeLevel(String upgradeType) {
        return upgradeLevels.getOrDefault(upgradeType, 0);
    }
    public void updateCapacity() {
        int baseCapacity = plugin.getConfig().getInt("backpack_types." + type + ".capacity");
        int upgradeLevel = getUpgradeLevel("capacity");
        double increasePerLevel = plugin.getConfig().getDouble("upgrades.capacity.increase_per_level", 100);
        this.capacity = baseCapacity + (int)(upgradeLevel * increasePerLevel);

    }

    // Method 'getCapacity()' is already defined in this class.
    // Remove or rename this method to resolve the duplicate definition.

    public boolean upgradeAttribute(String upgradeType) {
        int currentLevel = getUpgradeLevel(upgradeType);

        int maxLevel = plugin.getConfig().getInt("upgrades." + upgradeType + ".max_level");

        if (currentLevel < maxLevel) {
            upgradeLevels.put(upgradeType, currentLevel + 1);
            return true;
        }
        return false;
    }
    public double getCapacityMultiplier(FileConfiguration config) {
        int level = getUpgradeLevel("capacity");
        double baseIncrease = config.getDouble("upgrades.capacity.base_increase");
        double increaseMultiplier = config.getDouble("upgrades.capacity.increase_multiplier");
        return 1 + (baseIncrease * Math.pow(increaseMultiplier, level));

    }

    public double getSellMultiplier(FileConfiguration config) {
        int level = getUpgradeLevel("sell_multiplier");
        double baseIncrease = config.getDouble("upgrades.sell_multiplier.base_increase");
        double increaseMultiplier = config.getDouble("upgrades.sell_multiplier.increase_multiplier");
        return 1 + (baseIncrease * Math.pow(increaseMultiplier, level));
    }

    public boolean canAutoPickup(ItemStack item) {
        return autoPickupFilter.isEmpty() || autoPickupFilter.contains(item.getType());
    }

    public List<String> getItemLore(Material material, int amount, FileConfiguration config, EconomyManager economyManager) {
        String defaultLore = config.getString("item_lore.default", "{item_name} x{amount}");
        List<String> lore = new ArrayList<>();

        if (config.isConfigurationSection("item_lore.custom." + material.name())) {
            ConfigurationSection customSection = config.getConfigurationSection("item_lore.custom." + material.name());
            if (customSection.isList("lore")) {
                lore = customSection.getStringList("lore");
            } else {
                lore.add(customSection.getString("lore", defaultLore));
            }
        } else {
            lore.add(config.getString("item_lore.custom." + material.name(), defaultLore));
        }

        List<String> processedLore = new ArrayList<>();
        for (String line : lore) {
            processedLore.add(ChatColor.translateAlternateColorCodes('&',
                    line.replace("{item_name}", material.name())
                            .replace("{amount}", String.valueOf(amount))
                            .replace("{price}", String.format("%.2f", economyManager.getItemPrice(material)))));

        }

        return processedLore;
    }


    // Getters and setters
    public String getBackpackId() {
        return backpackId;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isAutoPickupEnabled() {
        return this.autoPickupEnabled;
    }

    public void setAutoPickupEnabled(boolean enabled) {
        this.autoPickupEnabled = enabled;
    }

    public HashMap<Material, Integer> getContents() {
        return contents;
    }

    public double getAutoPickupRange() {
        return autoPickupRange;
    }

    public void setAutoPickupRange(double autoPickupRange) {
        this.autoPickupRange = autoPickupRange;
    }

    public void setUUID(UUID uuid) {
        // This method is used when loading from database
        if (this.uuid == null) {
            this.uuid = uuid;
        }
    }
}

