package com.massivecraft.factions.mailbox;

import com.massivecraft.factions.bank.IdempotencyStore;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Durable payouts waiting for a player.
 *
 * <p>Delivery is <b>at-most-once</b>. The idempotency key is claimed before the
 * delivery sink runs. If the process dies after the key claim but before the
 * sink, the payment status is unknowable: recovery marks the claim
 * {@link State#DELIVERY_UNKNOWN} instead of pretending it was paid. Refunds
 * work the same way: a refund key claimed by a crash leaves the claim
 * {@link State#REFUND_UNKNOWN}. An operator must run
 * {@code /f mail reconcile <id> ...} (or {@link #resolveUnknown} /
 * {@link #resolveRefundUnknown}) after checking the economy. This deliberately
 * forbids automatic second payments and automatic refund retries; it does not
 * promise exactly-once delivery.
 *
 * @author MarcDoesntExists
 */
public final class FactionMailbox {

    private final MailboxStore store;
    private final Logger logger;
    private final IdempotencyStore idempotency;
    private final Clock clock;
    private final int maxPerPlayer;
    private final long ttlMillis;
    private final MailboxRecovery recovery;
    private final MailboxExpiry expiry;
    private final Map<String, Claim> claims = new LinkedHashMap<>();
    public FactionMailbox(Path file, IdempotencyStore idempotency, Clock clock, int maxPerPlayer,
                          long ttlMillis, Logger logger) {
        this.store = new MailboxStore(file, logger);
        this.idempotency = idempotency;
        this.clock = clock;
        this.maxPerPlayer = Math.max(1, maxPerPlayer);
        this.ttlMillis = Math.max(1L, ttlMillis);
        this.logger = logger;
        this.recovery = new MailboxRecovery(idempotency, logger);
        this.expiry = new MailboxExpiry(idempotency, clock, logger, this::save);
        this.claims.putAll(this.store.load());
        if (this.recovery.recover(this.claims)) {
            save();
        }
    }

    private static boolean isPending(Claim claim, long now) {
        return claim.state() == State.PENDING && claim.expiresAt() > now;
    }

    static String deliveryKey(String claimId) {
        return "mailbox:" + claimId;
    }

    static String refundKey(String claimId) {
        return "mailbox-refund:" + claimId;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Adds a payout under a caller-supplied id. Replaying the same id returns
     * the existing claim instead of creating a second one.
     */
    public synchronized Claim enqueue(String id, String playerId, double amount,
                                      String sourceAccount, String source, String reason) {
        if (id == null || id.isBlank() || playerId == null || playerId.isBlank()
            || !Double.isFinite(amount) || amount <= 0.0) {
            return null;
        }
        Claim existing = this.claims.get(id);
        if (existing != null) {
            return existing;
        }
        long now = this.clock.millis();
        long pending = this.claims.values().stream()
            .filter(claim -> claim.playerId().equals(playerId) && isPending(claim, now))
            .count();
        if (pending >= this.maxPerPlayer) {
            return null;
        }
        double exact = Math.round(amount * 100.0) / 100.0;
        if (exact <= 0.0) {
            return null;
        }
        Claim claim = new Claim(id, playerId, exact, nullToEmpty(sourceAccount),
            nullToEmpty(source), nullToEmpty(reason), now, now + this.ttlMillis, State.PENDING,
            false, 0L);
        this.claims.put(id, claim);
        if (!save()) {
            this.claims.remove(id);
            return null;
        }
        return claim;
    }

    public Claim enqueue(String playerId, double amount, String sourceAccount, String source,
                         String reason) {
        return enqueue(UUID.randomUUID().toString(), playerId, amount, sourceAccount, source, reason);
    }

    /**
     * Delivers one claim. A failed delivery releases the idempotency key so the
     * player can retry. A key already claimed without a completed record is
     * ambiguous and becomes {@link State#DELIVERY_UNKNOWN}; it is never
     * delivered automatically.
     */
    public Result claim(String playerId, String claimId, Delivery delivery, Refund refund) {
        if (playerId == null || claimId == null || delivery == null) {
            return Result.INVALID;
        }
        synchronized (this) {
            Claim claim = this.claims.get(claimId);
            if (claim == null) {
                return Result.NOT_FOUND;
            }
            if (!playerId.equals(claim.playerId())) {
                return Result.NOT_OWNED;
            }
            if (claim.state() == State.DELIVERED) {
                return Result.ALREADY_CLAIMED;
            }
            if (claim.state() == State.EXPIRED || claim.state() == State.REFUND_UNKNOWN) {
                return Result.EXPIRED;
            }
            if (claim.state() == State.DELIVERY_UNKNOWN) {
                return Result.DELIVERY_UNKNOWN;
            }
            long now = this.clock.millis();
            if (claim.expiresAt() <= now) {
                this.expiry.expireOne(this.claims, claim, refund);
                return Result.EXPIRED;
            }

            String key = deliveryKey(claim.id());
            if (this.idempotency.isClaimed(key)) {
                return markUnknown(claim);
            }
            if (!this.idempotency.claim(key)) {
                return this.idempotency.isClaimed(key) ? markUnknown(claim)
                    : Result.DELIVERY_FAILED;
            }
            boolean delivered;
            try {
                delivered = delivery.deliver(claim);
            } catch (RuntimeException exception) {
                this.logger.log(java.util.logging.Level.SEVERE,
                    "Mailbox delivery status is unknown for " + claim.id(), exception);
                return markUnknown(claim);
            }
            if (!delivered) {
                this.idempotency.release(key);
                return Result.DELIVERY_FAILED;
            }
            this.claims.put(claim.id(), withState(claim, State.DELIVERED));
            save();
            return Result.SUCCESS;
        }
    }

    /**
     * Expires stale claims and retries refunds that did not complete. Unknown
     * deliveries and refunds are left untouched for the operator.
     */
    public synchronized int expireStale(Refund refund) {
        return this.expiry.expireStale(this.claims, refund);
    }

    public synchronized List<Claim> pendingFor(String playerId) {
        long now = this.clock.millis();
        List<Claim> pending = new ArrayList<>();
        for (Claim claim : this.claims.values()) {
            if (claim.playerId().equals(playerId) && isPending(claim, now)) {
                pending.add(claim);
            }
        }
        pending.sort(Comparator.comparingLong(Claim::createdAt).thenComparing(Claim::id));
        return pending;
    }

    public synchronized int pendingCount() {
        long now = this.clock.millis();
        int count = 0;
        for (Claim claim : this.claims.values()) {
            if (isPending(claim, now)) {
                count++;
            }
        }
        return count;
    }

    public synchronized List<Claim> unknownClaims() {
        List<Claim> unknown = new ArrayList<>();
        for (Claim claim : this.claims.values()) {
            if (claim.state() == State.DELIVERY_UNKNOWN) {
                unknown.add(claim);
            }
        }
        unknown.sort(Comparator.comparingLong(Claim::createdAt).thenComparing(Claim::id));
        return unknown;
    }

    public synchronized List<Claim> unknownRefunds() {
        List<Claim> unknown = new ArrayList<>();
        for (Claim claim : this.claims.values()) {
            if (claim.state() == State.REFUND_UNKNOWN) {
                unknown.add(claim);
            }
        }
        unknown.sort(Comparator.comparingLong(Claim::createdAt).thenComparing(Claim::id));
        return unknown;
    }

    /**
     * Resolves an ambiguous delivery after checking the economy.
     *
     * @param delivered true when the payout reached the player; false releases
     *                  the key and puts the claim back in {@link State#PENDING}
     */
    public synchronized boolean resolveUnknown(String claimId, boolean delivered) {
        Claim claim = this.claims.get(claimId);
        if (claim == null || claim.state() != State.DELIVERY_UNKNOWN) {
            return false;
        }
        if (!delivered) {
            this.idempotency.release(deliveryKey(claim.id()));
        }
        this.claims.put(claim.id(), withState(claim,
            delivered ? State.DELIVERED : State.PENDING));
        return save();
    }

    /**
     * Resolves an ambiguous refund after checking the source account.
     *
     * @param refunded true when the source was credited; false releases the key
     *                 so the next expiry run retries the refund
     */
    public synchronized boolean resolveRefundUnknown(String claimId, boolean refunded) {
        return this.expiry.resolveUnknown(this.claims, claimId, refunded);
    }

    public synchronized Claim get(String claimId) {
        return this.claims.get(claimId);
    }

    public synchronized int retain(long now, long retentionMillis) {
        return this.store.retain(this.claims, now, retentionMillis);
    }

    /**
     * Idempotency keys that retention must not delete: pending or ambiguous
     * deliveries, and refunds that are ambiguous or have not completed yet.
     */
    public synchronized Set<String> protectedKeys() {
        Set<String> keys = new HashSet<>();
        for (Claim claim : this.claims.values()) {
            if (claim.state() == State.PENDING || claim.state() == State.DELIVERY_UNKNOWN) {
                keys.add(deliveryKey(claim.id()));
            }
            if (claim.state() == State.REFUND_UNKNOWN
                || claim.state() == State.EXPIRED && !claim.refunded()) {
                keys.add(refundKey(claim.id()));
            }
        }
        return keys;
    }

    public Path file() {
        return this.store.file();
    }

    private Result markUnknown(Claim claim) {
        this.claims.put(claim.id(), withState(claim, State.DELIVERY_UNKNOWN));
        save();
        this.logger.warning("Mailbox claim " + claim.id()
            + " has an unknown delivery; use /f mail reconcile " + claim.id() + " paid|retry.");
        return Result.DELIVERY_UNKNOWN;
    }

    private boolean save() {
        return this.store.save(this.claims.values());
    }

    private Claim withState(Claim claim, State state) {
        long completedAt = state == State.DELIVERED || state == State.EXPIRED
            ? this.clock.millis() : 0L;
        return new Claim(claim.id(), claim.playerId(), claim.amount(), claim.sourceAccount(),
            claim.source(), claim.reason(), claim.createdAt(), claim.expiresAt(), state,
            claim.refunded(), completedAt);
    }

    public enum State {
        PENDING, DELIVERY_UNKNOWN, DELIVERED, EXPIRED, REFUND_UNKNOWN
    }

    public enum Result {
        SUCCESS, NOT_FOUND, NOT_OWNED, ALREADY_CLAIMED, EXPIRED, DELIVERY_FAILED, DELIVERY_UNKNOWN,
        MAILBOX_FULL, INVALID
    }

    @FunctionalInterface
    public interface Delivery {
        boolean deliver(Claim claim);
    }

    @FunctionalInterface
    public interface Refund {
        boolean refund(Claim claim);
    }

    public record Claim(String id, String playerId, double amount, String sourceAccount,
                        String source, String reason, long createdAt, long expiresAt, State state,
                        boolean refunded, long completedAt) {
    }
}
