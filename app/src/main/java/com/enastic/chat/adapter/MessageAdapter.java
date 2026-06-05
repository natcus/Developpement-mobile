package com.enastic.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.enastic.chat.R;
import com.enastic.chat.model.Message;
import com.enastic.chat.utils.FileUtils;

import java.util.List;

/**
 * Adapter RecyclerView pour afficher les messages du chat.
 * Supporte le chargement d'images, audios et fichiers.
 * Gère correctement la mémoire du MediaPlayer (sans static).
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    public interface OnMessageInteractionListener {
        void onDeleteMessage(Message message);
        void onForwardMessage(Message message);
        void onReactionMessage(Message message, String emoji);
        void onReplyMessage(Message message);
        void onShowInfo(Message message);
    }

    private OnMessageInteractionListener interactionListener;
    private final List<Message> messageList;
    private List<Message> messageListFull; // Pour la recherche
    private String currentUserId;
    private String searchQuery = ""; // Pour surligner si besoin

    // Variables NON STATIQUES pour éviter les fuites de mémoire (Memory Leaks)
    private android.media.MediaPlayer mediaPlayer;
    private android.widget.ImageButton currentPlayButton;
    private String currentPlayingData;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.messageListFull = new java.util.ArrayList<>(messageList);
        this.currentUserId = currentUserId;
    }

    public void updateList(List<Message> newList) {
        this.messageList.clear();
        this.messageList.addAll(newList);
        notifyDataSetChanged();
    }

    public void setOnMessageInteractionListener(OnMessageInteractionListener listener) {
        this.interactionListener = listener;
    }

    /**
     * Arrête la lecture audio et libère les ressources.
     * DOIT être appelé par l'Activité (ex: dans onDestroy()).
     */
    public void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (currentPlayButton != null) {
            currentPlayButton.setImageResource(android.R.drawable.ic_media_play);
            currentPlayButton = null;
        }
        currentPlayingData = null;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            return new SentMessageViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
        } else {
            return new ReceivedMessageViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message, interactionListener);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message, interactionListener);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // --- ViewHolder Sent (NON STATIC) ---
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage, textTime, textReadReceipt, textReactions, textReply, textAudioDuration, textFile;
        private final ImageView imgMessage;
        private final View layoutFile, layoutReply, layoutAudio;
        private final android.widget.ImageButton btnPlayAudio;
        private final android.widget.SeekBar seekbarAudio;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessageSent);
            textTime = itemView.findViewById(R.id.textTimeSent);
            textReadReceipt = itemView.findViewById(R.id.textReadReceipt);
            imgMessage = itemView.findViewById(R.id.imgMessageSent);
            layoutFile = itemView.findViewById(R.id.layoutFileSent);
            textFile = itemView.findViewById(R.id.textFileSent);
            textReactions = itemView.findViewById(R.id.textReactionsSent);
            layoutReply = itemView.findViewById(R.id.layoutReplySent);
            textReply = itemView.findViewById(R.id.textReplySent);
            layoutAudio = itemView.findViewById(R.id.layoutAudioSent);
            btnPlayAudio = itemView.findViewById(R.id.btnPlayAudioSent);
            seekbarAudio = itemView.findViewById(R.id.seekbarAudioSent);
            textAudioDuration = itemView.findViewById(R.id.textAudioDurationSent);
        }

        void bind(Message message, OnMessageInteractionListener listener) {
            // Setup Reply
            if (message.getReplyToText() != null && !message.getReplyToText().isEmpty()) {
                layoutReply.setVisibility(View.VISIBLE);
                textReply.setText(message.getReplyToText());
            } else {
                layoutReply.setVisibility(View.GONE);
            }

            // Reset Visibility
            imgMessage.setVisibility(View.GONE);
            layoutFile.setVisibility(View.GONE);
            layoutAudio.setVisibility(View.GONE);
            textMessage.setVisibility(View.GONE);

            // Setup Media
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                imgMessage.setVisibility(View.VISIBLE);
                loadImage(imgMessage, message.getImageUrl());
                imgMessage.setOnClickListener(v -> {
                    new com.enastic.chat.utils.ImageViewerDialog(v.getContext(), message.getImageUrl(), message.getFileName()).show();
                });
            } else if (message.isAudio()) {
                layoutAudio.setVisibility(View.VISIBLE);
                textAudioDuration.setText(message.getAudioDuration() + "s");
                btnPlayAudio.setOnClickListener(v -> playAudio(message.getFileUrl(), btnPlayAudio, seekbarAudio));
            } else if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                layoutFile.setVisibility(View.VISIBLE);
                textFile.setText(message.getFileName());
                layoutFile.setOnClickListener(v -> showFileDialog(v.getContext(), message.getFileUrl(), message.getFileName(), message.getFileMimeType()));
            } else {
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText(message.getText());
            }

            itemView.setOnLongClickListener(v -> {
                showMessageOptions(v, message, listener);
                return true;
            });

            // Time & Status
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            textTime.setText(sdf.format(new java.util.Date(message.getTimestamp())));
            if (textReadReceipt != null) updateReadReceipt(textReadReceipt, message);
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage, textTime, textReactions, textReply, textAudioDuration, textFile;
        private final ImageView imgMessage, imgSenderAvatar;
        private final View layoutFile, layoutReply, layoutAudio;
        private final android.widget.ImageButton btnPlayAudio;
        private final android.widget.SeekBar seekbarAudio;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessageReceived);
            textTime = itemView.findViewById(R.id.textTimeReceived);
            imgMessage = itemView.findViewById(R.id.imgMessageReceived);
            imgSenderAvatar = itemView.findViewById(R.id.imgSenderAvatarReceived);
            layoutFile = itemView.findViewById(R.id.layoutFileReceived);
            textFile = itemView.findViewById(R.id.textFileReceived);
            textReactions = itemView.findViewById(R.id.textReactionsReceived);
            layoutReply = itemView.findViewById(R.id.layoutReplyReceived);
            textReply = itemView.findViewById(R.id.textReplyReceived);
            layoutAudio = itemView.findViewById(R.id.layoutAudioReceived);
            btnPlayAudio = itemView.findViewById(R.id.btnPlayAudioReceived);
            seekbarAudio = itemView.findViewById(R.id.seekbarAudioReceived);
            textAudioDuration = itemView.findViewById(R.id.textAudioDurationReceived);
        }

        void bind(Message message, OnMessageInteractionListener listener) {
            imgMessage.setVisibility(View.GONE);
            layoutAudio.setVisibility(View.GONE);
            layoutFile.setVisibility(View.GONE);
            textMessage.setVisibility(View.GONE);

            // Charger l'avatar de l'expéditeur
            if (imgSenderAvatar != null && message.getSenderProfileUrl() != null && !message.getSenderProfileUrl().isEmpty()) {
                loadImage(imgSenderAvatar, message.getSenderProfileUrl());
            }

            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                imgMessage.setVisibility(View.VISIBLE);
                loadImage(imgMessage, message.getImageUrl());
                imgMessage.setOnClickListener(v -> {
                    new com.enastic.chat.utils.ImageViewerDialog(v.getContext(), message.getImageUrl(), message.getFileName()).show();
                });
            } else if (message.isAudio()) {
                layoutAudio.setVisibility(View.VISIBLE);
                textAudioDuration.setText(message.getAudioDuration() + "s");
                btnPlayAudio.setOnClickListener(v -> playAudio(message.getFileUrl(), btnPlayAudio, seekbarAudio));
            } else if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                layoutFile.setVisibility(View.VISIBLE);
                textFile.setText(message.getFileName());
                layoutFile.setOnClickListener(v -> showFileDialog(v.getContext(), message.getFileUrl(), message.getFileName(), message.getFileMimeType()));
            } else {
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText(message.getText());
            }

            itemView.setOnLongClickListener(v -> {
                showMessageOptions(v, message, listener);
                return true;
            });

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            textTime.setText(sdf.format(new java.util.Date(message.getTimestamp())));
        }
    }

    // --- Media Helpers (NON STATIC) ---

    private void showMessageOptions(View v, Message message, OnMessageInteractionListener listener) {
        if (listener == null) return;
        android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);
        popup.getMenu().add("Répondre");
        popup.getMenu().add("Transférer");
        popup.getMenu().add("Infos");
        popup.getMenu().add("Supprimer");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Répondre")) listener.onReplyMessage(message);
            else if (title.equals("Transférer")) listener.onForwardMessage(message);
            else if (title.equals("Infos")) listener.onShowInfo(message);
            else if (title.equals("Supprimer")) listener.onDeleteMessage(message);
            return true;
        });
        popup.show();
    }

    private void loadImage(ImageView imageView, String data) {
        if (data.startsWith("http")) {
            com.bumptech.glide.Glide.with(imageView.getContext()).load(data).into(imageView);
        } else {
            try {
                byte[] decoded = Base64.decode(data, Base64.DEFAULT);
                com.bumptech.glide.Glide.with(imageView.getContext()).asBitmap().load(decoded).into(imageView);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void playAudio(String data, android.widget.ImageButton btn, android.widget.SeekBar sb) {
        if (mediaPlayer != null && data.equals(currentPlayingData)) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btn.setImageResource(android.R.drawable.ic_media_play);
            } else {
                mediaPlayer.start();
                btn.setImageResource(android.R.drawable.ic_media_pause);
                updateSeekBar(sb);
            }
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            if (currentPlayButton != null) currentPlayButton.setImageResource(android.R.drawable.ic_media_play);
        }

        mediaPlayer = new android.media.MediaPlayer();
        currentPlayingData = data;
        currentPlayButton = btn;

        try {
            if (data.startsWith("http")) {
                mediaPlayer.setDataSource(data);
            } else {
                File tempFile = File.createTempFile("temp_voice", ".m4a", btn.getContext().getCacheDir());
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(Base64.decode(data, Base64.DEFAULT));
                fos.close();
                mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            }
            
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                btn.setImageResource(android.R.drawable.ic_media_pause);
                sb.setMax(mp.getDuration());
                updateSeekBar(sb);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                btn.setImageResource(android.R.drawable.ic_media_play);
                sb.setProgress(0);
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showFileDialog(android.content.Context context, String fileUrl, String fileName, String mimeType) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(fileName != null && !fileName.isEmpty() ? fileName : "Fichier joint")
                .setMessage("Que voulez-vous faire avec ce fichier ?")
                .setPositiveButton("Ouvrir / Voir", (dialog, which) -> {
                    if (fileUrl.startsWith("http")) {
                        FileUtils.downloadFile(context, fileUrl, fileName, mimeType, true);
                    } else {
                        FileUtils.saveBase64ToFile(context, fileUrl, fileName, mimeType);
                    }
                })
                .setNegativeButton("Télécharger", (dialog, which) -> {
                    if (fileUrl.startsWith("http")) {
                        FileUtils.downloadFile(context, fileUrl, fileName, mimeType, false);
                    } else {
                        FileUtils.saveBase64ToFile(context, fileUrl, fileName, mimeType);
                    }
                })
                .setNeutralButton("Annuler", null)
                .show();
    }

    private void updateSeekBar(android.widget.SeekBar sb) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            sb.setProgress(mediaPlayer.getCurrentPosition());
            new android.os.Handler().postDelayed(() -> updateSeekBar(sb), 100);
        }
    }

    private void updateReadReceipt(TextView tv, Message m) {
        if (m.isSyncing()) { tv.setText("⌛"); tv.setTextColor(android.graphics.Color.GRAY); }
        else if (m.isRead()) { tv.setText("✓✓"); tv.setTextColor(android.graphics.Color.BLUE); }
        else if (m.isDelivered()) { tv.setText("✓✓"); tv.setTextColor(android.graphics.Color.GRAY); }
        else { tv.setText("✓"); tv.setTextColor(android.graphics.Color.GRAY); }
    }
}
