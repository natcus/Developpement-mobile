package com.enastic.chat;

import android.app.Application;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Classe Application personnalisée pour initialiser les configurations globales.
 */
public class ChatApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Configuration de Firestore pour le mode hors-ligne
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        firestore.setFirestoreSettings(settings);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int activityCount = 0;

            @Override
            public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(android.app.Activity activity) {
                activityCount++;
                if (activityCount == 1) {
                    updateOnlineStatus(true);
                }
            }

            @Override
            public void onActivityResumed(android.app.Activity activity) {}

            @Override
            public void onActivityPaused(android.app.Activity activity) {}

            @Override
            public void onActivityStopped(android.app.Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                    updateOnlineStatus(false);
                }
            }

            @Override
            public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) {}

            @Override
            public void onActivityDestroyed(android.app.Activity activity) {}
        });
    }

    private void updateOnlineStatus(boolean isOnline) {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("isOnline", isOnline);
            if (!isOnline) {
                updates.put("lastSeen", System.currentTimeMillis());
            }
            FirebaseFirestore.getInstance().collection("users").document(uid).update(updates);
        }
    }
}
