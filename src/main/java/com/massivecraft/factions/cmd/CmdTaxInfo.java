package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Shows the tax status for a faction, or the server-wide settings from the
 * console.
 *
 * @author MarcDoesntExists
 */
public class CmdTaxInfo extends FCommand {

    public CmdTaxInfo() {
        super();
        this.aliases.add("info");
        this.optionalArgs.put("faction", "yours");

        this.permission = Permission.TAX.node;
        this.disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        if (!Conf.taxEnabled || Main.main.factionTaxService == null) {
            msg(Lang.taxDisabled);
            return;
        }

        Faction faction = this.argIsSet(0) ? this.argAsFaction(0) : this.myFaction;
        if (faction == null || !faction.isNormal()) {
            if (this.sender instanceof Player) {
                msg(Lang.taxNoFaction);
                return;
            }
            this.showServer();
            return;
        }
        this.showFaction(faction);
    }

    private void showServer() {
        BankService bank = BankService.get();
        msg(Lang.taxServerInfo,
            "enabled",
            Conf.taxMode,
            Main.main.factionTaxService.intervalMinutes(),
            bank.format(Conf.taxBase),
            bank.format(Conf.taxPerMember),
            bank.format(Conf.taxPerChunk),
            bank.format(Conf.taxPercent),
            bank.format(Conf.taxCap),
            Conf.taxGracePeriods,
            Conf.taxInsolvencyAction);
    }

    private void showFaction(Faction faction) {
        long next = faction.getLastTaxAt() <= 0
            ? 0
            : faction.getLastTaxAt() + Main.main.factionTaxService.intervalMillis();
        String state;
        if (faction.isTaxInsolvent()) {
            state = String.format(Lang.taxInsolventState, Conf.taxInsolvencyAction.toUpperCase(Locale.ROOT));
        } else if (next <= 0) {
            state = Lang.taxNeverPaid;
        } else {
            state = String.format(Lang.taxNextCharge, Math.max(0, (next - System.currentTimeMillis()) / 60_000L));
        }
        msg(Lang.taxFactionInfo,
            faction.getTagPlain(),
            BankService.get().format(Main.main.factionTaxService.dueFor(faction)),
            faction.getTaxArrears(),
            state);
    }
}
