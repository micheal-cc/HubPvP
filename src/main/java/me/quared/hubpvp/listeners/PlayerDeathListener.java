package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.core.PvPManager;
import me.quared.hubpvp.util.StringUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Objects;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void handle(PlayerDeathEvent event) {
        HubPvP instance = HubPvP.getInstance();
        PvPManager pvpManager = instance.getPvpManager();

        if (event.getEntity().getKiller() == null) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (!pvpManager.isInPvP(victim) || !pvpManager.isInPvP(killer)) return;

        int healthOnKill = instance.getConfig().getInt("health-on-kill");

        event.setKeepInventory(true);
        event.setKeepLevel(true);

        victim.getInventory().setHeldItemSlot(0);

        if (healthOnKill != -1) {
            killer.setHealth(Math.min(killer.getHealth() + healthOnKill, Objects.requireNonNull(killer.getAttribute(Attribute.MAX_HEALTH)).getValue()));
            killer.sendMessage(StringUtil.colorize(Objects.requireNonNull(instance.getConfig().getString("health-gained-message")).replace("%extra%", String.valueOf(healthOnKill)).replace("%killed%", victim.getDisplayName())));
        }

        pvpManager.disablePvP(victim);

        victim.sendMessage(StringUtil.colorize(instance.getConfig().getString("lang.killed")).replace("%killer%", killer.getDisplayName()));
        killer.sendMessage(StringUtil.colorize(instance.getConfig().getString("lang.killed-other")).replace("%killed%", victim.getDisplayName()));

        event.setDeathMessage("");
    }

}
