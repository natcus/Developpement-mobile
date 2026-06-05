package com.enastic.chat;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView imageView = findViewById(R.id.imgFullScreen);
        ImageButton btnClose = findViewById(R.id.btnClose);

        String imageUrl = getIntent().getStringExtra("imageUrl");

        if (imageUrl != null) {
            if (imageUrl.startsWith("http")) {
                Glide.with(this).load(imageUrl).into(imageView);
            } else {
                try {
                    byte[] decodedString = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT);
                    Glide.with(this).asBitmap().load(decodedString).into(imageView);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        btnClose.setOnClickListener(v -> finish());
    }
}
