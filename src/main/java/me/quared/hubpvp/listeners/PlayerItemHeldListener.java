package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.core.PvPManager;
import me.quared.hubpvp.core.PvPState;
import me.quared.hubpvp.util.StringUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class PlayerItemHeldListener implements Listener {

	@EventHandler
	public void handle(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		ItemStack held = event.getPlayer().getInventory().getItem(event.getNewSlot());
		HubPvP instance = HubPvP.getInstance();
		PvPManager pvpManager = instance.getPvpManager();

		if (!player.hasPermission("hubpvp.use")) return;

		if (Objects.equals(held, pvpManager.getWeapon())) {
			if (pvpManager.getPlayerState(player) == PvPState.DISABLING) pvpManager.setPlayerState(player, PvPState.ON);
			if (pvpManager.getPlayerState(player) == PvPState.ENABLING) return;

			if (HubPvP.getInstance().getConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
				player.sendMessage(StringUtil.colorize(instance.getConfig().getString("lang.disabled-in-world")));
				return;
			}

			// Equipping
			if (!pvpManager.isInPvP(player)) {
				pvpManager.setPlayerState(player, PvPState.ENABLING);
				BukkitRunnable enableTask = new BukkitRunnable() {
					int time = instance.getConfig().getInt("enable-cooldown") + 1;

					public void run() {
						time--;
						pvpManager.setCooldownTime(player, time); // Track cooldown time
						if (pvpManager.getPlayerState(player) != PvPState.ENABLING || !held.isSimilar(pvpManager.getWeapon())) {
							pvpManager.removeTimer(player);
							pvpManager.removeCooldownTime(player);
							cancel();
						} else if (time == 0) {
							pvpManager.enablePvP(player);
							pvpManager.removeTimer(player);
							pvpManager.removeCooldownTime(player);
							cancel();
						} else {
							player.sendMessage(StringUtil.colorize(instance.getConfig().getString("lang.pvp-enabling").replaceAll("%time%", Integer.toString(time))));
						}
					}
				};
				pvpManager.putTimer(player, enableTask);
				enableTask.runTaskTimer(instance, 0L, 20L);
			}
		} else if (pvpManager.isInPvP(player)) {
			if (pvpManager.getPlayerState(player) == PvPState.ENABLING) pvpManager.setPlayerState(player, PvPState.OFF);
			if (pvpManager.getPlayerState(player) == PvPState.DISABLING) return;
			// Dequipping
			pvpManager.setPlayerState(player, PvPState.DISABLING);
			BukkitRunnable disableTask = new BukkitRunnable() {
				int time = instance.getConfig().getInt("disable-cooldown") + 1;

				public void run() {
					time--;
					pvpManager.setCooldownTime(player, time); // Track cooldown time
					if (pvpManager.getPlayerState(player) != PvPState.DISABLING || held != null && held.isSimilar(pvpManager.getWeapon())) {
						pvpManager.removeTimer(player);
						pvpManager.removeCooldownTime(player);
						cancel();
					} else if (time == 0) {
						pvpManager.disablePvP(player);
						pvpManager.removeTimer(player);
						pvpManager.removeCooldownTime(player);
						cancel();
					} else {
						player.sendMessage(StringUtil.colorize(instance.getConfig().getString("lang.pvp-disabling").replaceAll("%time%", Integer.toString(time))));
					}
				}
			};
			pvpManager.putTimer(player, disableTask);
			disableTask.runTaskTimer(instance, 0L, 20L);
		} else {
			// Not in PvP and not equipping
			pvpManager.setPlayerState(player, PvPState.OFF); // Ensure there isn't any lingering state
			pvpManager.removeTimer(player);
		}
	}

}
