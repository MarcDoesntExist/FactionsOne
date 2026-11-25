package com.massivecraft.factions.cmd;

import com.massivecraft.factions.ChatChannel;

/**
 * Sends a message to the sender's faction and its direct allies.
 *
 * @author MarcDoesntExists
 */
public final class CmdChatAlly extends CmdChatChannel {

    public CmdChatAlly() {
        super(ChatChannel.ALLY, "ally", "a");
    }
}
