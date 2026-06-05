package com.enastic.chat.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.enastic.chat.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel pour l'authentification.
 * Fait le lien entre AuthRepository et les vues (LoginActivity, RegisterActivity).
 * Survit aux changements de configuration (rotation d'écran).
 */
public class AuthViewModel extends ViewModel {

    // Repository qui gère toute la logique Firebase Auth
    private final AuthRepository authRepository;

    // LiveData observés par les Activities pour réagir aux résultats
    public MutableLiveData<Boolean> authSuccess = new MutableLiveData<>();
    public MutableLiveData<String> authError = new MutableLiveData<>();

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    /**
     * Retourne l'utilisateur Firebase actuellement connecté (null si déconnecté).
     * Utilisé pour vérifier l'état de connexion au démarrage.
     */
    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    /**
     * Lance le processus d'inscription via le repository.
     *
     * @param name     Nom de l'utilisateur
     * @param email    Adresse email
     * @param password Mot de passe
     */
    public void register(String name, String email, String password, String phoneNumber) {
        authRepository.register(name, email, password, phoneNumber, authSuccess, authError);
    }

    /**
     * Lance le processus de connexion via le repository.
     *
     * @param email    Adresse email
     * @param password Mot de passe
     */
    public void login(String email, String password) {
        authRepository.login(email, password, authSuccess, authError);
    }

    /**
     * Déconnecte l'utilisateur courant.
     */
    public void logout() {
        authRepository.logout();
    }

    /**
     * Met à jour le token de notification.
     */
    public void updateFcmToken(String token) {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            authRepository.updateFcmToken(currentUser.getUid(), token);
        }
    }
}
