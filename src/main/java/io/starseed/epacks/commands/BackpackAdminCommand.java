package io.starseed.epacks.commands;

import io.starseed.epacks.Epacks;
import io.starseed.epacks.backPack.Backpack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BackpackAdminCommand implements CommandExecutor {
    private final Epacks plugin;

    public BackpackAdminCommand(Epacks plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("admin_command_usage"));
            return true;
        }

        Player targetPlayer = plugin.getServer().getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player_not_found"));
            return true;
        }

        List<ItemStack> backpacks = getPlayerBackpacks(targetPlayer);
        if (backpacks.isEmpty()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player_no_backpack"));
            return true;
        }

        if (backpacks.size() == 1) {
            handleAdminAction(sender, targetPlayer, backpacks.get(0), args[1], Arrays.copyOfRange(args, 2, args.length));
        } else {
            openBackpackSelectionGUI((Player) sender, targetPlayer, args[1], Arrays.copyOfRange(args, 2, args.length));
        }

        return true;
    }
    private List<ItemStack> getPlayerBackpacks(Player player) {
        List<ItemStack> backpacks = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.isBackpackItem(item)) {
                backpacks.add(item);
            }
        }
        return backpacks;
    }

    private void viewBackpack(CommandSender sender, Player targetPlayer, Backpack backpack) {
        sender.sendMessage(plugin.getMessageManager().getMessage("admin_view_backpack_header", "player", targetPlayer.getName()));
        for (Map.Entry<Material, Integer> entry : backpack.getContents().entrySet()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("admin_view_backpack_item",
                    "item", entry.getKey().toString(), "amount", entry.getValue().toString()));
        }
    }

    private void clearBackpack(CommandSender sender, Player targetPlayer, Backpack backpack) {
        backpack.getContents().clear();
        plugin.getDatabaseManager().saveBackpack(backpack);
        sender.sendMessage(plugin.getMessageManager().getMessage("admin_clear_backpack_success", "player", targetPlayer.getName()));
        targetPlayer.sendMessage(plugin.getMessageManager().getMessage("backpack_cleared_by_admin"));
    }

    private void modifyBackpack(CommandSender sender, Player targetPlayer, Backpack backpack, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().getMessage("admin_modify_usage"));
            return;
        }

        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid_material"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid_amount"));
            return;
        }

        if (args[0].equalsIgnoreCase("add")) {
            backpack.addItem(new ItemStack(material, amount));
        } else if (args[0].equalsIgnoreCase("remove")) {
            backpack.removeItem(new ItemStack(material, amount));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid_modify_action"));
            return;
        }

        plugin.getDatabaseManager().saveBackpack(backpack);
        sender.sendMessage(plugin.getMessageManager().getMessage("admin_modify_backpack_success",
                "player", targetPlayer.getName(), "action", args[0], "item", material.toString(), "amount", Integer.toString(amount)));
        targetPlayer.sendMessage(plugin.getMessageManager().getMessage("backpack_modified_by_admin"));
    }

    private ItemStack findBackpackItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.isBackpackItem(item)) {
                return item;
            }
        }
        return null;
    }

    private void openBackpackSelectionGUI(Player admin, Player targetPlayer, String action, String[] additionalArgs) {
        Inventory gui = Bukkit.createInventory(null, 27, "Select " + targetPlayer.getName() + "'s Backpack");

        List<ItemStack> backpacks = getPlayerBackpacks(targetPlayer);
        for (int i = 0; i < backpacks.size(); i++) {
            gui.setItem(i, backpacks.get(i));
        }

        admin.openInventory(gui);

        // Store the action and additional args for later use
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onInventoryClick(InventoryClickEvent event) {
                if (event.getView().getTitle().equals("Select " + targetPlayer.getName() + "'s Backpack")) {
                    event.setCancelled(true);
                    if (event.getCurrentItem() != null && plugin.isBackpackItem(event.getCurrentItem())) {
                        handleAdminAction(admin, targetPlayer, event.getCurrentItem(), action, additionalArgs);
                        admin.closeInventory();
                        HandlerList.unregisterAll(this);
                    }
                }
            }
        }, plugin);
    }

    private void handleAdminAction(CommandSender sender, Player targetPlayer, ItemStack backpackItem, String action, String[] args) {
        String backpackId = plugin.getBackpackId(backpackItem);
        Backpack backpack = plugin.getBackpacks().get(backpackId);
        if (backpack == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("backpack_not_found"));
            return;
        }

        switch (action.toLowerCase()) {
            case "view":
                viewBackpack(sender, targetPlayer, backpack);
                break;
            case "clear":
                clearBackpack(sender, targetPlayer, backpack);
                break;
            case "modify":
                modifyBackpack(sender, targetPlayer, backpack, args);
                break;
            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("invalid_admin_action"));
        }
    }


}

