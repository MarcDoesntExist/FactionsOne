package com.massivecraft.factions;

/**
 * Decides whether a faction may receive a channel message. Kept free of Bukkit
 * types so the ally/truce boundary is covered by a plain unit test.
 *
 * @author MarcDoesntExists
 */
public final class ChatChannelResolver {

    private ChatChannelResolver() {
    }

    public static boolean accepts(ChatChannel channel, String senderFactionId,
                                  String recipientFactionId, String relation) {
        if (channel == ChatChannel.PUBLIC) {
            return true;
        }
        if (channel == null || senderFactionId == null || recipientFactionId == null) {
            return false;
        }
        if (senderFactionId.equals(recipientFactionId)) {
            return true;
        }
        return channel == ChatChannel.ALLY && "ALLY".equalsIgnoreCase(relation);
    }
}
