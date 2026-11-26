package com.massivecraft.factions.zcore;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class Lang {
    public static String permForbidden = "<b>You don't have permission to %s.";
    public static String permDoThat = "do that";

    public static String commandSenderMustBePlayer =
        "<b>This command can only be used by ingame players.";
    public static String commandToFewArgs = "<b>Too few arguments. <i>Use like this:";
    public static String commandToManyArgs =
        "<b>Strange argument \"<main>%s<b>\". <i>Use the command like this:";

    public static String featureNoPermission =
        "<b>Your faction does not allow you to use this feature.";
    public static String featureNotFound = "<b>That feature entry does not exist.";
    public static String featureMaxLevel = "<b>This upgrade is already at its maximum level.";
    public static String featureNotEnoughPoints = "<b>Your faction does not have enough points.";
    public static String featureNoSpace = "<b>Your inventory does not have enough space.";
    public static String featureNotReady = "<b>This mission is not ready to be claimed.";
    public static String featureSuccess = "<g>Done.";
    public static String featureCancelled = "<b>That action was cancelled.";
    public static String bankTransactionCancelled = "<b>That bank transaction was cancelled.";
    public static String warpTeleportCancelled = "<b>The warp teleport was cancelled.";
    public static String warpCostFailed = "<b>You cannot pay for this warp right now.";
    public static String shieldChangeCancelled = "<b>That shield change was cancelled.";
    public static String shieldCannotAttack = "<b>A shielded faction cannot attack or overclaim.";
    public static String roleChangeCancelled = "<b>That role change was cancelled.";
    public static String upgradeHeader = "<gold>Faction upgrades <gray>(points: <white>%s<gray>)";
    public static String upgradeLine = "<aqua>%s <gray>level %s/%s, cost %s";
    public static String upgradeNotFound = "<b>That upgrade does not exist.";
    public static String upgradeBought = "<g>Bought upgrade <h>%s<g>.";
    public static String upgradeMenuTitle = "<gold>Upgrades";
    public static String upgradeMenuName = "<gold>%s";
    public static String upgradeMenuLevel = "<gray>Level: <white>%s/%s";
    public static String upgradeMenuCost = "<gray>Cost: <white>%s points";
    public static String upgradeMenuClick = "<yellow>Click to buy";
    public static String upgradeMenuBought = "<g>Upgrade bought.";
    public static String memberLimit = "<b>!<white> The faction %s is at the limit of %d members, so %s cannot currently join.";
    public static String allyLimit = "<b>Your faction has reached its ally limit of %d.";
    public static String relationAllianceWar =
        "<b>You are allied with that faction. Use <c>/f neutral<b> before declaring war.";
    public static String relationBetrayalPenalty =
        "<b>Betrayal! Your faction lost <c>%s<b> power for attacking a former ally.";
    public static String relationBetrayalVictim =
        "<b>%s <i>betrayed the alliance and declared war on you.";
    public static String claimLimit = "<b>Limit reached. You can't claim more land!";
    public static String tntUsage = "<i>Use /f tnt deposit|withdraw|balance|fill|siphon.";
    public static String tntNoContainer = "<b>No dispenser was changed.";
    public static String featureNoPlayer = "<b>That player is not online.";
    public static String chatUsage = "<i>Use /f chat <public|faction|ally> [message], /fc <message> or /ac <message>.";
    public static String chatNoAllies = "<i>No allies are online.";
    public static String chatChannelSet = "<i>Chat channel set to <h>%s<i>.";
    public static String logsHeader = "<gold>Faction logs <gray>(page %s/%s)";
    public static String logsHeaderBank = "<gold>Faction logs <gray>(bank, page %s/%s)";
    public static String logsUsage = "<i>Usage: <h>/f logs [all|bank] [page]";
    public static String logsMenuTitle = "<gray>Faction logs";
    public static String logsMenuEntry = "<white>%s <gray>%s";
    public static String logsMenuTime = "<gray>%s";
    public static String logsMenuDetail = "<gray>%s";
    public static String logsLine = "<gray>%s <white>%s <aqua>%s <gray>%s";
    public static String logsEmpty = "<i>No activity logged yet.";
    public static String shopTitle = "<gold>Shop";
    public static String shopItemName = "<gold>%s";
    public static String shopLoreAmount = "<gray>Amount: <white>%s";
    public static String shopLoreCost = "<gray>Cost: <white>%s points";
    public static String shopLorePerk = "<gray>Perk: <white>%s";
    public static String shopLorePerkAmount = "<gray>Amount: <white>%s";
    public static String shopLoreDuration = "<gray>Duration: <white>%s min";
    public static String shopLoreClick = "<yellow>Click to buy";
    public static String shopPurchase = "<g>Purchase complete.";
    public static String missionTitle = "<aqua>Missions";
    public static String missionItemName = "<aqua>%s";
    public static String missionLoreDescription = "<gray>%s";
    public static String missionLoreProgress = "<gray>Progress: <white>%s/%s";
    public static String missionLoreReward = "<gray>Reward: <white>%s points";
    public static String missionLorePeriod = "<gray>Period: <white>%s";
    public static String missionLoreNextReset = "<gray>Next reset: <white>%s";
    public static String missionLoreMoney = "<gray>Money reward: <white>%s";
    public static String missionLoreItems = "<gray>Item reward: <white>%s";
    public static String missionRewardFailed = "<b>The mission reward could not be granted.";
    public static String missionCompleted = "<g>Completed";
    public static String missionClick = "<yellow>Click to claim";
    public static String missionNotReady = "<gray>Not ready";
    public static String missionClaimed = "<g>Mission reward claimed.";
    public static String bankInvalidAmount = "<b>Enter an amount greater than zero.";
    public static String bankReconcileResult = "<i>Bank reconciliation: <h>%d<i> compensation(s) retried, <h>%d<i> still pending.";
    public static String bankReconcileUsage = "<i>Usage: <h>/f admin bank reconcile|status";
    public static String bankStatusJournal = "<i>Bank journal: <h>%d<i> pending movement(s), <h>%d<i> pending compensation(s).";
    public static String bankStatusMailbox = "<i>Mailbox: <h>%d<i> pending payout(s), <h>%d<i> unknown delivery(ies), <h>%d<i> unknown refund(s).";
    public static String bankStatusRetention = "<i>Retention: journal <h>%d<i>d, idempotency <h>%d<i>d, mailbox <h>%d<i>d.";
    public static String bankStatusCompacted = "<i>Last storage compaction: <h>%s<i>.";
    public static String bankStatusNeverCompacted = "<i>No storage compaction has run since startup.";
    public static String mailboxDisabled = "<b>The faction mailbox is disabled on this server.";
    public static String mailboxEmpty = "<i>Your mailbox has no pending payouts.";
    public static String mailboxHeader = "<i>Mailbox: <h>%d<i> pending payout(s).";
    public static String mailboxLine = "<h>%s <gray>— <white>%s <gray>(%s)";
    public static String mailboxClaimed = "<g>Claimed <h>%s<i> from the mailbox.";
    public static String mailboxClaimAll = "<g>Claimed <h>%d<i> payout(s), total <h>%s<i>.";
    public static String mailboxClaimNone = "<b>No payout was claimed.";
    public static String mailboxNotFound = "<b>That payout does not exist.";
    public static String mailboxNotYours = "<b>That payout belongs to another player.";
    public static String mailboxAlreadyClaimed = "<b>That payout was already claimed.";
    public static String mailboxExpired = "<b>That payout expired and was returned to its source.";
    public static String mailboxDeliveryFailed = "<b>The payout could not be delivered; try again later.";
    public static String mailboxUnknown = "<b>The delivery status of that payout is unknown; ask an admin to reconcile it.";
    public static String mailboxNoUnknown = "<i>No mailbox delivery needs reconciliation.";
    public static String mailboxUnknownHeader = "<i>Mailbox deliveries with unknown status: <h>%d<i>.";
    public static String mailboxRefundUnknownHeader = "<i>Mailbox refunds with unknown status: <h>%d<i>.";
    public static String mailboxReconcileUsage = "<i>Usage: <h>/f mail reconcile [id] paid|retry";
    public static String mailboxReconcileNotFound = "<b>No unknown payout with that id.";
    public static String mailboxReconciledPaid = "<g>Marked <h>%s<i> as paid.";
    public static String mailboxReconciledRetry = "<g>Released <h>%s<i>; the player can claim it again.";
    public static String mailboxRefundReconcileUsage = "<i>Usage: <h>/f mail reconcile <id> refunded|retry-refund";
    public static String mailboxReconciledRefunded = "<g>Marked the refund of <h>%s<i> as completed.";
    public static String mailboxReconciledRefundRetry = "<g>Released the refund of <h>%s<i>; it will be retried.";
    public static String mailboxFull = "<b>That player's mailbox is full.";
    public static String mailboxSent = "<g>Queued <h>%s<i> for <h>%s<i> in the mailbox.";
    public static String mailboxSendFailed = "<b>The source account could not fund that payout.";
    public static String mailboxNoSource = "<b>No source account is available for this payout.";
    public static String mailboxInvalidAmount = "<b>Enter a valid amount greater than zero.";
    public static String mailboxOnlyPlayers = "<b>Only an in-game player can claim mailbox payouts.";

    public static String taxDisabled = "<b>Faction taxes are disabled on this server.";
    public static String taxNoFaction = "<b>You are not in a faction.";
    public static String taxServerInfo = "<i>Faction tax: <h>%s<i>, mode <h>%s<i>, interval <h>%d<i> min, base <h>%s<i>, per member <h>%s<i>, per chunk <h>%s<i>, percent <h>%s<i>, cap <h>%s<i>, grace <h>%d<i>, insolvency <h>%s<i>.";
    public static String taxFactionInfo = "<i>Tax for <h>%s<i>: current due <h>%s<i>, arrears <h>%d<i>, %s.";
    public static String taxNeverPaid = "no charge recorded yet";
    public static String taxNextCharge = "next charge in <h>%d<i> min";
    public static String taxInsolventState = "insolvent (<h>%s<i>)";
    public static String taxRunResult = "<i>Tax period processed: <h>%d<i> faction(s) charged.";
    public static String taxRunSkipped = "<i>No faction is due yet; use <h>/f tax run force<i> to force a period.";
    public static String taxFactionNotFound = "<b>No faction matches that id or tag.";
    public static String taxRetryCharged = "<g>Retried the unknown tax period for <h>%s<i>.";
    public static String taxRetryNone = "<b>No unknown tax period for that faction.";
    public static String taxRetryNoEconomy = "<b>The economy is unavailable; the period stays unknown.";
    public static String taxRetryFailed = "<b>The retry charge failed; the period stays unknown.";
    public static String taxArrearsNotice = "<b>Your faction missed a tax payment. Arrears: <h>%d<i>.";
    public static String taxInsolventNotice = "<b>%s is insolvent after <h>%d<i> missed tax periods; action: <h>%s<i>.";

    public static String shieldDisabled = "<b>Faction shields are disabled on this server.";
    public static String shieldNoFaction = "<b>You are not in a faction.";
    public static String shieldOwnOnly = "<b>You can only use your own faction's shield.";
    public static String shieldInactive = "inactive";
    public static String shieldState = "<h>%s<i> for <h>%d<i> more minute(s)";
    public static String shieldGrace = "grace";
    public static String shieldActive = "shielded";
    public static String shieldStatus = "<i>Shield for <h>%s<i>: %s.";
    public static String shieldLeaderOnly = "<b>Only the faction leader can change the shield.";
    public static String shieldInvalidHours = "<b>Enter a number of hours between 1 and %d.";
    public static String shieldSet = "<g>Shield enabled for <h>%s<i> for <h>%d<i> hour(s).";
    public static String shieldProtected = "<b>%s is protected by a shield.";

    public static String notInFaction = "<b>You are not in a faction.";
    public static String placeholderWilderness = "Wilderness";
    public static String teleportCancelled = "<b>Teleport cancelled: you moved.";
    public static String topHeader = "<i>Top factions by <h>%s<i>:";
    public static String topLine = "<h>#%d <i>%s<gray>: <white>%s";
    public static String topEmpty = "<i>No factions to rank yet.";
    public static String topInvalidMetric =
        "<b>Unknown metric. Use power, land, members, bank, points or value.";
    public static String claimValueDisabled =
        "<b>Claim value ranking is disabled in the configuration.";
    public static String powerFactionBreakdown = "<i>Faction: <h>%d/%d <i>power from <h>%d<i> member(s) on <h>%d<i> claim(s); bonus/malus <h>%s<i>.";
    public static String showState = "<a>Status: <i>%s <a>| <i>%s <a>| <i>%s";
    public static String showRaidable = "raidable (claims exceed power)";
    public static String showSafe = "not raidable";
    public static String showAtWar = "at war with you";
    public static String showNotAtWar = "not at war with you";
    public static String statusHeader = "<i>Faction status: <h>%s";
    public static String statusRole = "<i>Your role: <h>%s";
    public static String statusPower = "<i>Power: <h>%d/%d";
    public static String statusLand = "<i>Claims: <h>%d";
    public static String statusMembers = "<i>Members: <h>%d<i> online, <h>%d<i> total";
    public static String statusBank = "<i>Bank: <h>%s";
    public static String statusShield = "<i>Shield: %s";
    public static String statusTax = "<i>Next tax: %s";
    public static String statusTaxDisabled = "taxes disabled";
    public static String statusTaxNever = "not charged yet";
    public static String coordsLine = "<i>Chunk <h>%s<i> in world <h>%s<i>, owner <h>%s<i> (<h>%s<i>).";
    public static String coordsBlock = "<i>Your block: <h>%d %d %d<i> (offset in chunk <h>%d<i>, <h>%d<i>).";
    public static String nearHeader = "<i>Nearby enemy/neutral factions within <h>%d<i> chunks:";
    public static String nearLine = "<h>%s<i> — <h>%d<i> chunks, %s";
    public static String nearEmpty = "<i>No enemy or neutral factions nearby.";
    public static String stuckSearching = "<i>Looking for a safe spot...";
    public static String stuckWarmup = "<i>Teleporting in <h>%d<i> seconds. Do not move.";
    public static String stuckTeleported = "<g>You have been moved to safety.";
    public static String stuckUnavailable = "<b>The stuck command is unavailable right now.";
}
