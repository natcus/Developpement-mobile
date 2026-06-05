package com.enastic.chat.repository;

import androidx.lifecycle.MutableLiveData;
import com.enastic.chat.model.User;
import com.enastic.chat.model.Conversation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository gérant les utilisateurs et les contacts.
 */
public class UserRepository {
    private final FirebaseFirestore firestore;

    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Récupère TOUS les utilisateurs inscrits.
     */
    public void getUsers(MutableLiveData<List<User>> usersLive, MutableLiveData<String> errorLive) {
        firestore.collection("users")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { errorLive.setValue(e.getMessage()); return; }
                    if (snapshot != null) {
                        List<User> userList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : snapshot) {
                            User user = document.toObject(User.class);
                            user.setUid(document.getId());
                            userList.add(user);
                        }
                        usersLive.setValue(userList);
                    }
                });
    }

    /**
     * Recherche un utilisateur par email.
     */
    public void searchUserByEmail(String email, MutableLiveData<User> resultLive, MutableLiveData<String> errorLive) {
        firestore.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        User user = snapshot.getDocuments().get(0).toObject(User.class);
                        user.setUid(snapshot.getDocuments().get(0).getId());
                        resultLive.setValue(user);
                    } else {
                        resultLive.setValue(null);
                    }
                })
                .addOnFailureListener(e -> errorLive.setValue(e.getMessage()));
    }

    /**
     * Recherche un utilisateur par numéro de téléphone.
     */
    public void searchUserByPhone(String phone, MutableLiveData<User> resultLive, MutableLiveData<String> errorLive) {
        firestore.collection("users").whereEqualTo("phoneNumber", phone).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        User user = snapshot.getDocuments().get(0).toObject(User.class);
                        user.setUid(snapshot.getDocuments().get(0).getId());
                        resultLive.setValue(user);
                    } else {
                        resultLive.setValue(null);
                    }
                })
                .addOnFailureListener(e -> errorLive.setValue(e.getMessage()));
    }

    /**
     * Ajoute un utilisateur à la liste de contacts (optionnel, pour mise en avant).
     */
    public void addContact(String currentUserId, User contact, MutableLiveData<Boolean> successLive) {
        Map<String, Object> contactData = new HashMap<>();
        contactData.put("addedAt", System.currentTimeMillis());
        firestore.collection("users").document(currentUserId).collection("contacts").document(contact.getUid())
                .set(contactData)
                .addOnSuccessListener(aVoid -> successLive.setValue(true))
                .addOnFailureListener(e -> successLive.setValue(false));
    }

    public void setOnlineStatus(String userId, boolean isOnline) {
        if (userId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOnline", isOnline);
        if (!isOnline) updates.put("lastSeen", System.currentTimeMillis());
        firestore.collection("users").document(userId).update(updates);
    }

    public void getGroups(String userId, MutableLiveData<List<Conversation>> groupsLive, MutableLiveData<String> errorLive) {
        firestore.collection("conversations")
                .whereEqualTo("type", "group")
                .whereArrayContains("members", userId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) { errorLive.setValue(e.getMessage()); return; }
                    if (querySnapshot != null) {
                        List<Conversation> groupList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Conversation conv = document.toObject(Conversation.class);
                            conv.setConversationId(document.getId());
                            groupList.add(conv);
                        }
                        groupsLive.setValue(groupList);
                    }
                });
    }
}
