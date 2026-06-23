package com.stasis.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

public class StasisListener implements Listener {

    private final StasisPlugin plugin;
    private final StasisManager stasisManager;

    public StasisListener(StasisPlugin plugin, StasisManager stasisManager) {
        this.plugin = plugin;
        this.stasisManager = stasisManager;
    }

    /**
     * Intercept redstone events on pressure plates under stasis.
     * If the plate tries to go from powered → unpowered, cancel it.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (!stasisManager.isPressurePlate(block.getType())) return;

        Location loc = block.getLocation();
        if (stasisManager.isPlateUnderStasis(loc)) {
            // Keep it powered: override new current back to powered (15)
            if (event.getNewCurrent() == 0) {
                event.setNewCurrent(event.getOldCurrent() > 0 ? event.getOldCurrent() : 15);
            }
        }
    }

    /**
     * When a player casts their fishing rod (PlayerFishEvent with state FISHING),
     * check if it's a Stasis Rod and release stasis.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();

        if (!stasisManager.isStasisRod(rod)) return;

        // Any cast/use of the stasis rod releases stasis
        PlayerFishEvent.State state = event.getState();
        if (state == PlayerFishEvent.State.FISHING
                || state == PlayerFishEvent.State.FAILED_ATTEMPT
                || state == PlayerFishEvent.State.CAUGHT_FISH
                || state == PlayerFishEvent.State.CAUGHT_ENTITY
                || state == PlayerFishEvent.State.IN_GROUND) {

            boolean released = stasisManager.releaseStasis(player, rod);
            if (released) {
                event.setCancelled(true); // Don't actually cast/do fishing action
            }
        }
    }

    /**
     * Also release on right-click interact (in case the rod is used without casting,
     * e.g. right-click on air or block while offline fishing rod detection varies).
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.getType() != Material.FISHING_ROD) return;
        if (!stasisManager.isStasisRod(item)) return;

        // Right click with stasis rod releases stasis
        org.bukkit.event.block.Action action = event.getAction();
        if (action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {

            boolean released = stasisManager.releaseStasis(player, item);
            if (released) {
                event.setCancelled(true);
            }
        }
    }
}
