package com.enastic.chat.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.enastic.chat.model.Message;
import com.enastic.chat.model.User;
import com.enastic.chat.repository.ChatRepository;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

/**
 * ViewModel pour ChatActivity.
 * Gère la logique de la discussion (envoi, réception, statut d'écriture).
 */
public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository;
    
    public final MutableLiveData<List<Message>> messagesLive = new MutableLiveData<>();
    public final MutableLiveData<String> errorLive = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isOtherTypingLive = new MutableLiveData<>();
    public final MutableLiveData<List<Message>> moreMessagesLive = new MutableLiveData<>();
    public final MutableLiveData<User> otherUserPresenceLive = new MutableLiveData<>();

    private ListenerRegistration typingListener;
    private ListenerRegistration presenceListener;

    public ChatViewModel() {
        chatRepository = new ChatRepository();
    }

    /**
     * Initialise la discussion en écoutant les messages et le statut d'écriture.
     */
    public void initChat(String conversationId, String currentUserId) {
        listenToMessages(conversationId, 50); // Charge les 50 derniers messages
        listenToTypingStatus(conversationId, currentUserId);
    }

    public void listenToMessages(String conversationId, int limit) {
        chatRepository.listenToMessages(conversationId, limit, messagesLive, errorLive);
    }

    public void loadMoreMessages(String conversationId, long oldestTimestamp) {
        chatRepository.loadMoreMessages(conversationId, oldestTimestamp, 20, moreMessagesLive);
    }

    private void listenToTypingStatus(String conversationId, String currentUserId) {
        if (typingListener != null) typingListener.remove();
        typingListener = chatRepository.listenToTypingStatus(conversationId, currentUserId, isOtherTypingLive);
    }

    /**
     * Écoute les changements de présence (en ligne / hors-ligne) de l'autre utilisateur.
     * @param otherUserId L'UID de l'autre participant de la conversation.
     */
    public void listenToUserPresence(String otherUserId) {
        if (presenceListener != null) presenceListener.remove();
        presenceListener = chatRepository.listenToUserPresence(otherUserId, otherUserPresenceLive);
    }

    public void setTypingStatus(String conversationId, String userId, boolean isTyping) {
        chatRepository.setTypingStatus(conversationId, userId, isTyping);
    }

    /**
     * Envoie un message texte (supporte les réponses).
     */
    public void sendMessage(String conversationId, String senderId, String text, String replyToId, String replyToText) {
        Message message = new Message(senderId, text, System.currentTimeMillis());
        if (replyToId != null) {
            message.setReplyToId(replyToId);
            message.setReplyToText(replyToText);
        }
        chatRepository.sendMessage(conversationId, message, errorLive);
    }

    public void sendImageMessage(android.content.Context context, String conversationId, String senderId, android.net.Uri imageUri) {
        chatRepository.sendImageMessage(context, conversationId, senderId, imageUri, errorLive);
    }

    public void sendFileMessage(android.content.Context context, String conversationId, String senderId, android.net.Uri fileUri, String fileName, String mimeType) {
        chatRepository.sendFileMessage(context, conversationId, senderId, fileUri, fileName, mimeType, errorLive);
    }

    public void sendVoiceMessage(android.content.Context context, String conversationId, String senderId, String filePath, int duration) {
        chatRepository.sendVoiceMessage(conversationId, senderId, filePath, duration, errorLive);
    }

    public void sendExistingImageMessage(String conversationId, String senderId, String imageUrl) {
        chatRepository.sendExistingImageMessage(conversationId, senderId, imageUrl);
    }

    public void sendExistingFileMessage(String conversationId, String senderId, String fileUrl, String fileName, String mimeType) {
        chatRepository.sendExistingFileMessage(conversationId, senderId, fileUrl, fileName, mimeType);
    }

    public void markMessageAsRead(String conversationId, String messageId, String userId) {
        chatRepository.markMessageAsRead(conversationId, messageId, userId);
    }

    public void markMessageAsDelivered(String conversationId, String messageId) {
        chatRepository.markMessageAsDelivered(conversationId, messageId);
    }

    public void deleteMessage(String conversationId, String messageId) {
        chatRepository.deleteMessage(conversationId, messageId);
    }

    public void updateReaction(String conversationId, String messageId, String userId, String emoji) {
        chatRepository.updateReaction(conversationId, messageId, userId, emoji);
    }

    public void saveP2PMessage(String conversationId, Message message) {
        chatRepository.saveP2PMessage(conversationId, message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatRepository.detachListener();
        if (typingListener != null) {
            typingListener.remove();
            typingListener = null;
        }
        if (presenceListener != null) {
            presenceListener.remove();
            presenceListener = null;
        }
    }
}
