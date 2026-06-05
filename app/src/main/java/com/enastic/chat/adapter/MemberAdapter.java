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
import com.enastic.chat.model.User;

import java.util.List;

/**
 * Adapter simple pour afficher les membres d'un groupe dans GroupInfoActivity.
 */
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    public interface OnMemberClickListener {
        void onMemberClick(User user);
        void onMemberLongClick(User user);
    }

    private final List<User> memberList;
    private OnMemberClickListener listener;

    public MemberAdapter(List<User> memberList) {
        this.memberList = memberList;
    }

    public void setOnMemberClickListener(OnMemberClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = memberList.get(position);

        holder.textUserName.setText(user.getName());
        holder.textUserEmail.setText(user.getEmail());
        
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .transform(new CircleCrop())
                    .into(holder.imgUserIcon);
        } else {
            holder.imgUserIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        // Cacher les éléments inutiles pour la liste des membres
        holder.textUserTime.setVisibility(View.GONE);
        holder.viewOnlineStatus.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMemberClick(user);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onMemberLongClick(user);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName, textUserEmail, textUserTime;
        ImageView imgUserIcon;
        View viewOnlineStatus;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.textUserName);
            textUserEmail = itemView.findViewById(R.id.textUserEmail);
            textUserTime = itemView.findViewById(R.id.textUserTime);
            imgUserIcon = itemView.findViewById(R.id.imgUserIcon);
            viewOnlineStatus = itemView.findViewById(R.id.viewOnlineStatus);
        }
    }
}
