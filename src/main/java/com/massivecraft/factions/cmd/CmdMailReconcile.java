package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.mailbox.FactionMailbox;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.util.List;

/**
 * Admin reconciliation for mailbox deliveries interrupted between the
 * idempotency claim and the payout credit.
 *
 * @author MarcDoesntExists
 */
public class CmdMailReconcile extends FCommand {

    public CmdMailReconcile() {
        super();
        this.aliases.add("reconcile");
        this.optionalArgs.put("id", "");
        this.optionalArgs.put("outcome", "");
        this.permission = Permission.MAIL_SEND.node;
        this.disableOnLock = false;
        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        if (!Conf.mailboxEnabled || Main.main.mailbox == null) {
            msg(Lang.mailboxDisabled);
            return;
        }
        String claimId = this.argAsString(0, "");
        if (claimId.isBlank()) {
            List<FactionMailbox.Claim> unknown = Main.main.mailbox.unknownClaims();
            if (unknown.isEmpty()) {
                msg(Lang.mailboxNoUnknown);
                return;
            }
            msg(Lang.mailboxUnknownHeader, unknown.size());
            for (FactionMailbox.Claim claim : unknown) {
                msg(Lang.mailboxLine, claim.id(), BankService.get().format(claim.amount()),
                        claim.reason().isBlank() ? claim.source() : claim.reason());
            }
            return;
        }

        String outcome = this.argAsString(1, "");
        boolean paid = "paid".equalsIgnoreCase(outcome) || "delivered".equalsIgnoreCase(outcome);
        boolean retry = "retry".equalsIgnoreCase(outcome) || "pending".equalsIgnoreCase(outcome);
        if (!paid && !retry) {
            msg(Lang.mailboxReconcileUsage);
            return;
        }
        if (!Main.main.mailbox.resolveUnknown(claimId, paid)) {
            msg(Lang.mailboxReconcileNotFound);
            return;
        }
        msg(paid ? Lang.mailboxReconciledPaid : Lang.mailboxReconciledRetry, claimId);
    }
}
