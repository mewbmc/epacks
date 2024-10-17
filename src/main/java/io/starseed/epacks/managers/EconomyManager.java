package io.starseed.epacks.managers;

import io.starseed.epacks.Epacks;
import io.starseed.epacks.backPack.Backpack;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

import java.util.HashMap;
import java.util.Map;

public class EconomyManager {
    private final Epacks plugin;
    private Economy economy;
    private Map<Material, Double> itemPrices;

    public EconomyManager(Epacks plugin) {
        this.plugin = plugin;
        setupEconomy();
        loadItemPrices();
    }

    public Epacks getPlugin() {
        return plugin;
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void loadItemPrices() {
        itemPrices = new HashMap<>();
        FileConfiguration config = plugin.getConfig();
        double defaultPrice = config.getDouble("economy.default_sell_price", 1.0);
        for (Material material : Material.values()) {
            String path = "economy.custom_prices." + material.name();
            double price = config.getDouble(path, defaultPrice);
            itemPrices.put(material, price);
        }
    }

    public double getItemPrice(Material material) {
        return itemPrices.getOrDefault(material, 1.0);
    }

    public double sellItems(Player player, Backpack backpack) {
        double totalValue = 0.0;
        double sellMultiplier = backpack.getSellMultiplier(plugin.getConfig());
        for (Map.Entry<Material, Integer> entry : backpack.getContents().entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            double price = itemPrices.getOrDefault(material, 1.0);
            double value = price * amount * sellMultiplier;
            totalValue += value;
        }
        if (economy.depositPlayer(player, totalValue).transactionSuccess()) {
            backpack.getContents().clear();

            // Play sound
            String soundName = plugin.getConfig().getString("sounds.sell_items.sound", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) plugin.getConfig().getDouble("sounds.sell_items.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sounds.sell_items.pitch", 1.0);
            player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);

            // Show popup message
            String popupMessage = plugin.getMessageManager().getMessage("sell_popup", "amount", String.format("%.2f", totalValue));
            player.sendTitle("", popupMessage, 10, 40, 10);

            return totalValue;

        }
        player.sendMessage(plugin.getMessageManager().getMessage("sale_failed"));
        return 0.0;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public boolean hasEnough(Player player, double amount) {
        return economy.has(player, amount);
    }

    public void withdraw(Player player, double amount) {
        if (economy.withdrawPlayer(player, amount).transactionSuccess()) {
            player.sendMessage(plugin.getMessageManager().getMessage("money_withdrawn", "amount", String.format("%.2f", amount)));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("withdrawal_failed"));
        }
    }
}