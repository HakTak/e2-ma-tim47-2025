package com.example.projekatmobilne.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.services.TaskService;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.SlotViewHolder> {

    private List<Task> tasks = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    private final OnTaskClickListener clickListener;
    private final OnTaskStatusChangeListener statusChangeListener;
    private final DateProvider dateProvider;

    private final TaskService taskService = new TaskService();

    // ===================================================
    // INTERFEJSI
    // ===================================================

    public interface DateProvider {
        long getSelectedDate();
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnTaskStatusChangeListener {
        void onStatusChanged(Task task, TaskStatus newStatus, long dateContext);
    }

    // ===================================================
    // KONSTRUKTOR
    // ===================================================

    public CalendarTaskAdapter(OnTaskClickListener clickListener,
                               DateProvider dateProvider,
                               OnTaskStatusChangeListener statusChangeListener) {
        this.clickListener = clickListener;
        this.dateProvider = dateProvider;
        this.statusChangeListener = statusChangeListener;
    }

    // ===================================================
    // DATA
    // ===================================================

    public void setData(List<Task> tasks, List<Category> categories) {
        this.tasks = tasks;
        this.categories = categories;
        notifyDataSetChanged();
    }

    // ===================================================
    // RECYCLER
    // ===================================================

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

        long selectedDate = dateProvider.getSelectedDate();
        TaskStatus currentStatus = taskService.getStatusForDate(task, selectedDate);

        holder.tvStatus.setText(translateStatus(currentStatus));
        updateStatusColor(holder.tvStatus, currentStatus);
        Log.d("CALENDAR_ADAPTER", "Status za " + task.getTitle() + " na datum "
                + new Date(selectedDate) + " = " + currentStatus);

        holder.tvStatus.setOnClickListener(v -> showStatusMenu(v, task, selectedDate, currentStatus));

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(task.getExecutionTime())));

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

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    // ===================================================
    // PRIVATE HELPER METODE
    // ===================================================

    private void showStatusMenu(View view, Task task, long dateContext, TaskStatus currentStatus) {
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

            // VALIDACIJA PRE PROMENE STATUSA
            String validationError = taskService.canChangeStatus(task, dateContext, currentStatus, newStatus);
            if (validationError != null) {
                Toast.makeText(view.getContext(), validationError, Toast.LENGTH_LONG).show();
                return true;
            }

            Log.d("CALENDAR_ADAPTER", "Task: " + task.getTitle()
                    + " | Datum: " + new Date(dateContext)
                    + " | Novi status: " + newStatus);

            if (statusChangeListener != null) {
                statusChangeListener.onStatusChanged(task, newStatus, dateContext);
            }
            return true;
        });

        popup.show();
    }

    private void updateStatusColor(TextView tv, TaskStatus status) {
        switch (status) {
            case ACTIVE:    tv.setTextColor(Color.parseColor("#27AE60")); break;
            case DONE:      tv.setTextColor(Color.GRAY);                  break;
            case CANCELLED: tv.setTextColor(Color.RED);                   break;
            case PAUSED:    tv.setTextColor(Color.parseColor("#F39C12")); break;
            case UPCOMING:  tv.setTextColor(Color.parseColor("#9B59B6")); break;
            case FAILED:    tv.setTextColor(Color.parseColor("#E74C3C")); break;
        }
    }

    private String translateStatus(TaskStatus status) {
        switch (status) {
            case ACTIVE:    return "AKTIVAN";
            case DONE:      return "URAĐENO";
            case CANCELLED: return "OTKAZANO";
            case PAUSED:    return "PAUZIRANO";
            case UPCOMING:  return "NADOLAZEĆI";
            case FAILED:    return "NEUSPEŠAN";
            default:        return status.name();
        }
    }

    // ===================================================
    // VIEW HOLDER
    // ===================================================

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvStatus;
        MaterialCardView card;

        public SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime   = itemView.findViewById(R.id.tvSlotTime);
            tvTitle  = itemView.findViewById(R.id.tvSlotTitle);
            tvStatus = itemView.findViewById(R.id.tvSlotStatus);
            card     = itemView.findViewById(R.id.cardTaskSlot);
        }
    }
}