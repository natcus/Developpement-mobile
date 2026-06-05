package com.enastic.chat.repository;

import androidx.lifecycle.MutableLiveData;
import com.enastic.chat.model.Message;
import com.enastic.chat.model.User;
import com.enastic.chat.utils.Base64Utils;
import com.enastic.chat.utils.EncryptionUtils;
import com.enastic.chat.utils.ImageCompressor;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository gérant toutes les opérations de messagerie via Firestore.
 * Utilise un ExecutorService pour décharger le Thread Principal.
 */
public class ChatRepository {

    private final FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<String, String> profileCache = new HashMap<>(); // Cache userId -> profileImageUrl


    public ChatRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void sendMessage(String conversationId, Message message, MutableLiveData<String> errorLive) {
        executor.execute(() -> {
            if (message.getText() != null && !message.getText().isEmpty()) {
                message.setText(EncryptionUtils.encrypt(message.getText()));
            }
            firestore.collection("conversations").document(conversationId).collection("messages").add(message)
                    .addOnFailureListener(e -> errorLive.postValue(e.getMessage()));
        });
    }

    public void sendImageMessage(android.content.Context context, String conversationId, String senderId, android.net.Uri imageUri, MutableLiveData<String> errorLive) {
        executor.execute(() -> {
            byte[] imageBytes = ImageCompressor.compressUriToBytes(context, imageUri);
            if (imageBytes == null) {
                errorLive.postValue("Erreur compression image");
                return;
            }
            String fileName = "image_" + System.currentTimeMillis() + ".jpg";
            com.enastic.chat.utils.CloudinaryUploader.uploadBytes(imageBytes, fileName, "image", new com.enastic.chat.utils.CloudinaryUploader.UploadCallback() {
                @Override
                public void onSuccess(String secureUrl) {
                    Message message = new Message(senderId, "", System.currentTimeMillis());
                    message.setImageUrl(secureUrl);
                    firestore.collection("conversations").document(conversationId).collection("messages").add(message);
                }

                @Override
                public void onError(String error) {
                    errorLive.postValue(error);
                }
            });
        });
    }

    public void sendVoiceMessage(String conversationId, String senderId, String filePath, int duration, MutableLiveData<String> errorLive) {
        executor.execute(() -> {
            java.io.File audioFile = new java.io.File(filePath);
            if (!audioFile.exists()) {
                errorLive.postValue("Fichier audio introuvable");
                return;
            }
            com.enastic.chat.utils.CloudinaryUploader.uploadFile(audioFile, "video", new com.enastic.chat.utils.CloudinaryUploader.UploadCallback() {
                @Override
                public void onSuccess(String secureUrl) {
                    Message message = new Message(senderId, "", System.currentTimeMillis());
                    message.setFileUrl(secureUrl);
                    message.setAudio(true);
                    message.setAudioDuration(duration);
                    firestore.collection("conversations").document(conversationId).collection("messages").add(message);
                }

                @Override
                public void onError(String error) {
                    errorLive.postValue(error);
                }
            });
        });
    }

