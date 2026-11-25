package com.massivecraft.factions;

import com.massivecraft.factions.listeners.FactionsChatListener;

/**
 * Sends private channel messages. Recipients are filtered by
 * {@link ChatChannelResolver}; spies are handled separately by the listener.
 *
 * @author MarcDoesntExists
 */
public final class ChatChannelService {

    private ChatChannelService() {
    }

    public static int send(FPlayer sender, ChatChannel channel, String message) {
        if (sender == null || channel == null || channel == ChatChannel.PUBLIC
                || sender.getFaction() == null || message == null || message.isBlank()) {
            return 0;
        }
        Faction faction = sender.getFaction();
        String format = channel == ChatChannel.ALLY ? Conf.chatAllyFormat : Conf.chatFactionFormat;
        int recipients = 0;
        for (FPlayer other : FPlayers.i.getOnline()) {
            Faction otherFaction = other.getFaction();
            String relation = otherFaction == null
                    ? null
                    : faction.getRelationTo(otherFaction).name();
            boolean recipient = ChatChannelResolver.accepts(channel, faction.getId(),
                    otherFaction == null ? null : otherFaction.getId(), relation);
            if (recipient) {
                other.msg(format, sender.getName(), message);
                recipients++;
            }
        }
        FactionsChatListener.spy(sender, channel, message);
        faction.addLog(sender.getName(),
                channel == ChatChannel.ALLY ? "ALLY_CHAT" : "FACTION_CHAT", message);
        return recipients;
    }
}
