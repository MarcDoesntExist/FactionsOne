package de.erethon.factionsone.api;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionWarp;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.event.BankTransactionEvent;
import com.massivecraft.factions.struct.TerritoryAccess;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Public, read-mostly API for FactionsOne. Call it on the main server thread;
 * it is not thread-safe outside that thread.
 *
 * @author Daniel Saukel
 * @author MarcDoesntExists
 */
public class FactionsAPI {

    public static Faction getFaction(String id) {
        return id == null ? null : Factions.i.get(id);
    }

    public static Faction getFactionByTag(String tag) {
        return tag == null ? null : Factions.i.getByTag(tag);
    }

    /**
     * @return All of the claims of the faction.
     */
    public static Set<FLocation> getFactionClaims(Faction faction) {
        Set<FLocation> claims = new HashSet<>();
        if (faction == null) {
            return claims;
        }
        for (Map.Entry<FLocation, TerritoryAccess> entry : Board.flocationIds.entrySet()) {
            if (entry.getValue().getHostFactionID().equals(faction.getId())) {
                claims.add(entry.getKey());
            }
        }
        return claims;
    }

    public static Set<FLocation> getClaims(Faction faction) {
        return getFactionClaims(faction);
    }

    /**
     * @return The claim key containing the location, without changing it.
     */
    public static FLocation getClaimAt(Location location) {
        return location == null ? null : new FLocation(location);
    }

    public static int getClaimCount(Faction faction) {
        return faction == null ? 0 : getFactionClaims(faction).size();
    }

    /**
     * Unclaims all of the land of the faction.
     */
    public static void unclaimAll(Faction faction) {
        if (faction != null) {
            Board.unclaimAll(faction.getId());
        }
    }

    /**
     * @param uuid A player's UUID
     * @return The FPlayer that has this UUID
     */
    public static FPlayer getFPlayer(UUID uuid) {
        return uuid == null ? null : FPlayers.i.get(uuid.toString());
    }

    public static FPlayer getFPlayer(Player player) {
        return player == null ? null : FPlayers.i.get(player);
    }

    public static FPlayer getFPlayer(String id) {
        return id == null ? null : FPlayers.i.get(id);
    }

    public static double getBankBalance(Faction faction) {
        return faction == null ? 0.0 : BankService.get().balance(faction);
    }

    /**
     * Deposits money through the atomic bank service. The caller is responsible
     * for permission checks; this method does not bypass FPerm.
     */
    public static boolean depositToBank(Faction faction, double amount, String actor) {
        if (faction == null || !Double.isFinite(amount) || amount <= 0.0
                || !BankTransactionEvent.fire(faction, BankTransactionEvent.Type.DEPOSIT,
                amount, actor)) {
            return false;
        }
        return BankService.get().modifyMoney(faction, amount, null, "api deposit");
    }

    /**
     * Withdraws money through the atomic bank service. The caller is responsible
     * for permission checks; this method does not bypass FPerm.
     */
    public static boolean withdrawFromBank(Faction faction, double amount, String actor) {
        if (faction == null || !Double.isFinite(amount) || amount <= 0.0
                || !BankTransactionEvent.fire(faction, BankTransactionEvent.Type.WITHDRAW,
                amount, actor)) {
            return false;
        }
        return BankService.get().modifyMoney(faction, -amount, null, "api withdrawal");
    }

    public static Map<String, FactionWarp> getWarps(Faction faction) {
        return faction == null ? Collections.emptyMap() : faction.getWarps();
    }

    public static boolean setWarp(FPlayer fplayer, String name, String password) {
        return fplayer != null && Main.main != null && Main.main.warpService != null
                && Main.main.warpService.setWarp(fplayer, name, password);
    }

    public static boolean deleteWarp(FPlayer fplayer, String name) {
        return fplayer != null && Main.main != null && Main.main.warpService != null
                && Main.main.warpService.deleteWarp(fplayer, name);
    }

    public static int getUpgradeLevel(Faction faction, String id) {
        return faction == null ? 0 : faction.getUpgradeLevel(id);
    }
}
