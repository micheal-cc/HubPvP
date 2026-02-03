package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.core.PvPManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageByEntityListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH) // WORK ON WHITELIST/BLACKLISTED WORLDS
	public void handle(EntityDamageByEntityEvent event) {
		HubPvP instance = HubPvP.getInstance();
		PvPManager pvpManager = instance.getPvpManager();

		if (event.getEntity() instanceof Player damager && event.getDamager() instanceof Player damaged) {
			World world = damager.getLocation().getWorld();
			if (instance.getConfig().getStringList("disabled-worlds").contains(world.getName())) event.setCancelled(true);

			event.setCancelled(!pvpManager.isInPvP(damager) || !pvpManager.isInPvP(damaged));
		}
	}

}
