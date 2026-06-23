package com.stasis.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;

import java.util.*;

public class StasisManager {

    private final StasisPlugin plugin;
    private final NamespacedKey stasisKey;
    private final NamespacedKey stasisIdKey;

    // Maps stasis ID -> StasisEntry
    private final Map<String, StasisEntry> activeStasis = new HashMap<>();
    // Maps player UUID -> stasis ID (so we can look up by player)
    private final Map<UUID, String> playerToStasis = new HashMap<>();

    public StasisManager(StasisPlugin plugin) {
        this.plugin = plugin;
        this.stasisKey = new NamespacedKey(plugin, "stasis_rod");
        this.stasisIdKey = new NamespacedKey(plugin, "stasis_id");
    }

    public NamespacedKey getStasisKey() {
        return stasisKey;
    }

    public NamespacedKey getStasisIdKey() {
        return stasisIdKey;
    }

    /**
     * Activate stasis for a player standing on a pressure plate while holding a fishing rod.
     * Returns true on success, false with a reason message otherwise.
     */
    public boolean activateStasis(Player player, String[] result) {
        UUID uuid = player.getUniqueId();

        // Check if already in stasis
        if (playerToStasis.containsKey(uuid)) {
            result[0] = "§cYou already have an active stasis chamber! Use your Stasis Rod to release it first.";
            return false;
        }

        // Must be holding a fishing rod
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.FISHING_ROD) {
            result[0] = "§cYou must be holding a §5Fishing Rod§c to activate stasis!";
            return false;
        }

        // Find the pressure plate under the player
        Block plateBlock = getPressurePlateUnder(player);
        if (plateBlock == null) {
            result[0] = "§cYou must be standing on a §5Pressure Plate§c to activate stasis!";
            return false;
        }

        // Generate unique stasis ID
        String stasisId = uuid.toString() + "_" + System.currentTimeMillis();

        // Mark the fishing rod with stasis data
        ItemStack stasisRod = markRodAsStasis(held, stasisId);
        player.getInventory().setItemInMainHand(stasisRod);

        // Force pressure plate powered state
        forcePlatePowered(plateBlock, true);

        // Store entry
        Location plateLocation = plateBlock.getLocation().clone();
        StasisEntry entry = new StasisEntry(uuid, plateLocation, stasisId);
        activeStasis.put(stasisId, entry);
        playerToStasis.put(uuid, stasisId);

        result[0] = "§5§lStasis activated! §dYour pressure plate will remain powered. Cast your §5Stasis Rod§d to release.";
        return true;
    }

    /**
     * Release stasis when a player casts/uses their stasis rod.
     */
    public boolean releaseStasis(Player player, ItemStack rod) {
        UUID uuid = player.getUniqueId();
        String stasisId = getStasisIdFromRod(rod);
        if (stasisId == null) return false;

        // Must match this player's stasis
        String playerStasisId = playerToStasis.get(uuid);
        if (playerStasisId == null || !playerStasisId.equals(stasisId)) return false;

        StasisEntry entry = activeStasis.get(stasisId);
        if (entry == null) return false;

        // Release the plate
        Block plateBlock = entry.getPlateLocation().getBlock();
        if (isPressurePlate(plateBlock.getType())) {
            forcePlatePowered(plateBlock, false);
        }

        // Clean up rod NBT & rename back to plain fishing rod
        clearRodStasis(rod);

        // Remove from maps
        activeStasis.remove(stasisId);
        playerToStasis.remove(uuid);

        player.sendMessage(Component.text("Stasis released! ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("The pressure plate signal has been cut.", NamedTextColor.GRAY)));
        return true;
    }

    /**
     * Release all active stasis on plugin disable.
     */
    public void releaseAll() {
        for (StasisEntry entry : activeStasis.values()) {
            try {
                Block block = entry.getPlateLocation().getBlock();
                if (isPressurePlate(block.getType())) {
                    forcePlatePowered(block, false);
                }
            } catch (Exception ignored) {}
        }
        activeStasis.clear();
        playerToStasis.clear();
    }

    /**
     * Called by listener to keep the pressure plate powered on each tick
     * (Paper fires BlockRedstoneEvent when the plate tries to turn off).
     */
    public boolean isPlateUnderStasis(Location loc) {
        for (StasisEntry entry : activeStasis.values()) {
            Location plateLoc = entry.getPlateLocation();
            if (plateLoc.getWorld().equals(loc.getWorld())
                    && plateLoc.getBlockX() == loc.getBlockX()
                    && plateLoc.getBlockY() == loc.getBlockY()
                    && plateLoc.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActiveStasis(UUID uuid) {
        return playerToStasis.containsKey(uuid);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Block getPressurePlateUnder(Player player) {
        // Check feet block and one below
        Location feet = player.getLocation();
        Block feetBlock = feet.getBlock();
        Block belowBlock = feet.clone().subtract(0, 1, 0).getBlock();

        if (isPressurePlate(feetBlock.getType())) return feetBlock;
        if (isPressurePlate(belowBlock.getType())) return belowBlock;
        return null;
    }

    public boolean isPressurePlate(Material mat) {
        return mat == Material.STONE_PRESSURE_PLATE
                || mat == Material.OAK_PRESSURE_PLATE
                || mat == Material.SPRUCE_PRESSURE_PLATE
                || mat == Material.BIRCH_PRESSURE_PLATE
                || mat == Material.JUNGLE_PRESSURE_PLATE
                || mat == Material.ACACIA_PRESSURE_PLATE
                || mat == Material.DARK_OAK_PRESSURE_PLATE
                || mat == Material.MANGROVE_PRESSURE_PLATE
                || mat == Material.CHERRY_PRESSURE_PLATE
                || mat == Material.BAMBOO_PRESSURE_PLATE
                || mat == Material.CRIMSON_PRESSURE_PLATE
                || mat == Material.WARPED_PRESSURE_PLATE
                || mat == Material.LIGHT_WEIGHTED_PRESSURE_PLATE
                || mat == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                || mat == Material.POLISHED_BLACKSTONE_PRESSURE_PLATE;
    }

    private void forcePlatePowered(Block block, boolean powered) {
        BlockData data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.Powerable powerable) {
            powerable.setPowered(powered);
            block.setBlockData(powerable, true); // true = apply physics/redstone update
        }
    }

    private ItemStack markRodAsStasis(ItemStack rod, String stasisId) {
        ItemStack copy = rod.clone();
        ItemMeta meta = copy.getItemMeta();

        // Purple "Stasis Rod" name (italic false to override default italic)
        meta.displayName(
            Component.text("Stasis Rod")
                .color(NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
        );

        // Lore
        meta.lore(List.of(
            Component.text("Right-click / cast to release stasis")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));

        // Store stasis ID in persistent data
        meta.getPersistentDataContainer().set(stasisKey, PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(stasisIdKey, PersistentDataType.STRING, stasisId);

        copy.setItemMeta(meta);
        return copy;
    }

    private void clearRodStasis(ItemStack rod) {
        ItemMeta meta = rod.getItemMeta();
        if (meta == null) return;

        meta.displayName(null);
        meta.lore(null);
        meta.getPersistentDataContainer().remove(stasisKey);
        meta.getPersistentDataContainer().remove(stasisIdKey);
        rod.setItemMeta(meta);
    }

    public String getStasisIdFromRod(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(stasisKey, PersistentDataType.STRING)) return null;
        return meta.getPersistentDataContainer().get(stasisIdKey, PersistentDataType.STRING);
    }

    public boolean isStasisRod(ItemStack item) {
        return getStasisIdFromRod(item) != null;
    }
}
