package com.enastic.chat.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.enastic.chat.model.User;
import com.enastic.chat.repository.ChatRepository;
import com.enastic.chat.repository.UserRepository;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

public class MainViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private ListenerRegistration deliveryListener;
    
    public final MutableLiveData<List<User>> usersLive = new MutableLiveData<>();
    public final MutableLiveData<User> searchResultLive = new MutableLiveData<>();
    public final MutableLiveData<Boolean> addContactSuccessLive = new MutableLiveData<>();
    public final MutableLiveData<String> errorLive = new MutableLiveData<>();
    public final MutableLiveData<List<com.enastic.chat.model.Conversation>> groupsLive = new MutableLiveData<>();

    public MainViewModel() {
        userRepository = new UserRepository();
        chatRepository = new ChatRepository();
    }

    public void initGlobalStatus(String currentUserId) {
        if (deliveryListener != null) deliveryListener.remove();
        deliveryListener = chatRepository.listenForGlobalDelivery(currentUserId);
    }

    /**
     * Charge TOUS les utilisateurs.
     */
    public void loadUsers() {
        userRepository.getUsers(usersLive, errorLive);
    }

    /**
     * Recherche un utilisateur par email ou par téléphone de manière intelligente.
     */
    public void searchUser(String query) {
        if (query.contains("@")) {
            userRepository.searchUserByEmail(query, searchResultLive, errorLive);
        } else {
            userRepository.searchUserByPhone(query, searchResultLive, errorLive);
        }
    }

    public void searchUserByPhone(String phone) {
        userRepository.searchUserByPhone(phone, searchResultLive, errorLive);
    }

    /**
     * Ajoute un contact.
     */
    public void addContact(String currentUserId, User contact) {
        userRepository.addContact(currentUserId, contact, addContactSuccessLive);
    }

    public void loadGroups(String userId) {
        userRepository.getGroups(userId, groupsLive, errorLive);
    }

    public void setOnlineStatus(String userId, boolean isOnline) {
        userRepository.setOnlineStatus(userId, isOnline);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (deliveryListener != null) {
            deliveryListener.remove();
            deliveryListener = null;
        }
    }
}
