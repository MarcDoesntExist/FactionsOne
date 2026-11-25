package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.Lang;

/**
 * Sends a message to the sender's faction and its allies.
 *
 * @author MarcDoesntExists
 */
public final class CmdChatAlly extends FCommand {

    public CmdChatAlly() {
        super();
        this.aliases.add("ally");
        this.aliases.add("a");
        this.requiredArgs.add("message");
        this.errorOnToManyArgs = false;
        this.permission = Permission.CHAT.node;
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        if (!FPerm.CHAT_ALLY.has(this.fme, this.myFaction, true)) {
            return;
        }
        String message = String.join(" ", this.args);
        if (message.isBlank()) {
            return;
        }
        Faction faction = this.myFaction;
        int recipients = 0;
        for (FPlayer other : FPlayers.i.getOnline()) {
            if (other.getFaction() == null) {
                continue;
            }
            if (other.getFaction() != faction
                    && faction.getRelationTo(other.getFaction()) != Rel.ALLY) {
                continue;
            }
            other.msg(Conf.chatAllyFormat, this.fme.getName(), message);
            recipients++;
        }
        faction.addLog(this.fme.getName(), "ALLY_CHAT", message);
        if (recipients == 0) {
            msg(Lang.chatNoAllies);
        }
    }
}
