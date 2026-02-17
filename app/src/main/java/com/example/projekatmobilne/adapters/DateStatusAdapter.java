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
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.*;

public class DateStatusAdapter extends RecyclerView.Adapter<DateStatusAdapter.DateStatusViewHolder> {

    private List<DateStatusItem> items = new ArrayList<>();
    private OnStatusChangeListener statusChangeListener;
    private Task task;

    public interface OnDateRemoveListener {
        void onDateRemove(long dateTimestamp);
    }

    private OnDateRemoveListener onDateRemoveListener;

    public interface OnStatusChangeListener {
        void onStatusChanged(long dateTimestamp, TaskStatus newStatus);
    }

    public DateStatusAdapter(OnStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    public DateStatusAdapter(OnStatusChangeListener statusListener, OnDateRemoveListener dateRemoveListener) {
        this.statusChangeListener = statusListener;
        this.onDateRemoveListener = dateRemoveListener;
    }

    public void setData(Task task) {
        this.task = task;
        this.items.clear();

        if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
            // Jednokratni task - samo jedan datum
            TaskStatus status = task.getStatusForDate(task.getExecutionTime());
            items.add(new DateStatusItem(task.getExecutionTime(), status));
        } else {
            // Ponavljajući task - svi datumi iz recurringDates
            if (task.getRecurringDates() != null) {
                for (Long timestamp : task.getRecurringDates()) {
                    TaskStatus status = task.getStatusForDate(timestamp);
                    items.add(new DateStatusItem(timestamp, status));
                }
            }
        }

        notifyDataSetChanged();
        Log.d("DATE_STATUS_ADAPTER", "Učitano " + items.size() + " datuma");
    }

    @NonNull
    @Override
    public DateStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_status, parent, false);
        return new DateStatusViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DateStatusViewHolder holder, int position) {
        DateStatusItem item = items.get(position);

        // Formatiranje datuma
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("sr", "RS"));

        holder.tvDateLabel.setText(dateFormat.format(new Date(item.timestamp)));
        holder.tvDateDay.setText(dayFormat.format(new Date(item.timestamp)));

        // Status
        holder.tvStatusText.setText(translateStatus(item.status));
        updateStatusColors(holder, item.status);

        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDateDialog(v, item.timestamp, item.status);
            return true;
        });

        // Klik na ceo item otvara meni za promenu statusa
        holder.itemView.setOnClickListener(v -> showStatusMenu(v, item.timestamp));
    }

    private void showStatusMenu(View view, long timestamp) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add("Postavi: AKTIVAN");
        popup.getMenu().add("Postavi: URAĐENO");
        popup.getMenu().add("Postavi: OTKAZANO");

        if (task.getFrequencyType() == FrequencyType.RECURRING) {
            popup.getMenu().add("Postavi: PAUZIRANO");
        }

        popup.setOnMenuItemClickListener(item -> {
            TaskStatus newStatus;
            String choice = item.getTitle().toString();

            if (choice.contains("AKTIVAN")) newStatus = TaskStatus.ACTIVE;
            else if (choice.contains("URAĐENO")) newStatus = TaskStatus.DONE;
            else if (choice.contains("OTKAZANO")) newStatus = TaskStatus.CANCELLED;
            else newStatus = TaskStatus.PAUSED;

            Log.d("DATE_STATUS_ADAPTER", "Status promenjen za datum: " + new Date(timestamp) + " -> " + newStatus);

            if (statusChangeListener != null) {
                statusChangeListener.onStatusChanged(timestamp, newStatus);
            }
            return true;
        });
        popup.show();
    }

    private void updateStatusColors(DateStatusViewHolder holder, TaskStatus status) {
        String icon;
        int textColor;
        int bgColor;

        switch (status) {
            case DONE:
                icon = "✅";
                textColor = Color.parseColor("#27AE60");
                bgColor = Color.parseColor("#E8F5E9");
                break;
            case CANCELLED:
                icon = "❌";
                textColor = Color.parseColor("#E74C3C");
                bgColor = Color.parseColor("#FFEBEE");
                break;
            case PAUSED:
                icon = "⏸️";
                textColor = Color.parseColor("#F39C12");
                bgColor = Color.parseColor("#FFF3E0");
                break;
            case UPCOMING:
                icon = "🔜";
                textColor = Color.parseColor("#9B59B6");
                bgColor = Color.parseColor("#F3E5F5");
                break;
            default: // ACTIVE
                icon = "⭕";
                textColor = Color.parseColor("#3498DB");
                bgColor = Color.parseColor("#E3F2FD");
                break;
        }

        holder.tvStatusIcon.setText(icon);
        holder.tvStatusText.setTextColor(textColor);
        holder.tvStatusText.setBackgroundColor(bgColor);
    }


    private String translateStatus(TaskStatus status) {
        if (status == null) return "AKTIVAN";
        switch (status) {
            case DONE: return "URAĐENO";
            case PAUSED: return "PAUZIRANO";
            case CANCELLED: return "OTKAZANO";
            case UPCOMING: return "NADOLAZEĆI";
            default: return "AKTIVAN";
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DateStatusViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateLabel, tvDateDay, tvStatusIcon, tvStatusText;
        MaterialCardView cardDateStatus;

        public DateStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateLabel = itemView.findViewById(R.id.tvDateLabel);
            tvDateDay = itemView.findViewById(R.id.tvDateDay);
            tvStatusIcon = itemView.findViewById(R.id.tvStatusIcon);
            tvStatusText = itemView.findViewById(R.id.tvStatusText);
            cardDateStatus = itemView.findViewById(R.id.cardDateStatus);
        }
    }
    /**
     * AŽURIRANA METODA: Dijalog sa upozorenjom o brisanju svih budućih datuma
     */
    private void showDeleteDateDialog(View view, long timestamp, TaskStatus status) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateStr = dateFormat.format(new Date(timestamp));

        // >>> PROVERA: Da li je datum DONE? <
        if (status == TaskStatus.DONE) {
            // ❌ ZABRANJEN DELETE
            new AlertDialog.Builder(view.getContext())
                    .setTitle("❌ Brisanje zabranjeno")
                    .setMessage("Ne možete obrisati završen termin (" + dateStr + ").\n\nOvo čuva vašu istoriju i zarađeni XP.")
                    .setPositiveButton("U REDU", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            Log.d("DATE_STATUS_ADAPTER", "DELETE ZABRANJEN - Datum " + dateStr + " je DONE");
            return;
        }

        // >>> IZBROJAJ KOLIKO DATUMA ĆE SE OBRISATI <
        int futureCount = 0;
        for (DateStatusItem item : items) {
            if (item.timestamp >= timestamp) {
                futureCount++;
            }
        }

        // >>> PRIKAŽI UPOZORENJE SA BROJEM <
        String message;
        if (futureCount == 1) {
            message = "Da li želite da uklonite termin " + dateStr + " iz ponavljanja?";
        } else {
            message = "⚠️ UPOZORENJE: Biće uklonjeno " + futureCount + " termina!\n\n" +
                    "Uklanjanjem termina " + dateStr + " brišu se i svi termini posle njega.\n\n" +
                    "Da li želite da nastavite?";
        }

        // ✅ DOZVOLJENO - Prikaži potvrdu
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

    // Pomoćna klasa za držanje podataka
    static class DateStatusItem {
        long timestamp;
        TaskStatus status;

        DateStatusItem(long timestamp, TaskStatus status) {
            this.timestamp = timestamp;
            this.status = status;
        }
    }

}