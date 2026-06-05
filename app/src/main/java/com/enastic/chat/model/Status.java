package com.enastic.chat.model;

/**
 * Modèle pour les statuts / stories.
 * Un statut disparaît après 24 heures (géré par Firestore TTL ou logique applicative).
 */
public class Status {
    private String statusId;
    private String userId;
    private String userName;
    private String userProfileUrl;
    private String imageUrl; // Base64 ou URL
    private String caption;
    private long timestamp;

    public Status() {}

    public Status(String userId, String userName, String userProfileUrl, String imageUrl, String caption) {
        this.userId = userId;
        this.userName = userName;
        this.userProfileUrl = userProfileUrl;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters et Setters
    public String getStatusId() { return statusId; }
    public void setStatusId(String statusId) { this.statusId = statusId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserProfileUrl() { return userProfileUrl; }
    public void setUserProfileUrl(String userProfileUrl) { this.userProfileUrl = userProfileUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
