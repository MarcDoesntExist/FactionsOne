package com.massivecraft.factions.economy;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.api.SkyAPI;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.bank.IdempotencyStore;
import com.massivecraft.factions.event.FactionTaxCollectedEvent;
import com.massivecraft.factions.event.FactionTaxFailedEvent;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.time.Clock;
import java.util.Comparator;
import java.util.Set;

/**
 * Charges the periodic faction tax through {@link BankService}, applies the
 * arrears policy and runs the configured insolvency action.
 *
 * <p>Every period is journaled before its idempotency key is claimed. A crash
 * between the claim and the debit leaves the period
 * {@link TaxPeriodJournal.State#TAX_UNKNOWN}; it is never charged again
 * automatically and only {@link #retryUnknown(Faction)} can resolve it.
 *
 * @author MarcDoesntExists
 */
public final class FactionTaxService {

    public enum RetryResult {
        CHARGED, NONE, NO_ECONOMY, FAILED
    }

    private final BankService bank;
    private final Clock clock;
    private final IdempotencyStore idempotency;
    private final TaxPeriodJournal journal;
    private boolean warnedNoEconomy;

    public FactionTaxService(BankService bank, Clock clock) {
        this(bank, clock, null, null);
    }

    public FactionTaxService(BankService bank, Clock clock, IdempotencyStore idempotency) {
        this(bank, clock, idempotency, null);
    }

    public FactionTaxService(BankService bank, Clock clock, IdempotencyStore idempotency,
                             TaxPeriodJournal journal) {
        this.bank = bank;
        this.clock = clock;
        this.idempotency = idempotency;
        this.journal = journal;
        if (journal != null) {
            journal.recover(idempotency);
        }
    }

    /**
     * Processes every eligible faction and returns how many were charged.
     *
     * @param force when true the interval check and the journal are bypassed
     */
    public int run(boolean force) {
        if (!Conf.taxEnabled) {
            return 0;
        }
        if (!this.bank.available()) {
            if (!this.warnedNoEconomy && Main.main != null) {
                Main.main.getLogger().warning("Faction tax is enabled but the economy is unavailable; skipping the run.");
                this.warnedNoEconomy = true;
            }
            return 0;
        }
        this.warnedNoEconomy = false;

        TaxSettings settings = this.settings();
        long now = this.clock.millis();
        long interval = intervalMillis();
        int charged = 0;

        for (Faction faction : Factions.i.get()) {
            if (!isEligible(faction)) {
                continue;
            }
            if (!force && faction.getLastTaxAt() > 0 && now - faction.getLastTaxAt() < interval) {
                continue;
            }

            double due = TaxCalculator.due(taxableMembers(faction), faction.getLandRounded(),
                    this.bank.balance(faction), settings);
            if (due <= 0.0) {
                faction.setLastTaxAt(now);
                continue;
            }

            String key = "tax:" + faction.getId() + ":" + (now / interval);
            String claim = null;
            if (!force) {
                claim = key;
                TaxPeriodJournal.Entry existing = this.journal == null
                        ? null : this.journal.entry(claim);
                if (isResolved(existing)) {
                    faction.setLastTaxAt(now);
                    continue;
                }
                if (existing == null && this.journal != null
                        && !this.journal.begin(claim, faction.getId(), due, now)) {
                    continue;
                }
                if (this.idempotency != null) {
                    if (this.idempotency.isClaimed(claim)) {
                        if (this.journal != null) {
                            this.journal.unknown(claim, "claimed without a settled record");
                        }
                        faction.setLastTaxAt(now);
                        continue;
                    }
                    if (!this.idempotency.claim(claim)) {
                        continue;
                    }
                }
            } else if (this.journal != null) {
                // Force may repeat a settled period, but an ambiguous charge is
                // resolved only by the explicit retry, never by a blanket run.
                TaxPeriodJournal.Entry existing = this.journal.entry(key);
                if (existing != null
                        && existing.state() == TaxPeriodJournal.State.TAX_UNKNOWN) {
                    if (Main.main != null) {
                        Main.main.getLogger().warning("Skipping unknown tax period " + key
                                + "; use /f tax run " + faction.getId() + " retry.");
                    }
                    continue;
                }
            }

            if (this.bank.modifyMoney(faction, -due, "to pay the faction tax", "for the faction tax")) {
                if (claim != null && this.journal != null) {
                    this.journal.settled(claim, "paid " + due);
                }
                faction.setTaxArrears(0);
                faction.setTaxInsolvent(false);
                faction.setLastTaxAt(now);
                faction.addLog("tax", "TAX_PAID", this.bank.format(due));
                this.fire(new FactionTaxCollectedEvent(faction.getId(), faction.getTagPlain(), due, now));
                charged++;
            } else {
                if (claim != null) {
                    if (this.idempotency != null) {
                        this.idempotency.release(claim);
                    }
                    if (this.journal != null) {
                        this.journal.abort(claim, "NO_FUNDS");
                    }
                }
                this.handleShortfall(faction, due, now);
            }
        }
        return charged;
    }

