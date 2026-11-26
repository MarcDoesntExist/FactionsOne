package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.mailbox.FactionMailbox;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.util.List;

/**
 * Lists pending mailbox payouts. Claiming and sending are subcommands.
 *
 * @author MarcDoesntExists
 */
public class CmdMail extends FCommand {

    public final CmdMailClaim cmdMailClaim = new CmdMailClaim();
    public final CmdMailSend cmdMailSend = new CmdMailSend();
    public final CmdMailReconcile cmdMailReconcile = new CmdMailReconcile();

    public CmdMail() {
        super();
        this.aliases.add("mail");
        this.permission = Permission.MAIL.node;
        this.disableOnLock = false;
        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
        this.setHelpShort("faction mailbox");
        this.addSubCommand(this.cmdMailClaim);
        this.addSubCommand(this.cmdMailSend);
        this.addSubCommand(this.cmdMailReconcile);
    }

    @Override
    public void perform() {
        if (!Conf.mailboxEnabled || Main.main.mailbox == null) {
            msg(Lang.mailboxDisabled);
            return;
        }
        List<FactionMailbox.Claim> pending = Main.main.mailbox.pendingFor(fme.getId());
        if (pending.isEmpty()) {
            msg(Lang.mailboxEmpty);
            return;
        }
        msg(Lang.mailboxHeader, pending.size());
        for (FactionMailbox.Claim claim : pending) {
            msg(Lang.mailboxLine, claim.id(), BankService.get().format(claim.amount()),
                claim.reason().isBlank() ? claim.source() : claim.reason());
        }
    }
}
