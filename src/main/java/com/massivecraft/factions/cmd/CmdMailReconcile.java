package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.mailbox.FactionMailbox;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.util.List;

/**
 * Admin reconciliation for mailbox deliveries and refunds interrupted between
 * the idempotency claim and the actual credit.
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
            showUnknown();
            return;
        }
        String outcome = this.argAsString(1, "");
        FactionMailbox.Claim claim = Main.main.mailbox.get(claimId);
        if (claim != null && claim.state() == FactionMailbox.State.REFUND_UNKNOWN) {
            resolveRefund(claimId, outcome);
            return;
        }
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

    private void resolveRefund(String claimId, String outcome) {
        boolean completed = "refunded".equalsIgnoreCase(outcome);
        boolean retry = "retry-refund".equalsIgnoreCase(outcome);
        if (!completed && !retry) {
            msg(Lang.mailboxRefundReconcileUsage);
            return;
        }
        if (!Main.main.mailbox.resolveRefundUnknown(claimId, completed)) {
            msg(Lang.mailboxReconcileNotFound);
            return;
        }
        msg(completed ? Lang.mailboxReconciledRefunded : Lang.mailboxReconciledRefundRetry,
            claimId);
    }

    private void showUnknown() {
        List<FactionMailbox.Claim> deliveries = Main.main.mailbox.unknownClaims();
        List<FactionMailbox.Claim> refunds = Main.main.mailbox.unknownRefunds();
        if (deliveries.isEmpty() && refunds.isEmpty()) {
            msg(Lang.mailboxNoUnknown);
            return;
        }
        if (!deliveries.isEmpty()) {
            msg(Lang.mailboxUnknownHeader, deliveries.size());
            print(deliveries);
        }
        if (!refunds.isEmpty()) {
            msg(Lang.mailboxRefundUnknownHeader, refunds.size());
            print(refunds);
        }
    }

    private void print(List<FactionMailbox.Claim> claims) {
        for (FactionMailbox.Claim claim : claims) {
            msg(Lang.mailboxLine, claim.id(), BankService.get().format(claim.amount()),
                claim.reason().isBlank() ? claim.source() : claim.reason());
        }
    }
}
