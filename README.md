# Enastic Chat 💬

Une application de messagerie Android en temps réel complète, moderne et sécurisée.

## 🌟 Fonctionnalités

*   **Messagerie en temps réel** : Discutez instantanément avec vos contacts.
*   **Partage de Médias** : Envoyez des images, des fichiers et des notes vocales en toute fluidité (Hébergé sur Cloudinary).
*   **Stories / Statuts** : Partagez des moments éphémères avec vos amis (photos, textes, vidéos).
*   **Chiffrement des messages** : Vos conversations sont protégées par un chiffrement AES.
*   **Réactions et Accusés de réception** : Réagissez aux messages avec des emojis et voyez quand vos messages sont distribués/lus (✓✓).
*   **Discussions de Groupe** : Créez des groupes et gérez les membres.
*   **Notifications Push** : Ne manquez aucun message grâce à Firebase Cloud Messaging (FCM).
*   **Authentification Sécurisée** : Inscription et connexion via Firebase Auth (Email/Mot de passe) avec support Biométrique (Empreinte digitale).

## 🛠 Technologies et Architecture

*   **Langage** : Java
*   **Architecture** : MVVM (Model-View-ViewModel)
*   **Interface Utilisateur** : XML, Material Design Components, Animations dynamiques
*   **Backend & Base de données** : Firebase Firestore (NoSQL, Realtime)
*   **Authentification** : Firebase Auth
*   **Stockage de fichiers** : Cloudinary (pour un stockage optimisé et illimité)
*   **Gestion des images** : Glide
*   **Notifications** : Firebase Cloud Messaging (FCM)

## 🚀 Installation et Configuration

Pour exécuter ce projet localement, vous devez configurer vos propres clés Firebase et Cloudinary.

1. **Cloner le dépôt**
   ```bash
   git clone https://github.com/natcus/Developpement-mobile.git
   ```

2. **Configuration Firebase**
   * Allez sur la [Console Firebase](https://console.firebase.google.com/) et créez un nouveau projet.
   * Ajoutez une application Android avec le package `com.enastic.chat`.
   * Téléchargez le fichier `google-services.json` et placez-le dans le dossier `app/` de votre projet Android Studio. *(Ce fichier est ignoré par Git pour des raisons de sécurité).*
   * Dans Firebase, activez **Firestore Database** et l'**Authentification (Email/Mot de passe)**.

3. **Configuration Cloudinary**
   * L'application utilise Cloudinary pour gérer les gros fichiers (images, audios) afin de préserver les quotas Firebase.
   * Remplacez les identifiants Cloudinary dans le fichier `CloudinaryUploader.java` avec vos propres informations (Cloud Name, API Key, Upload Preset).

4. **Compiler et Exécuter**
   * Ouvrez le projet dans **Android Studio**.
   * Laissez Gradle synchroniser les dépendances.
   * Cliquez sur le bouton "Run" (▶️) pour lancer l'application sur un émulateur ou votre appareil physique.

## 🔐 Sécurité et Confidentialité
* Le fichier `google-services.json` et les clés d'API sont exclus du dépôt via `.gitignore`.
* Les mots de passe ne sont jamais stockés en clair.
* Les communications sont chiffrées de bout en bout pour garantir la confidentialité des échanges (AES).

---
*Développé avec ❤️ pour Android.*