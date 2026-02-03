package me.quared.hubpvp.core;

import dev.lone.itemsadder.api.CustomStack;
import lombok.Getter;
import me.quared.hubpvp.HubPvP;
import me.quared.hubpvp.util.ItemUtil;
import me.quared.hubpvp.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class PvPManager {

	private final Map<Player, PvPState> playerPvpStates;
	private final Map<Player, BukkitRunnable> currentTimers;
	private final Map<Player, Integer> cooldownTimes;
	private final List<OldPlayerData> oldPlayerDataList;
	private final Set<Player> pendingWeaponPlayers;

	private ItemStack weapon, helmet, chestplate, leggings, boots;

	public PvPManager() {
		playerPvpStates = new HashMap<>();
		currentTimers = new HashMap<>();
		cooldownTimes = new HashMap<>();
		oldPlayerDataList = new ArrayList<>();
		pendingWeaponPlayers = new HashSet<>();

		loadItems();
	}

	public void loadItems() {
		// Load generic items section
		var cfg = HubPvP.getInstance().getConfig();
		var itemsSection = cfg.getConfigurationSection("items");

		// Weapon
		var wSec = itemsSection.getConfigurationSection("weapon");
		weapon = createItem(wSec, Material.DIAMOND_SWORD, "&cPvP Sword");

		// Armor pieces: helmet, chestplate, leggings, boots
		helmet = createItem(itemsSection.getConfigurationSection("helmet"), Material.DIAMOND_HELMET, " ");
		chestplate = createItem(itemsSection.getConfigurationSection("chestplate"), Material.DIAMOND_CHESTPLATE, " ");
		leggings = createItem(itemsSection.getConfigurationSection("leggings"), Material.DIAMOND_LEGGINGS, " ");
		boots = createItem(itemsSection.getConfigurationSection("boots"), Material.DIAMOND_BOOTS, " ");
	}

	private Enchantment parseEnchantment(String name) {
		if (name == null) return null;
		String n = name.trim().toLowerCase();
		String mapped = switch (n) {
			case "sharpness" -> "DAMAGE_ALL";
			case "smite" -> "DAMAGE_UNDEAD";
			case "bane_of_arthropods", "boas", "bane" -> "DAMAGE_ARTHROPODS";
			case "protection" -> "PROTECTION_ENVIRONMENTAL";
			case "projectile_protection" -> "PROTECTION_PROJECTILE";
			case "fire_protection" -> "PROTECTION_FIRE";
			case "blast_protection" -> "PROTECTION_EXPLOSIONS";
			case "unbreaking" -> "DURABILITY";
			case "mending" -> "MENDING";
			case "fire_aspect" -> "FIRE_ASPECT";
			case "knockback" -> "KNOCKBACK";
			case "efficiency" -> "DIG_SPEED";
			case "fortune" -> "LOOT_BONUS_BLOCKS";
			case "power" -> "ARROW_DAMAGE";
			case "infinity" -> "ARROW_INFINITE";
			default -> null;
		};

		if (mapped != null) {
			return Enchantment.getByName(mapped);
		}

		// Try direct lookup by uppercase name
		return Enchantment.getByName(name.toUpperCase().replace('-', '_'));
	}

	public void enablePvP(Player player) {
		setPlayerState(player, PvPState.ON);

		if (getOldData(player) != null) getOldPlayerDataList().remove(getOldData(player));
		getOldPlayerDataList().add(new OldPlayerData(player, player.getInventory().getArmorContents(), player.getAllowFlight()));

		player.setAllowFlight(false);
		player.getInventory().setHelmet(getHelmet());
		player.getInventory().setChestplate(getChestplate());
		player.getInventory().setLeggings(getLeggings());
		player.getInventory().setBoots(getBoots());

		player.sendMessage(StringUtil.colorize(HubPvP.getInstance().getConfig().getString("lang.pvp-enabled")));
	}

	public void setPlayerState(Player player, PvPState state) {
		playerPvpStates.put(player, state);
	}

	public OldPlayerData getOldData(Player player) {
		return oldPlayerDataList.stream().filter(data -> data.player().equals(player)).findFirst().orElse(null);
	}

	public void removePlayer(Player player) {
		disablePvP(player);
		playerPvpStates.remove(player);
		pendingWeaponPlayers.remove(player);
	}

	public void disablePvP(Player player) {
		setPlayerState(player, PvPState.OFF);

		OldPlayerData oldPlayerData = getOldData(player);
		if (oldPlayerData != null) {
			player.getInventory().setHelmet(oldPlayerData.armor()[3] == null ? new ItemStack(Material.AIR) : oldPlayerData.armor()[3]);
			player.getInventory().setChestplate(oldPlayerData.armor()[2] == null ? new ItemStack(Material.AIR) : oldPlayerData.armor()[2]);
			player.getInventory().setLeggings(oldPlayerData.armor()[1] == null ? new ItemStack(Material.AIR) : oldPlayerData.armor()[1]);
			player.getInventory().setBoots(oldPlayerData.armor()[0] == null ? new ItemStack(Material.AIR) : oldPlayerData.armor()[0]);
			player.setAllowFlight(oldPlayerData.canFly());
		}

		player.sendMessage(StringUtil.colorize(HubPvP.getInstance().getConfig().getString("lang.pvp-disabled")));
	}

	public void disable() {
		for (Player player : playerPvpStates.keySet()) {
			if (isInPvP(player)) {
				disablePvP(player);
			}
		}
		playerPvpStates.clear();
	}

	public boolean isInPvP(Player player) {
		return getPlayerState(player) == PvPState.ON || getPlayerState(player) == PvPState.DISABLING;
	}

	public PvPState getPlayerState(Player player) {
		return playerPvpStates.get(player);
	}

	public void giveWeapon(Player player) {
		player.getInventory().setItem(HubPvP.getInstance().getConfig().getInt("items.weapon.slot", 0), getWeapon());
	}

	public void giveWeaponIfNotPresent(Player player) {
		// Verifica se o jogador já tem a weapon no inventário
		if (hasWeapon(player)) {
			return;
		}

		// Verifica se a funcionalidade de esperar por fechamento de inventário está habilitada
		boolean waitForInventoryClose = HubPvP.getInstance().getConfig().getBoolean("wait-for-inventory-close", true);
		
		if (waitForInventoryClose) {
			// Verifica se o jogador tem um inventário/GUI aberto
			org.bukkit.inventory.InventoryView openInv = player.getOpenInventory();
			
			// Considera apenas GUIs customizados, não o inventário padrão do player
			boolean hasGUIOpen = false;
			if (openInv != null) {
				String title = openInv.getTitle();
				// Se não for o inventário padrão do crafting, é um GUI customizado
				hasGUIOpen = !title.equals("Crafting") && 
							 !openInv.getTopInventory().equals(player.getInventory());
			}
			
			if (hasGUIOpen) {
				// Player tem um GUI aberto, marca como pendente
				pendingWeaponPlayers.add(player);
				return;
			}
		}

		// Player não tem GUI aberto, entrega a espada
		giveWeapon(player);
	}

	public void giveWeaponToPendingPlayer(Player player) {
		if (pendingWeaponPlayers.contains(player)) {
			pendingWeaponPlayers.remove(player);
			
			// Verifica se ainda não tem a espada antes de dar
			if (!hasWeapon(player)) {
				giveWeapon(player);
			}
		}
	}

	public boolean hasWeapon(Player player) {
		ItemStack weapon = getWeapon();
		if (weapon == null) return false;
		
		for (ItemStack item : player.getInventory().getContents()) {
			if (item != null && weapon.isSimilar(item)) {
				return true;
			}
		}
		return false;
	}

	public void putTimer(Player player, BukkitRunnable timerTask) {
		if (getCurrentTimers().containsKey(player)) {
			getCurrentTimers().get(player).cancel();
		}
		getCurrentTimers().put(player, timerTask);
	}

	public void removeTimer(Player player) {
		if (getCurrentTimers().containsKey(player)) {
			getCurrentTimers().get(player).cancel();
		}
		getCurrentTimers().remove(player);
	}

	/**
	 * Creates an item from configuration section, supporting both Bukkit materials, Custom Model Data, and ItemsAdder custom items
	 */
	private ItemStack createItem(org.bukkit.configuration.ConfigurationSection section, Material fallback, String defaultName) {
		if (section == null) return new ItemStack(Material.AIR);

		// Check for ItemsAdder custom item first
		String itemsAdderIdKey = "itemsadder_id";
		if (section.contains(itemsAdderIdKey) && isItemsAdderAvailable()) {
			String itemsAdderId = section.getString(itemsAdderIdKey);
			if (itemsAdderId != null && !itemsAdderId.trim().isEmpty()) {
				ItemStack customItem = getItemsAdderItem(itemsAdderId);
				if (customItem != null) {
					// Apply custom name and lore from config if specified
					return applyCustomizations(customItem, section);
				} else {
					// ItemsAdder item not found, log warning and fall back to material
					Bukkit.getLogger().warning("[HubPvP] ItemsAdder item '" + itemsAdderId + "' not found! Falling back to material.");
				}
			}
		}

		// Fall back to regular Bukkit material
		String matName = section.getString("material", fallback.name());
		Material mat = Material.matchMaterial(matName);
		if (mat == null) mat = fallback;

		ItemUtil builder = new ItemUtil(mat)
				.setName(StringUtil.colorize(section.getString("name", defaultName)))
				.addItemFlag(ItemFlag.HIDE_UNBREAKABLE)
				.addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
				.addItemFlag(ItemFlag.HIDE_ENCHANTS)
				.setUnbreakable();

		// Apply Custom Model Data if specified (like DeluxeHubReloaded)
		if (section.contains("custom_model_data")) {
			int modelData = section.getInt("custom_model_data", -1);
			if (modelData != -1) {
				builder.setCustomModelData(modelData);
			}
		}

		// Apply lore if configured
		java.util.List<String> lore = section.getStringList("lore");
		if (lore != null && !lore.isEmpty()) {
			java.util.List<String> coloredLore = new java.util.ArrayList<>();
			for (String line : lore) {
				coloredLore.add(StringUtil.colorize(line));
			}
			builder.setLore(coloredLore);
		}

		// Apply enchantments from config (if any)
		for (String ench : section.getStringList("enchantments")) {
			if (ench == null || ench.isBlank()) continue;
			String[] parts = ench.split(":");
			String name = parts[0].trim();
			int level = 1;
			if (parts.length > 1) {
				try { level = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) {}
			}
			Enchantment e = parseEnchantment(name);
			if (e != null) builder.addEnchantment(e, level);
		}

		return builder.toItemStack();
	}

	/**
	 * Applies customizations from config to an ItemsAdder item
	 */
	private ItemStack applyCustomizations(ItemStack baseItem, org.bukkit.configuration.ConfigurationSection section) {
		ItemUtil builder = new ItemUtil(baseItem);

		// Apply custom name if specified
		if (section.contains("name")) {
			String customName = section.getString("name");
			if (customName != null && !customName.trim().isEmpty()) {
				builder.setName(StringUtil.colorize(customName));
			}
		}

		// Apply Custom Model Data if specified (overrides ItemsAdder model)
		if (section.contains("custom_model_data")) {
			int modelData = section.getInt("custom_model_data", -1);
			if (modelData != -1) {
				builder.setCustomModelData(modelData);
			}
		}

		// Apply lore if configured
		java.util.List<String> lore = section.getStringList("lore");
		if (lore != null && !lore.isEmpty()) {
			java.util.List<String> coloredLore = new java.util.ArrayList<>();
			for (String line : lore) {
				coloredLore.add(StringUtil.colorize(line));
			}
			builder.setLore(coloredLore);
		}

		// Apply enchantments from config (if any)
		for (String ench : section.getStringList("enchantments")) {
			if (ench == null || ench.isBlank()) continue;
			String[] parts = ench.split(":");
			String name = parts[0].trim();
			int level = 1;
			if (parts.length > 1) {
				try { level = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) {}
			}
			Enchantment e = parseEnchantment(name);
			if (e != null) builder.addEnchantment(e, level);
		}

		// Ensure PvP item flags are applied
		builder.addItemFlag(ItemFlag.HIDE_UNBREAKABLE)
				.addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
				.addItemFlag(ItemFlag.HIDE_ENCHANTS)
				.setUnbreakable();

		return builder.toItemStack();
	}

	/**
	 * Gets an ItemsAdder custom item by its ID
	 */
	private ItemStack getItemsAdderItem(String itemId) {
		try {
			CustomStack stack = CustomStack.getInstance(itemId);
			return stack != null ? stack.getItemStack() : null;
		} catch (Exception e) {
			Bukkit.getLogger().warning("[HubPvP] Error getting ItemsAdder item '" + itemId + "': " + e.getMessage());
			return null;
		}
	}

	/**
	 * Checks if ItemsAdder plugin is available
	 */
	private boolean isItemsAdderAvailable() {
		return Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
	}

	/**
	 * Sets the cooldown time for a player
	 */
	public void setCooldownTime(Player player, int time) {
		cooldownTimes.put(player, time);
	}

	/**
	 * Gets the remaining cooldown time for a player
	 */
	public int getCooldownTime(Player player) {
		return cooldownTimes.getOrDefault(player, 0);
	}

	/**
	 * Removes cooldown time for a player
	 */
	public void removeCooldownTime(Player player) {
		cooldownTimes.remove(player);
	}

}
