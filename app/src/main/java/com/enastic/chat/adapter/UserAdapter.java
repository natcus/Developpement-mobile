package com.enastic.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.enastic.chat.R;
import com.enastic.chat.model.User;
import com.enastic.chat.model.Message;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> userList;
    private final OnUserClickListener listener;
    private final String currentUserId;
    private final FirebaseFirestore firestore;
    private final Map<String, ListenerRegistration> listenerMap = new HashMap<>();

    public interface OnUserClickListener {
        void onGlobalChatClick();
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, String currentUserId, OnUserClickListener listener) {
        this.userList = userList;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        if (position == 0) {
            // Premier élément : Chat Global
            holder.textUserName.setText("🌐 Chat Global");
            holder.textUserEmail.setText("Discussion publique avec tous les utilisateurs");
            holder.imgUserIcon.setImageResource(android.R.drawable.ic_dialog_map);
            holder.itemView.setOnClickListener(v -> listener.onGlobalChatClick());
        } else {
            // Autres éléments : Utilisateurs (position - 1 car on a ajouté Chat Global en haut)
            User user = userList.get(position - 1);
            
            // Masquer l'utilisateur actuel s'il est dans la liste (on ne veut pas se parler à soi-même)
            if (user.getUid() != null && user.getUid().equals(currentUserId)) {
                holder.itemView.setVisibility(View.GONE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                return;
            } else {
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            holder.textUserName.setText("👤 " + user.getName());
            holder.textUserTime.setText(""); // Réinitialiser le temps
            holder.textUserEmail.setText(user.getEmail()); // Par défaut, on affiche l'email
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                String url = user.getProfileImageUrl();
                if (url.startsWith("http")) {
                    com.bumptech.glide.Glide.with(holder.itemView.getContext())
                            .load(url).transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                            .into(holder.imgUserIcon);
                } else {
                    try {
                        byte[] decodedString = android.util.Base64.decode(url, android.util.Base64.DEFAULT);
                        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                                .asBitmap().load(decodedString)
                                .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                                .into(holder.imgUserIcon);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                holder.imgUserIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
            }
            
            holder.viewOnlineStatus.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);
            
            if (!user.isOnline() && user.getLastSeen() > 0) {
                java.text.SimpleDateFormat sdfLastSeen = new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault());
                holder.textUserTime.setText("Vu le " + sdfLastSeen.format(new java.util.Date(user.getLastSeen())));
            }

            
            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));

            // Récupérer le dernier message en temps réel
            String convoId = generatePrivateConversationId(currentUserId, user.getUid());
            
            // Nettoyer l'ancien listener si la vue est recyclée
            if (listenerMap.containsKey(user.getUid())) {
                listenerMap.get(user.getUid()).remove();
            }

            ListenerRegistration registration = firestore.collection("conversations")
                    .document(convoId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null || snapshot.isEmpty()) {
                            holder.textUserEmail.setText("Appuyez pour discuter");
                            holder.textUserTime.setText("");
                            return;
                        }
                        Message lastMsg = snapshot.getDocuments().get(0).toObject(Message.class);
                        if (lastMsg != null) {
                            String prefix = lastMsg.getSenderId().equals(currentUserId) ? "Vous : " : "";
                            String textToShow;
                            if (lastMsg.getImageUrl() != null && !lastMsg.getImageUrl().isEmpty()) {
                                textToShow = "📷 Photo";
                            } else if (lastMsg.isAudio()) {
                                textToShow = "🎤 Message vocal";
                            } else if (lastMsg.getFileUrl() != null && !lastMsg.getFileUrl().isEmpty()) {
                                textToShow = "📄 " + (lastMsg.getFileName() != null ? lastMsg.getFileName() : "Fichier");
                            } else {
                                textToShow = com.enastic.chat.utils.EncryptionUtils.decrypt(lastMsg.getText());
                            }
                            holder.textUserEmail.setText(prefix + textToShow);
                            
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                            holder.textUserTime.setText(sdf.format(new java.util.Date(lastMsg.getTimestamp())));
                        }
                    });
            listenerMap.put(user.getUid(), registration);
        }
    }

    private String generatePrivateConversationId(String uid1, String uid2) {
        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    public void updateList(List<User> newList) {
        userList.clear();
        userList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {

        // +1 pour le Chat Global en première position
        return userList.size() + 1;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName, textUserEmail, textUserTime;
        ImageView imgUserIcon;
        View viewOnlineStatus;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.textUserName);
            textUserEmail = itemView.findViewById(R.id.textUserEmail);
            textUserTime = itemView.findViewById(R.id.textUserTime);
            imgUserIcon = itemView.findViewById(R.id.imgUserIcon);
            viewOnlineStatus = itemView.findViewById(R.id.viewOnlineStatus);
        }
    }
}
