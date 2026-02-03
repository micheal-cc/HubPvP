package me.quared.hubpvp.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.core.PvPManager;
import me.quared.hubpvp.core.PvPState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for HubPvP
 * Provides placeholders like %hubpvp_status%, %hubpvp_state%, etc.
 */
public class HubPvPPlaceholders extends PlaceholderExpansion {

    private final HubPvP plugin;

    public HubPvPPlaceholders(HubPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hubpvp";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PvPManager pvpManager = plugin.getPvpManager();
        
        switch (params.toLowerCase()) {
            case "status":
                return getStatusSymbol(player, pvpManager);
                
            case "state":
                return getStateString(player, pvpManager);
                
            case "is_pvp":
                return pvpManager.isInPvP(player) ? "true" : "false";
                
            default:
                return null;
        }
    }

    /**
     * Gets the PvP status symbol - returns ᝵ if PvP is ON, empty string otherwise
     */
    private String getStatusSymbol(Player player, PvPManager pvpManager) {
        // Return the symbol only if PvP is actually ON
        if (pvpManager.isInPvP(player)) {
            PvPState state = pvpManager.getPlayerState(player);
            return (state == PvPState.ON) ? "᝵" : "";
        }
        
        return ""; // Empty string when PvP is OFF, ENABLING, or DISABLING
    }

    /**
     * Gets the state as a readable string
     */
    private String getStateString(Player player, PvPManager pvpManager) {
        if (!pvpManager.getPlayerPvpStates().containsKey(player)) {
            return "Off";
        }
        
        PvPState state = pvpManager.getPlayerState(player);
        
        return switch (state) {
            case ON -> "PvP On";
            case ENABLING -> "Activating";
            case DISABLING -> "Deactivating";
            case OFF -> "Off";
        };
    }


}