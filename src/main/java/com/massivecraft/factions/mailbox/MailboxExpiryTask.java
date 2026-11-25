package com.massivecraft.factions.mailbox;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;

/**
 * Expires stale mailbox payouts, retries their refunds and compacts old
 * durable bank/mailbox state.
 *
 * @author MarcDoesntExists
 */
public final class MailboxExpiryTask implements Runnable {

    @Override
    public void run() {
        Main main = Main.main;
        if (main == null) {
            return;
        }
        if (main.mailbox != null && Conf.mailboxEnabled) {
            int expired = main.mailbox.expireStale(main::refundMailboxClaim);
            if (expired > 0) {
                main.log("Expired " + expired + " mailbox payout(s).");
            }
            int unknown = main.mailbox.unknownClaims().size();
            if (unknown > 0) {
                main.log(unknown + " mailbox payout(s) have an unknown delivery; "
                        + "run /f mail reconcile to resolve them.");
            }
        }
        main.compactStorage();
    }
}
