package com.example.projekatmobilne.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.AllianceInvitation;

import java.util.List;

public class AllianceInvitationAdapter extends RecyclerView.Adapter<AllianceInvitationAdapter.InvitationViewHolder> {

    public interface InvitationListener {
        void onAccept(AllianceInvitation invitation);
        void onDecline(AllianceInvitation invitation);
    }

    private List<AllianceInvitation> invitations;
    private final InvitationListener listener;

    public AllianceInvitationAdapter(List<AllianceInvitation> invitations, InvitationListener listener) {
        this.invitations = invitations;
        this.listener = listener;
    }

    public void updateInvitations(List<AllianceInvitation> newInvitations) {
        this.invitations = newInvitations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        holder.bind(invitations.get(position));
    }

    @Override
    public int getItemCount() {
        return invitations != null ? invitations.size() : 0;
    }

    class InvitationViewHolder extends RecyclerView.ViewHolder {
        TextView tvInvitationText;
        Button btnAccept, btnDecline;

        InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInvitationText = itemView.findViewById(R.id.tvInvitationText);
            btnAccept = itemView.findViewById(R.id.btnAcceptInvitation);
            btnDecline = itemView.findViewById(R.id.btnDeclineInvitation);
        }

        void bind(AllianceInvitation invitation) {
            tvInvitationText.setText(invitation.getFromUsername()
                    + " te poziva u savez '" + invitation.getAllianceName() + "'");
            btnAccept.setOnClickListener(v -> listener.onAccept(invitation));
            btnDecline.setOnClickListener(v -> listener.onDecline(invitation));
        }
    }
}