package com.massivecraft.factions.mailbox;

import com.massivecraft.factions.bank.IdempotencyStore;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Repairs the crash windows left in the mailbox state.
 *
 * <p>A pending claim whose delivery key is already claimed is ambiguous, and
 * so is an expired, unrefunded claim whose refund key is claimed. Both are
 * marked unknown and never retried automatically; the operator reconciles
 * them explicitly.
 *
 * Extracted from {@link FactionMailbox}.
 *
 * @author MarcDoesntExists
 */
final class MailboxRecovery {

    private final IdempotencyStore idempotency;
    private final Logger logger;

    MailboxRecovery(IdempotencyStore idempotency, Logger logger) {
        this.idempotency = idempotency;
        this.logger = logger;
    }

    boolean recover(Map<String, FactionMailbox.Claim> claims) {
        boolean changed = false;
        boolean ambiguous = false;
        for (FactionMailbox.Claim claim : new ArrayList<>(claims.values())) {
            if (claim.state() == FactionMailbox.State.PENDING
                    && this.idempotency.isClaimed(FactionMailbox.deliveryKey(claim.id()))) {
                claims.put(claim.id(), withState(claim, FactionMailbox.State.DELIVERY_UNKNOWN));
                changed = true;
                ambiguous = true;
            }
            if (claim.state() == FactionMailbox.State.EXPIRED && !claim.refunded()
                    && !claim.sourceAccount().isBlank()
                    && this.idempotency.isClaimed(FactionMailbox.refundKey(claim.id()))) {
                claims.put(claim.id(), withState(claim, FactionMailbox.State.REFUND_UNKNOWN));
                changed = true;
                ambiguous = true;
            }
            if (claim.state() == FactionMailbox.State.DELIVERY_UNKNOWN
                    || claim.state() == FactionMailbox.State.REFUND_UNKNOWN) {
                ambiguous = true;
            }
        }
        if (ambiguous) {
            this.logger.warning("Mailbox recovery found ambiguous deliveries or refunds; "
                    + "reconcile them manually with /f mail reconcile.");
        }
        return changed;
    }

    private static FactionMailbox.Claim withState(FactionMailbox.Claim claim,
                                                  FactionMailbox.State state) {
        return new FactionMailbox.Claim(claim.id(), claim.playerId(), claim.amount(),
                claim.sourceAccount(), claim.source(), claim.reason(), claim.createdAt(),
                claim.expiresAt(), state, claim.refunded(), claim.completedAt());
    }
}
