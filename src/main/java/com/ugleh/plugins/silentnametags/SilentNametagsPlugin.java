package com.ugleh.plugins.silentnametags;

import org.bukkit.plugin.java.JavaPlugin;

public class SilentNametagsPlugin extends JavaPlugin {

    private SilentNametagManager nametagManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.nametagManager = new SilentNametagManager(this);
        nametagManager.registerRecipe();
        getServer().getPluginManager().registerEvents(new EntityInteractListener(this, nametagManager), this);
    }

    @Override
    public void onDisable() {

    }

    public SilentNametagManager getNametagManager() {
        return nametagManager;
    }
}
