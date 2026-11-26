package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.bank.BankStatus;
import com.massivecraft.factions.bank.StorageRetention;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.time.Instant;

/**
 * Administrative bank maintenance: {@code status} prints a read-only snapshot,
 * {@code reconcile} retries every compensation that failed during a transfer.
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
        if ("status".equalsIgnoreCase(action)) {
            showStatus();
            return;
        }
        if (!"reconcile".equalsIgnoreCase(action)) {
            msg(Lang.bankReconcileUsage);
            return;
        }
        int repaired = BankService.get().retryPendingCompensations();
        msg(Lang.bankReconcileResult, repaired, BankService.get().pendingCompensations());
    }

    private void showStatus() {
        BankStatus status = BankStatus.of(Main.main.bankJournal, Main.main.mailbox);
        msg(Lang.bankStatusJournal, status.pendingJournal(), status.pendingCompensations());
        msg(Lang.bankStatusMailbox, status.pendingMailbox(), status.unknownDeliveries(),
            status.unknownRefunds());
        msg(Lang.bankStatusRetention, Conf.bankJournalRetentionDays,
            Conf.idempotencyRetentionDays, Conf.mailboxRetentionDays);
        long last = StorageRetention.lastCompactedAt();
        if (last <= 0L) {
            msg(Lang.bankStatusNeverCompacted);
        } else {
            msg(Lang.bankStatusCompacted, Instant.ofEpochMilli(last).toString());
        }
    }
}
