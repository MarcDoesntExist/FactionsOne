package com.massivecraft.factions;

import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.util.RelationUtil;
import com.massivecraft.factions.zcore.persist.Entity;
import com.massivecraft.factions.util.Color;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class Faction extends Entity implements EconomyParticipator {
    // FIELD: relationWish
    private final Map<String, Rel> relationWish;

    // FIELD: fplayers
    // speedy lookup of players in faction
    private final transient Set<FPlayer> fplayers = new HashSet<>();

    // FIELD: invites
    // Where string is a lowercase player name
    private final Set<String> invites;
    // FIELD: banned player ids
    private final FactionBanList bannedPlayers;
    // FIELDS: Flag management
    // TODO: This will save... defaults if they where changed to...
    private final Map<FFlag, Boolean> flagOverrides;
    // FIELDS: Permission <-> Groups management
    private final Map<FPerm, Set<Rel>> permOverrides;
    // FIELD: account (fake field)
    // Bank functions
    public double money;
    private Map<String, FactionWarp> warps;
    private int tntBank;
    private int factionPoints;
    private Map<String, Integer> upgradeLevels;
    private Map<String, Integer> missionProgress;
    private Set<String> completedMissions;
    private List<FactionLogEntry> logs;
    private int taxArrears;
    private boolean taxInsolvent;
    private long lastTaxAt;
    private long createdAt;
    private long graceUntil;
    private long shieldUntil;
    // FIELD: open
    private boolean open;
    // FIELD: tag
    private String tag;
    // FIELD: description
    private String description;
    // FIELD: home
    private LazyLocation home;
    // FIELD: lastOnlineTime
    private long lastOnlineTime;
    // FIELD: powerBoost
    // special increase/decrease to default and max power for this faction
    private double powerBoost;

    public Faction() {
        this.relationWish = new HashMap<>();
        this.invites = new HashSet<String>();
        this.bannedPlayers = new FactionBanList();
        this.open = Conf.newFactionsDefaultOpen;
        this.tag = "???";
        this.description = "Default faction description :(";
        this.money = 0.0;
        this.warps = new LinkedHashMap<String, FactionWarp>();
        this.tntBank = 0;
        this.factionPoints = 0;
        this.upgradeLevels = new LinkedHashMap<String, Integer>();
        this.missionProgress = new LinkedHashMap<String, Integer>();
        this.completedMissions = new HashSet<String>();
        this.logs = new ArrayList<FactionLogEntry>();
        this.powerBoost = 0.0;
        this.flagOverrides = new LinkedHashMap<FFlag, Boolean>();
        this.permOverrides = new LinkedHashMap<FPerm, Set<Rel>>();
    }

    public Set<String> getInvites() {
        return invites;
    }

    public void invite(FPlayer fplayer) {
        this.invites.add(fplayer.getId());
    }

    public void deinvite(FPlayer fplayer) {
        this.invites.remove(fplayer.getId());
    }

    public boolean isInvited(FPlayer fplayer) {
        return this.invites.contains(fplayer.getId());
    }

    public boolean isBanned(String playerId) {
        return this.bannedPlayers.isBanned(playerId);
    }

    public boolean isBanned(FPlayer fplayer) {
        return fplayer != null && this.isBanned(fplayer.getId());
    }

    public void ban(FPlayer fplayer) {
        if (fplayer != null) {
            this.ban(fplayer.getId());
        }
    }

    public void ban(String playerId) {
        this.bannedPlayers.ban(playerId);
    }

    public void unban(FPlayer fplayer) {
        if (fplayer != null) {
            this.unban(fplayer.getId());
        }
    }

    public void unban(String playerId) {
        this.bannedPlayers.unban(playerId);
    }

    public Set<String> getBannedPlayerIds() {
        return this.bannedPlayers.getIds();
    }

    public void sanitizeBannedPlayers() {
        this.bannedPlayers.sanitize();
    }

    public boolean getOpen() {
        return open;
    }

    public void setOpen(boolean isOpen) {
        open = isOpen;
    }

    public Component getTag() {
        return Component.text(this.tag);
    }

    /**
     * Plain tag for storage, logs, events and validation.
     */
    public String getTagPlain() {
        return this.tag;
    }

    public void setTag(String str) {
        if (Conf.factionTagForceUpperCase) {
            str = str.toUpperCase();
        }
        this.tag = str;
    }

    public Component getTag(RelationParticipator observer) {
        Component ret = this.getTag();
        if (observer == null) {
            return ret;
        }
        return ret.colorIfAbsent(this.getColorTo(observer).toTextColor());
    }

    public String getComparisonTag() {
        return MiscUtil.getComparisonString(this.tag);
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public boolean hasHome() {
        return this.getHome() != null;
    }

    public Location getHome() {
        confirmValidHome();
        return (this.home != null) ? this.home.getLocation() : null;
    }

    public void setHome(Location home) {
        this.home = new LazyLocation(home);
    }

    public void confirmValidHome() {
        if (!Conf.homesMustBeInClaimedTerritory || this.home == null || (
                this.home.getLocation() != null
                        && Board.getFactionAt(new FLocation(this.home.getLocation())) == this))
            return;

        msg("<b>Your faction home has been un-set since it is no longer in your territory.");
        this.home = null;
    }

    public String getAccountId() {
        // Account creation is the bank gateway's job; reading this id must stay free
        // of economy calls so it can be used as a lock key.
        return "faction-" + this.getId();
    }
    // Contains the modifications to the default values

    public double getPowerBoost() {
        return this.powerBoost;
    }

    public void setPowerBoost(double powerBoost) {
        this.powerBoost = powerBoost;
    }

    public boolean getFlag(FFlag flag) {
        if (flag == FFlag.OPEN) {
            return this.open;
        }
        Boolean ret = this.flagOverrides.get(flag);
        if (ret == null)
            ret = flag.getDefault();
        return ret;
    }
    // Contains the modifications to the default values

    public void setFlag(FFlag flag, boolean value) {
        if (flag == FFlag.OPEN) {
            this.open = value;
            return;
        }
        Boolean defaultValue = Conf.factionFlagDefaults.get(flag);
        if (defaultValue != null && defaultValue.equals(value)) {
            this.flagOverrides.remove(flag);
            return;
        }
        this.flagOverrides.put(flag, value);
    }

    /**
     * Removes unknown flag keys and null values left by older or newer data.
     */
    public void sanitizeFlagOverrides() {
        if (this.flagOverrides == null) {
            return;
        }
        this.flagOverrides.keySet().removeIf(Objects::isNull);
        this.flagOverrides.values().removeIf(Objects::isNull);
        this.flagOverrides.remove(FFlag.OPEN);
    }
	
	/*
	public void addPermittedRelation(FPerm perm, Rel rel)
	{
		Set<Rel> newPermittedRelations = EnumSet.noneOf(Rel.class);
		newPermittedRelations.addAll(this.getPermittedRelations(perm));
		newPermittedRelations.add(rel);
		this.setPermittedRelations(perm, newPermittedRelations);
	}
	
	public void removePermittedRelation(FPerm perm, Rel rel)
	{
		Set<Rel> newPermittedRelations = EnumSet.noneOf(Rel.class);
		newPermittedRelations.addAll(this.getPermittedRelations(perm));
		newPermittedRelations.remove(rel);
		this.setPermittedRelations(perm, newPermittedRelations);
	}*/

    public Set<Rel> getPermittedRelations(FPerm perm) {
        Set<Rel> ret = this.permOverrides.get(perm);
        if (ret == null)
            ret = perm.getDefault();
        return ret;
    }

    /**
     * Removes unknown permission keys and null values left by older or newer data.
     */
    public void sanitizePermOverrides() {
        if (this.permOverrides == null) {
            return;
        }
        this.permOverrides.keySet().removeIf(Objects::isNull);
        this.permOverrides.values().removeIf(Objects::isNull);
        for (Set<Rel> rels : this.permOverrides.values()) {
            rels.removeIf(Objects::isNull);
        }
    }

    /**
     * Copies legacy WITHDRAW overrides to BANK_WITHDRAW when the new key is absent.
     * Keeps custom faction permissions working after the bank permission split.
     */
    public void migrateLegacyWithdrawPermission() {
        if (this.permOverrides == null || this.permOverrides.containsKey(FPerm.BANK_WITHDRAW)) {
            return;
        }
        Set<Rel> legacy = this.permOverrides.get(FPerm.WITHDRAW);
        if (legacy != null) {
            this.permOverrides.put(FPerm.BANK_WITHDRAW, new HashSet<Rel>(legacy));
        }
    }

    /**
     * Adds the co-leader role to stored overrides for permissions where it is now
     * part of the default matrix. Called once during configuration migration.
     */
    public void migrateColeaderPermissions() {
        if (this.permOverrides == null) {
            return;
        }
        for (Map.Entry<FPerm, Set<Rel>> entry : this.permOverrides.entrySet()) {
            if (entry.getKey().defaultDefaultValue.contains(Rel.COLEADER) && entry.getValue() != null) {
                entry.getValue().add(Rel.COLEADER);
            }
        }
    }

    public void setRelationPermitted(FPerm perm, Rel rel, boolean permitted) {
        Set<Rel> newPermittedRelations = EnumSet.noneOf(Rel.class);
        newPermittedRelations.addAll(this.getPermittedRelations(perm));
        if (permitted) {
            newPermittedRelations.add(rel);
        } else {
            newPermittedRelations.remove(rel);
        }
        this.setPermittedRelations(perm, newPermittedRelations);
    }

    public void setPermittedRelations(FPerm perm, Set<Rel> rels) {
        if (perm.getDefault().equals(rels)) {
            this.permOverrides.remove(perm);
            return;
        }
        this.permOverrides.put(perm, rels);
    }


    // -------------------------------------------- //
    // Construct
    // -------------------------------------------- //

    public void setPermittedRelations(FPerm perm, Rel... rels) {
        Set<Rel> temp = new HashSet<Rel>();
        temp.addAll(Arrays.asList(rels));
        this.setPermittedRelations(perm, temp);
    }


    // -------------------------------
    // Understand the types
    // -------------------------------

    // TODO: These should be gone after the refactoring...

    public boolean isNormal() {
        //return ! (this.isNone() || this.isSafeZone() || this.isWarZone());
        return !this.isNone();
    }

    public boolean isNone() {
        return this.getId().equals("0");
    }

    // -------------------------------
    // Relation and relation colors
    // -------------------------------

    @Override
    public Component describeTo(RelationParticipator observer, boolean ucfirst) {
        return RelationUtil.describeThatToMe(this, observer, ucfirst);
    }

    @Override
    public Component describeTo(RelationParticipator observer) {
        return RelationUtil.describeThatToMe(this, observer);
    }

    @Override
    public Rel getRelationTo(RelationParticipator observer) {
        return RelationUtil.getRelationOfThatToMe(this, observer);
    }

    @Override
    public Rel getRelationTo(RelationParticipator observer, boolean ignorePeaceful) {
        return RelationUtil.getRelationOfThatToMe(this, observer, ignorePeaceful);
    }

    @Override
    public Color getColorTo(RelationParticipator observer) {
        return RelationUtil.getColorOfThatToMe(this, observer);
    }

    public Rel getRelationWish(Faction otherFaction) {
        if (this.relationWish.containsKey(otherFaction.getId())) {
            return this.relationWish.get(otherFaction.getId());
        }
        return Rel.NEUTRAL;
    }

    public void setRelationWish(Faction otherFaction, Rel relation) {
        if (this.relationWish.containsKey(otherFaction.getId()) && relation.equals(Rel.NEUTRAL)) {
            this.relationWish.remove(otherFaction.getId());
        } else {
            this.relationWish.put(otherFaction.getId(), relation);
        }
    }

    public Map<Rel, List<Component>> getFactionTagsPerRelation(RelationParticipator rp) {
        return getFactionTagsPerRelation(rp, false);
    }

    // onlyNonNeutral option provides substantial performance boost on large servers for listing only non-neutral factions
    public Map<Rel, List<Component>> getFactionTagsPerRelation(RelationParticipator rp,
                                                               boolean onlyNonNeutral) {
        Map<Rel, List<Component>> ret = new HashMap<Rel, List<Component>>();
        for (Rel rel : Rel.values()) {
            ret.put(rel, new ArrayList<Component>());
        }
        for (Faction faction : Factions.i.get()) {
            Rel relation = faction.getRelationTo(this);
            if (onlyNonNeutral && relation == Rel.NEUTRAL)
                continue;
            ret.get(relation).add(faction.getTag(rp));
        }
        return ret;
    }

    // TODO: Implement a has enough feature.
    //----------------------------------------------//
    // Power
    //----------------------------------------------//
    public double getPower() {
        if (this.getFlag(FFlag.INFPOWER)) {
            return 999999;
        }

        double ret = 0;
        for (FPlayer fplayer : fplayers) {
            ret += fplayer.getPower();
        }
        if (Conf.powerFactionMax > 0 && ret > Conf.powerFactionMax) {
            ret = Conf.powerFactionMax;
        }
        return ret + this.powerBoost;
    }

    public double getPowerMax() {
        if (this.getFlag(FFlag.INFPOWER)) {
            return 999999;
        }

        double ret = 0;
        for (FPlayer fplayer : fplayers) {
            ret += fplayer.getPowerMax();
        }
        if (Conf.powerFactionMax > 0 && ret > Conf.powerFactionMax) {
            ret = Conf.powerFactionMax;
        }
        return ret + this.powerBoost;
    }

    public int getPowerRounded() {
        return (int) Math.round(this.getPower());
    }

    public int getPowerMaxRounded() {
        return (int) Math.round(this.getPowerMax());
    }

    public int getLandRounded() {
        return Board.getFactionCoordCount(this);
    }

    public int getLandRoundedInWorld(String worldName) {
        return Board.getFactionCoordCountInWorld(this, worldName);
    }

    public boolean hasLandInflation() {
        return this.getLandRounded() > this.getPowerRounded();
    }

    // -------------------------------
    // FPlayers
    // -------------------------------

    // maintain the reference list of FPlayers in this faction
    public void refreshFPlayers() {
        fplayers.clear();
        if (this.isNone())
            return;

        for (FPlayer fplayer : FPlayers.i.get()) {
            if (fplayer.getFaction() == this) {
                fplayers.add(fplayer);
            }
        }
    }

    protected boolean addFPlayer(FPlayer fplayer) {
        if (this.isNone())
            return false;

        return fplayers.add(fplayer);
    }

    protected boolean removeFPlayer(FPlayer fplayer) {
        if (this.isNone())
            return false;

        return fplayers.remove(fplayer);
    }

    public Set<FPlayer> getFPlayers() {
        // return a shallow copy of the FPlayer list, to prevent tampering and concurrency issues
        Set<FPlayer> ret = new HashSet<>(fplayers);
        return ret;
    }

    public Set<FPlayer> getFPlayersWhereOnline(boolean online) {
        Set<FPlayer> ret = new HashSet<FPlayer>();

        for (FPlayer fplayer : fplayers) {
            if (fplayer.isOnline() == online) {
                ret.add(fplayer);
            }
        }

        return ret;
    }

    public FPlayer getFPlayerLeader() {
        //if ( ! this.isNormal()) return null;

        for (FPlayer fplayer : fplayers) {
            if (fplayer.getRole() == Rel.LEADER) {
                return fplayer;
            }
        }
        return null;
    }

    public ArrayList<FPlayer> getFPlayersWhereRole(Rel role) {
        ArrayList<FPlayer> ret = new ArrayList<>();
        //if ( ! this.isNormal()) return ret;

        for (FPlayer fplayer : fplayers) {
            if (fplayer.getRole() == role) {
                ret.add(fplayer);
            }
        }

        return ret;
    }

    public ArrayList<Player> getOnlinePlayers() {
        ArrayList<Player> ret = new ArrayList<>();
        //if (this.isPlayerFreeType()) return ret;

        for (Player player : Main.main.getServer().getOnlinePlayers()) {
            FPlayer fplayer = FPlayers.i.get(player);
            if (fplayer.getFaction() == this) {
                ret.add(player);
            }
        }

        return ret;
    }

    // used when current leader is about to be removed from the faction; promotes new leader, or disbands faction if no other members left
    public void promoteNewLeader() {
        if (!this.isNormal())
            return;
        if (this.getFlag(FFlag.PERMANENT) && Conf.permanentFactionsDisableLeaderPromotion)
            return;

        FPlayer oldLeader = this.getFPlayerLeader();

        // Prefer a co-leader, then an officer, then a normal member.
        ArrayList<FPlayer> replacements = this.getFPlayersWhereRole(Rel.COLEADER);
        if (replacements.isEmpty())
            replacements = this.getFPlayersWhereRole(Rel.OFFICER);
        if (replacements.isEmpty())
            replacements = this.getFPlayersWhereRole(Rel.MEMBER);

        if (replacements == null || replacements
                .isEmpty()) {  // faction leader is the only member; one-man faction
            if (this.getFlag(FFlag.PERMANENT)) {
                if (oldLeader != null)
                    oldLeader.setRole(Rel.MEMBER);
                return;
            }

            // no members left and faction isn't permanent, so disband it
            if (Conf.logFactionDisband)
                Main.main.log("The faction " + this.getTagPlain() + " (" + this.getId()
                        + ") has been disbanded since it has no members left.");

            for (FPlayer fplayer : FPlayers.i.getOnline()) {
                fplayer.msg("The faction %s<i> was disbanded.", this.getTag(fplayer));
            }

            this.detach();
        } else {  // promote new faction leader
            if (oldLeader != null)
                oldLeader.setRole(Rel.MEMBER);
            replacements.get(0).setRole(Rel.LEADER);
            this.msg(
                    "<i>Faction leader <h>%s<i> has been removed. %s<i> has been promoted as the new faction leader.",
                    oldLeader == null ? "" : oldLeader.getName(), replacements.get(0).getName());
            Main.main.log("Faction " + this.getTagPlain() + " (" + this.getId()
                    + ") leader was removed. Replacement leader: " + replacements.get(0).getName());
        }
    }

    //----------------------------------------------//
    // Messages
    //----------------------------------------------//
    public void msg(String message, Object... args) {
        this.sendMessage(Main.main.txt.parse(message, args));
    }

    public void sendMessage(String message) {
        this.sendMessage(Main.main.txt.parse(message));
    }

    public void sendMessage(Component message) {
        for (FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
            fplayer.sendMessage(message);
        }
    }

    public void sendMessage(List<Component> messages) {
        for (FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
            fplayer.sendMessage(messages);
        }
    }

    //----------------------------------------------//
    // Offline Faction Protection
    //----------------------------------------------//
    public void updateLastOnlineTime() {
        // We have either gained or a lost a player.
        if (this.isNone())
            return;

        this.lastOnlineTime = System.currentTimeMillis();
    }

    public boolean hasOfflineExplosionProtection() {
        if (!Conf.protectOfflineFactionsFromExplosions || this.isNone())
            return false;

        long timeUntilNoboom =
                this.getLastOnlineTime() + (long) (Conf.offlineExplosionProtectionDelay * 60 * 1000);

        //No protection if players are online.
        if (this.getOnlinePlayers().size() > 0)
            return false;

        // No Protection while timeUntilNoboom is greater than current system time.
        return timeUntilNoboom <= System.currentTimeMillis();
    }

    public long getLastOnlineTime() {
        return this.lastOnlineTime;
    }

    //----------------------------------------------//
    // Deprecated
    //----------------------------------------------//

    /**
     * @deprecated As of release 1.7, replaced by {@link #getFPlayerLeader()}
     */
    @Deprecated
    public FPlayer getFPlayerAdmin() {
        return getFPlayerLeader();
    }

    /**
     * @deprecated As of release 1.7, replaced by {@link #getFlag()}
     */
    @Deprecated
    public boolean isPeaceful() {
        return this.getFlag(FFlag.PEACEFUL);
    }

    /**
     * @deprecated As of release 1.7, replaced by {@link #getFlag()}
     */
    @Deprecated
    public boolean getPeacefulExplosionsEnabled() {
        return this.getFlag(FFlag.EXPLOSIONS);
    }

    /**
     * @deprecated As of release 1.7, replaced by {@link #getFlag()}
     */
    @Deprecated
    public boolean noExplosionsInTerritory() {
        return !this.getFlag(FFlag.EXPLOSIONS);
    }

    /**
     * @deprecated As of release 1.7, replaced by {@link #getFlag()}
     */
    @Deprecated
    public boolean isSafeZone() {
        return !this.getFlag(FFlag.EXPLOSIONS);
    }

    //----------------------------------------------//
    // Warps
    //----------------------------------------------//

    public Map<String, FactionWarp> getWarps() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, FactionWarp>(this.warps));
    }

    public FactionWarp getWarp(String name) {
        return name == null ? null : this.warps.get(name.toLowerCase(Locale.ROOT));
    }

    public void putWarp(String name, FactionWarp warp) {
        if (name != null && warp != null) {
            this.warps.put(name.toLowerCase(Locale.ROOT), warp);
        }
    }

    public void removeWarp(String name) {
        if (name != null) {
            this.warps.remove(name.toLowerCase(Locale.ROOT));
        }
    }

    //----------------------------------------------//
    // TNT bank, points, upgrades and missions
    //----------------------------------------------//

    public int getTntBank() {
        return this.tntBank;
    }

    public void setTntBank(int value) {
        this.tntBank = Math.max(0, value);
    }

    public int getFactionPoints() {
        return this.factionPoints;
    }

    public int getTaxArrears() {
        return this.taxArrears;
    }

    public void setTaxArrears(int value) {
        this.taxArrears = Math.max(0, value);
        if (this.taxArrears == 0) {
            this.taxInsolvent = false;
        }
    }

    public boolean isTaxInsolvent() {
        return this.taxInsolvent;
    }

    public void setTaxInsolvent(boolean value) {
        this.taxInsolvent = value;
    }

    public long getLastTaxAt() {
        return this.lastTaxAt;
    }

    public void setLastTaxAt(long value) {
        this.lastTaxAt = Math.max(0, value);
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(long value) {
        this.createdAt = Math.max(0, value);
    }

    public long getGraceUntil() {
        return this.graceUntil;
    }

    public void setGraceUntil(long value) {
        this.graceUntil = Math.max(0, value);
    }

    public long getShieldUntil() {
        return this.shieldUntil;
    }

    public void setShieldUntil(long value) {
        this.shieldUntil = Math.max(0, value);
    }

    public int addFactionPoints(int delta) {
        long value = (long) this.factionPoints + delta;
        this.factionPoints = (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
        if (Main.main != null && Main.main.rankingService != null) {
            Main.main.rankingService.invalidate();
        }
        return this.factionPoints;
    }

    public int getUpgradeLevel(String id) {
        if (id == null) {
            return 0;
        }
        Integer level = this.upgradeLevels.get(id.toLowerCase(Locale.ROOT));
        return level == null ? 0 : level;
    }

    public void setUpgradeLevel(String id, int level) {
        if (id != null) {
            this.upgradeLevels.put(id.toLowerCase(Locale.ROOT), Math.max(0, level));
        }
    }

    public int getMissionProgress(String id) {
        if (id == null) {
            return 0;
        }
        Integer progress = this.missionProgress.get(id.toLowerCase(Locale.ROOT));
        return progress == null ? 0 : progress;
    }

    public void setMissionProgress(String id, int progress) {
        if (id != null) {
            this.missionProgress.put(id.toLowerCase(Locale.ROOT), Math.max(0, progress));
        }
    }

    public boolean isMissionCompleted(String id) {
        return id != null && this.completedMissions.contains(id.toLowerCase(Locale.ROOT));
    }

    public void markMissionCompleted(String id) {
        if (id != null) {
            this.completedMissions.add(id.toLowerCase(Locale.ROOT));
        }
    }

    //----------------------------------------------//
    // Audit log
    //----------------------------------------------//

    public void addLog(String actor, String action, String detail) {
        this.logs.add(new FactionLogEntry(System.currentTimeMillis(), actor, action, detail));
        int limit = Math.max(1, Conf.factionLogLimit);
        while (this.logs.size() > limit) {
            this.logs.remove(0);
        }
    }

    public List<FactionLogEntry> getLogs() {
        return Collections.unmodifiableList(new ArrayList<FactionLogEntry>(this.logs));
    }

    /**
     * Repairs fields missing from older factions.json files and removes invalid entries.
     */
    public void sanitizeExtendedData() {
        if (this.warps == null) {
            this.warps = new LinkedHashMap<String, FactionWarp>();
        }
        this.warps.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null
                || entry.getValue().getLocation() == null);

        if (this.upgradeLevels == null) {
            this.upgradeLevels = new LinkedHashMap<String, Integer>();
        }
        this.upgradeLevels.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        if (this.missionProgress == null) {
            this.missionProgress = new LinkedHashMap<String, Integer>();
        }
        this.missionProgress.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        if (this.completedMissions == null) {
            this.completedMissions = new HashSet<String>();
        }
        this.completedMissions.removeIf(Objects::isNull);

        if (this.logs == null) {
            this.logs = new ArrayList<FactionLogEntry>();
        }
        this.logs.removeIf(Objects::isNull);
        while (this.logs.size() > Math.max(1, Conf.factionLogLimit)) {
            this.logs.remove(0);
        }
        this.tntBank = Math.max(0, this.tntBank);
        this.factionPoints = Math.max(0, this.factionPoints);
        this.taxArrears = Math.max(0, this.taxArrears);
        this.lastTaxAt = Math.max(0, this.lastTaxAt);
        if (this.taxArrears == 0) {
            this.taxInsolvent = false;
        }
        this.createdAt = Math.max(0, this.createdAt);
        this.graceUntil = Math.max(0, this.graceUntil);
        this.shieldUntil = Math.max(0, this.shieldUntil);
    }

    //----------------------------------------------//
    // Persistance and entity management
    //----------------------------------------------//


    @Override
    public void postDetach() {
        if (Econ.shouldBeUsed()) {
            Econ.setBalance(getAccountId(), 0);
        }

        // Clean the board
        Board.clean();

        // Clean the fplayers
        FPlayers.i.clean();
    }
}
