package com.enastic.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

public class StoryActivity extends AppCompatActivity {

    private ImageView imgStoryContent, imgStoryUserProfile;
    private TextView txtStoryUserName;
    private ProgressBar progressBarStory;
    private View viewPrevious, viewNext;

    private Handler handler;
    private Runnable runnable;
    private int progress = 0;
    private final int STORY_DURATION = 5000; // 5 secondes
    private final int UPDATE_INTERVAL = 50; // Mise à jour toutes les 50ms
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Mettre en plein écran
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_story);

        imgStoryContent = findViewById(R.id.imgStoryContent);
        imgStoryUserProfile = findViewById(R.id.imgStoryUserProfile);
        txtStoryUserName = findViewById(R.id.txtStoryUserName);
        progressBarStory = findViewById(R.id.progressBarStory);
        viewPrevious = findViewById(R.id.viewPrevious);
        viewNext = findViewById(R.id.viewNext);

        String imageUrl = getIntent().getStringExtra("imageUrl");
        String userName = getIntent().getStringExtra("userName");
        String userProfileUrl = getIntent().getStringExtra("userProfileUrl");

        txtStoryUserName.setText(userName != null ? userName : "Inconnu");

        // Charger l'image de la story
        Glide.with(this).load(imageUrl).into(imgStoryContent);

        // Charger la photo de profil
        if (userProfileUrl != null && !userProfileUrl.isEmpty()) {
            if (userProfileUrl.length() > 100) {
                try {
                    byte[] decodedString = android.util.Base64.decode(userProfileUrl, android.util.Base64.DEFAULT);
                    Glide.with(this).load(decodedString).transform(new CircleCrop()).into(imgStoryUserProfile);
                } catch (Exception e) {
                    Glide.with(this).load(userProfileUrl).transform(new CircleCrop()).into(imgStoryUserProfile);
                }
            } else {
                Glide.with(this).load(userProfileUrl).transform(new CircleCrop()).into(imgStoryUserProfile);
            }
        } else {
            imgStoryUserProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        setupClickListeners();
        startStoryTimer();
    }

    private void setupClickListeners() {
        // Appuyer longuement pour mettre en pause
        View.OnTouchListener pauseTouchListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isPaused = true;
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isPaused = false;
                    // Simuler le clic si on a relâché rapidement
                    if (event.getEventTime() - event.getDownTime() < 200) {
                        v.performClick();
                    }
                    return true;
            }
            return false;
        };

        viewPrevious.setOnTouchListener(pauseTouchListener);
        viewNext.setOnTouchListener(pauseTouchListener);

        viewPrevious.setOnClickListener(v -> finish()); // Retour arrière
        viewNext.setOnClickListener(v -> finish()); // Passer à la suivante (ferme pour l'instant)
    }

    private void startStoryTimer() {
        handler = new Handler(Looper.getMainLooper());
        progressBarStory.setMax(100);

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    progress += (100 * UPDATE_INTERVAL) / STORY_DURATION;
                    progressBarStory.setProgress(progress);

                    if (progress >= 100) {
                        finish(); // Fermer la story
                    } else {
                        handler.postDelayed(this, UPDATE_INTERVAL);
                    }
                } else {
                    handler.postDelayed(this, UPDATE_INTERVAL); // Boucler sans augmenter la progression
                }
            }
        };
        handler.postDelayed(runnable, UPDATE_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
