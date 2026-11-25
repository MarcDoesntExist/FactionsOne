package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

/**
 * Parent for faction chat channels.
 *
 * @author MarcDoesntExists
 */
public final class CmdChat extends FCommand {

    public final CmdChatPublic cmdChatPublic = new CmdChatPublic();
    public final CmdChatFaction cmdChatFaction = new CmdChatFaction();
    public final CmdChatAlly cmdChatAlly = new CmdChatAlly();

    public CmdChat() {
        super();
        this.aliases.add("chat");
        this.permission = Permission.CHAT.node;
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
        this.addSubCommand(this.cmdChatPublic);
        this.addSubCommand(this.cmdChatFaction);
        this.addSubCommand(this.cmdChatAlly);
    }

    @Override
    public void perform() {
        msg(Lang.chatUsage);
    }
}
