package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.economy.FactionTaxService;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

/**
 * Runs one faction-tax period or retries an ambiguous one.
 *
 * <p>{@code /f tax run [force]} runs the period; {@code /f tax run <faction>
 * retry} is the only way to charge a period left
 * {@link com.massivecraft.factions.economy.TaxPeriodJournal.State#TAX_UNKNOWN}.
 *
 * @author MarcDoesntExists
 */
public class CmdTaxRun extends FCommand {

    public CmdTaxRun() {
        super();
        this.aliases.add("run");
        this.optionalArgs.put("faction", "");
        this.optionalArgs.put("action", "");

        this.permission = Permission.TAX_RUN.node;
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
        String first = this.argAsString(0, "");
        String second = this.argAsString(1, "");
        if ("retry".equalsIgnoreCase(second)) {
            retry(first);
            return;
        }
        boolean force = "force".equalsIgnoreCase(first);
        int charged = Main.main.factionTaxService.run(force);
        if (charged == 0 && !force) {
            msg(Lang.taxRunSkipped);
            return;
        }
        msg(Lang.taxRunResult, charged);
    }

    private void retry(String identifier) {
        Faction faction = Factions.i.get(identifier);
        if (faction == null) {
            faction = Factions.i.getByTag(identifier);
        }
        if (faction == null) {
            msg(Lang.taxFactionNotFound);
            return;
        }
        FactionTaxService.RetryResult result = Main.main.factionTaxService.retryUnknown(faction);
        switch (result) {
            case CHARGED -> msg(Lang.taxRetryCharged, faction.getTagPlain());
            case NONE -> msg(Lang.taxRetryNone);
            case NO_ECONOMY -> msg(Lang.taxRetryNoEconomy);
            case FAILED -> msg(Lang.taxRetryFailed);
        }
    }
}
