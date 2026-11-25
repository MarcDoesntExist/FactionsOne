package com.massivecraft.factions.cmd;

import com.massivecraft.factions.zcore.CommandVisibility;
import com.massivecraft.factions.zcore.MCommand;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdAutoHelp extends FCommand {
    public CmdAutoHelp() {
        super();
        this.aliases.add("?");
        this.aliases.add("h");
        this.aliases.add("help");

        this.setHelpShort("");

        this.optionalArgs.put("page", "1");

        // Help must stay usable while the plugin is locked.
        this.disableOnLock = false;
    }

    @Override
    public void perform() {
        if (this.commandChain.size() == 0)
            return;
        MCommand<?> pcmd = this.commandChain.get(this.commandChain.size() - 1);

        ArrayList<Component> lines = new ArrayList<Component>();

        for (String line : pcmd.helpLong) {
            lines.add(p.txt.parse(line));
        }

        for (MCommand<?> scmd : pcmd.subCommands) {
            if (scmd.visibility == CommandVisibility.VISIBLE || (
                    scmd.visibility == CommandVisibility.SECRET && scmd
                            .validSenderPermissions(sender, false))) {
                lines.add(scmd.getUseageTemplate(this.commandChain, true));
            }
        }

        sendMessage(p.txt.getPage(lines, this.argAsInt(0, 1),
                "Help for command \"" + pcmd.aliases.get(0) + "\""));
    }
}
