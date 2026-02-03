package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.core.PvPManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        PvPManager pvpManager = HubPvP.getInstance().getPvpManager();
        
        String title = event.getView().getTitle();
        
        // Ignora fechamento do inventário padrão do player
        if (title.equals("Crafting")) {
            return;
        }
        
        // Pequeno delay para garantir que o inventário foi completamente fechado
        // antes de tentar dar a espada
        org.bukkit.Bukkit.getScheduler().runTaskLater(HubPvP.getInstance(), () -> {
            if (player.isOnline()) {
                pvpManager.giveWeaponToPendingPlayer(player);
            }
        }, 1L);
    }
}