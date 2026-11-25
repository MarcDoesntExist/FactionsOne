package com.massivecraft.factions.bank;

import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.Color;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for journal-backed bank compensation.
 *
 * @author MarcDoesntExists
 */
class BankServiceJournalTest {

    private static final double DELTA = 1.0e-9;

    @TempDir
    Path directory;

    @Test
    void failedRefundBecomesPendingAndRetryCreditsIt() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 100.0);
        gateway.rejectAllDeposits = true;
        BankJournal journal = new BankJournal(this.directory.resolve("journal.jsonl"),
                Logger.getLogger("test"));
        BankService bank = new BankService(gateway, () -> true, delta -> {
        }, journal);

        assertEquals(BankService.Result.BACKEND_ERROR,
                bank.transfer(null, new Participant("a"), new Participant("b"), 40.0, false));
        assertEquals(60.0, gateway.get("a"), DELTA);
        assertEquals(1, journal.pendingCompensationCount());

        gateway.rejectAllDeposits = false;
        assertEquals(1, bank.retryPendingCompensations());
        assertEquals(100.0, gateway.get("a"), DELTA);
        assertEquals(0, journal.pendingCompensationCount());
        assertEquals(BankJournal.State.COMPENSATED, journal.entries().get(0).state());
    }

    @Test
    void successfulTransferIsCommittedNotPending() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 100.0);
        BankJournal journal = new BankJournal(this.directory.resolve("journal.jsonl"),
                Logger.getLogger("test"));
        BankService bank = new BankService(gateway, () -> true, delta -> {
        }, journal);

        assertEquals(BankService.Result.SUCCESS,
                bank.transfer(null, new Participant("a"), new Participant("b"), 25.0, false));
        assertEquals(75.0, gateway.get("a"), DELTA);
        assertEquals(25.0, gateway.get("b"), DELTA);
        assertEquals(0, journal.pendingCompensationCount());
        assertEquals(BankJournal.State.COMMITTED, journal.entries().get(0).state());
    }

    @Test
    void retryOnAHealthyBackendDoesNothing() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 10.0);
        BankJournal journal = new BankJournal(this.directory.resolve("journal.jsonl"),
                Logger.getLogger("test"));
        BankService bank = new BankService(gateway, () -> true, delta -> {
        }, journal);
        assertEquals(0, bank.retryPendingCompensations());
        assertEquals(10.0, gateway.get("a"), DELTA);
    }

    private static final class FakeGateway implements BankGateway {

        private final Map<String, Double> balances = new ConcurrentHashMap<>();
        private volatile boolean rejectAllDeposits;

        void set(String accountId, double amount) {
            this.balances.put(accountId, amount);
        }

        double get(String accountId) {
            return this.balances.getOrDefault(accountId, 0.0);
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public boolean hasAccount(String accountId) {
            return this.balances.containsKey(accountId);
        }

        @Override
        public double balance(String accountId) {
            return get(accountId);
        }

        @Override
        public boolean deposit(String accountId, double amount) {
            if (this.rejectAllDeposits) {
                return false;
            }
            this.balances.merge(accountId, amount, Double::sum);
            return true;
        }

        @Override
        public boolean withdraw(String accountId, double amount) {
            double current = get(accountId);
            if (current + DELTA < amount) {
                return false;
            }
            this.balances.put(accountId, current - amount);
            return true;
        }

        @Override
        public String format(double amount) {
            return String.format("%.2f", amount);
        }
    }

    private static final class Participant implements EconomyParticipator {

        private final String accountId;

        Participant(String accountId) {
            this.accountId = accountId;
        }

        @Override
        public String getAccountId() {
            return this.accountId;
        }

        @Override
        public void msg(String str, Object... args) {
        }

        @Override
        public Component describeTo(RelationParticipator observer) {
            return Component.text(this.accountId);
        }

        @Override
        public Component describeTo(RelationParticipator observer, boolean ucfirst) {
            return Component.text(this.accountId);
        }

        @Override
        public Rel getRelationTo(RelationParticipator observer) {
            return Rel.NEUTRAL;
        }

        @Override
        public Rel getRelationTo(RelationParticipator observer, boolean ignorePeaceful) {
            return Rel.NEUTRAL;
        }

        @Override
        public Color getColorTo(RelationParticipator observer) {
            return Color.WHITE;
        }
    }
}
