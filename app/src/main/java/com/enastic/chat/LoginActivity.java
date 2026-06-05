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
 * Écran de connexion de l'application.
 * Permet à l'utilisateur de se connecter avec email/mot de passe.
 * Redirige vers ChatActivity en cas de succès.
 */
public class LoginActivity extends AppCompatActivity {

    // Composants de l'interface
    private EditText editEmail, editPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView textGoToRegister;

    // ViewModel pour gérer la logique d'authentification
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialisation du ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Vérification de l'état de connexion : si déjà connecté → aller au chat
        if (authViewModel.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        // Liaison des composants de l'interface
        initViews();

        // Observation des LiveData du ViewModel
        observeViewModel();

        // Gestionnaire du bouton de connexion
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Navigation vers l'écran d'inscription
        textGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    /**
     * Lie les vues aux variables Java.
     */
    private void initViews() {
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        textGoToRegister = findViewById(R.id.textGoToRegister);
    }

    /**
     * Observe les LiveData du ViewModel pour réagir aux résultats d'authentification.
     */
    private void observeViewModel() {
        // Succès de la connexion → redirection vers l'écran principal
        authViewModel.authSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                progressBar.setVisibility(View.GONE);
                navigateToMain();
            }
        });

        // Erreur de connexion → affichage d'un message traduit
        authViewModel.authError.observe(this, error -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                
                String friendlyError = "Erreur de connexion";
                if (error.contains("password")) {
                    friendlyError = "Le mot de passe est incorrect.";
                } else if (error.contains("no user record") || error.contains("identifier")) {
                    friendlyError = "L'email est incorrect.";
                } else if (error.contains("badly formatted")) {
                    friendlyError = "L'email est incorrect.";
                } else if (error.contains("network")) {
                    friendlyError = "Erreur réseau. Vérifiez votre connexion.";
                } else if (error.contains("too many requests")) {
                    friendlyError = "Trop de tentatives. Veuillez réessayer plus tard.";
                } else {
                    friendlyError = error;
                }
                
                Toast.makeText(this, friendlyError, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Valide les champs et lance la tentative de connexion.
     */
    private void attemptLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Validation des champs obligatoires
        if (email.isEmpty()) {
            editEmail.setError("Email requis");
            return;
        }
        if (password.isEmpty()) {
            editPassword.setError("Mot de passe requis");
            return;
        }

        // Affichage de l'indicateur de chargement
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Délégation au ViewModel
        authViewModel.login(email, password);
    }

    /**
     * Redirige vers MainActivity et ferme LoginActivity.
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
