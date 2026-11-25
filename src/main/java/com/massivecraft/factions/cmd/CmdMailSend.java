package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.mailbox.FactionMailbox;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Queues a payout for an online or offline player. The console uses the
 * configured universe account; a player uses the faction bank when possible.
 *
 * @author MarcDoesntExists
 */
public class CmdMailSend extends FCommand {

    public CmdMailSend() {
        super();
        this.aliases.add("send");
        this.requiredArgs.add("player");
        this.requiredArgs.add("amount");
        this.optionalArgs.put("reason", "mail");
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
        String targetName = this.argAsString(0);
        Double amount = this.argAsDouble(1);
        if (targetName == null || amount == null || !Double.isFinite(amount) || amount <= 0.0) {
            msg(Lang.mailboxInvalidAmount);
            return;
        }

        String sourceAccount;
        if (senderIsConsole) {
            sourceAccount = Conf.econUniverseAccount;
            if (sourceAccount == null || sourceAccount.isBlank()) {
                msg(Lang.mailboxNoSource);
                return;
            }
        } else {
            Faction faction = fme == null ? null : fme.getFaction();
            sourceAccount = faction != null && faction.isNormal()
                    ? faction.getAccountId() : fme.getAccountId();
        }
        if (!BankService.get().debitAccount(sourceAccount, amount)) {
            msg(Lang.mailboxSendFailed);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        FactionMailbox.Claim claim = Main.main.mailbox.enqueue(target.getUniqueId().toString(),
                amount, sourceAccount, sender.getName(), this.argAsString(2, "mail"));
        if (claim == null) {
            BankService.get().creditAccount(sourceAccount, amount);
            msg(Lang.mailboxFull);
            return;
        }
        msg(Lang.mailboxSent, BankService.get().format(claim.amount()), targetName);
        Main.main.log(sender.getName() + " queued " + claim.amount() + " for " + targetName
                + " in the mailbox.");
    }
}
