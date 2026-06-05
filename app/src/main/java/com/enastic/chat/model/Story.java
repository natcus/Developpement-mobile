package com.enastic.chat.model;

public class Story {
    private String id;
    private String userId;
    private String userName;
    private String userProfileUrl;
    private String imageUrl;
    private long timestamp;

    public Story() {
        // Constructeur vide requis par Firebase
    }

    public Story(String id, String userId, String userName, String userProfileUrl, String imageUrl, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userProfileUrl = userProfileUrl;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserProfileUrl() { return userProfileUrl; }
    public void setUserProfileUrl(String userProfileUrl) { this.userProfileUrl = userProfileUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
