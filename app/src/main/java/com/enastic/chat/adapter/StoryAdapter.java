package com.enastic.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.enastic.chat.R;
import com.enastic.chat.model.Story;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private List<Story> storyList;
    private OnStoryClickListener listener;
    private String currentUserProfileUrl;

    public interface OnStoryClickListener {
        void onAddStoryClick();
        void onStoryClick(Story story);
    }

    public StoryAdapter(List<Story> storyList, String currentUserProfileUrl, OnStoryClickListener listener) {
        this.storyList = storyList;
        this.currentUserProfileUrl = currentUserProfileUrl;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        if (position == 0) {
            // Le premier élément est toujours "Ajouter une Story"
            holder.txtStoryUserName.setText("Votre Story");
            holder.imgAddStoryIcon.setVisibility(View.VISIBLE);
            holder.imgStoryUser.setBackgroundResource(0); // Pas d'anneau coloré
            
            if (currentUserProfileUrl != null && !currentUserProfileUrl.isEmpty()) {
                if (currentUserProfileUrl.length() > 100) { // Base64
                    try {
                        byte[] decodedString = android.util.Base64.decode(currentUserProfileUrl, android.util.Base64.DEFAULT);
                        Glide.with(holder.itemView.getContext()).load(decodedString).transform(new CircleCrop()).into(holder.imgStoryUser);
                    } catch (Exception e) {
                        Glide.with(holder.itemView.getContext()).load(currentUserProfileUrl).transform(new CircleCrop()).into(holder.imgStoryUser);
                    }
                } else {
                    Glide.with(holder.itemView.getContext()).load(currentUserProfileUrl).transform(new CircleCrop()).into(holder.imgStoryUser);
                }
            } else {
                holder.imgStoryUser.setImageResource(android.R.drawable.ic_menu_myplaces);
            }

            holder.itemView.setOnClickListener(v -> listener.onAddStoryClick());
        } else {
            // Les autres éléments sont les stories des autres
            Story story = storyList.get(position - 1);
            holder.txtStoryUserName.setText(story.getUserName());
            holder.imgAddStoryIcon.setVisibility(View.GONE);
            holder.imgStoryUser.setBackgroundResource(R.drawable.bg_story_ring); // Anneau coloré

            if (story.getUserProfileUrl() != null && !story.getUserProfileUrl().isEmpty()) {
                if (story.getUserProfileUrl().length() > 100) {
                    try {
                        byte[] decodedString = android.util.Base64.decode(story.getUserProfileUrl(), android.util.Base64.DEFAULT);
                        Glide.with(holder.itemView.getContext()).load(decodedString).transform(new CircleCrop()).into(holder.imgStoryUser);
                    } catch (Exception e) {
                        Glide.with(holder.itemView.getContext()).load(story.getUserProfileUrl()).transform(new CircleCrop()).into(holder.imgStoryUser);
                    }
                } else {
                    Glide.with(holder.itemView.getContext()).load(story.getUserProfileUrl()).transform(new CircleCrop()).into(holder.imgStoryUser);
                }
            } else {
                holder.imgStoryUser.setImageResource(android.R.drawable.ic_menu_myplaces);
            }

            holder.itemView.setOnClickListener(v -> listener.onStoryClick(story));
        }
    }

    @Override
    public int getItemCount() {
        return storyList.size() + 1; // +1 pour le bouton "Ajouter une Story"
    }

    public void updateCurrentUserProfileUrl(String url) {
        this.currentUserProfileUrl = url;
        notifyItemChanged(0);
    }

    static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgStoryUser, imgAddStoryIcon;
        TextView txtStoryUserName;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgStoryUser = itemView.findViewById(R.id.imgStoryUser);
            imgAddStoryIcon = itemView.findViewById(R.id.imgAddStoryIcon);
            txtStoryUserName = itemView.findViewById(R.id.txtStoryUserName);
        }
    }
}
