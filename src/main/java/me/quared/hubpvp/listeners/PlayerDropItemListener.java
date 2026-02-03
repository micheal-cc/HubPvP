package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.core.PvPManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerDropItemListener implements Listener {

    @EventHandler
    public void handle(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        PvPManager pvpManager = HubPvP.getInstance().getPvpManager();

        if (pvpManager.isInPvP(player)) {
            if (item.isSimilar(pvpManager.getWeapon())) {
                event.setCancelled(true);
            } else if (item.getType().toString().toLowerCase().contains("armor")) { // very bad way of doing this, feel free to make a new branch to update
                event.setCancelled(true);
            }
        }
    }

}
