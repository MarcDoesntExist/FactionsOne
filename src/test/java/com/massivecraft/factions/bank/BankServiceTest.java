package com.massivecraft.factions.bank;

import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.Color;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for the atomic faction bank.
 *
 * <p>Covers the regressions that motivated the service: a negative amount
 * silently reversing the transfer, a balance check racing the withdrawal, and
 * a failed credit losing the debited money.
 *
 * @author MarcDoesntExists
 */
class BankServiceTest {

    private static final int THREADS = 8;
    private static final double DELTA = 1.0e-9;

    @Test
    void transferMovesMoneyOnce() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 100.0);
        gateway.set("b", 25.0);
        BankService bank = new BankService(gateway);

        assertEquals(BankService.Result.SUCCESS,
                bank.transfer(null, new Participant("a"), new Participant("b"), 40.0, false));
        assertEquals(60.0, gateway.get("a"), DELTA);
        assertEquals(65.0, gateway.get("b"), DELTA);
    }

    @Test
    void rejectsZeroNegativeAndNonFiniteAmounts() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 100.0);
        gateway.set("b", 0.0);
        BankService bank = new BankService(gateway);

        for (double amount : new double[]{0.0, -0.0, -50.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertEquals(BankService.Result.INVALID_AMOUNT,
                    bank.transfer(null, new Participant("a"), new Participant("b"), amount, false),
                    "amount " + amount);
        }
        assertEquals(100.0, gateway.get("a"), DELTA);
        assertEquals(0.0, gateway.get("b"), DELTA);
    }

    @Test
    void negativeDepositCannotReverseIntoAWithdrawal() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("player", 100.0);
        gateway.set("faction", 500.0);
        BankService bank = new BankService(gateway);

        // /f money deposit -100 used to flip the direction silently.
        assertFalse(bank.transfer(new Participant("player"), new Participant("player"),
                new Participant("faction"), -100.0, false) == BankService.Result.SUCCESS);
        assertEquals(100.0, gateway.get("player"), DELTA);
        assertEquals(500.0, gateway.get("faction"), DELTA);
    }

    @Test
    void insufficientFundsLeaveBothBalancesUntouched() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 30.0);
        gateway.set("b", 10.0);
        BankService bank = new BankService(gateway);

        assertEquals(BankService.Result.NOT_ENOUGH_MONEY,
                bank.transfer(null, new Participant("a"), new Participant("b"), 30.01, false));
        assertEquals(30.0, gateway.get("a"), DELTA);
        assertEquals(10.0, gateway.get("b"), DELTA);
    }

    @Test
    void failedCreditRefundsTheDebit() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 100.0);
        gateway.set("b", 0.0);
        gateway.rejectDepositsTo = "b";
        BankService bank = new BankService(gateway);

        assertEquals(BankService.Result.BACKEND_ERROR,
                bank.transfer(null, new Participant("a"), new Participant("b"), 40.0, false));
        assertEquals(100.0, gateway.get("a"), "the debit is put back");
        assertEquals(0.0, gateway.get("b"), DELTA);
    }

    @Test
    void failedRefundIsReportedAsBackendError() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 100.0);
        gateway.set("b", 0.0);
        gateway.rejectAllDeposits = true;
        BankService bank = new BankService(gateway);

        assertEquals(BankService.Result.BACKEND_ERROR,
                bank.transfer(null, new Participant("a"), new Participant("b"), 40.0, false));
        assertEquals(60.0, gateway.get("a"), "the debit is gone, but the caller knows");
        assertEquals(0.0, gateway.get("b"), DELTA);
    }

    @Test
    void concurrentTransfersCannotOverdraw() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.racy = true;
        gateway.set("a", 100.0);
        gateway.set("b", 0.0);
        BankService bank = new BankService(gateway);

        List<BankService.Result> results = race(
                () -> bank.transfer(null, new Participant("a"), new Participant("b"), 20.0, false));

        long succeeded = results.stream()
                .filter(result -> result == BankService.Result.SUCCESS)
                .count();
        assertEquals(5, succeeded, "exactly five transfers fit in the balance");
        assertEquals(0.0, gateway.get("a"), DELTA);
        assertEquals(100.0, gateway.get("b"), DELTA, "no transfer is counted twice");
    }

    @Test
    void sameAccountMovesAreSerialized() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 1000.0);
        gateway.trackConcurrency = true;
        BankService bank = new BankService(gateway);

        race(() -> bank.transfer(null, new Participant("a"), new Participant("b"), 10.0, false));

        assertEquals(1, gateway.maxActive.get(), "one movement at a time per account");
        assertEquals(920.0, gateway.get("a"), DELTA);
        assertEquals(80.0, gateway.get("b"), DELTA);
    }

    @Test
    void concurrentOppositeTransfersDoNotDeadlock() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.racy = true;
        gateway.set("a", 100.0);
        gateway.set("b", 100.0);
        BankService bank = new BankService(gateway);
        Participant a = new Participant("a");
        Participant b = new Participant("b");
        AtomicInteger turn = new AtomicInteger();

        List<BankService.Result> results = race(() -> turn.getAndIncrement() % 2 == 0
                ? bank.transfer(null, a, b, 10.0, false)
                : bank.transfer(null, b, a, 10.0, false));

        assertEquals(THREADS, results.size());
        assertEquals(200.0, gateway.get("a") + gateway.get("b"), DELTA,
                "opposite transfers conserve the money");
    }

    @Test
    void modifyMoneyDebitsOnlyWhenTheBalanceCoversIt() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 5.0);
        BankService bank = new BankService(gateway);

        assertFalse(bank.modifyMoney(new Participant("a"), -10.0, "to test", "for testing"));
        assertEquals(5.0, gateway.get("a"), DELTA);

        assertTrue(bank.modifyMoney(new Participant("a"), -5.0, "to test", "for testing"));
        assertEquals(0.0, gateway.get("a"), DELTA);

        assertTrue(bank.modifyMoney(new Participant("a"), 2.5, null, "for testing"));
        assertEquals(2.5, gateway.get("a"), DELTA);
    }

    @Test
    void transferToTheSameAccountIsANoOp() {
        FakeGateway gateway = new FakeGateway();
        gateway.set("a", 100.0);
        BankService bank = new BankService(gateway);

        assertEquals(BankService.Result.SUCCESS,
                bank.transfer(null, new Participant("a"), new Participant("a"), 10.0, false));
        assertEquals(100.0, gateway.get("a"), DELTA);
    }

    @Test
    void disabledEconomyRejectsEveryMovementButKeepsChecksPermissive() {
        FakeGateway gateway = new FakeGateway();
        gateway.available = false;
        gateway.set("a", 100.0);
        BankService bank = new BankService(gateway);

        assertEquals(BankService.Result.DISABLED,
                bank.transfer(null, new Participant("a"), new Participant("b"), 10.0, false));
        assertFalse(bank.modifyMoney(new Participant("a"), -10.0, "to test", "for testing"));
        assertTrue(bank.hasAtLeast(new Participant("a"), 999.0, "to test"),
                "disabled economy never blocks a command");
    }

    private <T> List<T> race(Callable<T> operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return operation.call();
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS), "all racers must be ready");
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Backend double that can deliberately widen the check-then-withdraw race
     * the service is supposed to close.
     */
    private static final class FakeGateway implements BankGateway {

        private final Map<String, Double> balances = new ConcurrentHashMap<>();
        private volatile boolean available = true;
        private volatile boolean rejectAllDeposits = false;
        private volatile String rejectDepositsTo = null;
        private volatile boolean racy = false;
        private volatile boolean trackConcurrency = false;
        private final AtomicInteger active = new AtomicInteger();
        final AtomicInteger maxActive = new AtomicInteger();

        void set(String accountId, double amount) {
            this.balances.put(accountId, amount);
        }

        double get(String accountId) {
            return this.balances.getOrDefault(accountId, 0.0);
        }

        @Override
        public boolean available() {
            return this.available;
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
            enter();
            try {
                if (this.rejectAllDeposits || accountId.equals(this.rejectDepositsTo)) {
                    return false;
                }
                this.balances.merge(accountId, amount, Double::sum);
                return true;
            } finally {
                exit();
            }
        }

        @Override
        public boolean withdraw(String accountId, double amount) {
            enter();
            try {
                double current = get(accountId);
                if (current + DELTA < amount) {
                    return false;
                }
                if (this.racy) {
                    LockSupport.parkNanos(200_000L);
                }
                // Deliberately not atomic: the stale balance is written back, so two
                // racers can both spend the same money without the service lock.
                this.balances.put(accountId, current - amount);
                return true;
            } finally {
                exit();
            }
        }

        private void enter() {
            if (!this.trackConcurrency) {
                return;
            }
            this.maxActive.accumulateAndGet(this.active.incrementAndGet(), Math::max);
            LockSupport.parkNanos(1_000_000L);
        }

        private void exit() {
            if (this.trackConcurrency) {
                this.active.decrementAndGet();
            }
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
            // Assertions use balances, not chat.
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
