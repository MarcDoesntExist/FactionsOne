package com.massivecraft.factions.mailbox;

import com.massivecraft.factions.bank.IdempotencyStore;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Expiry and refund state machine for the faction mailbox.
 *
 * <p>Refunds are at-most-once like deliveries: if the process stops after the
 * refund idempotency key is claimed but before the credit is confirmed, the
 * claim becomes {@link FactionMailbox.State#REFUND_UNKNOWN}. It is never
 * retried automatically; an operator decides with
 * {@code /f mail reconcile <id> refunded|retry-refund} after checking the
 * source account.
 *
 * Extracted from {@link FactionMailbox}.
 *
 * @author MarcDoesntExists
 */
final class MailboxExpiry {

    private final IdempotencyStore idempotency;
    private final Clock clock;
    private final Logger logger;
    private final BooleanSupplier persist;

    MailboxExpiry(IdempotencyStore idempotency, Clock clock, Logger logger,
                  BooleanSupplier persist) {
        this.idempotency = idempotency;
        this.clock = clock;
        this.logger = logger;
        this.persist = persist;
    }

    int expireStale(Map<String, FactionMailbox.Claim> claims, FactionMailbox.Refund refund) {
        long now = this.clock.millis();
        int expired = 0;
        for (FactionMailbox.Claim claim : new ArrayList<>(claims.values())) {
            if (claim.state() == FactionMailbox.State.PENDING && claim.expiresAt() <= now) {
                FactionMailbox.Claim updated = withState(claim,
                        FactionMailbox.State.EXPIRED, now);
                claims.put(claim.id(), updated);
                this.persist.getAsBoolean();
                refundOne(claims, updated, refund);
                expired++;
            }
        }
        for (FactionMailbox.Claim claim : new ArrayList<>(claims.values())) {
            if (claim.state() == FactionMailbox.State.EXPIRED && !claim.refunded()) {
                refundOne(claims, claim, refund);
            }
        }
        return expired;
    }

    void expireOne(Map<String, FactionMailbox.Claim> claims, FactionMailbox.Claim claim,
                   FactionMailbox.Refund refund) {
        FactionMailbox.Claim latest = claims.getOrDefault(claim.id(), claim);
        if (latest.state() != FactionMailbox.State.EXPIRED) {
            latest = withState(latest, FactionMailbox.State.EXPIRED, this.clock.millis());
            claims.put(latest.id(), latest);
            this.persist.getAsBoolean();
        }
        if (!latest.refunded()) {
            refundOne(claims, latest, refund);
        }
    }

    boolean resolveUnknown(Map<String, FactionMailbox.Claim> claims, String claimId,
                           boolean refunded) {
        FactionMailbox.Claim claim = claims.get(claimId);
        if (claim == null || claim.state() != FactionMailbox.State.REFUND_UNKNOWN) {
            return false;
        }
        if (refunded) {
            claims.put(claim.id(), withRefunded(withState(claim,
                    FactionMailbox.State.EXPIRED, claim.completedAt()), true));
        } else {
            this.idempotency.release(FactionMailbox.refundKey(claim.id()));
            claims.put(claim.id(), withState(claim, FactionMailbox.State.EXPIRED,
                    claim.completedAt()));
        }
        return this.persist.getAsBoolean();
    }

    private void refundOne(Map<String, FactionMailbox.Claim> claims, FactionMailbox.Claim claim,
                           FactionMailbox.Refund refund) {
        if (claim.refunded()) {
            return;
        }
        if (claim.sourceAccount().isBlank()) {
            markRefunded(claims, claim);
            return;
        }
        if (refund == null) {
            this.logger.warning("No refund sink available for expired mailbox claim " + claim.id());
            return;
        }
        String key = FactionMailbox.refundKey(claim.id());
        if (this.idempotency.isClaimed(key)) {
            markUnknown(claims, claim);
            return;
        }
        if (!this.idempotency.claim(key)) {
            return;
        }
        boolean refunded;
        try {
            refunded = refund.refund(claim);
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Mailbox refund failed for " + claim.id(), exception);
            refunded = false;
        }
        if (!refunded) {
            this.idempotency.release(key);
            return;
        }
        markRefunded(claims, claim);
    }

    private void markUnknown(Map<String, FactionMailbox.Claim> claims,
                             FactionMailbox.Claim claim) {
        claims.put(claim.id(), withState(claim, FactionMailbox.State.REFUND_UNKNOWN,
                claim.completedAt()));
        this.persist.getAsBoolean();
        this.logger.warning("Mailbox refund " + claim.id()
                + " has an unknown status; use /f mail reconcile " + claim.id()
                + " refunded|retry-refund.");
    }

    private void markRefunded(Map<String, FactionMailbox.Claim> claims,
                              FactionMailbox.Claim claim) {
        FactionMailbox.Claim latest = claims.getOrDefault(claim.id(), claim);
        claims.put(latest.id(), withRefunded(latest, true));
        this.persist.getAsBoolean();
    }

    private static FactionMailbox.Claim withState(FactionMailbox.Claim claim,
                                                  FactionMailbox.State state, long completedAt) {
        return new FactionMailbox.Claim(claim.id(), claim.playerId(), claim.amount(),
                claim.sourceAccount(), claim.source(), claim.reason(), claim.createdAt(),
                claim.expiresAt(), state, claim.refunded(), completedAt);
    }

    private static FactionMailbox.Claim withRefunded(FactionMailbox.Claim claim, boolean refunded) {
        return new FactionMailbox.Claim(claim.id(), claim.playerId(), claim.amount(),
                claim.sourceAccount(), claim.source(), claim.reason(), claim.createdAt(),
                claim.expiresAt(), claim.state(), refunded, claim.completedAt());
    }
}
