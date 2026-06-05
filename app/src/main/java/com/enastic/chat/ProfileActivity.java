package com.enastic.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.enastic.chat.model.User;
import com.enastic.chat.utils.ImageCompressor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activité de gestion du profil utilisateur.
 * Version SANS STORAGE : Utilise le Base64 pour stocker l'image directement dans Firestore.
 * C'est 100% gratuit et ne nécessite pas d'activation de Firebase Storage.
 */
public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private EditText editProfileName, editProfilePhone;
    private Button btnSaveProfile, btnDeletePhoto;
    private ProgressBar progressBar;
    private com.google.android.material.materialswitch.MaterialSwitch switchBiometric;
    private android.content.SharedPreferences prefs;

    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).transform(new CircleCrop()).into(imgProfile);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imgProfile = findViewById(R.id.imgProfile);
        editProfileName = findViewById(R.id.editProfileName);
        editProfilePhone = findViewById(R.id.editProfilePhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnDeletePhoto = findViewById(R.id.btnDeletePhoto);
        progressBar = findViewById(R.id.progressBarProfile);
        switchBiometric = findViewById(R.id.switchBiometric);
        prefs = getSharedPreferences("SecurityPrefs", MODE_PRIVATE);
        
        switchBiometric.setChecked(prefs.getBoolean("biometric_enabled", false));
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !com.enastic.chat.utils.BiometricHelper.isBiometricAvailable(this)) {
                Toast.makeText(this, "La biométrie n'est pas disponible sur cet appareil", Toast.LENGTH_SHORT).show();
                switchBiometric.setChecked(false);
                return;
            }
            prefs.edit().putBoolean("biometric_enabled", isChecked).apply();
        });

        Toolbar toolbar = findViewById(R.id.toolbarProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        if (currentUser == null) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserProfile();

        findViewById(R.id.btnChangePhoto).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        
        if (btnDeletePhoto != null) {
            btnDeletePhoto.setOnClickListener(v -> deleteProfilePhoto());
        }
    }

    private void loadUserProfile() {
        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        editProfileName.setText(user.getName());
                        editProfilePhone.setText(user.getPhoneNumber());
                        String photoData = user.getProfileImageUrl();
                        if (photoData != null && !photoData.isEmpty()) {
                            // Si la photo est en Base64
                            if (photoData.length() > 100) { 
                                try {
                                    byte[] decodedString = Base64.decode(photoData, Base64.DEFAULT);
                                    Glide.with(this).load(decodedString).transform(new CircleCrop()).into(imgProfile);
                                } catch (Exception e) {
                                    Glide.with(this).load(photoData).transform(new CircleCrop()).into(imgProfile);
                                }
                            } else {
                                // Sinon c'est une URL classique
                                Glide.with(this).load(photoData).transform(new CircleCrop()).into(imgProfile);
                            }
                        }
                    }
                });
    }

    private void saveProfile() {
        String newName = editProfileName.getText().toString().trim();
        String newPhone = editProfilePhone.getText().toString().trim();
        
        if (newName.isEmpty()) {
            Toast.makeText(this, "Le nom ne peut pas être vide", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        if (selectedImageUri != null) {
            String base64Image = ImageCompressor.compressUriToBase64(this, selectedImageUri);
            if (base64Image == null) {
                progressBar.setVisibility(View.GONE);
                btnSaveProfile.setEnabled(true);
                Toast.makeText(this, "Erreur de traitement image", Toast.LENGTH_SHORT).show();
                return;
            }
            updateFirestoreProfile(newName, newPhone, base64Image);
        } else {
            updateFirestoreProfile(newName, newPhone, null);
        }
    }

    private void deleteProfilePhoto() {
        selectedImageUri = null;
         updateFirestoreProfile(editProfileName.getText().toString().trim(), editProfilePhone.getText().toString().trim(), "");
    }

    private void updateFirestoreProfile(String name, String phone, String photoData) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phoneNumber", phone);
        if (photoData != null) {
            updates.put("profileImageUrl", photoData);
        }

        firestore.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    if (photoData != null && photoData.isEmpty()) {
                        imgProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
                    }
                    Toast.makeText(this, "Profil mis à jour (Base64)", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
