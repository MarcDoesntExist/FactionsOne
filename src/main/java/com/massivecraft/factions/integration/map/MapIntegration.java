package com.massivecraft.factions.integration.map;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Rel;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Owns the optional web map integration. The feature is off by default and
 * never fails plugin startup when BlueMap is absent or its API changes.
 *
 * @author MarcDoesntExists
 */
public final class MapIntegration {
    private static final int API_RETRY_TICKS = 20;
    private static final int API_MAX_ATTEMPTS = 100;

    private final Main plugin;
    private MapRenderer renderer;
    private int refreshTaskId = -1;
    private int retryTaskId = -1;
    private int retryAttempts;
    private boolean invalidReferenceLogged;

    public MapIntegration(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!Conf.mapEnabled) {
            return;
        }
        Plugin blueMap = plugin.getServer().getPluginManager().getPlugin("BlueMap");
        if (blueMap == null || !blueMap.isEnabled()) {
            plugin.getLogger().info("BlueMap is not installed; web map integration is disabled.");
            return;
        }
        // BlueMap registers its API after it has finished loading resources.
        retryTaskId = plugin.getServer().getScheduler()
            .runTaskLater(plugin, this::tryStart, API_RETRY_TICKS).getTaskId();
    }

    private void tryStart() {
        retryTaskId = -1;
        if (renderer != null) {
            return;
        }
        try {
            renderer = new BlueMapRenderer();
        } catch (IllegalStateException notReady) {
            retryAttempts++;
            if (retryAttempts < API_MAX_ATTEMPTS) {
                retryTaskId = plugin.getServer().getScheduler()
                    .runTaskLater(plugin, this::tryStart, API_RETRY_TICKS).getTaskId();
            } else {
                plugin.getLogger().info("BlueMap API did not become ready; web map integration "
                    + "is disabled.");
            }
            return;
        } catch (Throwable failure) {
            plugin.getLogger().info("Could not hook to BlueMap; web map integration is disabled: "
                + failure.getClass().getSimpleName());
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(new MapRefreshListener(this), plugin);
        plugin.getLogger().info("BlueMap web map integration enabled.");
        requestRefresh();
    }

    public void requestRefresh() {
        if (renderer == null) {
            return;
        }
        if (refreshTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(refreshTaskId);
        }
        long delay = Math.max(1, Math.min(1200, Conf.mapRefreshDebounceTicks));
        refreshTaskId = plugin.getServer().getScheduler()
            .runTaskLater(plugin, this::refreshNow, delay).getTaskId();
    }

    private void refreshNow() {
        refreshTaskId = -1;
        if (renderer == null) {
            return;
        }
        try {
            renderer.render(buildClaims());
        } catch (RuntimeException failure) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                "BlueMap refresh failed; web map integration is disabled.", failure);
            closeRenderer();
        }
    }

    private List<MapClaim> buildClaims() {
        Faction reference = resolveReference();
        boolean relationMode = reference != null;
        Map<String, List<ClaimKey>> byFaction = new HashMap<>();
        for (Map.Entry<FLocation, ?> entry : Board.flocationIds.entrySet()) {
            FLocation location = entry.getKey();
            String factionId = Board.getIdAt(location);
            if ("0".equals(factionId)) {
                continue;
            }
            byFaction.computeIfAbsent(factionId, ignored -> new ArrayList<>())
                .add(new ClaimKey(location.getWorldName(), (int) location.getX(),
                    (int) location.getZ()));
        }

        List<MapClaim> claims = new ArrayList<>();
        for (Map.Entry<String, List<ClaimKey>> entry : byFaction.entrySet()) {
            Faction faction = Factions.i.get(entry.getKey());
            if (faction == null) {
                continue;
            }
            Rel relation = relationMode ? faction.getRelationTo(reference) : null;
            int rgb = MapColor.colorFor(faction.getId(), faction.getTagPlain(), relation,
                relationMode);
            String label = faction.getTagPlain();
            for (ClaimRect rect : ClaimAreaMerger.merge(entry.getValue())) {
                claims.add(new MapClaim(rect.world(), rect.minX(), rect.minZ(), rect.maxX(),
                    rect.maxZ(), label, rgb));
            }
        }
        return claims;
    }

    private Faction resolveReference() {
        String configured = Conf.mapReferenceFaction;
        if (configured == null || configured.isBlank()) {
            return null;
        }
        Faction reference = Factions.i.get(configured.trim());
        if (reference == null) {
            for (Faction faction : Factions.i.get()) {
                if (faction.getTagPlain().equalsIgnoreCase(configured.trim())) {
                    reference = faction;
                    break;
                }
            }
        }
        if (reference == null) {
            if (!invalidReferenceLogged) {
                plugin.getLogger().warning("Unknown map-reference-faction '" + configured
                    + "'; faction colors will be used.");
                invalidReferenceLogged = true;
            }
            return null;
        }
        return reference;
    }

    public void close() {
        if (refreshTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(refreshTaskId);
            refreshTaskId = -1;
        }
        if (retryTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(retryTaskId);
            retryTaskId = -1;
        }
        closeRenderer();
    }

    private void closeRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }

    /**
     * @return the active provider name for diagnostics, or {@code none}
     */
    public String providerName() {
        return renderer == null ? "none" : renderer.name().toLowerCase(Locale.ROOT);
    }
}
