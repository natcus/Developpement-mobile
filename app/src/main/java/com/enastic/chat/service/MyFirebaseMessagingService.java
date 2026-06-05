package com.enastic.chat.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.enastic.chat.MainActivity;
import com.enastic.chat.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Log pour vérifier la réception
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = "Nouveau Message";
        String body = "";
        String conversationId = null;
        String chatTitle = null;

        // 1. Récupérer les données de la charge utile (data)
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            conversationId = remoteMessage.getData().get("conversationId");
            chatTitle = remoteMessage.getData().get("chatTitle");
            // Si le titre/corps sont dans data (envoyé par notre serveur)
            if (remoteMessage.getData().containsKey("title")) title = remoteMessage.getData().get("title");
            if (remoteMessage.getData().containsKey("body")) body = remoteMessage.getData().get("body");
        }

        // 2. Si c'est une notification Firebase standard
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (body != null && !body.isEmpty()) {
            sendNotification(title, body, conversationId, chatTitle);
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Sauvegarder dans Firestore si l'utilisateur est connecté
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .update("fcmToken", token);
        }
    }

    private void sendNotification(String title, String messageBody, String conversationId, String chatTitle) {
        Intent intent;
        if (conversationId != null) {
            intent = new Intent(this, com.enastic.chat.ChatActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("chatTitle", chatTitle);
            intent.putExtra("currentUserId", com.google.firebase.auth.FirebaseAuth.getInstance().getUid());
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (conversationId != null ? conversationId.hashCode() : 0), intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "chat_notifications";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        // Style "Messagerie" professionnel
        androidx.core.app.Person user = new androidx.core.app.Person.Builder()
                .setName(title) // Le titre contient souvent le nom de l'expéditeur
                .build();

        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(user)
                .addMessage(messageBody, System.currentTimeMillis(), user);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground) // Utiliser l'icône de l'app
                        .setStyle(style)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setColor(getResources().getColor(R.color.primary))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(new long[]{1000, 1000});

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((conversationId != null ? conversationId.hashCode() : 0), notificationBuilder.build());
    }
}
