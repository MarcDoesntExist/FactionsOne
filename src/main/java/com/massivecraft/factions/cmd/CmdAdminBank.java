package com.massivecraft.factions.cmd;

import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

/**
 * Administrative bank maintenance. Reconcile retries every compensation that
 * failed during a transfer.
 *
 * @author MarcDoesntExists
 */
public class CmdAdminBank extends FCommand {

    public CmdAdminBank() {
        super();
        this.aliases.add("bank");
        this.optionalArgs.put("action", "reconcile");
        this.permission = Permission.ADMIN.node;
        this.disableOnLock = false;
        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        String action = this.argAsString(0, "reconcile");
        if (!"reconcile".equalsIgnoreCase(action)) {
            msg(Lang.bankReconcileUsage);
            return;
        }
        int repaired = BankService.get().retryPendingCompensations();
        msg(Lang.bankReconcileResult, repaired, BankService.get().pendingCompensations());
    }
}
