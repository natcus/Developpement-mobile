package com.enastic.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.enastic.chat.viewmodel.AuthViewModel;

/**
 * Écran d'inscription de l'application.
 * Permet à un nouvel utilisateur de créer un compte avec nom, email et mot de passe.
 * Redirige vers MainActivity après inscription réussie.
 */
public class RegisterActivity extends AppCompatActivity {

    // Composants de l'interface
    private EditText editName, editEmail, editPassword, editPhone;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView textGoToLogin;

    // ViewModel pour gérer la logique d'inscription
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialisation du ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Liaison des composants de l'interface
        initViews();

        // Observation des LiveData du ViewModel
        observeViewModel();

        // Gestionnaire du bouton d'inscription
        btnRegister.setOnClickListener(v -> attemptRegister());

        // Navigation retour vers l'écran de connexion
        textGoToLogin.setOnClickListener(v -> {
            finish(); // Retour à LoginActivity
        });
    }

    /**
     * Lie les vues aux variables Java.
     */
    private void initViews() {
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editPassword = findViewById(R.id.editPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        textGoToLogin = findViewById(R.id.textGoToLogin);
    }

    /**
     * Observe les LiveData du ViewModel pour réagir aux résultats d'inscription.
     */
    private void observeViewModel() {
        // Succès → redirection vers l'écran principal
        authViewModel.authSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                progressBar.setVisibility(View.GONE);
                navigateToMain();
            }
        });

        // Erreur → affichage d'un message traduit
        authViewModel.authError.observe(this, error -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                
                String friendlyError = "Erreur d'inscription";
                if (error.contains("already in use")) {
                    friendlyError = "Cet email est déjà utilisé par un autre compte.";
                } else if (error.contains("badly formatted")) {
                    friendlyError = "L'adresse email n'est pas valide.";
                } else if (error.contains("network")) {
                    friendlyError = "Erreur réseau. Vérifiez votre connexion.";
                } else {
                    friendlyError = error;
                }
                
                Toast.makeText(this, friendlyError, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Valide les champs et lance la tentative d'inscription.
     */
    private void attemptRegister() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Validation des champs obligatoires
        if (name.isEmpty()) {
            editName.setError("Nom requis");
            return;
        }
        if (email.isEmpty()) {
            editEmail.setError("Email requis");
            return;
        }
        if (phone.isEmpty()) {
            editPhone.setError("Téléphone requis");
            return;
        }
        if (password.length() < 6) {
            editPassword.setError("Mot de passe minimum 6 caractères");
            return;
        }

        // Affichage de l'indicateur de chargement
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Délégation au ViewModel
        authViewModel.register(name, email, password, phone);
    }

    /**
     * Redirige vers MainActivity et ferme toute la pile d'activités.
     */
    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
