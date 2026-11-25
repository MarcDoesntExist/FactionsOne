package com.massivecraft.factions.bank;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.zcore.Lang;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * Single entry point for every faction bank movement.
 *
 * <p>A movement locks the accounts involved (in a stable order), validates the
 * amount in minor units, debits before crediting, and puts the debit back when
 * the credit fails. Concurrent or repeated calls therefore cannot create or
 * lose money. Successful movements are audited on the faction log.
 *
 * @author MarcDoesntExists
 */
public final class BankService {

    public enum Result {
        SUCCESS, DISABLED, INVALID_AMOUNT, NOT_ENOUGH_MONEY, BACKEND_ERROR, FORBIDDEN
    }

    private static final double EPSILON = 1.0e-6;
    private static volatile BankService installed = new BankService(null);

    private final BankGateway gateway;
    private final BooleanSupplier enabled;
    private final DoubleConsumer universeMirror;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public BankService(BankGateway gateway) {
        this(gateway, () -> true, delta -> {
        });
    }

    public BankService(BankGateway gateway, BooleanSupplier enabled, DoubleConsumer universeMirror) {
        this.gateway = gateway;
        this.enabled = enabled;
        this.universeMirror = universeMirror;
    }

    public static BankService get() {
        return installed;
    }

    public static void install(BankGateway gateway, BooleanSupplier enabled,
                               DoubleConsumer universeMirror) {
        installed = new BankService(gateway, enabled, universeMirror);
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
        return withLockedAccount(accountId, () -> this.gateway.balance(accountId));
    }

    /**
     * Moves money between two participants. Negative, zero and non-finite
     * amounts are rejected instead of reversing the direction of the transfer.
     */
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
        Result result = withLockedAccounts(fromAccount, toAccount, () -> {
            if (!this.gateway.withdraw(fromAccount, exact)) {
                return Result.NOT_ENOUGH_MONEY;
            }
            if (this.gateway.deposit(toAccount, exact)) {
                return Result.SUCCESS;
            }
            // The credit failed: the debit must go back before reporting failure.
            if (!this.gateway.deposit(fromAccount, exact)) {
                logFailure(fromAccount, toAccount, exact);
            }
            return Result.BACKEND_ERROR;
        });

        if (result == Result.SUCCESS) {
            audit(invoker, from, to, exact);
            if (notify) {
                BankNotifier.sendTransferInfo(invoker, from, to, format(exact));
            }
        } else if (result == Result.NOT_ENOUGH_MONEY && notify && invoker != null) {
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

    /**
     * Applies a signed delta to one account. Positive deltas are credited,
     * negative deltas are debited atomically.
     */
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

    /**
     * Raw credit used by the economy helpers (legacy migration, universe account).
     */
    public boolean creditAccount(String accountId, double amount) {
        if (!available() || accountId == null) {
            return false;
        }
        Long minor = minorUnits(amount);
        if (minor == null) {
            return false;
        }
        return withLockedAccount(accountId, () -> this.gateway.deposit(accountId, exactAmount(minor)));
    }

    /**
     * Raw debit used by the economy helpers. Fails when the balance is short.
     */
    public boolean debitAccount(String accountId, double amount) {
        if (!available() || accountId == null) {
            return false;
        }
        Long minor = minorUnits(amount);
        if (minor == null) {
            return false;
        }
        return withLockedAccount(accountId, () -> this.gateway.withdraw(accountId, exactAmount(minor)));
    }

    /**
     * Sets an account to an amount by moving only the difference.
     */
    public boolean setAccountBalance(String accountId, double amount) {
        if (!available() || accountId == null || !Double.isFinite(amount) || amount < 0.0) {
            return false;
        }
        return withLockedAccount(accountId, () -> {
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
    }

    /**
     * Moves money on the configured universe account, if it exists.
     */
    public boolean modifyUniverseMoney(String accountId, double delta) {
        if (!available() || accountId == null || accountId.isEmpty() || !Double.isFinite(delta)
                || delta == 0.0 || !this.gateway.hasAccount(accountId)) {
            return false;
        }
        return withLockedAccount(accountId, () -> delta < 0
                ? this.gateway.withdraw(accountId, -delta)
                : this.gateway.deposit(accountId, delta));
    }

    private boolean credit(EconomyParticipator participant, double exact, String forDoingThis) {
        boolean success = withLockedAccount(participant.getAccountId(),
                () -> this.gateway.deposit(participant.getAccountId(), exact));
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
        boolean success = withLockedAccount(participant.getAccountId(),
                () -> this.gateway.withdraw(participant.getAccountId(), exact));
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

    private void audit(EconomyParticipator invoker, EconomyParticipator from,
                       EconomyParticipator to, double exact) {
        String actor = invoker == null ? "console"
                : invoker instanceof FPlayer ? ((FPlayer) invoker).getName()
                : invoker.getAccountId();
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

    private <T> T withLockedAccount(String accountId, Supplier<T> action) {
        ReentrantLock lock = lockFor(accountId);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private <T> T withLockedAccounts(String first, String second, Supplier<T> action) {
        if (first.equals(second)) {
            return withLockedAccount(first, action);
        }
        String low = first.compareTo(second) <= 0 ? first : second;
        String high = low.equals(first) ? second : first;
        ReentrantLock lowLock = lockFor(low);
        ReentrantLock highLock = lockFor(high);
        lowLock.lock();
        try {
            highLock.lock();
            try {
                return action.get();
            } finally {
                highLock.unlock();
            }
        } finally {
            lowLock.unlock();
        }
    }

    private ReentrantLock lockFor(String accountId) {
        return this.locks.computeIfAbsent(accountId, key -> new ReentrantLock());
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
