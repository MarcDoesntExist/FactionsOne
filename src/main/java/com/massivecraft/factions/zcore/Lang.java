package com.massivecraft.factions.zcore;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class Lang {
    public static final String permForbidden = "<b>You don't have permission to %s.";
    public static final String permDoThat = "do that";

    public static final String commandSenderMustBePlayer =
            "<b>This command can only be used by ingame players.";
    public static final String commandToFewArgs = "<b>Too few arguments. <i>Use like this:";
    public static final String commandToManyArgs =
            "<b>Strange argument \"<main>%s<b>\". <i>Use the command like this:";

    public static final String featureNoPermission =
            "<b>Your faction does not allow you to use this feature.";
    public static final String featureNotFound = "<b>That feature entry does not exist.";
    public static final String featureMaxLevel = "<b>This upgrade is already at its maximum level.";
    public static final String featureNotEnoughPoints = "<b>Your faction does not have enough points.";
    public static final String featureNoSpace = "<b>Your inventory does not have enough space.";
    public static final String featureNotReady = "<b>This mission is not ready to be claimed.";
    public static final String featureSuccess = "<g>Done.";
    public static final String bankInvalidAmount = "<b>Enter an amount greater than zero.";

    public static final String taxDisabled = "<b>Faction taxes are disabled on this server.";
    public static final String taxNoFaction = "<b>You are not in a faction.";
    public static final String taxServerInfo = "<i>Faction tax: <h>%s<i>, interval <h>%d<i> min, base <h>%s<i>, per member <h>%s<i>, per chunk <h>%s<i>, cap <h>%s<i>, grace <h>%d<i>, insolvency <h>%s<i>.";
    public static final String taxFactionInfo = "<i>Tax for <h>%s<i>: current due <h>%s<i>, arrears <h>%d<i>, %s.";
    public static final String taxNeverPaid = "no charge recorded yet";
    public static final String taxNextCharge = "next charge in <h>%d<i> min";
    public static final String taxInsolventState = "insolvent (<h>%s<i>)";
    public static final String taxRunResult = "<i>Tax period processed: <h>%d<i> faction(s) charged.";
    public static final String taxRunSkipped = "<i>No faction is due yet; use <h>/f tax run force<i> to force a period.";
    public static final String taxArrearsNotice = "<b>Your faction missed a tax payment. Arrears: <h>%d<i>.";
    public static final String taxInsolventNotice = "<b>%s is insolvent after <h>%d<i> missed tax periods; action: <h>%s<i>.";

    public static final String shieldDisabled = "<b>Faction shields are disabled on this server.";
    public static final String shieldNoFaction = "<b>You are not in a faction.";
    public static final String shieldOwnOnly = "<b>You can only use your own faction's shield.";
    public static final String shieldInactive = "inactive";
    public static final String shieldState = "<h>%s<i> for <h>%d<i> more minute(s)";
    public static final String shieldGrace = "grace";
    public static final String shieldActive = "shielded";
    public static final String shieldStatus = "<i>Shield for <h>%s<i>: %s.";
    public static final String shieldLeaderOnly = "<b>Only the faction leader can change the shield.";
    public static final String shieldInvalidHours = "<b>Enter a number of hours between 1 and %d.";
    public static final String shieldSet = "<g>Shield enabled for <h>%s<i> for <h>%d<i> hour(s).";
    public static final String shieldProtected = "<b>%s is protected by a shield.";

    public static final String notInFaction = "<b>You are not in a faction.";
    public static final String teleportCancelled = "<b>Teleport cancelled: you moved.";
    public static final String topHeader = "<i>Top factions by <h>%s<i>:";
    public static final String topLine = "<h>#%d <i>%s<gray>: <white>%s";
    public static final String topEmpty = "<i>No factions to rank yet.";
    public static final String topInvalidMetric = "<b>Unknown metric. Use power, land, members, bank or points.";
    public static final String statusHeader = "<i>Faction status: <h>%s";
    public static final String statusRole = "<i>Your role: <h>%s";
    public static final String statusPower = "<i>Power: <h>%d/%d";
    public static final String statusLand = "<i>Claims: <h>%d";
    public static final String statusMembers = "<i>Members: <h>%d<i> online, <h>%d<i> total";
    public static final String statusBank = "<i>Bank: <h>%s";
    public static final String statusShield = "<i>Shield: %s";
    public static final String statusTax = "<i>Next tax: %s";
    public static final String statusTaxDisabled = "taxes disabled";
    public static final String statusTaxNever = "not charged yet";
    public static final String coordsLine = "<i>Chunk <h>%s<i> in world <h>%s<i>, owner <h>%s<i> (<h>%s<i>).";
    public static final String coordsBlock = "<i>Your block: <h>%d %d %d<i> (offset in chunk <h>%d<i>, <h>%d<i>).";
    public static final String nearHeader = "<i>Nearby enemy/neutral factions within <h>%d<i> chunks:";
    public static final String nearLine = "<h>%s<i> — <h>%d<i> chunks, %s";
    public static final String nearEmpty = "<i>No enemy or neutral factions nearby.";
    public static final String stuckSearching = "<i>Looking for a safe spot...";
    public static final String stuckWarmup = "<i>Teleporting in <h>%d<i> seconds. Do not move.";
    public static final String stuckTeleported = "<g>You have been moved to safety.";
    public static final String stuckUnavailable = "<b>The stuck command is unavailable right now.";
}
