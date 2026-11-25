package com.massivecraft.factions.cmd;

import com.massivecraft.factions.ChatChannel;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.util.Locale;

/**
 * Selects public chat, or sends one message through the normal vanilla chat
 * path. Public messages never receive a Factions prefix.
 *
 * @author MarcDoesntExists
 */
public final class CmdChatPublic extends FCommand {

    public CmdChatPublic() {
        super();
        this.aliases.add("public");
        this.aliases.add("p");
        this.optionalArgs.put("message", "");
        this.errorOnToManyArgs = false;
        this.permission = Permission.CHAT.node;
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        String message = String.join(" ", this.args).trim();
        if (message.isEmpty()) {
            this.fme.setChatChannel(ChatChannel.PUBLIC);
            msg(Lang.chatChannelSet, ChatChannel.PUBLIC.name().toLowerCase(Locale.ROOT));
            return;
        }
        this.fme.getPlayer().chat(message);
    }
}
