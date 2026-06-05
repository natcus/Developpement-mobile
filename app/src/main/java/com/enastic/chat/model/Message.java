package com.enastic.chat.model;

import com.google.firebase.firestore.PropertyName;

/**
 * Modèle représentant un message dans une conversation.
 * Correspond à la structure Firestore :
 * conversations/{conversationId}/messages/{messageId}
 */
public class Message {

    private String messageId;  // Identifiant unique du message (facultatif, peut être l'ID Firestore)
    private String senderId;   // UID de l'utilisateur expéditeur
    private String text;       // Contenu textuel du message
    private String imageUrl;   // Optionnel: Image attachée
    private long timestamp;    // Horodatage en millisecondes (epoch)
    
    @PropertyName("read")
    private boolean isRead;    // Message lu ou non
    
    @PropertyName("delivered")
    private boolean isDelivered; // Message reçu par l'autre appareil
    
    private String fileBase64;
    private String fileMimeType;
    private String fileUrl;     // URL vers Firebase Storage
    private String fileName;
    private java.util.Map<String, String> reactions; // Map de userId -> emoji (ex: "uid123" -> "❤️")
    private java.util.Map<String, Long> readBy;      // Map de userId -> timestamp (quand l'utilisateur a lu le message)
    
    private String replyToId;   // ID du message auquel on répond
    private String replyToText; // Aperçu du texte du message répondu
    
    private boolean isAudio;    // Indique si c'est un message vocal
    private int audioDuration;  // Durée en secondes
    
    private boolean syncing;    // Transient field: true si le message n'est pas encore synchronisé (hors-ligne)
    private String senderProfileUrl; // URL de la photo de profil de l'expéditeur (pour l'avatar dans les bulles reçues)


    // Constructeur vide requis par Firestore pour la désérialisation automatique

    public Message() {}

    // Constructeur utilisé lors de l'envoi d'un nouveau message
    public Message(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters et Setters

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("read")
    public boolean isRead() {
        return isRead;
    }

    @PropertyName("read")
    public void setRead(boolean read) {
        isRead = read;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileBase64() {
        return fileBase64;
    }

    public void setFileBase64(String fileBase64) {
        this.fileBase64 = fileBase64;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public void setFileMimeType(String fileMimeType) {
        this.fileMimeType = fileMimeType;
    }

    @PropertyName("delivered")
    public boolean isDelivered() {
        return isDelivered;
    }

    @PropertyName("delivered")
    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public java.util.Map<String, String> getReactions() {
        return reactions;
    }

    public void setReactions(java.util.Map<String, String> reactions) {
        this.reactions = reactions;
    }

    public java.util.Map<String, Long> getReadBy() {
        return readBy;
    }

    public void setReadBy(java.util.Map<String, Long> readBy) {
        this.readBy = readBy;
    }

    public String getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }

    public String getReplyToText() {
        return replyToText;
    }

    public void setReplyToText(String replyToText) {
        this.replyToText = replyToText;
    }

    public boolean isAudio() {
        return isAudio;
    }

    public void setAudio(boolean audio) {
        isAudio = audio;
    }

    public int getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(int audioDuration) {
        this.audioDuration = audioDuration;
    }

    @com.google.firebase.firestore.Exclude
    public boolean isSyncing() {
        return syncing;
    }

    @com.google.firebase.firestore.Exclude
    public void setSyncing(boolean syncing) {
        this.syncing = syncing;
    }

    public String getSenderProfileUrl() {
        return senderProfileUrl;
    }

    public void setSenderProfileUrl(String senderProfileUrl) {
        this.senderProfileUrl = senderProfileUrl;
    }
}

