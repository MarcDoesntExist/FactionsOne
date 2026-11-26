package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FactionLogEntry;
import com.massivecraft.factions.FactionLogView;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Displays the persisted faction audit log.
 *
 * @author MarcDoesntExists
 */
public final class CmdLogs extends FCommand {

    private static final int PAGE_SIZE = 8;

    public CmdLogs() {
        super();
        this.aliases.add("logs");
        this.aliases.add("log");
        this.optionalArgs.put("filter", "all");
        this.optionalArgs.put("page", "1");
        this.permission = Permission.LOGS.node;
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        if (!FPerm.LOGS.has(this.fme, this.myFaction, true)) {
            return;
        }
        String filter = this.argAsString(0, "all");
        boolean bankOnly = "bank".equalsIgnoreCase(filter);
        int pageIndex;
        if ("all".equalsIgnoreCase(filter) || bankOnly) {
            pageIndex = 1;
        } else if (filter != null && filter.matches("\\d+")) {
            pageIndex = 0;
        } else {
            msg(Lang.logsUsage);
            return;
        }
        List<FactionLogEntry> entries = FactionLogView.filter(this.myFaction.getLogs(), bankOnly);
        int page = Math.max(1, this.argAsInt(pageIndex, 1));
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages);
        int start = Math.max(0, entries.size() - page * PAGE_SIZE);
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        msg(bankOnly ? Lang.logsHeaderBank : Lang.logsHeader, page, pages);
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");
        for (int i = start; i < end; i++) {
            FactionLogEntry entry = entries.get(i);
            msg(Lang.logsLine,
                format.format(new Date(entry.getTimestamp())),
                entry.getActor(), entry.getAction(), entry.getDetail());
        }
        if (entries.isEmpty()) {
            msg(Lang.logsEmpty);
        }
    }
}
