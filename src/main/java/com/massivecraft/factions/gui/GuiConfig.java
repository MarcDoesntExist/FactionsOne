package com.massivecraft.factions.gui;

import com.massivecraft.factions.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;

/**
 * Loads the editable gui.yml shipped with the plugin.
 *
 * @author MarcDoesntExists
 */
public final class GuiConfig {

    private final Main plugin;
    private final YamlConfiguration config;
    private final GuiItemFactory itemFactory;

    public GuiConfig(Main plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        this.itemFactory = new GuiItemFactory(plugin);
    }

    public Component title(String path, String fallback) {
        return this.plugin.txt.parse(this.config.getString(path, fallback));
    }

    public int rows(String path, int fallback) {
        return Math.max(1, Math.min(6, this.config.getInt(path, fallback)));
    }

    public int slot(String path, int fallback) {
        return this.config.getInt(path, fallback);
    }

    public String text(String path, String fallback) {
        return this.config.getString(path, fallback);
    }

    public List<String> list(String path) {
        return this.config.getStringList(path);
    }

    public ItemStack item(String key, Player viewer) {
        ConfigurationSection section = this.config.getConfigurationSection("items." + key);
        return section == null
            ? new ItemStack(Material.BARRIER)
            : this.itemFactory.create(section, viewer);
    }
}
