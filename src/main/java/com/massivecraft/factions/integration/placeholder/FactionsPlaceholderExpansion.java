package com.massivecraft.factions.integration.placeholder;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.rank.FactionRankEntry;
import com.massivecraft.factions.rank.FactionRankingService;
import com.massivecraft.factions.shield.ShieldPolicy;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exposes faction data through PlaceholderAPI. Every request is read-only and
 * returns an empty string or {@code 0} when the player or faction is missing.
 *
 * @author MarcDoesntExists
 */
public final class FactionsPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public FactionsPlaceholderExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "factionsone";
    }

    @Override
    public String getAuthor() {
        return "MarcDoesntExists";
    }

    @Override
    public String getVersion() {
        return this.plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        try {
            return this.resolve(player, params == null ? "" : params.toLowerCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private String resolve(OfflinePlayer player, String key) {
        FPlayer fplayer = player == null ? null
                : FPlayers.i.get(player.getUniqueId().toString());
        Faction faction = fplayer == null ? null : fplayer.getFaction();

        switch (key) {
            case "faction":
                return faction == null || faction.isNone() ? "Wilderness" : faction.getTagPlain();
            case "role":
                return fplayer == null || fplayer.getRole() == null
                        ? "" : fplayer.getRole().name();
            case "power":
                return fplayer == null ? "0" : String.valueOf(fplayer.getPowerRounded());
            case "powermax":
                return fplayer == null ? "0" : String.valueOf(fplayer.getPowerMaxRounded());
            case "claims":
                return faction == null ? "0" : String.valueOf(faction.getLandRounded());
            case "members_online":
                return faction == null ? "0"
                        : String.valueOf(faction.getFPlayersWhereOnline(true).size());
            case "members_total":
                return faction == null ? "0" : String.valueOf(faction.getFPlayers().size());
            case "bank":
                return faction == null ? "0"
                        : BankService.get().format(BankService.get().balance(faction));
            case "points":
                return faction == null ? "0" : String.valueOf(faction.getFactionPoints());
            case "shield_active":
                return faction != null && ShieldPolicy.active(faction.getShieldUntil(),
                        System.currentTimeMillis()) ? "true" : "false";
            case "shield_remaining":
                return faction == null || Main.main.shieldService == null ? "0"
                        : String.valueOf(Main.main.shieldService.remainingMinutes(faction));
            case "grace_active":
                return faction != null && ShieldPolicy.active(faction.getGraceUntil(),
                        System.currentTimeMillis()) ? "true" : "false";
            default:
                break;
        }

        if (key.startsWith("upgrade_")) {
            return faction == null ? "0"
                    : String.valueOf(faction.getUpgradeLevel(key.substring("upgrade_".length())));
        }
        if (key.startsWith("relation_")) {
            return this.relation(faction, key.substring("relation_".length()));
        }
        if (key.startsWith("rank_")) {
            return this.rank(faction, key.substring("rank_".length()));
        }
        return null;
    }

    private String relation(Faction faction, String tag) {
        if (faction == null || tag.isEmpty()) {
            return "";
        }
        Faction target = Factions.i.getByTag(tag);
        return target == null ? "" : faction.getRelationTo(target).name();
    }

    private String rank(Faction faction, String criterion) {
        if (faction == null || Main.main.rankingService == null) {
            return "0";
        }
        FactionRankEntry.Metric metric = FactionRankEntry.Metric.parse(criterion);
        if (metric == null) {
            return "";
        }
        List<FactionRankEntry> entries =
                new ArrayList<>(Main.main.rankingService.snapshot().entries());
        entries.sort(FactionRankEntry.order(metric));
        for (int index = 0; index < entries.size(); index++) {
            if (faction.getId().equals(entries.get(index).id())) {
                return String.valueOf(index + 1);
            }
        }
        return "0";
    }
}
