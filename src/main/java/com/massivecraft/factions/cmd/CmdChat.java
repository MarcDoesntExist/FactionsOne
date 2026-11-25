package com.massivecraft.factions.cmd;

/**
 * Parent for faction chat channels.
 *
 * @author MarcDoesntExists
 */
public final class CmdChat extends FCommand {

    public final CmdChatAlly cmdChatAlly = new CmdChatAlly();

    public CmdChat() {
        super();
        this.aliases.add("chat");
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
        this.addSubCommand(this.cmdChatAlly);
    }

    @Override
    public void perform() {
        msg("<i>Use /f chat ally <message> to talk with your allies.");
    }
}
