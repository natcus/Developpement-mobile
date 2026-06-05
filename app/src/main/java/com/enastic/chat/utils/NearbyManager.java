package com.enastic.chat.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.enastic.chat.model.Message;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Gère la communication Peer-to-Peer via Google Nearby Connections.
 * Permet d'envoyer des messages sans aucune connexion internet.
 */
public class NearbyManager {
    private static final String TAG = "NearbyManager";
    private static final String SERVICE_ID = "com.enastic.chat.OFFLINE_SYNC";
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    public interface NearbyListener {
        void onMessageReceived(Message message);
        void onConnectionStatusChanged(String endpointId, boolean connected);
    }

    private final Context context;
    private final ConnectionsClient connectionsClient;
    private final String currentUserId;
    private final NearbyListener listener;
    private final Map<String, String> connectedEndpoints = new HashMap<>(); // endpointId -> userId

    public NearbyManager(Context context, String currentUserId, NearbyListener listener) {
        this.context = context;
        this.connectionsClient = Nearby.getConnectionsClient(context);
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    /**
     * Démarre la visibilité (Advertising) et la recherche (Discovery) simultanément.
     */
    public void startNearbyMode() {
        startAdvertising();
        startDiscovery();
    }

    public void stopNearbyMode() {
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
        connectionsClient.stopAllEndpoints();
        connectedEndpoints.clear();
    }

    private void startAdvertising() {
        AdvertisingOptions options = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startAdvertising(currentUserId, SERVICE_ID, connectionLifecycleCallback, options)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Advertising started..."))
                .addOnFailureListener(e -> Log.e(TAG, "Advertising failed: " + e.getMessage()));
    }

    private void startDiscovery() {
        DiscoveryOptions options = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Discovery started..."))
                .addOnFailureListener(e -> Log.e(TAG, "Discovery failed: " + e.getMessage()));
    }

    public void sendMessage(Message message) {
        String json = new Gson().toJson(message);
        Payload payload = Payload.fromBytes(json.getBytes(StandardCharsets.UTF_8));
        
        for (String endpointId : connectedEndpoints.keySet()) {
            connectionsClient.sendPayload(endpointId, payload);
            Log.d(TAG, "Message sent via P2P to " + endpointId);
        }
    }

    // --- Callbacks ---

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            Log.d(TAG, "Connection initiated with " + info.getEndpointName());
            connectionsClient.acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "Connected to " + endpointId);
                connectedEndpoints.put(endpointId, endpointId); // On pourra améliorer en récupérant l'UID
                if (listener != null) listener.onConnectionStatusChanged(endpointId, true);
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.d(TAG, "Disconnected from " + endpointId);
            connectedEndpoints.remove(endpointId);
            if (listener != null) listener.onConnectionStatusChanged(endpointId, false);
        }
    };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            Log.d(TAG, "Endpoint found: " + info.getEndpointName());
            connectionsClient.requestConnection(currentUserId, endpointId, connectionLifecycleCallback);
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.d(TAG, "Endpoint lost: " + endpointId);
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                byte[] bytes = payload.asBytes();
                if (bytes != null) {
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    try {
                        Message message = new Gson().fromJson(json, Message.class);
                        if (listener != null) listener.onMessageReceived(message);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding P2P message: " + e.getMessage());
                    }
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
            // Optionnel: suivi de transfert pour les fichiers lourds
        }
    };
}
