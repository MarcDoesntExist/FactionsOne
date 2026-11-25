package com.massivecraft.factions.cmd;

import com.massivecraft.factions.ChatChannel;
import com.massivecraft.factions.ChatChannelService;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

import java.util.Locale;

/**
 * Shared behavior for the faction and ally chat commands.
 *
 * @author MarcDoesntExists
 */
abstract class CmdChatChannel extends FCommand {

    protected final ChatChannel channel;

    protected CmdChatChannel(ChatChannel channel, String... aliases) {
        super();
        this.channel = channel;
        for (String alias : aliases) {
            this.aliases.add(alias);
        }
        this.optionalArgs.put("message", "");
        this.errorOnToManyArgs = false;
        this.permission = Permission.CHAT.node;
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        if (this.channel == ChatChannel.ALLY && !FPerm.CHAT_ALLY.has(this.fme, this.myFaction, true)) {
            return;
        }
        String message = String.join(" ", this.args).trim();
        if (message.isEmpty()) {
            this.fme.setChatChannel(this.channel);
            msg(Lang.chatChannelSet, this.channel.name().toLowerCase(Locale.ROOT));
            return;
        }
        int recipients = ChatChannelService.send(this.fme, this.channel, message);
        if (this.channel == ChatChannel.ALLY && recipients == 0) {
            msg(Lang.chatNoAllies);
        }
    }
}
