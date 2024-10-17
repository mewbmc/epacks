package io.starseed.epacks.commands;

import io.starseed.epacks.Epacks;
import io.starseed.epacks.backPack.Backpack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BackpackCommand implements CommandExecutor {
    private final Epacks plugin;

    public BackpackCommand(Epacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (player.hasPermission("epacks.use")) {
                ItemStack backpackItem = player.getInventory().getItemInMainHand();
                if (plugin.isBackpackItem(backpackItem)) {
                    plugin.openBackpack(player, backpackItem);
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("holding_backpack"));
                }
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "get":
                if (player.hasPermission("epacks.get")) {
                    giveBackpackItem(player, args.length > 1 ? args[1] : "basic");
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                }
                break;
            case "give":
                if (player.hasPermission("epacks.give")) {
                    if (args.length < 3) {
                        player.sendMessage(plugin.getMessageManager().getMessage("invalid_give_command"));
                        return true;
                    }
                    String targetPlayerName = args[1];
                    String backpackType = args[2];
                    Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
                    if (targetPlayer == null) {
                        player.sendMessage(plugin.getMessageManager().getMessage("player_not_found"));
                        return true;
                    }
                    giveBackpackItem(targetPlayer, backpackType);
                    player.sendMessage(plugin.getMessageManager().getMessage("backpack_given", "player", targetPlayerName, "type", backpackType));
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                }
                break;

            case "types":
                if (player.hasPermission("epacks.types")) {
                    plugin.getBackpackTypesGUI().openBackpackTypesGUI(player);
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                }
                break;
            case "reload":
                if (player.hasPermission("epacks.reload")) {
                    reloadConfig(player);
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                }
                break;
            case "upgrade":
                if (player.hasPermission("epacks.upgrade")) {
                    ItemStack backpackItem = player.getInventory().getItemInMainHand();
                    if (plugin.isBackpackItem(backpackItem)) {
                        String backpackId = plugin.getBackpackId(backpackItem);
                        Backpack backpack = plugin.getBackpacks().get(backpackId);
                        plugin.getUpgradeGUI().openUpgradeGUI(player, backpack);
                    } else {
                        player.sendMessage(plugin.getMessageManager().getMessage("holding_backpack"));
                    }
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                }
                break;

            case "help":
                sendHelpMessage(player);
                break;
            default:
                player.sendMessage(plugin.getMessageManager().getMessage("unknown_command"));
        }
        return true;
    }

    private void giveBackpackItem(Player player, String type) {
        ItemStack backpackItem = plugin.createBackpackItem(type, player.getName());
        player.getInventory().addItem(backpackItem);
        player.sendMessage(plugin.getMessageManager().getMessage("backpack_received", "type", type));
    }

    private void upgradeBackpack(Player player) {
        ItemStack backpackItem = player.getInventory().getItemInMainHand();
        if (!plugin.isBackpackItem(backpackItem)) {
            player.sendMessage("You must be holding a backpack to upgrade it.");
            return;
        }

        String backpackId = plugin.getBackpackId(backpackItem);
        Backpack backpack = plugin.getBackpacks().get(backpackId);

        if (backpack == null) {
            player.sendMessage("This backpack doesn't seem to be registered. Please contact an administrator.");
            return;
        }

        if (plugin.getLevelManager().canUpgradeBackpack(player, backpack)) {
            if (backpack.upgrade(plugin.getConfig())) {
                player.sendMessage("Your backpack has been upgraded!");
                // Update the item in the player's hand to reflect the upgrade
                player.getInventory().setItemInMainHand(plugin.updateBackpackItem(backpackItem, backpack));
            } else {
                player.sendMessage("Your backpack is already at the highest level!");
            }
        } else {
            player.sendMessage("You don't meet the level requirement to upgrade your backpack!");
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("Backpack Commands:");
        player.sendMessage("/backpack - Open the backpack you're holding");
        player.sendMessage("/backpack get [type] - Get a backpack item");
        player.sendMessage("/backpack upgrade - Upgrade the backpack you're holding");
        player.sendMessage("/backpack types - View available backpack types");
        player.sendMessage("/backpack give <player> <type> - Give a backpack to a player");
        player.sendMessage("/backpack help - Show this help message");
        player.sendMessage("/backpack reload - Reload the plugin configuration");

    }

    private void reloadConfig(Player player) {
        plugin.reloadConfig();
        plugin.getMessageManager().reloadMessages();
        player.sendMessage(plugin.getMessageManager().getMessage("config_reloaded"));
    }

}

