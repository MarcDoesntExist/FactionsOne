package com.massivecraft.factions.bank;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.iface.EconomyParticipator;

import java.util.HashSet;
import java.util.Set;

/**
 * Chat notifications for bank movements.
 *
 * @author MarcDoesntExists
 */
public final class BankNotifier {

    private BankNotifier() {
    }

    public static Set<FPlayer> recipients(EconomyParticipator participant) {
        Set<FPlayer> fplayers = new HashSet<>();
        if (participant instanceof FPlayer) {
            fplayers.add((FPlayer) participant);
        } else if (participant instanceof Faction) {
            fplayers.addAll(((Faction) participant).getFPlayers());
        }
        return fplayers;
    }

    public static void sendTransferInfo(EconomyParticipator invoker, EconomyParticipator from,
                                        EconomyParticipator to, String amount) {
        Set<FPlayer> recipients = new HashSet<>();
        recipients.addAll(recipients(invoker));
        recipients.addAll(recipients(from));
        recipients.addAll(recipients(to));

        if (invoker == null) {
            for (FPlayer recipient : recipients) {
                recipient.msg("<h>%s<i> was transfered from <h>%s<i> to <h>%s<i>.",
                    amount, from.describeTo(recipient), to.describeTo(recipient));
            }
        } else if (invoker == from) {
            for (FPlayer recipient : recipients) {
                recipient.msg("<h>%s<i> <h>gave %s<i> to <h>%s<i>.",
                    from.describeTo(recipient, true), amount, to.describeTo(recipient));
            }
        } else if (invoker == to) {
            for (FPlayer recipient : recipients) {
                recipient.msg("<h>%s<i> <h>took %s<i> from <h>%s<i>.",
                    to.describeTo(recipient, true), amount, from.describeTo(recipient));
            }
        } else {
            for (FPlayer recipient : recipients) {
                recipient.msg("<h>%s<i> transfered <h>%s<i> from <h>%s<i> to <h>%s<i>.",
                    invoker.describeTo(recipient, true), amount,
                    from.describeTo(recipient), to.describeTo(recipient));
            }
        }
    }
}
