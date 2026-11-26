package com.massivecraft.factions.bank;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.iface.EconomyParticipator;

/**
 * Small adapter that lets {@link BankService} journal movements without
 * spreading the null checks through every operation.
 *
 * @author MarcDoesntExists
 */
record BankJournalSupport(BankJournal journal) {

    static String actor(EconomyParticipator invoker) {
        if (invoker == null) {
            return "console";
        }
        return invoker instanceof FPlayer ? ((FPlayer) invoker).getName() : invoker.getAccountId();
    }

    String start(String operation, String from, String to, double amount, String actor,
                 String compensationAccount, double compensationAmount, String detail) {
        return this.journal == null ? "" : this.journal.begin(operation, from, to, amount, actor,
            compensationAccount, compensationAmount, detail);
    }

    boolean failed(String journalId) {
        return this.journal != null && journalId == null;
    }

    void finish(String journalId, boolean success, String detail) {
        if (this.journal == null || journalId == null || journalId.isEmpty()) {
            return;
        }
        if (success) {
            this.journal.commit(journalId, detail);
        } else {
            this.journal.abort(journalId, detail);
        }
    }

    void compensationPending(String journalId, String detail) {
        if (this.journal != null && journalId != null && !journalId.isEmpty()) {
            this.journal.compensationPending(journalId, detail);
        }
    }
}
