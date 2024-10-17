package io.starseed.epacks.GUI;

import io.starseed.epacks.Epacks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BackpackTypesGUI {
    private final Epacks plugin;

    public BackpackTypesGUI(Epacks plugin) {
        this.plugin = plugin;
    }

    public void openBackpackTypesGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, ChatColor.DARK_PURPLE + "Backpack Types");

        // Add border
        ItemStack borderItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderMeta);

        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, borderItem);
            }
        }

        // Add backpack types
        ConfigurationSection backpackTypes = plugin.getConfig().getConfigurationSection("backpack_types");
        if (backpackTypes != null) {
            int slot = 11;
            for (String type : backpackTypes.getKeys(false)) {
                if (slot % 9 == 7) slot += 3;
                if (slot >= 34) break;
                inventory.setItem(slot, createBackpackTypeItem(type));
                slot++;
            }
        }

        player.openInventory(inventory);
    }


    private ItemStack createBackpackTypeItem(String type) {
        ConfigurationSection typeConfig = plugin.getConfig().getConfigurationSection("backpack_types." + type);
        Material material = Material.valueOf(typeConfig.getString("material", "CHEST"));
        String name = ChatColor.translateAlternateColorCodes('&', typeConfig.getString("name", type));
        List<String> lore = typeConfig.getStringList("lore");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            line = line.replace("{capacity}", String.valueOf(typeConfig.getInt("capacity")));
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Backpack Types")) {
            event.setCancelled(true);
        }
    }
}
