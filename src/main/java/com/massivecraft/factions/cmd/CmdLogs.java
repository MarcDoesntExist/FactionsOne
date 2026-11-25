package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FactionLogEntry;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;

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
        List<FactionLogEntry> entries = this.myFaction.getLogs();
        int page = Math.max(1, this.argAsInt(0, 1));
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages);
        int start = Math.max(0, entries.size() - page * PAGE_SIZE);
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        msg("<gold>Faction logs <gray>(page %s/%s)", page, pages);
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");
        for (int i = start; i < end; i++) {
            FactionLogEntry entry = entries.get(i);
            msg("<gray>%s <white>%s <aqua>%s <gray>%s",
                    format.format(new Date(entry.getTimestamp())),
                    entry.getActor(), entry.getAction(), entry.getDetail());
        }
        if (entries.isEmpty()) {
            msg("<i>No activity logged yet.");
        }
    }
}
