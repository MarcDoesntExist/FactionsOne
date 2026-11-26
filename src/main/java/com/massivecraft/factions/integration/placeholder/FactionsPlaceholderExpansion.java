package com.massivecraft.factions.integration.placeholder;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.shield.ShieldPolicy;
import com.massivecraft.factions.struct.FFlag;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

import static com.massivecraft.factions.integration.placeholder.PlaceholderSupport.*;

/**
 * Exposes faction data through PlaceholderAPI. Every request is read-only and
 * returns an empty string or {@code 0} when the player or faction is missing.
 *
 * @author MarcDoesntExists
 */
public final class FactionsPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final PlaceholderSupport support;

    public FactionsPlaceholderExpansion(Main plugin) {
        this.plugin = plugin;
        this.support = new PlaceholderSupport(plugin);
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
            case "faction_tag":
            case "faction_name":
                return tag(faction);
            case "faction_id":
                return faction == null ? "" : faction.getId();
            case "faction_description":
                return faction == null ? "" : text(faction.getDescription());
            case "faction_leader":
                return faction == null || faction.getFPlayerLeader() == null
                    ? "" : text(faction.getFPlayerLeader().getName());
            case "faction_open":
                return bool(faction != null && faction.getOpen());
            case "faction_peaceful":
                return bool(faction != null && faction.getFlag(FFlag.PEACEFUL));
            case "faction_raidable":
                return bool(faction != null && faction.hasLandInflation());
            case "faction_power":
                return number(faction == null ? 0.0 : faction.getPower());
            case "faction_power_max":
                return number(faction == null ? 0.0 : faction.getPowerMax());
            case "faction_power_rounded":
                return integer(faction == null ? 0 : faction.getPowerRounded());
            case "faction_power_max_rounded":
                return integer(faction == null ? 0 : faction.getPowerMaxRounded());
            case "faction_power_boost":
                return number(faction == null ? 0.0 : faction.getPowerBoost());
            case "faction_land":
            case "faction_claims":
            case "faction_land_rounded":
            case "land":
                return integer(faction == null ? 0 : faction.getLandRounded());
            case "faction_members":
            case "faction_members_total":
                return integer(faction == null ? 0 : faction.getFPlayers().size());
            case "faction_members_online":
            case "faction_online":
                return integer(faction == null ? 0
                    : faction.getFPlayersWhereOnline(true).size());
            case "faction_bank":
            case "faction_bank_formatted":
                return faction == null ? "0" : BankService.get().format(
                    BankService.get().balance(faction));
            case "faction_bank_raw":
            case "faction_bank_value":
                return number(faction == null ? 0.0 : BankService.get().balance(faction));
            case "faction_points":
                return integer(faction == null ? 0 : faction.getFactionPoints());
            case "faction_tnt":
                return integer(faction == null ? 0 : faction.getTntBank());
            case "faction_warps":
                return integer(faction == null ? 0 : faction.getWarps().size());
            case "faction_home":
                return home(faction);
            case "faction_home_world":
                return world(homeLocation(faction));
            case "faction_home_x":
                return coordinate(homeLocation(faction), 'x');
            case "faction_home_y":
                return coordinate(homeLocation(faction), 'y');
            case "faction_home_z":
                return coordinate(homeLocation(faction), 'z');
            case "faction_shield_active":
                return bool(faction != null && ShieldPolicy.active(
                    faction.getShieldUntil(), System.currentTimeMillis()));
            case "faction_shield_remaining":
                return integer(faction == null ? 0 : (int) ShieldPolicy.remainingMinutes(
                    faction.getShieldUntil(), System.currentTimeMillis()));
            case "faction_grace_active":
                return bool(faction != null && ShieldPolicy.active(
                    faction.getGraceUntil(), System.currentTimeMillis()));
            case "faction_grace_remaining":
                return integer(faction == null ? 0 : (int) ShieldPolicy.remainingMinutes(
                    faction.getGraceUntil(), System.currentTimeMillis()));
            case "faction_tax_arrears":
                return integer(faction == null ? 0 : faction.getTaxArrears());
            case "faction_tax_insolvent":
                return bool(faction != null && faction.isTaxInsolvent());
            case "faction_created":
                return epoch(faction == null ? 0L : faction.getCreatedAt());
            case "faction_created_iso":
                return iso(faction == null ? 0L : faction.getCreatedAt());
            case "faction_last_online":
                return epoch(faction == null ? 0L : faction.getLastOnlineTime());
            case "faction_last_online_iso":
                return iso(faction == null ? 0L : faction.getLastOnlineTime());
            case "faction_value":
                return number(faction == null || this.plugin.claimValueService == null
                    ? 0.0 : this.plugin.claimValueService.value(faction.getId()));
            case "role":
            case "role_name":
                return fplayer == null || fplayer.getRole() == null
                    ? "" : fplayer.getRole().name();
            case "title":
                return fplayer == null ? "" : text(fplayer.getTitle());
            case "power":
                return number(fplayer == null ? 0.0 : fplayer.getPower());
            case "power_rounded":
                return integer(fplayer == null ? 0 : fplayer.getPowerRounded());
            case "powermax":
            case "power_max":
                return number(fplayer == null ? 0.0 : fplayer.getPowerMax());
            case "powermax_rounded":
            case "power_max_rounded":
                return integer(fplayer == null ? 0 : fplayer.getPowerMaxRounded());
            case "power_min":
                return number(fplayer == null ? 0.0 : fplayer.getPowerMin());
            case "power_boost":
                return number(fplayer == null ? 0.0 : fplayer.getPowerBoost());
            case "has_faction":
                return bool(fplayer != null && fplayer.hasFaction());
            case "factionless":
                return bool(fplayer == null || !fplayer.hasFaction());
            case "chat_channel":
                return fplayer == null || fplayer.getChatChannel() == null
                    ? "" : fplayer.getChatChannel().name().toLowerCase(Locale.ROOT);
            case "last_login":
                return epoch(fplayer == null ? 0L : fplayer.getLastLoginTime());
            case "last_login_iso":
                return iso(fplayer == null ? 0L : fplayer.getLastLoginTime());
            case "in_own_territory":
                return bool(fplayer != null && fplayer.isInOwnTerritory());
            case "in_ally_territory":
                return bool(fplayer != null && fplayer.isInAllyTerritory());
            case "in_neutral_territory":
                return bool(fplayer != null && fplayer.isInNeutralTerritory());
            case "in_enemy_territory":
                return bool(fplayer != null && fplayer.isInEnemyTerritory());
            case "in_wilderness":
                return bool(this.support.isWilderness(player));
            case "territory":
            case "territory_tag":
                return this.support.territory(player, 0);
            case "territory_relation":
                return this.support.territory(player, 1);
            case "territory_color":
                return this.support.territory(player, 2);
            case "members_online":
                return integer(faction == null ? 0
                    : faction.getFPlayersWhereOnline(true).size());
            case "members_total":
                return integer(faction == null ? 0 : faction.getFPlayers().size());
            case "claims":
                return integer(faction == null ? 0 : faction.getLandRounded());
            case "bank":
                return faction == null ? "0" : BankService.get().format(
                    BankService.get().balance(faction));
            case "bank_raw":
                return number(faction == null ? 0.0 : BankService.get().balance(faction));
            case "points":
                return integer(faction == null ? 0 : faction.getFactionPoints());
            case "tnt":
                return integer(faction == null ? 0 : faction.getTntBank());
            case "factions_total":
            case "factions":
                return integer(this.support.factionCount(false));
            case "factions_online":
                return integer(this.support.factionCount(true));
            case "players_online":
                return integer(Bukkit.getOnlinePlayers().size());
            case "players_total":
                return integer(FPlayers.i.get().size());
            case "players_in_factions":
                return integer(this.support.playersInFactions());
            default:
                break;
        }

        if (key.startsWith("faction_upgrade_")) {
            String upgrade = key.substring("faction_upgrade_".length());
            return faction == null || upgrade.isEmpty()
                ? "0" : integer(faction.getUpgradeLevel(upgrade));
        }
        if (key.startsWith("faction_mission_")) {
            String mission = key.substring("faction_mission_".length());
            boolean completed = mission.endsWith("_completed");
            String id = completed
                ? mission.substring(0, mission.length() - "_completed".length())
                : mission;
            if (faction == null || id.isEmpty()) {
                return completed ? "false" : "0";
            }
            return completed ? bool(faction.isMissionCompleted(id))
                : integer(faction.getMissionProgress(id));
        }
        if (key.startsWith("faction_flag_")) {
            FFlag flag = FFlag.parse(key.substring("faction_flag_".length()));
            return faction == null || flag == null ? "" : bool(faction.getFlag(flag));
        }
        if (key.startsWith("faction_perm_")) {
            return this.support.permission(faction, key.substring("faction_perm_".length()));
        }
        if (key.startsWith("faction_warp_")) {
            return this.support.warp(faction, key.substring("faction_warp_".length()));
        }
        if (key.startsWith("faction_relation_")) {
            return this.support.relation(faction, key.substring("faction_relation_".length()));
        }
        if (key.startsWith("relation_")) {
            return this.support.relation(faction, key.substring("relation_".length()));
        }
        if (key.startsWith("faction_rank_")) {
            return this.support.rank(faction, key.substring("faction_rank_".length()));
        }
        if (key.startsWith("rank_")) {
            return this.support.rank(faction, key.substring("rank_".length()));
        }
        if (key.startsWith("top_")) {
            return this.support.top(key);
        }
        return "";
    }

}
