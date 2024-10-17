package io.starseed.epacks.GUI;

import com.samjakob.spigui.SpiGUI;
import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import io.starseed.epacks.Epacks;
import io.starseed.epacks.backPack.Backpack;
import io.starseed.epacks.managers.EconomyManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BackpackGUI {
    private final Epacks plugin;
    private final SpiGUI spiGUI;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 200; // 200 milliseconds cooldown
    private final EconomyManager economyManager = null;


    public BackpackGUI(Epacks plugin) {
        this.plugin = plugin;
        this.spiGUI = new SpiGUI(plugin);


    }

    private boolean checkCooldown(Player player) {
        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(player.getUniqueId())) {
            long lastClickTime = cooldowns.get(player.getUniqueId());
            if (currentTime - lastClickTime < COOLDOWN_TIME) {
                return false;
            }
        }
        cooldowns.put(player.getUniqueId(), currentTime);
        return true;
    }

    public void openBackpackGUI(Player player, Backpack backpack) {
        SGMenu menu = spiGUI.create("Your Backpack", 6);

        // Set up border
        for (int i = 0; i < 54; i++) {
            if (i < 10 || i > 43 || i % 9 == 0 || i % 9 == 8) {
                menu.setButton(i, createBorderItem());
            }
        }

        // Add backpack contents
        int slot = 10;
        for (Map.Entry<Material, Integer> entry : backpack.getContents().entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            menu.setButton(slot, createContentItem(backpack, material, amount));
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot > 43) break;
        }

        // Add function buttons
        menu.setButton(48, createSellButton(backpack));
        menu.setButton(49, createUpgradeButton(backpack));
        int infoSlot = plugin.getConfig().getInt("backpack_gui.chest_info.slot", 4);
        menu.setButton(infoSlot, createChestInfoItem(backpack));
        menu.setButton(50, createAutoPickupToggleButton(backpack));
        int showcaseSlot = plugin.getConfig().getInt("backpack_gui.showcase_button.slot", 51);
        menu.setButton(showcaseSlot, createShowcaseButton());

        player.openInventory(menu.getInventory());
    }

    private SGButton createBorderItem() {
        return new SGButton(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }

    private SGButton createContentItem(Backpack backpack, Material material, int amount) {
        ItemStack item = new ItemStack(material, 1);
        List<String> lore = backpack.getItemLore(material, amount, plugin.getConfig(), plugin.getEconomyManager());
        return new SGButton(new ItemBuilder(item)
                .name(material.name())
                .lore(lore)
                .build());
    }


    private SGButton createShowcaseButton() {
        FileConfiguration config = plugin.getConfig();
        String materialName = config.getString("backpack_gui.showcase_button.material", "BOOK");
        Material material = Material.valueOf(materialName);
        String name = config.getString("backpack_gui.showcase_button.name", "Backpack Types Showcase");
        List<String> lore = config.getStringList("backpack_gui.showcase_button.lore");

        return new SGButton(new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build())
                .withListener((InventoryClickEvent event) -> {
                    Player player = (Player) event.getWhoClicked();
                    if (checkCooldown(player)) {
                        plugin.getBackpackTypesGUI().openBackpackTypesGUI(player);
                    }
                });
    }

    private SGButton createUpgradeButton(Backpack backpack) {
        return new SGButton(new ItemBuilder(Material.DIAMOND)
                .name("Upgrade Backpack")
                .lore("Click to upgrade your backpack")
                .build())
                .withListener((InventoryClickEvent event) -> {
                    Player player = (Player) event.getWhoClicked();
                    if (checkCooldown(player)) {
                        plugin.getUpgradeGUI().openUpgradeGUI(player, backpack);
                    }
                });
    }
    private SGButton createSellButton(Backpack backpack) {
        return new SGButton(new ItemBuilder(Material.GOLD_INGOT)
                .name(plugin.getMessageManager().getMessage("sell_button_name"))
                .lore(plugin.getMessageManager().getMessage("sell_button_lore"))
                .build())
                .withListener((InventoryClickEvent event) -> {
                    Player player = (Player) event.getWhoClicked();
                    if (checkCooldown(player)) {
                        ItemStack backpackItem = player.getInventory().getItemInMainHand();
                        double earnings = plugin.sellBackpackContents(player, backpackItem);
                        player.sendMessage(plugin.getMessageManager().getMessage("sold_contents", "amount", String.format("%.2f", earnings)));
                        openBackpackGUI(player, backpack);
                    }
                });
    }

    private SGButton createChestInfoItem(Backpack backpack) {
        FileConfiguration config = plugin.getConfig();
        String materialName = config.getString("backpack_gui.chest_info.material", "CHEST");
        Material material = Material.valueOf(materialName);
        String name = config.getString("backpack_gui.chest_info.name", "Backpack Info");
        List<String> lore = config.getStringList("backpack_gui.chest_info.lore");

        double sellMultiplier = backpack.getSellMultiplier(config);

        lore = lore.stream().map(line -> line
                .replace("{type}", backpack.getType())
                .replace("{capacity}", String.valueOf(backpack.getCapacity()))
                .replace("{current_items}", String.valueOf(backpack.getTotalItems()))
                .replace("{max_capacity}", String.valueOf(backpack.getCapacity()))
                .replace("{auto_pickup_status}", backpack.isAutoPickupEnabled() ? "Enabled" : "Disabled")
                .replace("{sell_multiplier}", String.format("%.2fx", sellMultiplier))
        ).collect(Collectors.toList());

        return new SGButton(new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build());
    }


    private SGButton createAutoPickupToggleButton(Backpack backpack) {
        Material material = backpack.isAutoPickupEnabled() ? Material.HOPPER : Material.BARRIER;
        String status = backpack.isAutoPickupEnabled() ? "enabled" : "disabled";
        return new SGButton(new ItemBuilder(material)
                .name(plugin.getMessageManager().getMessage("auto_pickup_button_name", "status", status))
                .lore(plugin.getMessageManager().getMessage("auto_pickup_button_lore"))
                .build())
                .withListener((InventoryClickEvent event) -> {
                    Player player = (Player) event.getWhoClicked();
                    if (checkCooldown(player)) {
                        backpack.setAutoPickupEnabled(!backpack.isAutoPickupEnabled());
                        plugin.getDatabaseManager().saveBackpack(backpack);
                        player.sendMessage(plugin.getMessageManager().getMessage(backpack.isAutoPickupEnabled() ? "auto_pickup_enabled" : "auto_pickup_disabled"));
                        openBackpackGUI(player, backpack);
                    }
                });
    }
}
