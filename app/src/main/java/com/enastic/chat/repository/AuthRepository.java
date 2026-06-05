package com.enastic.chat.repository;

import androidx.lifecycle.MutableLiveData;

import com.enastic.chat.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository gérant toutes les opérations d'authentification Firebase.
 * Responsabilités : inscription, connexion et déconnexion.
 * Communique les résultats via MutableLiveData observé par AuthViewModel.
 */
public class AuthRepository {

    // Instance unique de Firebase Auth
    private final FirebaseAuth firebaseAuth;

    // Instance unique de Firestore pour sauvegarder le profil utilisateur
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Retourne l'utilisateur Firebase actuellement connecté, ou null.
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Inscrit un nouvel utilisateur avec email / mot de passe.
     * En cas de succès, crée aussi son profil dans Firestore (collection "users").
     *
     * @param name         Nom d'affichage saisi par l'utilisateur
     * @param email        Adresse email
     * @param password     Mot de passe
     * @param successLive  LiveData notifié avec true si l'inscription réussit
     * @param errorLive    LiveData notifié avec le message d'erreur si échec
     */
    public void register(String name, String email, String password, String phoneNumber,
                         MutableLiveData<Boolean> successLive,
                         MutableLiveData<String> errorLive) {

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Récupère l'utilisateur Firebase nouvellement créé
                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser != null) {
                        // Construit l'objet User à sauvegarder dans Firestore
                        User newUser = new User(firebaseUser.getUid(), name, email, phoneNumber);

                        // Sauvegarde dans la collection "users" avec l'UID comme clé du document
                        firestore.collection("users")
                                .document(firebaseUser.getUid())
                                .set(newUser)
                                .addOnSuccessListener(aVoid -> successLive.setValue(true))
                                .addOnFailureListener(e -> errorLive.setValue(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> errorLive.setValue(e.getMessage()));
    }

    /**
     * Connecte un utilisateur existant avec email / mot de passe.
     *
     * @param email        Adresse email
     * @param password     Mot de passe
     * @param successLive  LiveData notifié avec true si la connexion réussit
     * @param errorLive    LiveData notifié avec le message d'erreur si échec
     */
    public void login(String email, String password,
                      MutableLiveData<Boolean> successLive,
                      MutableLiveData<String> errorLive) {

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> successLive.setValue(true))
                .addOnFailureListener(e -> errorLive.setValue(e.getMessage()));
    }

    /**
     * Déconnecte l'utilisateur actuellement connecté.
     */
    public void logout() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("isOnline", false);
            updates.put("lastSeen", System.currentTimeMillis());
            firestore.collection("users").document(uid).update(updates)
                    .addOnCompleteListener(task -> firebaseAuth.signOut());
        } else {
            firebaseAuth.signOut();
        }
    }

    /**
     * Met à jour le token FCM de l'utilisateur dans Firestore.
     */
    public void updateFcmToken(String userId, String token) {
        if (userId != null && token != null) {
            firestore.collection("users").document(userId)
                    .update("fcmToken", token);
        }
    }
}
