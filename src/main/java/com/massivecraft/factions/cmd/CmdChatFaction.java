package com.massivecraft.factions.cmd;

import com.massivecraft.factions.ChatChannel;

/**
 * Sends a message only to the sender's faction, or selects it as active.
 *
 * @author MarcDoesntExists
 */
public final class CmdChatFaction extends CmdChatChannel {

    public CmdChatFaction() {
        super(ChatChannel.FACTION, "faction", "f");
    }
}
