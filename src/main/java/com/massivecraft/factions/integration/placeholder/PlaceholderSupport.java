package com.massivecraft.factions.integration.placeholder;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionWarp;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.rank.FactionRankEntry;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dynamic faction lookups and formatting shared by the PlaceholderAPI
 * expansion. All methods are read-only.
 *
 * @author MarcDoesntExists
 */
public final class PlaceholderSupport {

    private final Main plugin;

    public PlaceholderSupport(Main plugin) {
        this.plugin = plugin;
    }

    String permission(Faction faction, String value) {
        int split = value.lastIndexOf('_');
        if (faction == null || split <= 0 || split == value.length() - 1) {
            return "";
        }
        FPerm perm = FPerm.parse(value.substring(0, split));
        Rel relation = Rel.parse(value.substring(split + 1));
        return perm == null || relation == null
                ? "" : bool(faction.getPermittedRelations(perm).contains(relation));
    }

    String relation(Faction faction, String value) {
        if (faction == null || value.isEmpty()) {
            return "";
        }
        boolean color = value.endsWith("_color");
        String tag = color ? value.substring(0, value.length() - "_color".length()) : value;
        Faction target = Factions.i.getByTag(tag);
        if (target == null) {
            for (Faction candidate : Factions.i.get()) {
                if (candidate.getTagPlain().equalsIgnoreCase(tag)) {
                    target = candidate;
                    break;
                }
            }
        }
        if (target == null) {
            return "";
        }
        return color ? faction.getColorTo(target).toString()
                : faction.getRelationTo(target).name();
    }

    String warp(Faction faction, String value) {
        if (faction == null || value.isEmpty()) {
            return "";
        }
        String field = "location";
        String name = value;
        for (String suffix : List.of("_world", "_x", "_y", "_z")) {
            if (value.endsWith(suffix)) {
                field = suffix.substring(1);
                name = value.substring(0, value.length() - suffix.length());
                break;
            }
        }
        FactionWarp warp = faction.getWarp(name);
        Location location = warp == null || warp.getLocation() == null
                ? null : warp.getLocation().getLocation();
        return switch (field) {
            case "world" -> world(location);
            case "x" -> coordinate(location, 'x');
            case "y" -> coordinate(location, 'y');
            case "z" -> coordinate(location, 'z');
            default -> location(location);
        };
    }

    String rank(Faction faction, String value) {
        FactionRankEntry.Metric metric = FactionRankEntry.Metric.parse(value);
        if (faction == null || metric == null || this.plugin.rankingService == null) {
            return metric == null ? "" : "0";
        }
        List<FactionRankEntry> entries = new ArrayList<>(
                this.plugin.rankingService.snapshot().entries());
        entries.sort(FactionRankEntry.order(metric));
        for (int index = 0; index < entries.size(); index++) {
            if (faction.getId().equals(entries.get(index).id())) {
                return integer(index + 1);
            }
        }
        return "0";
    }

    String top(String key) {
        String[] parts = key.split("_");
        if (parts.length != 4 || this.plugin.rankingService == null) {
            return "";
        }
        FactionRankEntry.Metric metric = FactionRankEntry.Metric.parse(parts[1]);
        if (metric == null) {
            return "";
        }
        int position;
        try {
            position = Integer.parseInt(parts[2]);
        } catch (NumberFormatException exception) {
            return "";
        }
        if (position < 1 || position > 50) {
            return "";
        }
        List<FactionRankEntry> top = this.plugin.rankingService.top(metric, position);
        if (top.size() < position) {
            return "";
        }
        FactionRankEntry entry = top.get(position - 1);
        return switch (parts[3]) {
            case "tag" -> entry.tag();
            case "value" -> switch (metric) {
                case BANK -> BankService.get().format(entry.bank());
                case LAND -> integer(entry.land());
                case MEMBERS -> integer(entry.members());
                case POINTS -> integer(entry.points());
                case POWER -> number(entry.power());
                case VALUE -> number(entry.claimValue());
            };
            default -> "";
        };
    }

    String territory(OfflinePlayer player, int mode) {
        Player online = online(player);
        if (online == null) {
            return "";
        }
        Faction faction = Board.getFactionAt(new FLocation(online));
        if (faction == null) {
            return "";
        }
        if (mode == 0) {
            return tag(faction);
        }
        FPlayer fplayer = FPlayers.i.get(online.getUniqueId().toString());
        if (fplayer == null) {
            return "";
        }
        return mode == 1 ? faction.getRelationTo(fplayer).name()
                : faction.getColorTo(fplayer).toString();
    }

    boolean isWilderness(OfflinePlayer player) {
        Player online = online(player);
        if (online == null) {
            return false;
        }
        Faction faction = Board.getFactionAt(new FLocation(online));
        return faction != null && faction.isNone();
    }

    int factionCount(boolean onlineOnly) {
        int count = 0;
        for (Faction faction : Factions.i.get()) {
            if (faction.isNormal() && (!onlineOnly
                    || !faction.getFPlayersWhereOnline(true).isEmpty())) {
                count++;
            }
        }
        return count;
    }

    int playersInFactions() {
        int count = 0;
        for (FPlayer fplayer : FPlayers.i.get()) {
            if (fplayer.hasFaction()) {
                count++;
            }
        }
        return count;
    }

    static Player online(OfflinePlayer player) {
        return player == null ? null : Bukkit.getPlayer(player.getUniqueId());
    }

    static String tag(Faction faction) {
        return faction == null || faction.isNone()
                ? Lang.placeholderWilderness : text(faction.getTagPlain());
    }

    static Location homeLocation(Faction faction) {
        return faction == null ? null : faction.getHomeLocation();
    }

    static String home(Faction faction) {
        return location(homeLocation(faction));
    }

    static String location(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + "," + location.getBlockX() + ","
                + location.getBlockY() + "," + location.getBlockZ();
    }

    static String world(Location location) {
        return location == null || location.getWorld() == null
                ? "" : location.getWorld().getName();
    }

    static String coordinate(Location location, char axis) {
        if (location == null) {
            return "";
        }
        return switch (axis) {
            case 'x' -> integer(location.getBlockX());
            case 'y' -> integer(location.getBlockY());
            case 'z' -> integer(location.getBlockZ());
            default -> "";
        };
    }

    static String text(String value) {
        return value == null ? "" : value;
    }

    static String bool(boolean value) {
        return value ? "true" : "false";
    }

    static String integer(int value) {
        return String.valueOf(value);
    }

    static String number(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    static String epoch(long millis) {
        return millis <= 0L ? "0" : String.valueOf(millis / 1000L);
    }

    static String iso(long millis) {
        return millis <= 0L ? "" : Instant.ofEpochMilli(millis).toString();
    }
}
