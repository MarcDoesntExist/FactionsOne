package com.massivecraft.factions.zcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.zcore.persist.EM;
import com.massivecraft.factions.zcore.persist.SaveTask;
import com.massivecraft.factions.zcore.util.DiscUtil;
import com.massivecraft.factions.zcore.util.PermUtil;
import com.massivecraft.factions.zcore.util.Persist;
import com.massivecraft.factions.zcore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public abstract class MPlugin extends JavaPlugin {
    // Some utils
    public Persist persist;
    public TextUtil txt;
    public PermUtil perm;

    // Persist related
    public Gson gson;
    // Listeners
    public MPluginSecretPlayerListener mPluginSecretPlayerListener;
    // These are not supposed to be used directly.
    // They are loaded and used through the TextUtil instance for the plugin.
    public Map<String, String> rawTags = new LinkedHashMap<String, String>();
    protected boolean loadSuccessful = false;
    private Integer saveTask = null;
    private boolean autoSave = true;
    // -------------------------------------------- //
    // ENABLE
    // -------------------------------------------- //
    private long timeEnableStart;

    public boolean getAutoSave() {
        return this.autoSave;
    }

    public void setAutoSave(boolean val) {
        this.autoSave = val;
    }

    public boolean preEnable() {
        log("=== ENABLE START ===");
        timeEnableStart = System.currentTimeMillis();

        // Ensure basefolder exists!
        this.getDataFolder().mkdirs();

        if (!this.prepareData()) {
            log(Level.SEVERE, "Data preparation failed; refusing to enable.");
            return false;
        }

        // Create Utility Instances
        this.perm = new PermUtil(this);
        this.persist = new Persist(this);

        // GSON 2.1 is now embedded in CraftBukkit, used by the auto-updater: https://github.com/Bukkit/CraftBukkit/commit/0ed1d1fdbb1e0bc09a70bc7bfdf40c1de8411665
        //		if ( ! lib.require("gson.jar", "http://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.1/gson-2.1.jar")) return false;
        this.gson = this.getGsonBuilder().create();

        this.txt = new TextUtil();
        initTXT();

        // Create and register listeners
        this.mPluginSecretPlayerListener = new MPluginSecretPlayerListener(this);

        // Register recurring tasks
        if (saveTask == null && Conf.saveToFileEveryXMinutes > 0.0) {
            long saveTicks = (long) (20 * 60
                * Conf.saveToFileEveryXMinutes); // Approximately every 30 min by default
            saveTask = Bukkit.getServer().getScheduler()
                .scheduleSyncRepeatingTask(this, new SaveTask(this), saveTicks, saveTicks);
        }

        loadSuccessful = true;
        return true;
    }

    public void postEnable() {
        log("=== ENABLE DONE (Took " + (System.currentTimeMillis() - timeEnableStart) + "ms) ===");
    }

    /**
     * Hook for plugins that must version or migrate their data before any
     * file is read or written. Returning false aborts enable.
     */
    protected boolean prepareData() {
        return true;
    }

    public void onDisable() {
        if (saveTask != null) {
            this.getServer().getScheduler().cancelTask(saveTask);
            saveTask = null;
        }
        // only save data if plugin actually loaded successfully
        if (loadSuccessful)
            EM.saveAllToDisc();
        // Writes run on a background queue: wait for them and stop the worker before
        // the class loader goes away. A later save starts a fresh worker.
        DiscUtil.shutdownAsync(15000L);
        log("Disabled");
    }

    // -------------------------------------------- //
    // Some inits...
    // You are supposed to override these in the plugin if you aren't satisfied with the defaults
    // The goal is that you always will be satisfied though.
    // -------------------------------------------- //

    public void suicide() {
        log("Now I suicide!");
        this.getServer().getPluginManager().disablePlugin(this);
    }

    // -------------------------------------------- //
    // LANG AND TAGS
    // -------------------------------------------- //

    public GsonBuilder getGsonBuilder() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE);
    }

    public void addRawTags() {
        this.rawTags.put("l", "<green>"); // logo
        this.rawTags.put("a", "<gold>"); // art
        this.rawTags.put("n", "<silver>"); // notice
        this.rawTags.put("i", "<yellow>"); // info
        this.rawTags.put("g", "<lime>"); // good
        this.rawTags.put("b", "<rose>"); // bad
        this.rawTags.put("h", "<pink>"); // highligh
        this.rawTags.put("c", "<aqua>"); // command
        this.rawTags.put("main", "<teal>"); // parameter
    }

    public void initTXT() {
        this.addRawTags();

        Type type = new TypeToken<Map<String, String>>() {
        }.getType();

        Map<String, String> tagsFromFile = this.persist.load(type, "tags");
        if (tagsFromFile != null)
            this.rawTags.putAll(tagsFromFile);
        this.persist.save(this.rawTags, "tags");

        for (Entry<String, String> rawTag : this.rawTags.entrySet()) {
            this.txt.tags.put(rawTag.getKey(), TextUtil.parseColor(rawTag.getValue()));
            this.txt.tagStyles.put(rawTag.getKey(), TextUtil.styleFromConfig(rawTag.getValue()));
        }
    }

    // -------------------------------------------- //
    // HOOKS
    // -------------------------------------------- //
    public void preAutoSave() {

    }

    public void postAutoSave() {

    }

    // -------------------------------------------- //
    // LOGGING
    // -------------------------------------------- //
    public void log(Object msg) {
        log(Level.INFO, msg);
    }

    public void log(String str, Object... args) {
        log(Level.INFO, this.txt.parse(str, args));
    }

    public void log(Level level, String str, Object... args) {
        log(level, this.txt.parse(str, args));
    }

    public void log(Level level, Object msg) {
        String text = msg instanceof Component ? TextUtil.plain((Component) msg) : String.valueOf(msg);
        Bukkit.getLogger().log(level, "[" + this.getPluginMeta().getName() + " v"
            + this.getPluginMeta().getVersion() + "] " + text);
    }
}
