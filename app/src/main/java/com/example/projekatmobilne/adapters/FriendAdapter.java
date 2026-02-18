package com.example.projekatmobilne.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.User;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface FriendListener {
        void onViewProfile(User user);
        void onInviteToAlliance(User user);
    }

    private List<User> friends;
    private final FriendListener listener;
    private boolean showInviteButton = false;

    public FriendAdapter(List<User> friends, FriendListener listener) {
        this.friends = friends;
        this.listener = listener;
    }

    public void setShowInviteButton(boolean show) {
        this.showInviteButton = show;
        notifyDataSetChanged();
    }

    public void updateFriends(List<User> newFriends) {
        this.friends = newFriends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User user = friends.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return friends != null ? friends.size() : 0;
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUsername, tvLevel, tvTitle;
        Button btnProfile, btnInvite;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgFriendAvatar);
            tvUsername = itemView.findViewById(R.id.tvFriendUsername);
            tvLevel = itemView.findViewById(R.id.tvFriendLevel);
            tvTitle = itemView.findViewById(R.id.tvFriendTitle);
            btnProfile = itemView.findViewById(R.id.btnViewProfile);
            btnInvite = itemView.findViewById(R.id.btnInviteToAlliance);
        }

        void bind(User user) {
            tvUsername.setText(user.getUsername());
            tvLevel.setText("Nivo " + user.getLevel());
            tvTitle.setText(user.getTitle() != null ? user.getTitle() : "");

            // Avatar
            setAvatar(imgAvatar, user.getAvatar());

            btnProfile.setOnClickListener(v -> listener.onViewProfile(user));

            if (showInviteButton) {
                btnInvite.setVisibility(View.VISIBLE);
                btnInvite.setOnClickListener(v -> listener.onInviteToAlliance(user));
            } else {
                btnInvite.setVisibility(View.GONE);
            }
        }

        private void setAvatar(ImageView imageView, String avatar) {
            if (avatar == null) return;
            switch (avatar) {
                case "avatar_1": imageView.setImageResource(R.drawable.avatar_1); break;
                case "avatar_2": imageView.setImageResource(R.drawable.avatar_2); break;
                case "avatar_3": imageView.setImageResource(R.drawable.avatar_3); break;
                case "avatar_4": imageView.setImageResource(R.drawable.avatar_4); break;
                case "avatar_5": imageView.setImageResource(R.drawable.avatar_5); break;
                default: imageView.setImageResource(R.drawable.avatar_1); break;
            }
        }
    }
}