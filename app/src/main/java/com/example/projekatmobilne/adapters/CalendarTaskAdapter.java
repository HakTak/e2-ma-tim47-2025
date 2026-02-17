package com.example.projekatmobilne.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.SlotViewHolder> {

    private List<Task> tasks = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private OnTaskClickListener clickListener;

    // >>> NOVO: Dodaj callback za promenu statusa <
    private OnTaskStatusChangeListener statusChangeListener;

    public interface DateProvider {
        long getSelectedDate();
    }

    private DateProvider dateProvider;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    // >>> NOVI INTERFACE: Za promenu statusa <
    public interface OnTaskStatusChangeListener {
        void onStatusChanged(Task task, TaskStatus newStatus, long dateContext);
    }

    // >>> IZMENJENO: Dodaj statusChangeListener parametar <
    public CalendarTaskAdapter(OnTaskClickListener clickListener,
                               DateProvider dateProvider,
                               OnTaskStatusChangeListener statusChangeListener) {
        this.clickListener = clickListener;
        this.dateProvider = dateProvider;
        this.statusChangeListener = statusChangeListener;
    }

    public void setData(List<Task> tasks, List<Category> categories) {
        this.tasks = tasks;
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_slot, parent, false);
        return new SlotViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvTitle.setText(task.getTitle());

        // Uzmi status za selektovani datum iz kalendara
        long selectedDate = dateProvider.getSelectedDate();
        TaskStatus currentStatus = task.getStatusForDate(selectedDate);

        holder.tvStatus.setText(currentStatus.name());
        updateStatusColor(holder.tvStatus, currentStatus);
        Log.d("CALENDAR_ADAPTER", "Status za " + task.getTitle() + " na datum " + new Date(selectedDate) + " = " + currentStatus);

        // >>> NOVO: Dodaj onClick na status TextView <
        holder.tvStatus.setOnClickListener(v -> showStatusMenu(v, task, selectedDate));

        // Formatiranje vremena (HH:mm)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(task.getExecutionTime())));

        // Boja na osnovu kategorije
        String colorHex = "#B2BEC3";
        for (Category c : categories) {
            if (c.getId().equals(task.getCategoryId())) {
                colorHex = c.getColorHex();
                break;
            }
        }
        holder.card.setCardBackgroundColor(Color.parseColor(colorHex));

        holder.itemView.setOnClickListener(v -> clickListener.onTaskClick(task));
    }

    // >>> NOVA METODA: Prikaži popup meni za promenu statusa <
    private void showStatusMenu(View view, Task task, long dateContext) {
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

            Log.d("CALENDAR_ADAPTER", "=== Promena statusa iz kalendara ===");
            Log.d("CALENDAR_ADAPTER", "Task: " + task.getTitle());
            Log.d("CALENDAR_ADAPTER", "Datum: " + new Date(dateContext));
            Log.d("CALENDAR_ADAPTER", "Novi status: " + newStatus);

            // Pozovi callback
            if (statusChangeListener != null) {
                statusChangeListener.onStatusChanged(task, newStatus, dateContext);
            }
            return true;
        });
        popup.show();
    }

    // >>> NOVA METODA: Boje statusa <
    private void updateStatusColor(TextView tv, TaskStatus status) {
        switch (status) {
            case ACTIVE: tv.setTextColor(Color.parseColor("#27AE60")); break;
            case DONE: tv.setTextColor(Color.GRAY); break;
            case CANCELLED: tv.setTextColor(Color.RED); break;
            case PAUSED: tv.setTextColor(Color.parseColor("#F39C12")); break;
            case UPCOMING: tv.setTextColor(Color.parseColor("#9B59B6")); break;
        }
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvStatus;
        MaterialCardView card;

        public SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvSlotTime);
            tvTitle = itemView.findViewById(R.id.tvSlotTitle);
            tvStatus = itemView.findViewById(R.id.tvSlotStatus);
            card = itemView.findViewById(R.id.cardTaskSlot);
        }
    }
}