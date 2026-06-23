package com.stasis.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class StasisPlugin extends JavaPlugin {

    private StasisManager stasisManager;

    @Override
    public void onEnable() {
        stasisManager = new StasisManager(this);

        getCommand("stasis").setExecutor(new StasisCommand(this, stasisManager));
        getServer().getPluginManager().registerEvents(new StasisListener(this, stasisManager), this);

        getLogger().info("StasisPlugin enabled! Remote redstone via stasis chambers.");
    }

    @Override
    public void onDisable() {
        if (stasisManager != null) {
            stasisManager.releaseAll();
        }
        getLogger().info("StasisPlugin disabled. All stasis chambers released.");
    }

    public StasisManager getStasisManager() {
        return stasisManager;
    }
}
