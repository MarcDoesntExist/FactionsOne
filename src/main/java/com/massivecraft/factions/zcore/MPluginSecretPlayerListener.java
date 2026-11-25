package com.massivecraft.factions.zcore;

import com.massivecraft.factions.zcore.persist.EM;
import com.massivecraft.factions.zcore.persist.Entity;
import com.massivecraft.factions.zcore.persist.EntityCollection;
import com.massivecraft.factions.zcore.persist.PlayerEntityCollection;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class MPluginSecretPlayerListener implements Listener {
    public MPlugin p;

    public MPluginSecretPlayerListener(MPlugin p) {
        this.p = p;
        Bukkit.getPluginManager().registerEvents(this, this.p);
    }

    // PlayerLoginEvent is deprecated on Paper: it creates the player entity early
    // and bypasses a server validation step. Joining is the supported hook point.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (EntityCollection<? extends Entity> ecoll : EM.class2Entities.values()) {
            if (ecoll instanceof PlayerEntityCollection) {
                ecoll.get(event.getPlayer().getUniqueId().toString());
            }
        }
    }
}
