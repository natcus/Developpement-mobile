package com.enastic.chat;

import android.content.Intent;
import android.os.Bundle;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.enastic.chat.adapter.MessageAdapter;
import com.enastic.chat.model.Message;
import com.enastic.chat.model.User;
import com.enastic.chat.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activité gérant l'écran de discussion entre deux utilisateurs.
 */
public class ChatActivity extends AppCompatActivity {

    private String conversationId;
    private String currentUserId;
    private String otherUserId;   // UID de l'autre participant (conversations privées)
    private ChatViewModel chatViewModel;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText editMessage;
    private ImageButton btnSend;
    private com.enastic.chat.utils.NearbyManager nearbyManager;
    private boolean isNearbyEnabled = false;

    // État de frappe (Typing)
    private boolean isTyping = false;
    private final Handler typingHandler = new Handler();
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_nearby) {
            toggleNearby();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleNearby() {
        if (isNearbyEnabled) {
            isNearbyEnabled = false;
            nearbyManager.stopNearbyMode();
            Toast.makeText(this, "Mode proximité désactivé", Toast.LENGTH_SHORT).show();
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                nearbyPermissionLauncher.launch(new String[]{
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.BLUETOOTH_ADVERTISE,
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.NEARBY_WIFI_DEVICES
                });
            } else {
                nearbyPermissionLauncher.launch(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                });
            }
        }
    }

    private void startNearby() {
        isNearbyEnabled = true;
        nearbyManager.startNearbyMode();
        Toast.makeText(this, "Recherche de personnes à proximité...", Toast.LENGTH_SHORT).show();
    }

    private void initNearbyManager() {
        nearbyManager = new com.enastic.chat.utils.NearbyManager(this, currentUserId, new com.enastic.chat.utils.NearbyManager.NearbyListener() {
            @Override
            public void onMessageReceived(Message message) {
                chatViewModel.saveP2PMessage(conversationId, message);
            }

            @Override
            public void onConnectionStatusChanged(String endpointId, boolean connected) {
                runOnUiThread(() -> {
                    String status = connected ? "Connecté à proximité" : "Déconnecté de proximité";
                    Toast.makeText(ChatActivity.this, status, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatViewModel.setTypingStatus(conversationId, currentUserId, false);
        if (nearbyManager != null) nearbyManager.stopNearbyMode();
        if (messageAdapter != null) messageAdapter.stopAudio();
    }

    private final Runnable typingRunnable = () -> {
        isTyping = false;
        chatViewModel.setTypingStatus(conversationId, currentUserId, false);
    };


    private final ActivityResultLauncher<String[]> nearbyPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (boolean granted : result.values()) {
                    if (!granted) allGranted = false;
                }
                if (allGranted) {
                    startNearby();
                } else {
                    Toast.makeText(this, "Permissions refusées pour le mode proximité", Toast.LENGTH_SHORT).show();
                }
            });

    // État de réponse (Reply)
    private String replyingToMessageId = null;
    private String replyingToText = null;
    private View layoutReplyPreview;
    private TextView textReplyPreview;
    private ImageButton btnCloseReply;

    // Enregistrement Audio
    private com.enastic.chat.utils.AudioRecorder audioRecorder;
    private boolean isRecording = false;

    // Lanceur pour choisir une image dans la galerie
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Envoyer une image")
                        .setMessage("Voulez-vous envoyer cette image ?")
                        .setPositiveButton("Envoyer", (dialog, which) -> {
                            Toast.makeText(this, "Envoi de l'image...", Toast.LENGTH_SHORT).show();
                            chatViewModel.sendImageMessage(this, conversationId, currentUserId, uri);
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
                }
            });

    // Lanceur pour choisir un fichier
    private final ActivityResultLauncher<String> pickFileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Envoyer un fichier")
                        .setMessage("Voulez-vous envoyer ce fichier ?")
                        .setPositiveButton("Envoyer", (dialog, which) -> {
                            Toast.makeText(this, "Préparation du fichier...", Toast.LENGTH_SHORT).show();
                            String fileName = com.enastic.chat.utils.FileUtils.getFileName(this, uri);
                            String mimeType = getContentResolver().getType(uri);
                            chatViewModel.sendFileMessage(this, conversationId, currentUserId, uri, fileName, mimeType);
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Récupération des données passées par MainActivity
        conversationId = getIntent().getStringExtra("conversationId");
        currentUserId = getIntent().getStringExtra("currentUserId");
        otherUserId   = getIntent().getStringExtra("otherUserId");
        String chatTitle = getIntent().getStringExtra("chatTitle");
        
        if (chatTitle == null) chatTitle = "Chat";

        if (conversationId == null || currentUserId == null) {
            Toast.makeText(this, "Données manquantes", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(chatTitle);
        }

        toolbar.setOnClickListener(v -> {
            if (conversationId.startsWith("group_")) {
                Intent intent = new Intent(this, GroupInfoActivity.class);
                intent.putExtra("conversationId", conversationId);
                startActivity(intent);
            }
        });

        initViews();
        setupRecyclerView();
        
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        observeViewModel();
        initNearbyManager();
        chatViewModel.initChat(conversationId, currentUserId);

        // Démarrer l'écoute de présence si c'est une conversation privée
        if (otherUserId != null && !otherUserId.isEmpty()) {
            chatViewModel.listenToUserPresence(otherUserId);
        }

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        ImageButton btnAttachImage = findViewById(R.id.btnAttachImage);
        ImageButton btnAttachFile = findViewById(R.id.btnAttachFile);
        
        layoutReplyPreview = findViewById(R.id.layoutReplyPreview);
        textReplyPreview = findViewById(R.id.textReplyPreview);
        btnCloseReply = findViewById(R.id.btnCloseReply);
        
        btnAttachImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnAttachFile.setOnClickListener(v -> pickFileLauncher.launch("*/*"));
        btnCloseReply.setOnClickListener(v -> cancelReply());
        
        audioRecorder = new com.enastic.chat.utils.AudioRecorder();
        setupAudioButton();

        editMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Basculer entre icône envoi et micro
                if (s.toString().trim().isEmpty()) {
                    btnSend.setImageResource(android.R.drawable.ic_btn_speak_now);
                } else {
                    btnSend.setImageResource(android.R.drawable.ic_menu_send);
                }

                if (!isTyping) {
                    isTyping = true;
                    chatViewModel.setTypingStatus(conversationId, currentUserId, true);
                }
                typingHandler.removeCallbacks(typingRunnable);
                typingHandler.postDelayed(typingRunnable, 1500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);

        messageAdapter.setOnMessageInteractionListener(new MessageAdapter.OnMessageInteractionListener() {
            @Override
            public void onDeleteMessage(Message message) {
                new androidx.appcompat.app.AlertDialog.Builder(ChatActivity.this)
                    .setTitle("Supprimer")
                    .setMessage("Supprimer pour tout le monde ?")
                    .setPositiveButton("Supprimer", (dialog, which) -> {
                        chatViewModel.deleteMessage(conversationId, message.getMessageId());
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
            }

            @Override
            public void onForwardMessage(Message message) {
                showForwardDialog(message);
            }

            @Override
            public void onReactionMessage(Message message, String emoji) {
                chatViewModel.updateReaction(conversationId, message.getMessageId(), currentUserId, emoji);
            }

            @Override
            public void onReplyMessage(Message message) {
                showReplyPreview(message);
            }

            @Override
            public void onShowInfo(Message message) {
                showMessageInfo(message);
            }
        });


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);
        
        setupSwipeToReply();
    }

    private void setupSwipeToReply() {
        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback swipeCallback = 
            new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Message message = messageList.get(position);
                showReplyPreview(message);
                messageAdapter.notifyItemChanged(position); 
            }
        };
        new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void showReplyPreview(Message message) {
        replyingToMessageId = message.getMessageId();
        replyingToText = message.getText();
        if (replyingToText == null || replyingToText.isEmpty()) {
            if (message.getImageUrl() != null) replyingToText = "📷 Photo";
            else if (message.getFileUrl() != null) replyingToText = "📄 Fichier";
        }
        
        textReplyPreview.setText(replyingToText);
        layoutReplyPreview.setVisibility(View.VISIBLE);
        editMessage.requestFocus();
    }

    private void cancelReply() {
        replyingToMessageId = null;
        replyingToText = null;
        layoutReplyPreview.setVisibility(View.GONE);
    }

    private void showMessageInfo(Message message) {
        if (message.getReadBy() == null || message.getReadBy().isEmpty()) {
            Toast.makeText(this, "Aucune information de lecture", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder info = new StringBuilder("Lu par :\n");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault());
        
        for (String uid : message.getReadBy().keySet()) {
            long time = message.getReadBy().get(uid);
            info.append("- ").append(uid).append(" à ").append(sdf.format(new java.util.Date(time))).append("\n");
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Infos du message")
                .setMessage(info.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            if (replyingToMessageId != null) {
                cancelReply();
            }
            
            // Envoi Cloud (Firestore)
            chatViewModel.sendMessage(conversationId, currentUserId, text, replyingToMessageId, replyingToText); 
            
            // Envoi P2P (Nearby) si activé
            if (isNearbyEnabled && nearbyManager != null) {
                Message p2pMsg = new Message();
                p2pMsg.setText(com.enastic.chat.utils.EncryptionUtils.encrypt(text));
                p2pMsg.setSenderId(currentUserId);
                p2pMsg.setTimestamp(System.currentTimeMillis());
                p2pMsg.setReplyToText(replyingToText);
                nearbyManager.sendMessage(p2pMsg);
            }

            editMessage.setText("");
        }
    }

    private void observeViewModel() {
        chatViewModel.messagesLive.observe(this, messages -> {
            if (messages != null) {
                messageList.clear();
                messageList.addAll(messages);
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) recyclerView.scrollToPosition(messageList.size() - 1);
                
                for (Message msg : messages) {
                    if (!msg.getSenderId().equals(currentUserId) && msg.getMessageId() != null) {
                        if (!msg.isDelivered()) chatViewModel.markMessageAsDelivered(conversationId, msg.getMessageId());
                        if (!msg.isRead()) chatViewModel.markMessageAsRead(conversationId, msg.getMessageId(), currentUserId);
                    }
                }
            }
        });

        chatViewModel.isOtherTypingLive.observe(this, isOtherTyping -> {
            // La frappe a la priorité sur le statut de présence
            if (Boolean.TRUE.equals(isOtherTyping)) {
                updateToolbarSubtitle("en train d'écrire...");
            } else {
                // Revenir au statut de présence
                User presenceUser = chatViewModel.otherUserPresenceLive.getValue();
                if (presenceUser != null) {
                    updateToolbarSubtitle(formatPresence(presenceUser));
                } else {
                    updateToolbarSubtitle(null);
                }
            }
        });

        chatViewModel.otherUserPresenceLive.observe(this, user -> {
            // N'écraser le sous-titre que si l'utilisateur n'est PAS en train d'écrire
            if (!Boolean.TRUE.equals(chatViewModel.isOtherTypingLive.getValue())) {
                updateToolbarSubtitle(formatPresence(user));
            }
        });


        chatViewModel.moreMessagesLive.observe(this, olderMessages -> {
            if (olderMessages != null && !olderMessages.isEmpty()) {
                messageList.addAll(0, olderMessages);
                messageAdapter.notifyItemRangeInserted(0, olderMessages.size());
            }
        });

        chatViewModel.errorLive.observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && lm.findFirstVisibleItemPosition() == 0 && !messageList.isEmpty() && dy < 0) {
                    // L'utilisateur a scrollé tout en haut
                    long oldestTimestamp = messageList.get(0).getTimestamp();
                    chatViewModel.loadMoreMessages(conversationId, oldestTimestamp);
                }
            }
        });
    }


    private void showForwardDialog(Message messageToForward) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener(querySnapshot -> {
                List<com.enastic.chat.model.User> users = querySnapshot.toObjects(com.enastic.chat.model.User.class);
                List<com.enastic.chat.model.User> otherUsers = new ArrayList<>();
                for (com.enastic.chat.model.User u : users) {
                    if (u.getUid() != null && !u.getUid().equals(currentUserId)) otherUsers.add(u);
                }
                
                String[] names = new String[otherUsers.size()];
                for (int i = 0; i < otherUsers.size(); i++) names[i] = otherUsers.get(i).getName();
                
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Transférer à...")
                    .setItems(names, (dialog, which) -> {
                        com.enastic.chat.model.User targetUser = otherUsers.get(which);
                        forwardMessageTo(messageToForward, targetUser);
                    })
                    .show();
            });
    }

    private void forwardMessageTo(Message original, com.enastic.chat.model.User target) {
        String targetConvoId = generatePrivateConversationId(currentUserId, target.getUid());
        if (original.getImageUrl() != null && !original.getImageUrl().isEmpty()) {
            if (original.getFileName() != null) {
                chatViewModel.sendExistingFileMessage(targetConvoId, currentUserId, original.getImageUrl(), original.getFileName(), original.getFileMimeType());
            } else {
                chatViewModel.sendExistingImageMessage(targetConvoId, currentUserId, original.getImageUrl());
            }
        } else {
            chatViewModel.sendMessage(targetConvoId, currentUserId, original.getText(), null, null);
        }
        Toast.makeText(this, "Transféré à " + target.getName(), Toast.LENGTH_SHORT).show();
    }

    private String generatePrivateConversationId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        android.view.MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        searchView.setQueryHint("Rechercher un message...");
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterMessages(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMessages(newText);
                return true;
            }
        });
        
        return true;
    }

    private void filterMessages(String query) {
        if (query.isEmpty()) {
            messageAdapter.updateList(messageList);
        } else {
            List<Message> filtered = new ArrayList<>();
            for (Message m : messageList) {
                if (m.getText() != null && m.getText().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(m);
                }
            }
            messageAdapter.updateList(filtered);
        }
    }

    private void setupAudioButton() {
        btnSend.setImageResource(android.R.drawable.ic_btn_speak_now);
        btnSend.setOnLongClickListener(v -> {
            if (editMessage.getText().toString().trim().isEmpty()) {
                startRecording();
                return true;
            }
            return false;
        });

        btnSend.setOnTouchListener((v, event) -> {
            if (isRecording && event.getAction() == android.view.MotionEvent.ACTION_UP) {
                stopRecording();
            }
            return false;
        });
    }

    private void startRecording() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }
        isRecording = true;
        audioRecorder.startRecording(this);
        Toast.makeText(this, "Enregistrement en cours...", Toast.LENGTH_SHORT).show();
        btnSend.setScaleX(1.5f);
        btnSend.setScaleY(1.5f);
    }

    private void stopRecording() {
        isRecording = false;
        btnSend.setScaleX(1.0f);
        btnSend.setScaleY(1.0f);
        com.enastic.chat.utils.AudioRecorder.RecordingResult result = audioRecorder.stopRecording();
        if (result != null) {
            chatViewModel.sendVoiceMessage(this, conversationId, currentUserId, result.filePath, result.durationSeconds);
        } else {
            Toast.makeText(this, "Message trop court", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Helpers de présence ────────────────────────────────────────────

    /** Met à jour le sous-titre de la toolbar sur le thread principal. */
    private void updateToolbarSubtitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    /**
     * Formate le statut de présence d'un utilisateur.
     * Retourne "En ligne", ou "Vu il y a X" selon son état.
     */
    private String formatPresence(User user) {
        if (user == null) return null;
        if (user.isOnline()) return "En ligne";

        long lastSeen = user.getLastSeen();
        if (lastSeen <= 0) return null;

        long diffMs  = System.currentTimeMillis() - lastSeen;
        long diffMin = diffMs / 60_000;
        long diffH   = diffMs / 3_600_000;
        long diffJ   = diffMs / 86_400_000;

        if (diffMin < 1)  return "Vu à l'instant";
        if (diffMin < 60) return "Vu il y a " + diffMin + " min";
        if (diffH < 24)   return "Vu il y a " + diffH + " h";
        return "Vu il y a " + diffJ + " j";
    }
}
