package com.example.projekatmobilne.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.FriendRequest;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    public interface RequestListener {
        void onAccept(FriendRequest request);
        void onDecline(FriendRequest request);
    }

    private List<FriendRequest> requests;
    private final RequestListener listener;

    public FriendRequestAdapter(List<FriendRequest> requests, RequestListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    public void updateRequests(List<FriendRequest> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(requests.get(position));
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        Button btnAccept, btnDecline;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvRequestUsername);
            btnAccept = itemView.findViewById(R.id.btnAcceptRequest);
            btnDecline = itemView.findViewById(R.id.btnDeclineRequest);
        }

        void bind(FriendRequest request) {
            tvUsername.setText(request.getFromUsername() + " želi da bude tvoj prijatelj");
            btnAccept.setOnClickListener(v -> listener.onAccept(request));
            btnDecline.setOnClickListener(v -> listener.onDecline(request));
        }
    }
}