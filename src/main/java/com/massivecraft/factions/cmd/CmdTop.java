package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.gui.TopMenu;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.rank.FactionRankEntry;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Prints the faction leaderboard, or opens the paged menu with
 * {@code /f top menu}.
 *
 * @author MarcDoesntExists
 */
public class CmdTop extends FCommand {

    public CmdTop() {
        super();
        this.aliases.add("top");
        this.optionalArgs.put("metric", "power");
        this.optionalArgs.put("limit", "10");

        this.permission = Permission.TOP.node;
        this.disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        String rawMetric = this.argAsString(0, "power");
        if ("menu".equalsIgnoreCase(rawMetric)) {
            if (!(this.sender instanceof Player)) {
                this.print(FactionRankEntry.Metric.POWER, 10);
                return;
            }
            new TopMenu(this.me, Main.main.guiConfig, FactionRankEntry.Metric.POWER,
                    Main.main.rankingService.top(FactionRankEntry.Metric.POWER, 50)).open();
            return;
        }

        FactionRankEntry.Metric metric = FactionRankEntry.Metric.parse(rawMetric);
        if (metric == null) {
            msg(Lang.topInvalidMetric);
            return;
        }
        this.print(metric, this.argAsInt(1, 10));
    }

    private void print(FactionRankEntry.Metric metric, int limit) {
        List<FactionRankEntry> entries = Main.main.rankingService.top(metric, limit);
        if (entries.isEmpty()) {
            msg(Lang.topEmpty);
            return;
        }
        msg(Lang.topHeader, metric.key());
        int rank = 1;
        for (FactionRankEntry entry : entries) {
            msg(Lang.topLine, rank++, entry.tag(), this.value(metric, entry));
        }
    }

    private String value(FactionRankEntry.Metric metric, FactionRankEntry entry) {
        return switch (metric) {
            case BANK -> Econ.moneyString(entry.bank());
            case POWER -> String.format(Locale.ROOT, "%.1f", entry.power());
            case LAND -> String.valueOf(entry.land());
            case MEMBERS -> String.valueOf(entry.members());
            case POINTS -> String.valueOf(entry.points());
        };
    }
}
