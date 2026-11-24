package com.massivecraft.factions.integration;

import com.massivecraft.factions.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Worldguard Region Checking.
 * <p>
 * WorldGuard is a soft dependency. This class deliberately contains no
 * WorldGuard types: the implementation is loaded only when the plugin is
 * installed, otherwise the JVM would fail to enable Factions with a
 * NoClassDefFoundError on servers without WorldGuard.
 *
 * @author Spathizilla
 * @author MarcDoesntExist
 */
public class Worldguard {
    private static Hook hook;
    private static boolean enabled = false;

    public static void init(Plugin plugin) {
        Plugin wgplug = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (wgplug == null || !wgplug.isEnabled()) {
            enabled = false;
            hook = null;
            Main.main.log("Could not hook to WorldGuard. WorldGuard checks are disabled.");
            return;
        }

        try {
            Class<?> hookClass = Class
                    .forName("com.massivecraft.factions.integration.WorldguardHook", true,
                            Worldguard.class.getClassLoader());
            hook = (Hook) hookClass.getDeclaredConstructor().newInstance();
            enabled = true;
            Main.main.log("Successfully hooked to WorldGuard.");
        } catch (Throwable failure) {
            enabled = false;
            hook = null;
            Main.main.log("Could not hook to WorldGuard. WorldGuard checks are disabled: "
                    + failure.getClass().getSimpleName());
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    // PVP Flag check
    // Returns:
    //   True: PVP is allowed
    //   False: PVP is disallowed
    public static boolean isPVP(Player player) {
        if (!enabled || hook == null) {
            // No WG hooks so we'll always bypass this check.
            return true;
        }
        return hook.isPVP(player);
    }

    // Check for Regions in the chunk the chunk
    // Returns:
    //   True: Regions found within chunk
    //   False: No regions found within chunk
    public static boolean checkForRegionsInChunk(Location loc) {
        if (!enabled || hook == null) {
            // No WG hooks so we'll always bypass this check.
            return false;
        }
        return hook.checkForRegionsInChunk(loc);
    }

    interface Hook {
        boolean isPVP(Player player);

        boolean checkForRegionsInChunk(Location loc);
    }
}
