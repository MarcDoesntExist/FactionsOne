package com.massivecraft.factions.bank;

import com.massivecraft.factions.mailbox.FactionMailbox;

/**
 * Read-only snapshot of the durable bank and mailbox counters shown by
 * {@code /f admin bank status}.
 *
 * <p>It only reads the in-memory stores: no save, no economy call and no
 * state transition.
 *
 * @author MarcDoesntExists
 */
public record BankStatus(int pendingJournal, int pendingCompensations, int pendingMailbox,
                         int unknownDeliveries, int unknownRefunds) {

    public static BankStatus of(BankJournal journal, FactionMailbox mailbox) {
        int pending = 0;
        if (journal != null) {
            for (BankJournal.Entry entry : journal.entries()) {
                if (entry.state() == BankJournal.State.PENDING) {
                    pending++;
                }
            }
        }
        return new BankStatus(
                pending,
                journal == null ? 0 : journal.pendingCompensationCount(),
                mailbox == null ? 0 : mailbox.pendingCount(),
                mailbox == null ? 0 : mailbox.unknownClaims().size(),
                mailbox == null ? 0 : mailbox.unknownRefunds().size());
    }
}
