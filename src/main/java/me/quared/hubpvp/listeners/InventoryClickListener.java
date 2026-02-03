package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.core.PvPManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

	@EventHandler
	public void handle(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		ItemStack item = event.getCurrentItem();
		PvPManager pvpManager = HubPvP.getInstance().getPvpManager();
		if (item == null) return;

		if (pvpManager.isInPvP(player)) {
			if (item.isSimilar(pvpManager.getWeapon())) {
				event.setCancelled(true);
			} else if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
				event.setCancelled(true);
			}
		}
	}

}
