package com.example.projekatmobilne.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.services.TaskService;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateStatusAdapter extends RecyclerView.Adapter<DateStatusAdapter.DateStatusViewHolder> {

    private List<DateStatusItem> items = new ArrayList<>();
    private final OnStatusChangeListener statusChangeListener;
    private OnDateRemoveListener onDateRemoveListener;
    private Task task;

    private final TaskService taskService = new TaskService();

    // ===================================================
    // INTERFEJSI
    // ===================================================

    public interface OnStatusChangeListener {
        void onStatusChanged(long dateTimestamp, TaskStatus newStatus);
    }

    public interface OnDateRemoveListener {
        void onDateRemove(long dateTimestamp);
    }

    // ===================================================
    // KONSTRUKTORI
    // ===================================================

    public DateStatusAdapter(OnStatusChangeListener statusListener) {
        this.statusChangeListener = statusListener;
    }

    public DateStatusAdapter(OnStatusChangeListener statusListener,
                             OnDateRemoveListener dateRemoveListener) {
        this.statusChangeListener = statusListener;
        this.onDateRemoveListener = dateRemoveListener;
    }

    // ===================================================
    // DATA
    // ===================================================

    public void setData(Task task) {
        this.task = task;
        this.items.clear();

        if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
            // Status kroz servis, ne direktno na modelu
            TaskStatus status = taskService.getStatusForDate(task, task.getExecutionTime());
            items.add(new DateStatusItem(task.getExecutionTime(), status));
        } else {
            if (task.getRecurringDates() != null) {
                for (Long timestamp : task.getRecurringDates()) {
                    TaskStatus status = taskService.getStatusForDate(task, timestamp);
                    items.add(new DateStatusItem(timestamp, status));
                }
            }
        }

        notifyDataSetChanged();
        Log.d("DATE_STATUS_ADAPTER", "Učitano " + items.size() + " datuma");
    }

    // ===================================================
    // RECYCLER
    // ===================================================

    @NonNull
    @Override
    public DateStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date_status, parent, false);
        return new DateStatusViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DateStatusViewHolder holder, int position) {
        DateStatusItem item = items.get(position);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat  = new SimpleDateFormat("EEEE", new Locale("sr", "RS"));

        holder.tvDateLabel.setText(dateFormat.format(new Date(item.timestamp)));
        holder.tvDateDay.setText(dayFormat.format(new Date(item.timestamp)));

        holder.tvStatusText.setText(translateStatus(item.status));
        updateStatusColors(holder, item.status);

        holder.itemView.setOnClickListener(v -> showStatusMenu(v, item.timestamp));
        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDateDialog(v, item.timestamp, item.status);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ===================================================
    // PRIVATE HELPER METODE
    // ===================================================

    private void showStatusMenu(View view, long timestamp) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add("Postavi: AKTIVAN");
        popup.getMenu().add("Postavi: URAĐENO");
        popup.getMenu().add("Postavi: OTKAZANO");

        if (task.getFrequencyType() == FrequencyType.RECURRING) {
            popup.getMenu().add("Postavi: PAUZIRANO");
        }

        popup.setOnMenuItemClickListener(item -> {
            String choice = item.getTitle().toString();
            TaskStatus newStatus;

            if (choice.contains("AKTIVAN"))       newStatus = TaskStatus.ACTIVE;
            else if (choice.contains("URAĐENO"))  newStatus = TaskStatus.DONE;
            else if (choice.contains("OTKAZANO")) newStatus = TaskStatus.CANCELLED;
            else                                  newStatus = TaskStatus.PAUSED;

            Log.d("DATE_STATUS_ADAPTER", "Status promenjen za datum: "
                    + new Date(timestamp) + " -> " + newStatus);

            if (statusChangeListener != null) {
                statusChangeListener.onStatusChanged(timestamp, newStatus);
            }
            return true;
        });
        popup.show();
    }

    private void showDeleteDateDialog(View view, long timestamp, TaskStatus status) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateStr = dateFormat.format(new Date(timestamp));

        // Validacija kroz servis
        String blockReason = new com.example.projekatmobilne.services.TaskValidationService()
                .canDeleteOccurrence(timestamp, status);

        if (blockReason != null) {
            new AlertDialog.Builder(view.getContext())
                    .setTitle("❌ Brisanje zabranjeno")
                    .setMessage(blockReason)
                    .setPositiveButton("U REDU", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            Log.d("DATE_STATUS_ADAPTER", "DELETE ZABRANJEN: " + blockReason);
            return;
        }

        // Izbrojaj koliko datuma će biti obrisano
        int futureCount = 0;
        for (DateStatusItem item : items) {
            if (item.timestamp >= timestamp) futureCount++;
        }

        String message;
        if (futureCount == 1) {
            message = "Da li želite da uklonite termin " + dateStr + " iz ponavljanja?";
        } else {
            message = "⚠️ UPOZORENJE: Biće uklonjeno " + futureCount + " termina!\n\n"
                    + "Uklanjanjem termina " + dateStr + " brišu se i svi termini posle njega.\n\n"
                    + "Da li želite da nastavite?";
        }

        new AlertDialog.Builder(view.getContext())
                .setTitle(futureCount == 1 ? "⚠️ Ukloni termin" : "⚠️ Ukloni " + futureCount + " termina")
                .setMessage(message)
                .setPositiveButton("UKLONI", (dialog, which) -> {
                    Log.d("DATE_STATUS_ADAPTER", "Uklanjam datum + buduće: " + dateStr);
                    if (onDateRemoveListener != null) {
                        onDateRemoveListener.onDateRemove(timestamp);
                    }
                })
                .setNegativeButton("OTKAŽI", null)
                .setIcon(android.R.drawable.ic_delete)
                .show();
    }

    private void updateStatusColors(DateStatusViewHolder holder, TaskStatus status) {
        String icon;
        int textColor;
        int bgColor;

        switch (status) {
            case DONE:
                icon = "✅"; textColor = Color.parseColor("#27AE60"); bgColor = Color.parseColor("#E8F5E9"); break;
            case CANCELLED:
                icon = "❌"; textColor = Color.parseColor("#E74C3C"); bgColor = Color.parseColor("#FFEBEE"); break;
            case PAUSED:
                icon = "⏸️"; textColor = Color.parseColor("#F39C12"); bgColor = Color.parseColor("#FFF3E0"); break;
            case UPCOMING:
                icon = "🔜"; textColor = Color.parseColor("#9B59B6"); bgColor = Color.parseColor("#F3E5F5"); break;
            default: // ACTIVE
                icon = "⭕"; textColor = Color.parseColor("#3498DB"); bgColor = Color.parseColor("#E3F2FD"); break;
        }

        holder.tvStatusIcon.setText(icon);
        holder.tvStatusText.setTextColor(textColor);
        holder.tvStatusText.setBackgroundColor(bgColor);
    }

    private String translateStatus(TaskStatus status) {
        if (status == null) return "AKTIVAN";
        switch (status) {
            case DONE:      return "URAĐENO";
            case PAUSED:    return "PAUZIRANO";
            case CANCELLED: return "OTKAZANO";
            case UPCOMING:  return "NADOLAZEĆI";
            default:        return "AKTIVAN";
        }
    }

    // ===================================================
    // VIEW HOLDER
    // ===================================================

    static class DateStatusViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateLabel, tvDateDay, tvStatusIcon, tvStatusText;
        MaterialCardView cardDateStatus;

        public DateStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateLabel   = itemView.findViewById(R.id.tvDateLabel);
            tvDateDay     = itemView.findViewById(R.id.tvDateDay);
            tvStatusIcon  = itemView.findViewById(R.id.tvStatusIcon);
            tvStatusText  = itemView.findViewById(R.id.tvStatusText);
            cardDateStatus = itemView.findViewById(R.id.cardDateStatus);
        }
    }

    // ===================================================
    // HELPER KLASA
    // ===================================================

    static class DateStatusItem {
        long timestamp;
        TaskStatus status;

        DateStatusItem(long timestamp, TaskStatus status) {
            this.timestamp = timestamp;
            this.status = status;
        }
    }
}