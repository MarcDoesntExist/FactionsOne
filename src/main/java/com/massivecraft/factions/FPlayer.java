package com.massivecraft.factions;

import com.massivecraft.factions.event.FPlayerLeaveEvent;
import com.massivecraft.factions.event.LandClaimEvent;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.Worldguard;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.RelationUtil;
import com.massivecraft.factions.zcore.persist.PlayerEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import com.massivecraft.factions.util.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * Logged in players always have exactly one FPlayer instance.
 * Logged out players may or may not have an FPlayer instance. They will always have one if they are part of a faction.
 * This is because only players with a faction are saved to disk (in order to not waste disk space).
 * <p>
 * The FPlayer is linked to a minecraft player using the player name.
 * <p>
 * The same instance is always returned for the same player.
 * This means you can use the == operator. No .equals method necessary.
 */

// TODO: The players are saved in non order.

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class FPlayer extends PlayerEntity implements EconomyParticipator {
    // FIELD: lastStoodAt
    private transient FLocation lastStoodAt = new FLocation();
    // Where did this player stand the last time we checked?
    // FIELD: factionId
    private String factionId;
    // FIELD: role
    private Rel role;
    // FIELD: title
    private String title;
    // FIELD: power
    private double power;
    // FIELD: powerBoost
    // special increase/decrease to min and max power for this player
    private double powerBoost;
    // FIELD: lastPowerUpdateTime
    private long lastPowerUpdateTime;
    // FIELD: lastLoginTime
    private long lastLoginTime;
    // FIELD: mapAutoUpdating
    private transient boolean mapAutoUpdating;
    // FIELD: autoClaimEnabled
    private transient Faction autoClaimFor;
    private transient boolean hasAdminMode = false;
    // FIELD: loginPvpDisabled
    private transient boolean loginPvpDisabled;

    // GSON need this noarg constructor.
    public FPlayer() {
        this.resetFactionData(false);
        this.power = Conf.powerPlayerStarting;
        this.lastPowerUpdateTime = System.currentTimeMillis();
        this.lastLoginTime = System.currentTimeMillis();
        this.mapAutoUpdating = false;
        this.autoClaimFor = null;
        this.loginPvpDisabled = Conf.noPVPDamageToOthersForXSecondsAfterLogin > 0;
        this.powerBoost = 0.0;

        if (!Conf.newPlayerStartingFactionID.equals("0") && Factions.i
                .exists(Conf.newPlayerStartingFactionID)) {
            this.factionId = Conf.newPlayerStartingFactionID;
        }
    }

    public FLocation getLastStoodAt() {
        return this.lastStoodAt;
    }

    public void setLastStoodAt(FLocation flocation) {
        this.lastStoodAt = flocation;
    }

    public Faction getFaction() {
        if (this.factionId == null) {
            return null;
        }
        return Factions.i.get(this.factionId);
    }

    public void setFaction(Faction faction) {
        Faction oldFaction = this.getFaction();
        if (oldFaction != null)
            oldFaction.removeFPlayer(this);
        faction.addFPlayer(this);
        this.factionId = faction.getId();
        if (Main.main != null && Main.main.rankingService != null) {
            Main.main.rankingService.invalidate();
        }
    }

    public String getFactionId() {
        return this.factionId;
    }

    public boolean hasFaction() {
        return !factionId.equals("0");
    }

    public Rel getRole() {
        return this.role;
    }

    public void setRole(Rel role) {
        this.role = role;
    }

    public String getTitle() {
        return this.hasFaction() ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPowerBoost() {
        return this.powerBoost;
    }

    public void setPowerBoost(double powerBoost) {
        this.powerBoost = powerBoost;
    }

    public boolean isMapAutoUpdating() {
        return mapAutoUpdating;
    }

    public void setMapAutoUpdating(boolean mapAutoUpdating) {
        this.mapAutoUpdating = mapAutoUpdating;
    }

    public Faction getAutoClaimFor() {
        return autoClaimFor;
    }

    public void setAutoClaimFor(Faction faction) {
        this.autoClaimFor = faction;
    }

    public boolean hasAdminMode() {
        return this.hasAdminMode;
    }

    public void setHasAdminMode(boolean val) {
        this.hasAdminMode = val;
    }

    // -------------------------------------------- //
    // Construct
    // -------------------------------------------- //

    // FIELD: account
    public String getAccountId() {
        return this.getId();
    }

    public final void resetFactionData(boolean doSpoutUpdate) {
        if (this.factionId != null && Factions.i.exists(
                this.factionId)) // Avoid infinite loop! TODO: I think that this is needed is a sign we need to refactor.
        {
            Faction currentFaction = this.getFaction();
            if (currentFaction != null) {
                currentFaction.removeFPlayer(this);
            }
        }

        this.factionId = "0"; // The default neutral faction

        this.role = Rel.MEMBER;
        this.title = "";
        this.autoClaimFor = null;
    }

    public void resetFactionData() {
        this.resetFactionData(true);
    }

    // -------------------------------------------- //
    // Getters And Setters
    // -------------------------------------------- //

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        losePowerFromBeingOffline();
        this.lastLoginTime = lastLoginTime;
        this.lastPowerUpdateTime = lastLoginTime;
        if (Conf.noPVPDamageToOthersForXSecondsAfterLogin > 0) {
            this.loginPvpDisabled = true;
        }
    }

    public boolean hasLoginPvpDisabled() {
        if (!loginPvpDisabled) {
            return false;
        }
        if (this.lastLoginTime + (Conf.noPVPDamageToOthersForXSecondsAfterLogin * 1000) < System
                .currentTimeMillis()) {
            this.loginPvpDisabled = false;
            return false;
        }
        return true;
    }

    //----------------------------------------------//
    // Title, Name, Faction Tag and Chat
    //----------------------------------------------//
    public String getName() {
        Player online = getPlayer();
        if (online != null) {
            return online.getName();
        }
        String id = getId();
        if (id == null) {
            return "";
        }
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(id));
            return player.getName() != null ? player.getName() : id;
        } catch (IllegalArgumentException legacyNameKey) {
            return id;
        }
    }

    public Component getTag() {
        return this.hasFaction() ? this.getFaction().getTag() : Component.empty();
    }

    public String getTagPlain() {
        return this.hasFaction() ? this.getFaction().getTagPlain() : "";
    }

    // Base concatenations:

    public String getNameAndSomething(String something) {
        String ret = this.role.getPrefix();
        if (something.length() > 0) {
            ret += something + " ";
        }
        ret += this.getName();
        return ret;
    }

    public Component getNameAndTitle() {
        return Component.text(this.getNameAndSomething(this.getTitle()));
    }

    public Component getNameAndTag() {
        return Component.text(this.getNameAndSomething(this.getTagPlain()));
    }

    // Colored concatenations:
    // These are used in information messages

    public Component getNameAndTitle(Faction faction) {
        return this.getNameAndTitle()
                .colorIfAbsent(this.getColorTo(faction).toTextColor());
    }

    public Component getNameAndTitle(FPlayer fplayer) {
        return this.getNameAndTitle()
                .colorIfAbsent(this.getColorTo(fplayer).toTextColor());
    }

    // Chat Tag:
    // These are injected into the format of global chat messages.

    public String getChatTag() {
        return this.hasFaction() ?
                String.format(Conf.chatTagFormat, this.role.getPrefix() + this.getTagPlain()) :
                "";
    }

    // Colored Chat Tag
    public String getChatTag(Faction faction) {
        return this.hasFaction() ? this.getRelationTo(faction).getColor() + getChatTag() : "";
    }

    public String getChatTag(FPlayer fplayer) {
        return this.hasFaction() ? this.getColorTo(fplayer) + getChatTag() : "";
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

    public Rel getRelationToLocation() {
        return Board.getFactionAt(new FLocation(this)).getRelationTo(this);
    }

    @Override
    public Color getColorTo(RelationParticipator observer) {
        return RelationUtil.getColorOfThatToMe(this, observer);
    }

    //----------------------------------------------//
    // Health
    //----------------------------------------------//
    public void heal(int amnt) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.setHealth(player.getHealth() + amnt);
    }

    //----------------------------------------------//
    // Power
    //----------------------------------------------//
    public double getPower() {
        this.updatePower();
        return this.power;
    }

    protected void alterPower(double delta) {
        this.power += delta;
        if (this.power > this.getPowerMax())
            this.power = this.getPowerMax();
        else if (this.power < this.getPowerMin())
            this.power = this.getPowerMin();
    }

    public double getPowerMax() {
        return Conf.powerPlayerMax + this.powerBoost;
    }

    public double getPowerMin() {
        return Conf.powerPlayerMin + this.powerBoost;
    }

    public int getPowerRounded() {
        return (int) Math.round(this.getPower());
    }

    public int getPowerMaxRounded() {
        return (int) Math.round(this.getPowerMax());
    }

    public int getPowerMinRounded() {
        return (int) Math.round(this.getPowerMin());
    }

    protected void updatePower() {
        if (this.isOffline()) {
            losePowerFromBeingOffline();
            if (!Conf.powerRegenOffline) {
                return;
            }
        }
        long now = System.currentTimeMillis();
        long millisPassed = now - this.lastPowerUpdateTime;
        this.lastPowerUpdateTime = now;

        Player thisPlayer = this.getPlayer();
        if (thisPlayer != null && thisPlayer.isDead())
            return;  // don't let dead players regain power until they respawn

        int millisPerMinute = 60 * 1000;
        double powerPerMinute = Conf.powerPerMinute;
        if (Conf.scaleNegativePower && this.power < 0) {
            powerPerMinute += (Math.sqrt(Math.abs(this.power)) * Math.abs(this.power))
                    / Conf.scaleNegativeDivisor;
        }
        this.alterPower(millisPassed * powerPerMinute / millisPerMinute);

    }

    protected void losePowerFromBeingOffline() {
        if (Conf.powerOfflineLossPerDay > 0.0 && this.power > Conf.powerOfflineLossLimit) {
            long now = System.currentTimeMillis();
            long millisPassed = now - this.lastPowerUpdateTime;
            this.lastPowerUpdateTime = now;

            double loss = millisPassed * Conf.powerOfflineLossPerDay / (24 * 60 * 60 * 1000);
            if (this.power - loss < Conf.powerOfflineLossLimit) {
                loss = this.power;
            }
            this.alterPower(-loss);
        }
    }

    public void onDeath() {
        this.updatePower();
        this.alterPower(-Conf.powerPerDeath);
    }

    //----------------------------------------------//
    // Territory
    //----------------------------------------------//
    public boolean isInOwnTerritory() {
        return Board.getFactionAt(new FLocation(this)) == this.getFaction();
    }
	
	/*public boolean isInOthersTerritory()
	{
		Faction factionHere = Board.getFactionAt(new FLocation(this));
		return factionHere != null && factionHere.isNormal() && factionHere != this.getFaction();
	}*/

	/*public boolean isInAllyTerritory()
	{
		return Board.getFactionAt(new FLocation(this)).getRelationTo(this) == Rel.ALLY;
	}*/

	/*public boolean isInNeutralTerritory()
	{
		return Board.getFactionAt(new FLocation(this)).getRelationTo(this) == Rel.NEUTRAL;
	}*/


    public boolean isInOthersTerritory() {
        Faction factionHere = Board.getFactionAt(new FLocation(this));
        return factionHere != null && factionHere.isNormal() && factionHere != this.getFaction();
    }

    public boolean isInAllyTerritory() {
        return Board.getFactionAt(new FLocation(this)).getRelationTo(this) == Rel.ALLY;
    }

    public boolean isInNeutralTerritory() {
        return Board.getFactionAt(new FLocation(this)).getRelationTo(this) == Rel.NEUTRAL;
    }

    public boolean isInEnemyTerritory() {
        return Board.getFactionAt(new FLocation(this)).getRelationTo(this) == Rel.ENEMY;
    }

    public void sendFactionHereMessage() {
        Faction factionHere = Board.getFactionAt(this.getLastStoodAt());
        Style infoStyle = Main.main.txt.getTagStyle("i");
        Component msg = Component.text(" ~ ", infoStyle).append(factionHere.getTag(this));
        if (factionHere.getDescription().length() > 0) {
            msg = msg.append(Component.text(" - " + factionHere.getDescription(), infoStyle));
        }
        this.sendMessage(msg);
    }


    // -------------------------------
    // Actions
    // -------------------------------

    public void leave(boolean makePay) {
        Faction myFaction = this.getFaction();
        makePay = makePay && Econ.shouldBeUsed() && !this.hasAdminMode();

        if (myFaction == null) {
            resetFactionData();
            return;
        }

        boolean perm = myFaction.getFlag(FFlag.PERMANENT);

        if (!perm && this.getRole() == Rel.LEADER && myFaction.getFPlayers().size() > 1) {
            msg("<b>You must give the admin role to someone else first.");
            return;
        }

        if (!Conf.canLeaveWithNegativePower && this.getPower() < 0) {
            msg("<b>You cannot leave until your power is positive.");
            return;
        }

        // if economy is enabled and they're not on the bypass list, make sure they can pay
        if (makePay && !Econ.hasAtLeast(this, Conf.econCostLeave, "to leave your faction."))
            return;

        FPlayerLeaveEvent leaveEvent =
                new FPlayerLeaveEvent(this, myFaction, FPlayerLeaveEvent.PlayerLeaveReason.LEAVE);
        Bukkit.getServer().getPluginManager().callEvent(leaveEvent);
        if (leaveEvent.isCancelled())
            return;

        // then make 'em pay (if applicable)
        if (makePay && !Econ.modifyMoney(this, -Conf.econCostLeave, "to leave your faction.",
                "for leaving your faction."))
            return;

        // Am I the last one in the faction?
        if (myFaction.getFPlayers().size() == 1) {
            // Transfer all money
            if (Econ.shouldBeUsed())
                Econ.transferMoney(this, myFaction, this,
                        Econ.getBalance(myFaction.getAccountId()));
        }

        if (myFaction.isNormal()) {
            for (FPlayer fplayer : myFaction.getFPlayersWhereOnline(true)) {
                fplayer.msg("%s<i> left %s<i>.", this.describeTo(fplayer, true),
                        myFaction.describeTo(fplayer));
            }

            if (Conf.logFactionLeave)
                Main.main.log(this.getName() + " left the faction: " + myFaction.getTagPlain());
        }

        this.resetFactionData();
        myFaction.updateLastOnlineTime();

        if (myFaction.isNormal() && !perm && myFaction.getFPlayers().isEmpty()) {
            // Remove this faction
            for (FPlayer fplayer : FPlayers.i.getOnline()) {
                fplayer.msg("<i>%s<i> was disbanded.", myFaction.describeTo(fplayer, true));
            }

            myFaction.detach();
            if (Conf.logFactionDisband)
                Main.main.log("The faction " + myFaction.getTagPlain() + " (" + myFaction.getId()
                        + ") was disbanded due to the last player (" + this.getName() + ") leaving.");
        }
    }

    public boolean canClaimForFactionAtLocation(Faction forFaction, Location location,
                                                boolean notifyFailure) {
        Component error = null;
        FLocation flocation = new FLocation(location);
        Faction myFaction = getFaction();
        Faction currentFaction = Board.getFactionAt(flocation);
        int ownedLand = forFaction.getLandRounded();

        if (Conf.worldGuardChecking && Worldguard.checkForRegionsInChunk(location)) {
            // Checks for WorldGuard regions in the chunk attempting to be claimed
            error = Main.main.txt.parse("<b>This land is protected");
        } else if (Conf.worldsNoClaiming.contains(flocation.getWorldName())) {
            error = Main.main.txt.parse("<b>Sorry, this world has land claiming disabled.");
        } else if (this.hasAdminMode()) {
            return true;
        } else if (forFaction == currentFaction) {
            error = Main.main.txt
                    .parse("%s<i> already own this land.", forFaction.describeTo(this, true));
        } else if (!FPerm.TERRITORY.has(this, forFaction, true)) {
            return false;
        } else if (forFaction.getFPlayers().size() < Conf.claimsRequireMinFactionMembers) {
            error = Main.main.txt
                    .parse("Factions must have at least <h>%s<b> members to claim land.",
                            Conf.claimsRequireMinFactionMembers);
        } else if (ownedLand >= forFaction.getPowerRounded()) {
            error = Main.main.txt.parse("<b>You can't claim more land! You need more power!");
        } else if (Conf.claimedLandsMax != 0 && ownedLand >= Conf.claimedLandsMax && !forFaction
                .getFlag(FFlag.INFPOWER)) {
            error = Main.main.txt.parse("<b>Limit reached. You can't claim more land!");
        } else if (!Conf.claimingFromOthersAllowed && currentFaction.isNormal()) {
            error = Main.main.txt.parse("<b>You may not claim land from others.");
        } else if (currentFaction.getRelationTo(forFaction).isAtLeast(Rel.TRUCE) && !currentFaction
                .isNone()) {
            error = Main.main.txt
                    .parse("<b>You can't claim this land due to your relation with the current owner.");
        } else if (Conf.claimsMustBeConnected && !this.hasAdminMode()
                && myFaction.getLandRoundedInWorld(flocation.getWorldName()) > 0 && !Board
                .isConnectedLocation(flocation, myFaction) && (
                !Conf.claimsCanBeUnconnectedIfOwnedByOtherFaction || !currentFaction.isNormal())) {
            if (Conf.claimsCanBeUnconnectedIfOwnedByOtherFaction)
                error = Main.main.txt.parse(
                        "<b>You can only claim additional land which is connected to your first claim or controlled by another faction!");
            else
                error = Main.main.txt.parse(
                        "<b>You can only claim additional land which is connected to your first claim!");
        } else if (currentFaction.isNormal()) {
            if (!currentFaction.hasLandInflation()) {
                // TODO more messages WARN current faction most importantly
                error = Main.main.txt.parse("%s<i> owns this land and is strong enough to keep it.",
                        currentFaction.getTag(this));
            } else if (!Board.isBorderLocation(flocation)) {
                error = Main.main.txt
                        .parse("<b>You must start claiming land at the border of the territory.");
            }
        }

        if (notifyFailure && error != null) {
            msg(error);
        }
        return error == null;
    }

    public boolean attemptClaim(Faction forFaction, Location location, boolean notifyFailure) {
        // notifyFailure is false if called by auto-claim; no need to notify on every failure for it
        // return value is false on failure, true on success

        FLocation flocation = new FLocation(location);
        Faction currentFaction = Board.getFactionAt(flocation);

        int ownedLand = forFaction.getLandRounded();

        if (!this.canClaimForFactionAtLocation(forFaction, location, notifyFailure))
            return false;

        // TODO: Add flag no costs??
        // if economy is enabled and they're not on the bypass list, make sure they can pay
        boolean mustPay = Econ.shouldBeUsed() && !this.hasAdminMode();
        double cost = 0.0;
        EconomyParticipator payee = null;
        if (mustPay) {
            cost = Econ.calculateClaimCost(ownedLand, currentFaction.isNormal());

            if (Conf.econClaimUnconnectedFee != 0.0
                    && forFaction.getLandRoundedInWorld(flocation.getWorldName()) > 0 && !Board
                    .isConnectedLocation(flocation, forFaction))
                cost += Conf.econClaimUnconnectedFee;

            if (Conf.bankEnabled && Conf.bankFactionPaysLandCosts && this.hasFaction())
                payee = this.getFaction();
            else
                payee = this;

            if (!Econ.hasAtLeast(payee, cost, "to claim this land"))
                return false;
        }

        LandClaimEvent claimEvent = new LandClaimEvent(flocation, forFaction, this);
        Bukkit.getServer().getPluginManager().callEvent(claimEvent);
        if (claimEvent.isCancelled())
            return false;

        // then make 'em pay (if applicable)
        if (mustPay && !Econ
                .modifyMoney(payee, -cost, "to claim this land", "for claiming this land"))
            return false;

        // announce success
        Set<FPlayer> informTheseFPlayers = new HashSet<FPlayer>();
        informTheseFPlayers.add(this);
        informTheseFPlayers.addAll(forFaction.getFPlayersWhereOnline(true));
        for (FPlayer fp : informTheseFPlayers) {
            fp.msg("<h>%s<i> claimed land for <h>%s<i> from <h>%s<i>.", this.describeTo(fp, true),
                    forFaction.describeTo(fp), currentFaction.describeTo(fp));
        }

        Board.setFactionAt(forFaction, flocation);
        forFaction.addLog(this.getName(), "CLAIM", flocation.getCoordString());
        if (Main.main.featureService != null) {
            Main.main.featureService.progress(forFaction, FeatureConfig.MissionType.CLAIM, 1);
        }

        if (Conf.logLandClaims)
            Main.main.log(this.getName() + " claimed land at (" + flocation.getCoordString()
                    + ") for the faction: " + forFaction.getTagPlain());

        return true;
    }

    // -------------------------------------------- //
    // Persistance
    // -------------------------------------------- //

    @Override
    public boolean shouldBeSaved() {
        if (this.hasFaction())
            return true;
        return this.getPowerRounded() != this.getPowerMaxRounded()
                && this.getPowerRounded() != (int) Math.round(Conf.powerPlayerStarting);
    }

    public void msg(String str, Object... args) {
        this.sendMessage(Main.main.txt.parse(str, args));
    }

    public void msg(Component message) {
        this.sendMessage(message);
    }
}
