package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.DiscUtil;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdReload extends FCommand {

    public CmdReload() {
        super();
        this.aliases.add("reload");

        //this.requiredArgs.add("");
        this.optionalArgs.put("file", "all");

        this.permission = Permission.RELOAD.node;
        this.disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        long timeInitStart = System.currentTimeMillis();
        // A save may still be queued: make sure the reload reads what was just saved.
        DiscUtil.flushAsync(5000L);

        String file = this.argAsString(0, "all").toLowerCase();
        String fileName;
        boolean success = true;

        if (file.startsWith("c")) {
            Conf.load();
            fileName = "conf.json";
        } else if (file.startsWith("b")) {
            success = Board.load();
            fileName = "board.json";
        } else if (file.startsWith("f")) {
            success = Factions.i.loadFromDisc();
            fileName = "factions.json";
        } else if (file.startsWith("p")) {
            success = FPlayers.i.loadFromDisc();
            fileName = "players.json";
        } else if (file.startsWith("a")) {
            Conf.load();
            success = FPlayers.i.loadFromDisc();
            success = Factions.i.loadFromDisc() && success;
            success = Board.load() && success;
            fileName = "all";
        } else {
            msg("<b>Invalid file specified. <i>Valid files: all, conf, board, factions, players");
            return;
        }

        long timeReload = (System.currentTimeMillis() - timeInitStart);

        if (!success) {
            msg("<b>Could not reload <h>%s<i> from disk, see the console for details.", fileName);
            return;
        }

        msg("<i>Reloaded <h>%s <i>from disk, took <h>%dms<i>.", fileName, timeReload);
    }

}
