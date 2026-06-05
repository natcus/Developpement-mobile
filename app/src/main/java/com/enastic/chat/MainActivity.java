package com.enastic.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import com.enastic.chat.adapter.StoryAdapter;
import com.enastic.chat.model.Story;
import com.enastic.chat.utils.CloudinaryUploader;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import com.enastic.chat.adapter.UserAdapter;
import com.enastic.chat.model.User;
import com.enastic.chat.viewmodel.AuthViewModel;
import com.enastic.chat.viewmodel.MainViewModel;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private MainViewModel mainViewModel;
    private RecyclerView recyclerViewUsers;
    private ProgressBar progressBarMain;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private boolean isAuthVerified = false;
    
    private RecyclerView recyclerViewStories;
    private StoryAdapter storyAdapter;
    private List<Story> storyList = new ArrayList<>();
    private String currentUserProfileUrl = "";
    private String currentUserName = "";
    
    private final ActivityResultLauncher<String> pickStoryImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadStoryImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        int themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        FirebaseUser currentUser = authViewModel.getCurrentUser();

        if (currentUser == null) { navigateToLogin(); return; }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Discussions");

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        recyclerViewStories = findViewById(R.id.recyclerViewStories);
        progressBarMain = findViewById(R.id.progressBarMain);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.initGlobalStatus(currentUser.getUid());

        setupRecyclerView(currentUser.getUid());
        setupStoriesRecyclerView();
        observeViewModel();

        progressBarMain.setVisibility(View.VISIBLE);
        mainViewModel.loadUsers(); // Restauration : Charge TOUT le monde
        mainViewModel.loadGroups(currentUser.getUid());
        loadStories();

        findViewById(R.id.fabCreateGroup).setOnClickListener(v -> showCreateGroupDialog());
        
        checkBiometricLock();
    }

    private void checkBiometricLock() {
        SharedPreferences securityPrefs = getSharedPreferences("SecurityPrefs", MODE_PRIVATE);
        boolean isBiometricEnabled = securityPrefs.getBoolean("biometric_enabled", false);

        if (isBiometricEnabled && !isAuthVerified) {
            // Cacher le contenu en attendant l'auth
            findViewById(R.id.recyclerViewUsers).setVisibility(View.GONE);
            
            com.enastic.chat.utils.BiometricHelper.showBiometricPrompt(this, new com.enastic.chat.utils.BiometricHelper.BiometricListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        isAuthVerified = true;
                        findViewById(R.id.recyclerViewUsers).setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "Bienvenue !", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Sécurité : " + error, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            });
        }
    }

    private void showAddContactDialog() {
        android.widget.EditText etSearch = new android.widget.EditText(this);
        etSearch.setHint("Email ou numéro de téléphone");
        etSearch.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | android.text.InputType.TYPE_CLASS_TEXT);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Trouver un utilisateur")
                .setMessage("Entrez l'email ou le numéro de téléphone pour le trouver rapidement.")
                .setView(etSearch)
                .setPositiveButton("Rechercher", (dialog, which) -> {
                    String query = etSearch.getText().toString().trim();
                    if (!query.isEmpty()) {
                        progressBarMain.setVisibility(View.VISIBLE);
                        mainViewModel.searchUser(query);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void observeViewModel() {
        // Liste globale des utilisateurs (Restauration)
        mainViewModel.usersLive.observe(this, users -> {
            progressBarMain.setVisibility(View.GONE);
            if (users != null) {
                userList.clear();
                userList.addAll(users);
                userAdapter.notifyDataSetChanged();
            }
        });

        // Résultat de recherche par email
        mainViewModel.searchResultLive.observe(this, user -> {
            progressBarMain.setVisibility(View.GONE);
            if (user != null) {
                String myUid = authViewModel.getCurrentUser().getUid();
                String convoId = (myUid.compareTo(user.getUid()) < 0)
                        ? myUid + "_" + user.getUid()
                        : user.getUid() + "_" + myUid;
                navigateToChat(convoId, user.getName(), myUid, user.getUid());
            } else {
                Toast.makeText(this, "Aucun utilisateur trouvé avec ces coordonnées.", Toast.LENGTH_SHORT).show();
            }
        });

        mainViewModel.errorLive.observe(this, error -> {
            progressBarMain.setVisibility(View.GONE);
            if (error != null) Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView(String currentUserId) {
        userAdapter = new UserAdapter(userList, currentUserId, new UserAdapter.OnUserClickListener() {
            @Override public void onGlobalChatClick() {
                navigateToChat("conversation_globale", "Chat Global", currentUserId, null);
            }
            @Override public void onUserClick(User selectedUser) {
                String convoId = (currentUserId.compareTo(selectedUser.getUid()) < 0)
                        ? currentUserId + "_" + selectedUser.getUid()
                        : selectedUser.getUid() + "_" + currentUserId;
                navigateToChat(convoId, selectedUser.getName(), currentUserId, selectedUser.getUid());
            }
        });
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    private void navigateToChat(String conversationId, String title, String currentUserId, String otherUserId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", conversationId);
        intent.putExtra("chatTitle", title);
        intent.putExtra("currentUserId", currentUserId);
        if (otherUserId != null) intent.putExtra("otherUserId", otherUserId);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showCreateGroupDialog() {
        android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("Nom du groupe");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Nouveau Groupe")
                .setView(etName)
                .setPositiveButton("Créer", (dialog, which) -> {
                    String name = etName.getText().toString();
                    if (!name.isEmpty()) createGroup(name);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void createGroup(String name) {
        FirebaseUser currentUser = authViewModel.getCurrentUser();
        if (currentUser == null) return;
        List<String> members = new ArrayList<>();
        members.add(currentUser.getUid());
        for (User u : userList) members.add(u.getUid()); 
        new com.enastic.chat.repository.ChatRepository().createGroup(name, members, new androidx.lifecycle.MutableLiveData<>(), mainViewModel.errorLive);
        Toast.makeText(this, "Groupe créé !", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_contact) {
            showAddContactDialog();
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            authViewModel.logout();
            navigateToLogin();
            return true;
        } else if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_theme) {
            showThemeDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showThemeDialog() {
        String[] options = {"Clair", "Sombre", "Système"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Choisir le thème")
                .setItems(options, (dialog, which) -> {
                    int mode = (which == 0) ? AppCompatDelegate.MODE_NIGHT_NO : (which == 1) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    sharedPreferences.edit().putInt("theme_mode", mode).apply();
                    AppCompatDelegate.setDefaultNightMode(mode);
                }).show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupStoriesRecyclerView() {
        storyAdapter = new StoryAdapter(storyList, currentUserProfileUrl, new StoryAdapter.OnStoryClickListener() {
            @Override
            public void onAddStoryClick() {
                pickStoryImageLauncher.launch("image/*");
            }

            @Override
            public void onStoryClick(Story story) {
                Intent intent = new Intent(MainActivity.this, StoryActivity.class);
                intent.putExtra("imageUrl", story.getImageUrl());
                intent.putExtra("userName", story.getUserName());
                intent.putExtra("userProfileUrl", story.getUserProfileUrl());
                startActivity(intent);
            }
        });
        recyclerViewStories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewStories.setAdapter(storyAdapter);
    }

    private void loadStories() {
        FirebaseUser user = authViewModel.getCurrentUser();
        if (user == null) return;

        // Récupérer le profil de l'utilisateur courant pour son cercle
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get().addOnSuccessListener(documentSnapshot -> {
                    User u = documentSnapshot.toObject(User.class);
                    if (u != null) {
                        currentUserProfileUrl = u.getProfileImageUrl();
                        currentUserName = u.getName();
                        storyAdapter.updateCurrentUserProfileUrl(currentUserProfileUrl);
                    }
                });

        long yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        FirebaseFirestore.getInstance().collection("stories")
                .whereGreaterThan("timestamp", yesterday)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        storyList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Story story = doc.toObject(Story.class);
                            if (story != null) storyList.add(story);
                        }
                        storyAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void uploadStoryImage(Uri uri) {
        progressBarMain.setVisibility(View.VISIBLE);
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageData = byteBuffer.toByteArray();

            CloudinaryUploader.uploadBytes(imageData, "story_" + System.currentTimeMillis() + ".jpg", "image", new CloudinaryUploader.UploadCallback() {
                @Override
                public void onSuccess(String secureUrl) {
                    saveStoryToFirestore(secureUrl);
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBarMain.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Erreur d'envoi: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            progressBarMain.setVisibility(View.GONE);
            Toast.makeText(this, "Erreur lecture image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveStoryToFirestore(String imageUrl) {
        FirebaseUser user = authViewModel.getCurrentUser();
        if (user == null) return;

        Story story = new Story(
                null,
                user.getUid(),
                currentUserName,
                currentUserProfileUrl,
                imageUrl,
                System.currentTimeMillis()
        );

        com.google.firebase.firestore.DocumentReference docRef = FirebaseFirestore.getInstance().collection("stories").document();
        story.setId(docRef.getId());

        docRef.set(story)
                .addOnSuccessListener(aVoid -> {
                    progressBarMain.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Story publiée !", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBarMain.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Erreur publication", Toast.LENGTH_SHORT).show();
                });
    }
}