    public void sendFileMessage(android.content.Context context, String conversationId, String senderId, android.net.Uri fileUri, String fileName, String mimeType, MutableLiveData<String> errorLive) {
        executor.execute(() -> {
            try {
                // Copier l'URI vers un fichier temporaire pour ne pas saturer la RAM avec de gros fichiers
                java.io.File tempFile = new java.io.File(context.getCacheDir(), fileName);
                java.io.InputStream is = context.getContentResolver().openInputStream(fileUri);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();

                com.enastic.chat.utils.CloudinaryUploader.uploadFile(tempFile, "auto", new com.enastic.chat.utils.CloudinaryUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String secureUrl) {
                        tempFile.delete(); // Nettoyage
                        Message message = new Message(senderId, "", System.currentTimeMillis());
                        message.setFileUrl(secureUrl);
                        message.setFileName(fileName);
                        message.setFileMimeType(mimeType);
                        firestore.collection("conversations").document(conversationId).collection("messages").add(message);
                    }

                    @Override
                    public void onError(String error) {
                        tempFile.delete(); // Nettoyage
                        errorLive.postValue(error);
                    }
                });
            } catch (Exception e) {
                errorLive.postValue(e.getMessage());
            }
        });
    }

    public void sendExistingImageMessage(String conversationId, String senderId, String imageUrl) {
        Message message = new Message(senderId, "", System.currentTimeMillis());
        message.setImageUrl(imageUrl);
        firestore.collection("conversations").document(conversationId).collection("messages").add(message);
    }

    public void sendExistingFileMessage(String conversationId, String senderId, String fileUrl, String fileName, String mimeType) {
        Message message = new Message(senderId, "", System.currentTimeMillis());
        message.setFileUrl(fileUrl);
        message.setFileName(fileName);
        message.setFileMimeType(mimeType);
        firestore.collection("conversations").document(conversationId).collection("messages").add(message);
    }

    public void listenToMessages(String conversationId, int limit, MutableLiveData<List<Message>> messagesLive, MutableLiveData<String> errorLive) {
        detachListener();
        listenerRegistration = firestore.collection("conversations").document(conversationId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { errorLive.postValue(error.getMessage()); return; }
                    if (snapshot != null) {
                        executor.execute(() -> {
                            List<Message> messages = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : snapshot) {
                                Message msg = doc.toObject(Message.class);
                                msg.setMessageId(doc.getId());
                                msg.setSyncing(doc.getMetadata().hasPendingWrites());
                                if (msg.getText() != null && !msg.getText().isEmpty()) {
                                    msg.setText(EncryptionUtils.decrypt(msg.getText()));
                                }
                                // Injecter la photo de profil depuis le cache
                                String senderId = msg.getSenderId();
                                if (senderId != null) {
                                    if (profileCache.containsKey(senderId)) {
                                        msg.setSenderProfileUrl(profileCache.get(senderId));
                                        messages.add(0, msg);
                                    } else {
                                        // Charger le profil et mettre en cache
                                        final Message finalMsg = msg;
                                        firestore.collection("users").document(senderId).get()
                                                .addOnSuccessListener(userDoc -> {
                                                    String url = userDoc.getString("profileImageUrl");
                                                    profileCache.put(senderId, url != null ? url : "");
                                                    finalMsg.setSenderProfileUrl(url != null ? url : "");
                                                });
                                        messages.add(0, msg);
                                    }
                                } else {
                                    messages.add(0, msg);
                                }
                            }
                            messagesLive.postValue(messages);
                        });
                    }
                });
    }

    public void loadMoreMessages(String conversationId, long oldestTimestamp, int limit, MutableLiveData<List<Message>> moreMessagesLive) {
        firestore.collection("conversations").document(conversationId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING).startAfter(oldestTimestamp).limit(limit).get()
                .addOnSuccessListener(snapshot -> {
                    executor.execute(() -> {
                        List<Message> older = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Message msg = doc.toObject(Message.class);
                            msg.setMessageId(doc.getId());
                            if (msg.getText() != null && !msg.getText().isEmpty()) {
                                msg.setText(EncryptionUtils.decrypt(msg.getText()));
                            }
                            older.add(0, msg);
                        }
                        moreMessagesLive.postValue(older);
                    });
                });
    }

    public ListenerRegistration listenForGlobalDelivery(String currentUserId) {
        return firestore.collectionGroup("messages").whereEqualTo("delivered", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                Message msg = dc.getDocument().toObject(Message.class);
                                if (msg != null && msg.getSenderId() != null && !msg.getSenderId().equals(currentUserId)) {
                                    dc.getDocument().getReference().update("delivered", true);
                                }
                            }
                        }
                    }
                });
    }

    public ListenerRegistration listenToTypingStatus(String conversationId, String currentUserId, MutableLiveData<Boolean> isTypingLive) {
        return firestore.collection("conversations").document(conversationId).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                Map<String, Object> typingMap = (Map<String, Object>) snapshot.get("typing");
                if (typingMap != null) {
                    for (String uid : typingMap.keySet()) {
                        if (!uid.equals(currentUserId)) isTypingLive.postValue((Boolean) typingMap.get(uid));
                    }
                }
            }
        });
    }

    public void setTypingStatus(String conversationId, String userId, boolean isTyping) {
        firestore.collection("conversations").document(conversationId).update("typing." + userId, isTyping)
                .addOnFailureListener(e -> {
                    Map<String, Object> data = new HashMap<>();
                    Map<String, Boolean> typing = new HashMap<>();
                    typing.put(userId, isTyping);
                    data.put("typing", typing);
                    firestore.collection("conversations").document(conversationId).set(data, SetOptions.merge());
                });
    }

    public void markMessageAsRead(String conversationId, String messageId, String userId) {
        firestore.collection("conversations").document(conversationId).collection("messages").document(messageId)
                .update("readBy." + userId, System.currentTimeMillis(), "read", true);
    }

    public void markMessageAsDelivered(String conversationId, String messageId) {
        firestore.collection("conversations").document(conversationId).collection("messages").document(messageId).update("delivered", true);
    }

    public void deleteMessage(String conversationId, String messageId) {
        firestore.collection("conversations").document(conversationId).collection("messages").document(messageId).delete();
    }

    public void updateReaction(String conversationId, String messageId, String userId, String emoji) {
        firestore.collection("conversations").document(conversationId).collection("messages").document(messageId).update("reactions." + userId, emoji);
    }

    public void createGroup(String groupName, List<String> members, MutableLiveData<String> successLive, MutableLiveData<String> errorLive) {
        String groupId = "GROUP_" + System.currentTimeMillis();
        com.enastic.chat.model.Conversation group = new com.enastic.chat.model.Conversation();
        group.setConversationId(groupId);
        group.setName(groupName);
        group.setMembers(members);
        group.setCreatorId(members.get(0)); // Le premier membre est le créateur
        java.util.List<String> admins = new java.util.ArrayList<>();
        admins.add(members.get(0));
        group.setAdmins(admins);
        group.setType("group");
        group.setLastMessageTimestamp(System.currentTimeMillis());
        group.setLastMessageText("Groupe créé");
        firestore.collection("conversations").document(groupId).set(group).addOnSuccessListener(aVoid -> successLive.postValue(groupId));
    }

    public void saveP2PMessage(String conversationId, Message message) {
        firestore.collection("conversations").document(conversationId).collection("messages").add(message);
    }

    public ListenerRegistration listenToUserPresence(String userId, MutableLiveData<User> userLive) {
        return firestore.collection("users").document(userId).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                User user = snapshot.toObject(User.class);
                if (user != null) {
                    user.setUid(snapshot.getId());
                    userLive.postValue(user);
                }
            }
        });
    }

    public void detachListener() {
        if (listenerRegistration != null) { listenerRegistration.remove(); listenerRegistration = null; }
    }
}
