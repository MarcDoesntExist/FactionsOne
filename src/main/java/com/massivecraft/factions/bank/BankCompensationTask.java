package com.massivecraft.factions.bank;

import com.massivecraft.factions.Main;

/**
 * Periodically retries refunds that failed during a transfer.
 *
 * @author MarcDoesntExists
 */
public final class BankCompensationTask implements Runnable {

    @Override
    public void run() {
        int repaired = BankService.get().retryPendingCompensations();
        if (Main.main == null) {
            return;
        }
        if (repaired > 0) {
            Main.main.log("Bank compensation retry credited " + repaired + " account(s).");
        }
        Main.main.compactStorage();
    }
}
