package com.massivecraft.factions;

import com.massivecraft.factions.event.WarpTeleportEvent;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.util.TeleportWarmupService;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Owns faction warp validation and delayed teleportation.
 *
 * @author MarcDoesntExists
 */
public final class WarpService {

    private static final String NAME_PATTERN = "[a-z0-9_-]{1,24}";

    private final Main plugin;
    private final FeatureService features;
    private final TeleportWarmupService warmup;
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();

    public WarpService(Main plugin, FeatureService features, TeleportWarmupService warmup) {
        this.plugin = plugin;
        this.features = features;
        this.warmup = warmup;
    }

    private static String normalize(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.matches(NAME_PATTERN) ? normalized : null;
    }

    public boolean setWarp(FPlayer fplayer, String name, String password) {
        if (!Conf.warpsEnabled) {
            fplayer.msg("<b>Faction warps are disabled.");
            return false;
        }
        if (!FPerm.SETWARP.has(fplayer, fplayer.getFaction(), true)) {
            return false;
        }
        String normalized = normalize(name);
        if (normalized == null) {
            fplayer.msg("<b>Warp names may contain only a-z, 0-9, _ and - (max 24).");
            return false;
        }
        if (Conf.warpMustBeInTerritory
            && Board.getFactionAt(new FLocation(fplayer.getPlayer())) != fplayer.getFaction()) {
            fplayer.msg("<b>You must be in your own territory to set a warp.");
            return false;
        }
        Faction faction = fplayer.getFaction();
        if (faction.getWarp(normalized) == null
            && faction.getWarps().size() >= this.features.warpLimit(faction)) {
            fplayer.msg("<b>Your faction has reached its warp limit.");
            return false;
        }
        if (password != null && password.length() > 64) {
            fplayer.msg("<b>The warp password is too long.");
            return false;
        }

        Location location = fplayer.getPlayer().getLocation();
        faction.putWarp(normalized, new FactionWarp(normalized, new LazyLocation(location),
            FactionWarp.hash(password)));
        faction.addLog(fplayer.getName(), "WARP_SET", normalized);
        fplayer.msg("<g>Warp <h>%s<g> saved.", normalized);
        return true;
    }

    public boolean deleteWarp(FPlayer fplayer, String name) {
        if (!FPerm.SETWARP.has(fplayer, fplayer.getFaction(), true)) {
            return false;
        }
        String normalized = normalize(name);
        if (normalized == null || fplayer.getFaction().getWarp(normalized) == null) {
            fplayer.msg("<b>That warp does not exist.");
            return false;
        }
        fplayer.getFaction().removeWarp(normalized);
        fplayer.getFaction().addLog(fplayer.getName(), "WARP_DELETE", normalized);
        fplayer.msg("<g>Warp <h>%s<g> removed.", normalized);
        return true;
    }

    public boolean teleport(FPlayer fplayer, String name, String password) {
        if (!Conf.warpsEnabled) {
            fplayer.msg("<b>Faction warps are disabled.");
            return false;
        }
        if (!FPerm.WARP.has(fplayer, fplayer.getFaction(), true)) {
            return false;
        }
        Faction faction = fplayer.getFaction();
        FactionWarp warp = faction.getWarp(normalize(name));
        if (warp == null || warp.getLocation() == null
            || warp.getLocation().getLocation() == null) {
            fplayer.msg("<b>That warp does not exist.");
            return false;
        }
        if (!warp.matches(password)) {
            fplayer.msg("<b>Wrong warp password.");
            return false;
        }

        Player player = fplayer.getPlayer();
        long now = System.currentTimeMillis();
        Long until = this.cooldowns.get(player.getUniqueId());
        if (until != null && until > now) {
            fplayer.msg("<b>Wait a moment before using another warp.");
            return false;
        }

        Location destination = warp.getLocation().getLocation();
        int seconds = Math.max(0, Conf.warpWarmupSeconds);
        WarpTeleportEvent event = new WarpTeleportEvent(fplayer, faction, warp, seconds);
        if (org.bukkit.Bukkit.getServer() != null) {
            org.bukkit.Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                fplayer.msg(Lang.warpTeleportCancelled);
                return false;
            }
        }
        double cost = Math.max(0.0, Conf.warpCost);
        if (cost > 0.0 && !Econ.modifyMoney(fplayer, -cost, "to use a warp",
            "for using a warp")) {
            fplayer.msg(Lang.warpCostFailed);
            return false;
        }
        if (seconds > 0) {
            fplayer.msg("<i>Warping in <h>%s<i> seconds. Do not move.", seconds);
        }
        this.warmup.start(player, seconds,
            () -> this.finish(fplayer, faction, warp, destination, cost),
            () -> this.refund(fplayer, cost));
        return true;
    }

    private boolean finish(FPlayer fplayer, Faction faction, FactionWarp warp, Location destination,
                           double cost) {
        Player player = fplayer.getPlayer();
        if (player == null) {
            return this.refund(fplayer, cost);
        }
        if (fplayer.getFaction() != faction || !FPerm.WARP.has(fplayer, faction, false)) {
            return this.refund(fplayer, cost);
        }
        try {
            player.teleport(destination);
        } catch (RuntimeException exception) {
            this.plugin.getLogger().warning("Warp teleport failed for " + player.getName() + ": "
                + exception.getMessage());
            return this.refund(fplayer, cost);
        }
        player.setFallDistance(0);
        this.cooldowns.put(player.getUniqueId(),
            System.currentTimeMillis() + Math.max(0, Conf.warpCooldownSeconds) * 1000L);
        faction.addLog(fplayer.getName(), "WARP_USE", warp.getName());
        fplayer.msg("<g>Warped to <h>%s<g>.", warp.getName());
        return true;
    }

    private boolean refund(FPlayer fplayer, double cost) {
        if (cost > 0.0) {
            Econ.modifyMoney(fplayer, cost, null, "warp refund");
        }
        return false;
    }

}
