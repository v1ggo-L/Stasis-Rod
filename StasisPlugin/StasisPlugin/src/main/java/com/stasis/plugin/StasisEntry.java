package com.stasis.plugin;

import org.bukkit.Location;
import java.util.UUID;

public class StasisEntry {

    private final UUID playerUUID;
    private final Location plateLocation;
    private final String stasisId;

    public StasisEntry(UUID playerUUID, Location plateLocation, String stasisId) {
        this.playerUUID = playerUUID;
        this.plateLocation = plateLocation;
        this.stasisId = stasisId;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Location getPlateLocation() {
        return plateLocation;
    }

    public String getStasisId() {
        return stasisId;
    }
}
