package com.massivecraft.factions;

import com.massivecraft.factions.struct.FPerm;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks faction flight and stops it when the player leaves allowed territory.
 *
 * @author MarcDoesntExists
 */
public final class FlyService implements Listener {

    private final Set<UUID> flying = new HashSet<UUID>();

    public boolean toggle(FPlayer fplayer) {
        Player player = fplayer.getPlayer();
        if (player == null || !FPerm.FLY.has(fplayer, fplayer.getFaction(), true)) {
            return false;
        }

        UUID id = player.getUniqueId();
        if (this.flying.contains(id)) {
            this.disable(player);
            fplayer.msg("<i>Faction flight disabled.");
            return true;
        }

        Faction here = Board.getFactionAt(new FLocation(player));
        if (here != fplayer.getFaction()) {
            fplayer.msg("<b>You can only fly in your own faction territory.");
            return false;
        }

        this.flying.add(id);
        player.setAllowFlight(true);
        player.setFlying(true);
        fplayer.msg("<g>Faction flight enabled.");
        return true;
    }

    public boolean isFlying(Player player) {
        return player != null && this.flying.contains(player.getUniqueId());
    }

    public void disable(Player player) {
        if (player == null || !this.flying.remove(player.getUniqueId())) {
            return;
        }
        player.setFlying(false);
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
        }
        player.setFallDistance(0);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!this.flying.contains(player.getUniqueId())) {
            return;
        }
        FPlayer fplayer = FPlayers.i.get(player);
        if (fplayer == null) {
            return;
        }
        Faction faction = fplayer.getFaction();
        if (Board.getFactionAt(new FLocation(player)) != faction) {
            this.disable(player);
            player.sendMessage(com.massivecraft.factions.Main.main.txt.parse(
                "<i>Faction flight disabled: you left your territory."));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.disable(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.disable(event.getPlayer());
    }
}
