package com.enastic.chat.model;

/**
 * Modèle représentant un utilisateur de l'application.
 * Correspond à la structure Firestore : users/{userId}
 */
public class User {

    private String uid;      // Identifiant unique Firebase Auth
    private String name;     // Nom d'affichage de l'utilisateur
    private String email;    // Adresse email
    private String profileImageUrl; // URL de la photo de profil
    private boolean isOnline; // Statut en ligne
    private String fcmToken;  // Token pour les notifications Push
    private long lastSeen;    // Timestamp du dernier accès
    private String phoneNumber; // Numéro de téléphone


    // Constructeur vide requis par Firestore pour la désérialisation automatique
    public User() {}

    // Constructeur complet pour créer un utilisateur lors de l'inscription
    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
    }

    public User(String uid, String name, String email, String phoneNumber) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getters et Setters

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

