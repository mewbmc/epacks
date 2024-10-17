package io.starseed.epacks;

import io.starseed.epacks.GUI.BackpackGUI;
import io.starseed.epacks.GUI.BackpackTypesGUI;
import io.starseed.epacks.GUI.UpgradeGUI;
import io.starseed.epacks.backPack.Backpack;
import io.starseed.epacks.commands.BackpackAdminCommand;
import io.starseed.epacks.commands.BackpackCommand;
import io.starseed.epacks.commands.BackpackTabCompleter;
import io.starseed.epacks.managers.DatabaseManager;
import io.starseed.epacks.managers.EconomyManager;
import io.starseed.epacks.managers.LevelManager;
import io.starseed.epacks.managers.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;

public class Epacks extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private HashMap<String, Backpack> backpacks;
    private BackpackGUI backpackGUI;
    private EconomyManager economyManager;
    private DatabaseManager databaseManager;
    private LevelManager levelManager;
    private NamespacedKey backpackKey;
    private NamespacedKey backpackTypeKey;
    private UpgradeGUI upgradeGUI;
    private MessageManager messageManager;
    private BackpackTypesGUI backpackTypesGUI;
    private Logger transactionLogger;
    private Map<UUID, Long> backpackCooldowns = new HashMap<>();
    private static final long COOLDOWN_DURATION = 2000; // 2 seconds cooldown

    private boolean checkCooldown(Player player) {
        if (backpackCooldowns.containsKey(player.getUniqueId())) {
            long lastUse = backpackCooldowns.get(player.getUniqueId());
            if (System.currentTimeMillis() - lastUse < COOLDOWN_DURATION) {
                return false;
            }
        }
        backpackCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        startPeriodicSave();

        backpacks = new HashMap<>();
        upgradeGUI = new UpgradeGUI(this);
        backpackGUI = new BackpackGUI(this);
        economyManager = new EconomyManager(this);
        databaseManager = new DatabaseManager(this);
        levelManager = new LevelManager(this);
        messageManager = new MessageManager(this);
        backpackTypesGUI = new BackpackTypesGUI(this);

        getCommand("backpack").setExecutor(new BackpackCommand(this));
        getCommand("backpack").setTabCompleter(new BackpackTabCompleter(this));
        getCommand("backpackadmin").setExecutor(new BackpackAdminCommand(this));

        backpackKey = new NamespacedKey(this, "backpack_id");
        backpackTypeKey = new NamespacedKey(this, "backpack_type");

        getServer().getPluginManager().registerEvents(this, this);

        setupTransactionLogger();

        getLogger().info(messageManager.getMessage("plugin_enabled"));
    }

    @Override
    public void onDisable() {
        for (Backpack backpack : backpacks.values()) {
            databaseManager.saveBackpack(backpack);
        }
        databaseManager.close();
        getLogger().info(messageManager.getMessage("plugin_disabled"));
    }

    private void setupTransactionLogger() {
        try {
            FileHandler fileHandler = new FileHandler(getDataFolder() + "/transactions.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            transactionLogger = Logger.getLogger("EpacksTransactions");
            transactionLogger.addHandler(fileHandler);
        } catch (IOException e) {
            getLogger().severe("Could not set up transaction logger: " + e.getMessage());
        }
    }

    public void logTransaction(String playerName, String action, String backpackId) {
        if (transactionLogger != null) {
            transactionLogger.info(String.format("[%s] Player: %s, Action: %s, BackpackID: %s",
                    new Date(), playerName, action, backpackId));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isBackpackItem(item)) {
                event.setCancelled(true);
                openBackpack(player, item);
            }
        }
    }
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack pickedUpItem = event.getItem().getItemStack();

        for (ItemStack item : player.getInventory().getContents()) {
            if (isBackpackItem(item)) {
                String backpackId = getBackpackId(item);
                Backpack backpack = backpacks.get(backpackId);

                if (backpack != null && backpack.isAutoPickupEnabled() && backpack.canAutoPickup(pickedUpItem)) {
                    if (backpack.addItem(pickedUpItem)) {
                        event.setCancelled(true);
                        event.getItem().remove();

                        // Play auto-pickup sound
                        String soundName = getConfig().getString("sounds.auto_pickup.sound", "ENTITY_ITEM_PICKUP");
                        float volume = (float) getConfig().getDouble("sounds.auto_pickup.volume", 0.5);
                        float pitch = (float) getConfig().getDouble("sounds.auto_pickup.pitch", 1.2);
                        player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);

                        return;
                    }
                }
            }
        }
    }

    public boolean isBackpackItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(backpackKey, PersistentDataType.STRING);
    }

    public void openBackpack(Player player, ItemStack backpackItem) {
        if (!checkCooldown(player)) {
            player.sendMessage(messageManager.getMessage("backpack_cooldown"));
            return;
        }
        String backpackId = getBackpackId(backpackItem);
        Backpack backpack = backpacks.get(backpackId);

        if (backpack == null) {
            backpack = databaseManager.loadBackpack(backpackId);
            if (backpack == null) {
                backpack = createNewBackpack(backpackId, getBackpackType(backpackItem));
            }
            backpacks.put(backpackId, backpack);
        }

        backpackGUI.openBackpackGUI(player, backpack);
    }

    public ItemStack createBackpackItem(String type, String ownerName) {
        Material backpackMaterial = Material.valueOf(config.getString("backpack_item.material", "CHEST"));
        String backpackName = config.getString("backpack_item.name", "{owner}'s Backpack");
        backpackName = backpackName.replace("{owner}", ownerName);
        List<String> lore = config.getStringList("backpack_types." + type + ".lore");

        ItemStack backpackItem = new ItemStack(backpackMaterial);
        ItemMeta meta = backpackItem.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', backpackName));

        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            line = line.replace("{capacity}", String.valueOf(config.getInt("backpack_types." + type + ".capacity")));
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);

        String backpackId = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(backpackKey, PersistentDataType.STRING, backpackId);
        meta.getPersistentDataContainer().set(backpackTypeKey, PersistentDataType.STRING, type);
        backpackItem.setItemMeta(meta);

        return backpackItem;
    }

    public String getBackpackId(ItemStack backpackItem) {
        if (backpackItem == null || !backpackItem.hasItemMeta()) {
            return null;
        }
        return backpackItem.getItemMeta().getPersistentDataContainer().get(backpackKey, PersistentDataType.STRING);
    }

    public String getBackpackType(ItemStack backpackItem) {
        if (backpackItem == null || !backpackItem.hasItemMeta()) {
            return null;
        }
        return backpackItem.getItemMeta().getPersistentDataContainer().get(backpackTypeKey, PersistentDataType.STRING);
    }

    public ItemStack updateBackpackItem(ItemStack backpackItem, Backpack backpack) {
        ItemMeta meta = backpackItem.getItemMeta();
        List<String> lore = meta.getLore();
        lore.set(1, "Capacity: " + backpack.getCapacity());
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(backpackTypeKey, PersistentDataType.STRING, backpack.getType());
        backpackItem.setItemMeta(meta);
        return backpackItem;
    }

    private Backpack createNewBackpack(String backpackId, String type) {
        return new Backpack(this, backpackId, type, config);
    }

    public double sellBackpackContents(Player player, ItemStack backpackItem) {
        String backpackId = getBackpackId(backpackItem);
        Backpack backpack = backpacks.get(backpackId);
        if (backpack == null || backpack.getContents().isEmpty()) {
            player.sendMessage(messageManager.getMessage("backpack_empty"));
            return 0.0;
        }

        return economyManager.sellItems(player, backpack);
    }

    private void startPeriodicSave() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Backpack backpack : backpacks.values()) {
                databaseManager.saveBackpack(backpack);
            }
            getLogger().info("Periodic backpack save completed.");
        }, 6000L, 6000L); // Save every 5 minutes (6000 ticks)
    }


    public UpgradeGUI getUpgradeGUI() {
        return upgradeGUI;
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public HashMap<String, Backpack> getBackpacks() {
        return backpacks;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public BackpackTypesGUI getBackpackTypesGUI() {
        return backpackTypesGUI;
    }
}