    /**
     * Retries the newest period left {@link TaxPeriodJournal.State#TAX_UNKNOWN}
     * for the faction. The idempotency key stays claimed: this is the only path
     * that may charge an ambiguous period, and it must be requested manually.
     */
    public RetryResult retryUnknown(Faction faction) {
        if (faction == null || this.journal == null) {
            return RetryResult.NONE;
        }
        TaxPeriodJournal.Entry entry = this.journal.latestUnknown(faction.getId());
        if (entry == null) {
            return RetryResult.NONE;
        }
        if (!this.bank.available()) {
            return RetryResult.NO_ECONOMY;
        }
        if (entry.amount() > 0.0 && !this.bank.modifyMoney(faction, -entry.amount(),
                "to pay the faction tax", "manual retry of " + entry.key())) {
            return RetryResult.FAILED;
        }
        long now = this.clock.millis();
        this.journal.settled(entry.key(), "manual retry");
        faction.setTaxArrears(0);
        faction.setTaxInsolvent(false);
        faction.setLastTaxAt(now);
        faction.addLog("tax", "TAX_PAID", this.bank.format(entry.amount()) + " retry");
        this.fire(new FactionTaxCollectedEvent(faction.getId(), faction.getTagPlain(),
                entry.amount(), now));
        return RetryResult.CHARGED;
    }

    /** Current amount one period would cost the faction. */
    public double dueFor(Faction faction) {
        if (faction == null) {
            return 0.0;
        }
        return TaxCalculator.due(taxableMembers(faction), faction.getLandRounded(),
                this.bank.balance(faction), this.settings());
    }

    /**
     * A resolved period (paid or ambiguous) must never be charged by the
     * periodic run again. Aborted periods moved no money and may be retried.
     */
    static boolean isResolved(TaxPeriodJournal.Entry entry) {
        return entry != null && (entry.state() == TaxPeriodJournal.State.SETTLED
                || entry.state() == TaxPeriodJournal.State.TAX_UNKNOWN);
    }

    /** Configured interval in minutes, at least one. */
    public long intervalMinutes() {
        return Math.max(1, Conf.taxIntervalMinutes);
    }

    /** Configured interval in milliseconds. */
    public long intervalMillis() {
        return this.intervalMinutes() * 60_000L;
    }

    /**
     * Counts members that contribute the per-member tax. A configured exempt
     * role only removes its members from that component; base and per-chunk
     * charges still apply.
     */
    static int taxableMembers(Faction faction) {
        int members = faction.getFPlayers().size();
        if (Conf.taxExemptRoles == null || Conf.taxExemptRoles.isEmpty()) {
            return members;
        }
        int exempt = 0;
        for (FPlayer member : faction.getFPlayers()) {
            Rel role = member.getRole();
            if (role == null) {
                continue;
            }
            for (String configured : Conf.taxExemptRoles) {
                if (configured != null && role.name().equalsIgnoreCase(configured.trim())) {
                    exempt++;
                    break;
                }
            }
        }
        return Math.max(0, members - exempt);
    }

    private TaxSettings settings() {
        return new TaxSettings(Conf.taxEnabled, Conf.taxBase, Conf.taxPerMember, Conf.taxPerChunk,
                Conf.taxCap, Conf.taxPercent, TaxSettings.Mode.parse(Conf.taxMode));
    }

    private static boolean isEligible(Faction faction) {
        return faction != null && faction.isNormal() && !faction.getFlag(FFlag.PERMANENT)
                && !faction.getFPlayers().isEmpty();
    }

    private void handleShortfall(Faction faction, double due, long now) {
        TaxPolicy.Outcome outcome = TaxPolicy.evaluate(false, faction.getTaxArrears(), Conf.taxGracePeriods);
        faction.setTaxArrears(outcome.arrears());
        faction.setLastTaxAt(now);
        faction.addLog("tax", "TAX_FAILED", this.bank.format(due) + " arrears=" + outcome.arrears());
        this.fire(new FactionTaxFailedEvent(faction.getId(), faction.getTagPlain(), due, outcome.arrears(), "NO_FUNDS"));

        if (Conf.taxNotifyOfficers) {
            this.notifyOfficers(faction, Lang.taxArrearsNotice, outcome.arrears());
        }

        if (!outcome.insolvent() || faction.isTaxInsolvent()) {
            return;
        }

        faction.setTaxInsolvent(true);
        TaxInsolvencyAction action = TaxInsolvencyAction.parse(Conf.taxInsolvencyAction);
        faction.addLog("tax", "TAX_INSOLVENT", "periods=" + outcome.arrears() + " action=" + action.name());
        if (action == TaxInsolvencyAction.UNCLAIM) {
            this.unclaimOne(faction);
        }
        if (Conf.taxNotifyOfficers) {
            this.notifyOfficers(faction, Lang.taxInsolventNotice, faction.getTagPlain(), outcome.arrears(), action.name());
        }
    }

    private void notifyOfficers(Faction faction, String message, Object... args) {
        for (FPlayer player : faction.getFPlayersWhereRole(Rel.LEADER)) {
            player.msg(message, args);
        }
        for (FPlayer player : faction.getFPlayersWhereRole(Rel.COLEADER)) {
            player.msg(message, args);
        }
        for (FPlayer player : faction.getFPlayersWhereRole(Rel.OFFICER)) {
            player.msg(message, args);
        }
    }

    private void unclaimOne(Faction faction) {
        Set<FLocation> claims = SkyAPI.getFactionClaims(faction);
        if (claims.isEmpty()) {
            return;
        }
        FLocation target = claims.stream()
                .min(Comparator.comparing(FLocation::getCoordString))
                .orElse(null);
        Faction wilderness = Factions.i.get("0");
        if (target == null || wilderness == null) {
            return;
        }
        Board.setFactionAt(wilderness, target);
        faction.addLog("tax", "TAX_UNCLAIM", target.toString());
    }

    private void fire(Event event) {
        if (Bukkit.getServer() != null) {
            Bukkit.getServer().getPluginManager().callEvent(event);
        }
    }
}
