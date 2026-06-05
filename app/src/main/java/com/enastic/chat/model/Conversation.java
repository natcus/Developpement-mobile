package com.enastic.chat.model;

import java.util.List;
import java.util.Map;

/**
 * Modèle représentant une conversation (privée ou groupe).
 */
public class Conversation {
    private String conversationId;
    private String type; // "private" ou "group"
    private String name; // Uniquement pour le type "group"
    private String imageUrl; // Uniquement pour le type "group"
    private List<String> members; // Liste des UIDs des membres
    private List<String> admins;  // Liste des UIDs des administrateurs
    private String creatorId;     // UID du créateur du groupe
    private Map<String, Boolean> typing; // État de frappe
    private long lastMessageTimestamp;
    private String lastMessageText;

    public Conversation() {}

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public Map<String, Boolean> getTyping() { return typing; }
    public void setTyping(Map<String, Boolean> typing) { this.typing = typing; }

    public List<String> getAdmins() { return admins; }
    public void setAdmins(List<String> admins) { this.admins = admins; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getLastMessageText() { return lastMessageText; }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }
}
