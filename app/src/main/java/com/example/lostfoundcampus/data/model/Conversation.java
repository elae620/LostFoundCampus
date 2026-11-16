package com.example.lostfoundcampus.data.model;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Conversation {

    public final String id;
    /** Les deux participants : l'utilisateur courant et l'autre personne. */
    public final List<String> participantIds = new ArrayList<>();
    @Nullable
    public final String relatedAnnouncementId;
    public final List<Message> messages = new ArrayList<>();
    public long lastMessageAt;

    public Conversation(String id, String me, String other, @Nullable String relatedAnnouncementId, long lastMessageAt) {
        this.id = id;
        participantIds.add(me);
        participantIds.add(other);
        this.relatedAnnouncementId = relatedAnnouncementId;
        this.lastMessageAt = lastMessageAt;
    }

    public String otherParticipant(String me) {
        for (String id : participantIds) {
            if (!id.equals(me)) return id;
        }
        return me;
    }

    @Nullable
    public Message lastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public boolean hasUnreadFor(String me) {
        for (Message m : messages) {
            if (!m.senderId.equals(me) && !m.isRead) return true;
        }
        return false;
    }
}
