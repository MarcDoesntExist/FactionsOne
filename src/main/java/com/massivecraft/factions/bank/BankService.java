package com.massivecraft.factions.bank;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.event.BankTransactionEvent;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.zcore.Lang;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

/**
 * Single entry point for every faction bank movement.
 *
 * @author MarcDoesntExists
 */
public final class BankService {

    public enum Result {
        SUCCESS, DISABLED, INVALID_AMOUNT, NOT_ENOUGH_MONEY, BACKEND_ERROR, FORBIDDEN, CANCELLED
    }
    private static final double EPSILON = 1.0e-6;
    private static volatile BankService installed = new BankService(null);

    private final BankGateway gateway;
    private final BooleanSupplier enabled;
    private final DoubleConsumer universeMirror;
    private final AccountLocks locks = new AccountLocks();
    private final BankJournalSupport journalSupport;

    public BankService(BankGateway gateway) {
        this(gateway, () -> true, delta -> {
        }, null);
    }
    public BankService(BankGateway gateway, BooleanSupplier enabled, DoubleConsumer universeMirror) {
        this(gateway, enabled, universeMirror, null);
    }
    public BankService(BankGateway gateway, BooleanSupplier enabled, DoubleConsumer universeMirror,
                       BankJournal journal) {
        this.gateway = gateway;
        this.enabled = enabled;
        this.universeMirror = universeMirror;
        this.journalSupport = new BankJournalSupport(journal);
    }
    public static BankService get() {
        return installed;
    }
    public static void install(BankGateway gateway, BooleanSupplier enabled,
                               DoubleConsumer universeMirror) {
        installed = new BankService(gateway, enabled, universeMirror);
    }
    public static void install(BankGateway gateway, BooleanSupplier enabled,
                               DoubleConsumer universeMirror, BankJournal journal) {
        installed = new BankService(gateway, enabled, universeMirror, journal);
    }
    public boolean available() {
        return this.enabled.getAsBoolean() && this.gateway != null && this.gateway.available();
    }
    public String format(double amount) {
        return this.gateway == null ? String.format("%.2f", amount) : this.gateway.format(amount);
    }
    public boolean hasAccount(String accountId) {
        return available() && accountId != null && this.gateway.hasAccount(accountId);
    }
    public double balance(EconomyParticipator participant) {
        return participant == null ? 0.0 : balance(participant.getAccountId());
    }
    public double balance(String accountId) {
        if (!available() || accountId == null) {
            return 0.0;
        }
        return this.locks.withLockedAccount(accountId, () -> this.gateway.balance(accountId));
    }
    public Result transfer(EconomyParticipator invoker, EconomyParticipator from,
                           EconomyParticipator to, double amount, boolean notify) {
        if (!available()) {
            return Result.DISABLED;
        }
        Long minor = minorUnits(amount);
        if (minor == null || from == null || to == null) {
            if (notify && invoker != null) {
                invoker.msg(Lang.bankInvalidAmount);
            }
            return Result.INVALID_AMOUNT;
        }

        String fromAccount = from.getAccountId();
        String toAccount = to.getAccountId();
        if (fromAccount == null || toAccount == null) {
            return Result.INVALID_AMOUNT;
        }
        if (fromAccount.equals(toAccount)) {
            return Result.SUCCESS;
        }
        if (!BankAccess.canControl(invoker, from)) {
            return Result.FORBIDDEN;
        }

        double exact = exactAmount(minor);
        Faction eventFaction = to instanceof Faction ? (Faction) to
                : from instanceof Faction ? (Faction) from : null;
        BankTransactionEvent.Type eventType = from instanceof Faction && to instanceof Faction
                ? BankTransactionEvent.Type.TRANSFER
                : to instanceof Faction ? BankTransactionEvent.Type.DEPOSIT
                : BankTransactionEvent.Type.WITHDRAW;
        if (!BankTransactionEvent.fire(eventFaction, eventType, exact,
                BankJournalSupport.actor(invoker))) {
            if (notify && invoker != null) {
                invoker.msg(Lang.bankTransactionCancelled);
            }
            return Result.CANCELLED;
        }
        String journalId = this.journalSupport.start("transfer", fromAccount, toAccount, exact,
                BankJournalSupport.actor(invoker), fromAccount, exact, "transfer");
        if (this.journalSupport.failed(journalId)) {
            return Result.BACKEND_ERROR;
        }
        boolean[] refundFailed = {false};
        Result result = this.locks.withLockedAccounts(fromAccount, toAccount, () -> {
            if (!this.gateway.withdraw(fromAccount, exact)) {
                return Result.NOT_ENOUGH_MONEY;
            }
            if (this.gateway.deposit(toAccount, exact)) {
                return Result.SUCCESS;
            }
            // The credit failed: the debit must go back before reporting failure.
            if (!this.gateway.deposit(fromAccount, exact)) {
                refundFailed[0] = true;
                logFailure(fromAccount, toAccount, exact);
            }
            return Result.BACKEND_ERROR;
        });

        if (result == Result.SUCCESS) {
            this.journalSupport.finish(journalId, true, "ok");
            audit(invoker, from, to, exact);
            if (notify) {
                BankNotifier.sendTransferInfo(invoker, from, to, format(exact));
            }
        } else if (refundFailed[0]) {
            this.journalSupport.compensationPending(journalId, "refund failed");
        } else {
            this.journalSupport.finish(journalId, false, result.name());
        }
        if (result == Result.NOT_ENOUGH_MONEY && notify && invoker != null) {
            invoker.msg("<h>%s<b> can't afford to transfer <h>%s<b> to %s<b>.",
                    from.describeTo(invoker, true), format(exact), to.describeTo(invoker));
        } else if (result == Result.BACKEND_ERROR && notify && invoker != null) {
            invoker.msg("Unable to transfer %s<b> to <h>%s<b> from <h>%s<b>.", format(exact),
                    to.describeTo(invoker), from.describeTo(invoker, true));
        }
        return result;
    }
    public boolean hasAtLeast(EconomyParticipator participant, double amount, String toDoThis) {
        if (!available()) {
            return true;
        }
        if (participant == null) {
            return false;
        }
        if (Double.isFinite(amount) && amount <= 0.0) {
            return true;
        }
        Long minor = minorUnits(amount);
        if (minor == null) {
            participant.msg(Lang.bankInvalidAmount);
            return false;
        }

        double exact = exactAmount(minor);
        if (balance(participant) + EPSILON >= exact) {
            return true;
        }
        if (toDoThis != null && !toDoThis.isEmpty()) {
            participant.msg("<h>%s<i> can't afford <h>%s<i> %s.",
                    participant.describeTo(participant, true), format(exact), toDoThis);
        }
        return false;
    }
    public boolean modifyMoney(EconomyParticipator participant, double delta, String toDoThis,
                               String forDoingThis) {
        if (!available() || participant == null || !Double.isFinite(delta)) {
            return false;
        }
        if (Math.abs(delta) < 0.005) {
            return true;
        }
        Long minor = minorUnits(Math.abs(delta));
        if (minor == null) {
            return false;
        }

        double exact = exactAmount(minor);
        String accountId = participant.getAccountId();
        if (accountId == null) {
            return false;
        }

        if (delta > 0) {
            return credit(participant, exact, forDoingThis);
        }
        return debit(participant, exact, toDoThis, forDoingThis);
    }
    public boolean creditAccount(String accountId, double amount) {
        if (!available() || accountId == null) {
            return false;
        }
        Long minor = minorUnits(amount);
        if (minor == null) {
            return false;
        }
        return rawCredit(accountId, exactAmount(minor));
    }
    public boolean debitAccount(String accountId, double amount) {
        if (!available() || accountId == null) {
            return false;
        }
        Long minor = minorUnits(amount);
        if (minor == null) {
            return false;
        }
        return rawDebit(accountId, exactAmount(minor));
    }
    public boolean setAccountBalance(String accountId, double amount) {
        if (!available() || accountId == null || !Double.isFinite(amount) || amount < 0.0) {
            return false;
        }
        String journalId = this.journalSupport.start("set", accountId, accountId, amount, "system",
                null, 0.0, "set");
        if (this.journalSupport.failed(journalId)) {
            return false;
        }
        boolean success = this.locks.withLockedAccount(accountId, () -> {
            double current = this.gateway.balance(accountId);
            double difference = amount - current;
            if (Math.abs(difference) < 0.005) {
                return true;
            }
            if (difference > 0) {
                return this.gateway.deposit(accountId, difference);
            }
            return this.gateway.withdraw(accountId, -difference);
        });
        this.journalSupport.finish(journalId, success, "set");
        return success;
    }
    public boolean modifyUniverseMoney(String accountId, double delta) {
        if (!available() || accountId == null || accountId.isEmpty() || !Double.isFinite(delta)
                || delta == 0.0 || !this.gateway.hasAccount(accountId)) {
            return false;
        }
        String journalId = this.journalSupport.start("universe", delta < 0 ? accountId : "",
                delta < 0 ? "" : accountId, Math.abs(delta), "system", null, 0.0, "universe");
        if (this.journalSupport.failed(journalId)) {
            return false;
        }
        boolean success = this.locks.withLockedAccount(accountId, () -> delta < 0
                ? this.gateway.withdraw(accountId, -delta)
                : this.gateway.deposit(accountId, delta));
        this.journalSupport.finish(journalId, success, "universe");
        return success;
    }
    public int retryPendingCompensations() {
        BankJournal journal = this.journalSupport.journal();
        if (journal == null || !available()) {
            return 0;
        }
        int repaired = 0;
        for (BankJournal.Entry entry : journal.pendingCompensations()) {
            boolean credited;
            try {
                credited = this.locks.withLockedAccount(entry.compensationAccount(),
                        () -> this.gateway.deposit(entry.compensationAccount(),
                                entry.compensationAmount()));
            } catch (RuntimeException exception) {
                if (Main.main != null) {
                    Main.main.log("Bank compensation for " + entry.id() + " failed: " + exception);
                }
                credited = false;
            }
            if (credited) {
                if (!journal.compensated(entry.id(), "retry") && Main.main != null) {
                    Main.main.log("Bank compensation " + entry.id()
                            + " was credited but its journal record could not be written.");
                }
                repaired++;
            }
        }
        return repaired;
    }
    public BankJournal journal() {
        return this.journalSupport.journal();
    }
    public int pendingCompensations() {
        BankJournal journal = this.journalSupport.journal();
        return journal == null ? 0 : journal.pendingCompensationCount();
    }
    private boolean credit(EconomyParticipator participant, double exact, String forDoingThis) {
        String accountId = participant.getAccountId();
        String journalId = this.journalSupport.start("credit", "", accountId, exact, "system", null,
                0.0, nullSafe(forDoingThis));
        if (this.journalSupport.failed(journalId)) {
            return false;
        }
        boolean success = rawCredit(accountId, exact);
        this.journalSupport.finish(journalId, success, success ? "ok" : "deposit failed");
        if (!success) {
            if (forDoingThis != null && !forDoingThis.isEmpty()) {
                participant.msg("<h>%s<i> would have gained <h>%s<i> %s, but the deposit failed.",
                        participant.describeTo(participant, true), format(exact), forDoingThis);
            }
            return false;
        }

        this.universeMirror.accept(-exact);
        if (forDoingThis != null && !forDoingThis.isEmpty()) {
            participant.msg("<h>%s<i> gained <h>%s<i> %s.",
                    participant.describeTo(participant, true), format(exact), forDoingThis);
        }
        if (participant instanceof Faction) {
            ((Faction) participant).addLog("bank", "BANK_CREDIT",
                    format(exact) + " " + nullSafe(forDoingThis));
        }
        invalidateRanking();
        return true;
    }
    private boolean debit(EconomyParticipator participant, double exact, String toDoThis,
                          String forDoingThis) {
        String accountId = participant.getAccountId();
        String journalId = this.journalSupport.start("debit", accountId, "", exact, "system", null,
                0.0, nullSafe(forDoingThis));
        if (this.journalSupport.failed(journalId)) {
            return false;
        }
        boolean success = rawDebit(accountId, exact);
        this.journalSupport.finish(journalId, success, success ? "ok" : "withdraw failed");
        if (!success) {
            if (toDoThis != null && !toDoThis.isEmpty()) {
                participant.msg("<h>%s<i> can't afford <h>%s<i> %s.",
                        participant.describeTo(participant, true), format(exact), toDoThis);
            }
            return false;
        }

        this.universeMirror.accept(exact);
        if (forDoingThis != null && !forDoingThis.isEmpty()) {
            participant.msg("<h>%s<i> lost <h>%s<i> %s.",
                    participant.describeTo(participant, true), format(exact), forDoingThis);
        }
        if (participant instanceof Faction) {
            ((Faction) participant).addLog("bank", "BANK_DEBIT",
                    format(exact) + " " + nullSafe(forDoingThis));
        }
        invalidateRanking();
        return true;
    }
    private boolean rawCredit(String accountId, double exact) {
        return this.locks.withLockedAccount(accountId, () -> this.gateway.deposit(accountId, exact));
    }
    private boolean rawDebit(String accountId, double exact) {
        return this.locks.withLockedAccount(accountId,
                () -> this.gateway.withdraw(accountId, exact));
    }
    private void audit(EconomyParticipator invoker, EconomyParticipator from,
                       EconomyParticipator to, double exact) {
        String actor = BankJournalSupport.actor(invoker);
        if (from instanceof Faction) {
            ((Faction) from).addLog(actor, "BANK_OUT", format(exact) + " -> " + label(to));
        }
        if (to instanceof Faction) {
            ((Faction) to).addLog(actor, "BANK_IN", format(exact) + " <- " + label(from));
        }
        invalidateRanking();
    }
    private static void invalidateRanking() {
        if (Main.main != null && Main.main.rankingService != null) {
            Main.main.rankingService.invalidate();
        }
    }
    private static String label(EconomyParticipator participant) {
        if (participant instanceof Faction) {
            return ((Faction) participant).getTagPlain();
        }
        return participant.getAccountId();
    }
    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
    private static Long minorUnits(double amount) {
        if (!Double.isFinite(amount)) {
            return null;
        }
        long minor = Math.round(amount * 100.0);
        return minor <= 0 ? null : minor;
    }
    private static double exactAmount(long minor) {
        return minor / 100.0;
    }
    private static void logFailure(String fromAccount, String toAccount, double amount) {
        String message = "Bank transfer failed and the refund did not complete: "
                + String.format("%.2f", amount) + " from " + fromAccount + " to " + toAccount
                + ". Reconcile these accounts manually.";
        if (Main.main != null) {
            Main.main.log(message);
        }
    }
}
