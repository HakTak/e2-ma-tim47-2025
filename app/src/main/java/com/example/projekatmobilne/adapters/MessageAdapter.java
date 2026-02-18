package com.example.projekatmobilne.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.AllianceMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<AllianceMessage> messages;
    private final String currentUserId;

    public MessageAdapter(List<AllianceMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    public void updateMessages(List<AllianceMessage> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        AllianceMessage message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AllianceMessage message = messages.get(position);
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(message.getTimestamp()));

        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message, time);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).bind(message, time);
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }

        void bind(AllianceMessage message, String time) {
            tvMessage.setText(message.getMessage());
            tvTime.setText(time);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvMessage, tvTime;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvSenderUsername);
            tvMessage = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }

        void bind(AllianceMessage message, String time) {
            tvUsername.setText(message.getSenderUsername());
            tvMessage.setText(message.getMessage());
            tvTime.setText(time);
        }
    }
}