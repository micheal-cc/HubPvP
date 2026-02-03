package me.quared.hubpvp.commands;

import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class HubPvPCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (!(sender.isOp())) return false;

		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("reload")) {
				HubPvP plugin = HubPvP.getInstance();
				plugin.reloadConfig();
				plugin.getPvpManager().loadItems();
				sender.sendMessage(StringUtil.colorize(plugin.getConfig().getString("lang.reloaded")));
				return false;
			}
		}

		sender.sendMessage(ChatColor.RED + "Invalid usage. Use: /" + label + " <args>");
		return false;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (args.length <= 1) {
			return Collections.singletonList("reload");
		}
		return null;
	}

}
