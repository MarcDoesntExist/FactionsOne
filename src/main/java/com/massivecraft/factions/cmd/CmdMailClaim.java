package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.mailbox.FactionMailbox;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.util.List;

/**
 * Claims one mailbox payout, or all of them with {@code /f mail claim all}.
 *
 * @author MarcDoesntExists
 */
public class CmdMailClaim extends FCommand {

    public CmdMailClaim() {
        super();
        this.aliases.add("claim");
        this.optionalArgs.put("id", "all");
        this.permission = Permission.MAIL.node;
        this.disableOnLock = false;
        senderMustBePlayer = true;
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
        String requested = this.argAsString(0, "all");
        if (!"all".equalsIgnoreCase(requested)) {
            FactionMailbox.Result result = claim(requested);
            report(result, this.argAsString(0, ""));
            return;
        }

        List<FactionMailbox.Claim> pending = Main.main.mailbox.pendingFor(fme.getId());
        int claimed = 0;
        double total = 0.0;
        for (FactionMailbox.Claim claim : pending) {
            FactionMailbox.Result result = claim(claim.id());
            if (result == FactionMailbox.Result.SUCCESS) {
                claimed++;
                total += claim.amount();
            } else if (result == FactionMailbox.Result.DELIVERY_FAILED) {
                report(result, claim.id());
            }
        }
        if (claimed == 0) {
            msg(Lang.mailboxClaimNone);
            return;
        }
        msg(Lang.mailboxClaimAll, claimed, BankService.get().format(total));
    }

    private FactionMailbox.Result claim(String claimId) {
        return Main.main.mailbox.claim(fme.getId(), claimId,
                claim -> BankService.get().creditAccount(claim.playerId(), claim.amount()),
                claim -> Main.main.refundMailboxClaim(claim));
    }

    private void report(FactionMailbox.Result result, String claimId) {
        switch (result) {
            case SUCCESS -> {
                FactionMailbox.Claim claim = Main.main.mailbox.get(claimId);
                msg(Lang.mailboxClaimed, BankService.get().format(claim == null ? 0.0 : claim.amount()));
            }
            case NOT_FOUND -> msg(Lang.mailboxNotFound);
            case NOT_OWNED -> msg(Lang.mailboxNotYours);
            case ALREADY_CLAIMED -> msg(Lang.mailboxAlreadyClaimed);
            case EXPIRED -> msg(Lang.mailboxExpired);
            case DELIVERY_FAILED -> msg(Lang.mailboxDeliveryFailed);
            case DELIVERY_UNKNOWN -> msg(Lang.mailboxUnknown);
            case MAILBOX_FULL -> msg(Lang.mailboxFull);
            default -> msg(Lang.mailboxClaimNone);
        }
    }
}
