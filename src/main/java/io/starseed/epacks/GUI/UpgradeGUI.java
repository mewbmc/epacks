package io.starseed.epacks.GUI;

import com.samjakob.spigui.SpiGUI;
import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import io.starseed.epacks.Epacks;
import io.starseed.epacks.backPack.Backpack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.apache.commons.lang.StringUtils;

public class UpgradeGUI {
    private final Epacks plugin;
    private final SpiGUI spiGUI;

    public UpgradeGUI(Epacks plugin) {
        this.plugin = plugin;
        this.spiGUI = new SpiGUI(plugin);
    }

    private SGButton createBorderItem() {
        return new SGButton(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    public void openUpgradeGUI(Player player, Backpack backpack) {
        SGMenu menu = spiGUI.create("Backpack Upgrades", 3);

        // Add border
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                menu.setButton(i, createBorderItem());
            }
        }

        menu.setButton(11, createUpgradeButton(player, backpack, "capacity"));
        menu.setButton(15, createUpgradeButton(player, backpack, "sell_multiplier"));

        player.openInventory(menu.getInventory());
    }


    private SGButton createUpgradeButton(Player player, Backpack backpack, String upgradeType) {
        FileConfiguration config = plugin.getConfig();
        int currentLevel = backpack.getUpgradeLevel(upgradeType);
        int maxLevel = config.getInt("upgrades." + upgradeType + ".max_level");
        double basePrice = config.getDouble("upgrades." + upgradeType + ".base_price");
        double priceMultiplier = config.getDouble("upgrades." + upgradeType + ".price_multiplier");

        double currentValue = upgradeType.equals("capacity") ? backpack.getCapacity() : calculateCurrentValue(backpack, upgradeType);
        double nextValue = calculateNextValue(backpack, upgradeType);
        double upgradeCost = calculateUpgradeCost(currentLevel, basePrice, priceMultiplier);

        String progressBar = createProgressBar(currentLevel, maxLevel);

        Material buttonMaterial = upgradeType.equals("capacity") ? Material.CHEST : Material.DIAMOND;

        return new SGButton(new ItemBuilder(buttonMaterial)
                .name(ChatColor.GOLD + StringUtils.capitalize(upgradeType) + " Upgrade")
                .lore(
                    ChatColor.GRAY + "Current: " + ChatColor.GREEN + String.format("%.2f", currentValue),
                    ChatColor.GRAY + "Next: " + ChatColor.YELLOW + String.format("%.2f", nextValue),
                    ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + String.format("%.2f", upgradeCost),
                    "",
                    ChatColor.GRAY + "Progress: " + progressBar,
                    ChatColor.GRAY + "Level: " + currentLevel + "/" + maxLevel
                )
                .build())
                .withListener(event -> {
                    if (plugin.getEconomyManager().hasEnough(player, upgradeCost)) {
                        if (backpack.upgradeAttribute(upgradeType)) {
                            plugin.getEconomyManager().withdraw(player, upgradeCost);
                            player.sendMessage(ChatColor.GREEN + "Upgrade successful!");
                            if (upgradeType.equals("capacity")) {
                                backpack.updateCapacity();
                            }
                            openUpgradeGUI(player, backpack);
                        } else {
                            player.sendMessage(ChatColor.RED + "Maximum upgrade level reached!");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Not enough money for this upgrade!");
                    }
                });
    }

    private String createProgressBar(int currentLevel, int maxLevel) {
        int barLength = 20;
        int filledBars = (int) ((double) currentLevel / maxLevel * barLength);
        StringBuilder bar = new StringBuilder(ChatColor.GREEN.toString());
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append("■");
            } else {
                bar.append(ChatColor.GRAY).append("■");
            }
        }
        return bar.toString();
    }

    private double calculateCurrentValue(Backpack backpack, String upgradeType) {
        if (upgradeType.equals("capacity")) {
            return backpack.getCapacity() * backpack.getCapacityMultiplier(plugin.getConfig());
        } else if (upgradeType.equals("sell_multiplier")) {
            return backpack.getSellMultiplier(plugin.getConfig());
        }
        return 0;
    }

    private double calculateNextValue(Backpack backpack, String upgradeType) {
        int nextLevel = backpack.getUpgradeLevel(upgradeType) + 1;
        FileConfiguration config = plugin.getConfig();
        if (upgradeType.equals("capacity")) {
            int baseCapacity = config.getInt("backpack_types." + backpack.getType() + ".capacity");
            double increasePerLevel = config.getDouble("upgrades.capacity.increase_per_level", 100);
            return baseCapacity + (nextLevel * increasePerLevel);
        } else if (upgradeType.equals("sell_multiplier")) {
            double baseIncrease = config.getDouble("upgrades.sell_multiplier.base_increase");
            double increaseMultiplier = config.getDouble("upgrades.sell_multiplier.increase_multiplier");
            return 1 + (baseIncrease * Math.pow(increaseMultiplier, nextLevel));
        }
        return 0;
    }

    private double calculateUpgradeCost(int currentLevel, double basePrice, double priceMultiplier) {
        return basePrice * Math.pow(priceMultiplier, currentLevel);
    }
}