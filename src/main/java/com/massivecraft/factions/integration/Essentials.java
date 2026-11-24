package com.massivecraft.factions.integration;

import com.earth2me.essentials.AsyncTeleport;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.massivecraft.factions.Conf;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class Essentials {

    private static IEssentials essentials;

    public static void setup() {
        Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess != null) {
            essentials = (IEssentials) ess;
        }
    }

    // return false if feature is disabled or Essentials isn't available
    public static boolean handleTeleport(Player player, Location loc) {
        if (!Conf.homesTeleportCommandEssentialsIntegration || essentials == null) {
            return false;
        }

        User user = essentials.getUser(player);
        AsyncTeleport teleport = user.getAsyncTeleport();
        Trade trade = new Trade(BigDecimal.valueOf(Conf.econCostHome), essentials);

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        result.whenComplete((success, failure) -> {
            if (failure != null) {
                player.sendMessage("Teleport failed: " + failure.getMessage());
            }
        });
        teleport.teleport(loc, trade, PlayerTeleportEvent.TeleportCause.PLUGIN, result);
        return true;
    }
}
