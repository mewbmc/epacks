package io.starseed.epacks.commands;

import io.starseed.epacks.Epacks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BackpackTabCompleter implements TabCompleter {
    private final Epacks plugin;

    public BackpackTabCompleter(Epacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("get");
            completions.add("upgrade");
            completions.add("help");
            completions.add("types");
            completions.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            plugin.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getConfig().getConfigurationSection("backpack_types").getKeys(false));
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
