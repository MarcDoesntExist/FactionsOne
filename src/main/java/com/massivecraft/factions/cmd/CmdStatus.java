package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.shield.ShieldService;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.util.Locale;

/**
 * One-screen summary of the player's faction: role, power, land, members,
 * bank, shield and the next tax period.
 *
 * @author MarcDoesntExists
 */
public class CmdStatus extends FCommand {

    public CmdStatus() {
        super();
        this.aliases.add("status");

        this.permission = Permission.STATUS.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        Faction faction = this.myFaction;
        if (faction == null || !faction.isNormal()) {
            msg(Lang.notInFaction);
            return;
        }

        msg(Lang.statusHeader, faction.getTagPlain());
        msg(Lang.statusRole, this.fme.getRole().name().toLowerCase(Locale.ROOT));
        msg(Lang.statusPower, faction.getPowerRounded(), faction.getPowerMaxRounded());
        msg(Lang.statusLand, faction.getLandRounded());
        msg(Lang.statusMembers, faction.getFPlayersWhereOnline(true).size(),
                faction.getFPlayers().size());
        msg(Lang.statusBank, Econ.moneyString(Econ.getBalance(faction.getAccountId())));

        ShieldService shields = Main.main.shieldService;
        String shield;
        if (shields == null || !shields.isProtected(faction)) {
            shield = Lang.shieldInactive;
        } else {
            shield = String.format(Lang.shieldState,
                    shields.isGrace(faction) ? Lang.shieldGrace : Lang.shieldActive,
                    shields.remainingMinutes(faction));
        }
        msg(Lang.statusShield, shield);

        String tax;
        if (!Conf.taxEnabled || Main.main.factionTaxService == null) {
            tax = Lang.statusTaxDisabled;
        } else if (faction.getLastTaxAt() <= 0) {
            tax = Lang.statusTaxNever;
        } else {
            long next = faction.getLastTaxAt() + Main.main.factionTaxService.intervalMillis();
            long minutes = Math.max(0, (next - System.currentTimeMillis()) / 60_000L);
            tax = String.format(Lang.taxNextCharge, minutes);
        }
        msg(Lang.statusTax, tax);
    }
}
