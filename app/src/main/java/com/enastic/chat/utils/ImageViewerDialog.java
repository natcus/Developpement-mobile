package com.enastic.chat.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;

/**
 * Dialogue plein écran pour afficher et télécharger une image envoyée ou reçue.
 */
public class ImageViewerDialog extends Dialog {

    private final String imageUrl;
    private final String fileName;

    public ImageViewerDialog(Context context, String imageUrl, String fileName) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.imageUrl = imageUrl;
        this.fileName = fileName != null && !fileName.isEmpty() ? fileName : "image_" + System.currentTimeMillis() + ".jpg";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Conteneur principal noir
        RelativeLayout root = new RelativeLayout(getContext());
        root.setBackgroundColor(Color.BLACK);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT));

        // ImageView au centre
        ImageView imageView = new ImageView(getContext());
        imageView.setId(ImageView.generateViewId());
        RelativeLayout.LayoutParams imgParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT);
        imgParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        imageView.setLayoutParams(imgParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(imageView);

        // Chargement de l'image
        if (imageUrl.startsWith("http")) {
            Glide.with(getContext()).load(imageUrl).into(imageView);
        } else {
            try {
                byte[] decoded = Base64.decode(imageUrl, Base64.DEFAULT);
                Glide.with(getContext()).asBitmap().load(decoded).into(imageView);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Impossible de charger l'image", Toast.LENGTH_SHORT).show();
            }
        }

        // Bouton de fermeture (Haut Gauche)
        ImageButton btnClose = new ImageButton(getContext());
        btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnClose.setColorFilter(Color.WHITE);
        RelativeLayout.LayoutParams closeParams = new RelativeLayout.LayoutParams(
                dpToPx(48), dpToPx(48));
        closeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        closeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        closeParams.setMargins(dpToPx(16), dpToPx(16), 0, 0);
        btnClose.setLayoutParams(closeParams);
        btnClose.setOnClickListener(v -> dismiss());
        root.addView(btnClose);

        // Bouton de téléchargement (Haut Droite)
        ImageButton btnDownload = new ImageButton(getContext());
        btnDownload.setImageResource(android.R.drawable.ic_menu_save);
        btnDownload.setBackgroundColor(Color.TRANSPARENT);
        btnDownload.setColorFilter(Color.WHITE);
        RelativeLayout.LayoutParams downloadParams = new RelativeLayout.LayoutParams(
                dpToPx(48), dpToPx(48));
        downloadParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        downloadParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        downloadParams.setMargins(0, dpToPx(16), dpToPx(16), 0);
        btnDownload.setLayoutParams(downloadParams);
        btnDownload.setOnClickListener(v -> {
            if (imageUrl.startsWith("http")) {
                FileUtils.downloadFile(getContext(), imageUrl, fileName, "image/jpeg", false);
            } else {
                FileUtils.saveBase64ToFile(getContext(), imageUrl, fileName, "image/jpeg");
            }
        });
        root.addView(btnDownload);

        setContentView(root);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
