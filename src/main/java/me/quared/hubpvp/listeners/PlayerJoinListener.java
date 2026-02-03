package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.core.OldPlayerData;
import me.quared.hubpvp.core.PvPManager;
import me.quared.hubpvp.core.PvPState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PvPManager pvpManager = HubPvP.getInstance().getPvpManager();

        // Cooldown configurável para entrega da sword
        int cooldownTicks = HubPvP.getInstance().getConfig().getInt("join-weapon-cooldown", 1) * 20;
        
        Bukkit.getScheduler().runTaskLater(HubPvP.getInstance(), () -> {
            if (player.hasPermission("hubpvp.use") && !HubPvP.getInstance().getConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
                // Usa giveWeaponIfNotPresent para evitar duplicação
                pvpManager.giveWeaponIfNotPresent(player);
            }
        }, cooldownTicks);

        pvpManager.getOldPlayerDataList().add(new OldPlayerData(player, player.getInventory().getArmorContents(), player.getAllowFlight()));
        pvpManager.setPlayerState(player, PvPState.OFF);
    }

}
