package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    @EventHandler
    public void handle(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection respawnSection = HubPvP.getInstance().getConfig().getConfigurationSection("respawn");

        if (!respawnSection.getBoolean("enabled")) return;

        if (respawnSection.getBoolean("use-world-spawn", false)) {
            event.setRespawnLocation(player.getWorld().getSpawnLocation());
        } else {
            Location spawn = new Location(
                    player.getWorld(),
                    respawnSection.getDouble("x"),
                    respawnSection.getDouble("y"),
                    respawnSection.getDouble("z"),
                    respawnSection.getInt("yaw"),
                    respawnSection.getInt("pitch")
            );
            event.setRespawnLocation(spawn);
        }
    }

}
