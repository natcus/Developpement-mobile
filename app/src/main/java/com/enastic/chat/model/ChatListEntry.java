package com.enastic.chat.model;

/**
 * Objet de données générique pour l'affichage dans la liste principale.
 */
public class ChatListEntry {
    private String id;
    private String name;
    private String subText;
    private String imageUrl;
    private boolean isGroup;
    private boolean isOnline;
    private long timestamp;

    public ChatListEntry(String id, String name, String subText, String imageUrl, boolean isGroup, boolean isOnline) {
        this.id = id;
        this.name = name;
        this.subText = subText;
        this.imageUrl = imageUrl;
        this.isGroup = isGroup;
        this.isOnline = isOnline;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSubText() { return subText; }
    public String getImageUrl() { return imageUrl; }
    public boolean isGroup() { return isGroup; }
    public boolean isOnline() { return isOnline; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setSubText(String subText) { this.subText = subText; }
}
