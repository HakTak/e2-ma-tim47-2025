package com.example.projekatmobilne.adapters;

import android.graphics.Color;
import android.graphics.Paint;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    private final OnTaskStatusChangeListener statusListener;
    private final OnTaskLongClickListener longClickListener;
    private final OnTaskClickListener clickListener;

    private final TaskService taskService = new TaskService();

    // ===================================================
    // INTERFEJSI
    // ===================================================

    public interface OnTaskStatusChangeListener {
        void onStatusChanged(Task task, TaskStatus newStatus, long dateContext);
    }

    public interface OnTaskLongClickListener {
        void onTaskLongClick(Task task);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    // ===================================================
    // KONSTRUKTOR
    // ===================================================

    public TaskAdapter(OnTaskStatusChangeListener statusListener,
                       OnTaskLongClickListener longClickListener,
                       OnTaskClickListener clickListener) {
        this.statusListener = statusListener;
        this.longClickListener = longClickListener;
        this.clickListener = clickListener;
    }

    // ===================================================
    // DATA
    // ===================================================

    public void setTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    // ===================================================
    // RECYCLER
    // ===================================================

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvXp.setText("+" + task.getTotalXp() + " XP");

        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(timeSdf.format(new Date(task.getExecutionTime())));

        long dateContext = taskService.getDateContext(task);

        if (task.getFrequencyType() == FrequencyType.RECURRING) {
            Long nextDate = task.getNextOccurrence(TaskService.getStartOfToday());
            if (nextDate != null) {
                SimpleDateFormat dateSdf = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
                holder.tvNextOccurrence.setText("Sledeće: " + dateSdf.format(new Date(nextDate)));
            } else {
                holder.tvNextOccurrence.setText("Završeno");
            }
            holder.tvNextOccurrence.setVisibility(View.VISIBLE);
        } else {
            holder.tvNextOccurrence.setVisibility(View.GONE);
        }

        TaskStatus currentStatus = taskService.getStatusForDate(task, dateContext);

        holder.tvStatus.setText(translateStatus(currentStatus));
        updateStatusColor(holder.tvStatus, currentStatus);
        holder.tvStatus.setOnClickListener(v -> showStatusMenu(v, task, dateContext, currentStatus));

        // Vizuelni efekti
        if (currentStatus == TaskStatus.DONE) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
        } else if (currentStatus == TaskStatus.FAILED) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.3f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1.0f);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onTaskLongClick(task);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTaskClick(task);
        });

        holder.cardView.setCardBackgroundColor(getCategoryColor(task.getCategoryId()));
    }

    @Override
    public int getItemCount() {
        return taskList.size();
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
            TaskStatus nextStatus;

            if (choice.contains("AKTIVAN"))       nextStatus = TaskStatus.ACTIVE;
            else if (choice.contains("URAĐENO"))  nextStatus = TaskStatus.DONE;
            else if (choice.contains("OTKAZANO")) nextStatus = TaskStatus.CANCELLED;
            else                                  nextStatus = TaskStatus.PAUSED;

            // VALIDACIJA PRE PROMENE STATUSA
            String validationError = taskService.canChangeStatus(task, dateContext, currentStatus, nextStatus);
            if (validationError != null) {
                Toast.makeText(view.getContext(), validationError, Toast.LENGTH_LONG).show();
                return true;
            }

            // Promeni status
            if (statusListener != null) statusListener.onStatusChanged(task, nextStatus, dateContext);

            // Ako je DONE → dodaj XP
            if (nextStatus == TaskStatus.DONE) {
                awardXPForTask(view.getContext(), task);
            }

            return true;
        });

        popup.show();
    }

    // Nova helper metoda
    private void awardXPForTask(android.content.Context context, Task task) {
        com.example.projekatmobilne.utils.SharedPrefsManager prefsManager =
                new com.example.projekatmobilne.utils.SharedPrefsManager(context);
        String userId = prefsManager.getUserId();

        if (userId != null) {
            // Kreraj XP tracking servis
            com.example.projekatmobilne.services.XPTrackingService xpTracker =
                    new com.example.projekatmobilne.services.XPTrackingService(context);

            // Izračunaj koliko XP zaista dobija
            com.example.projekatmobilne.services.XPTrackingService.XPResult result =
                    xpTracker.calculateAwardedXP(task.getDifficulty(), task.getImportance());

            if (result.hasEarnedXP()) {
                com.example.projekatmobilne.viewModels.UserViewModel userViewModel =
                        new androidx.lifecycle.ViewModelProvider(
                                (androidx.fragment.app.FragmentActivity) context
                        ).get(com.example.projekatmobilne.viewModels.UserViewModel.class);

                userViewModel.addXP(userId, result.totalXP);

                Toast.makeText(context,
                        "+" + result.getBreakdown(),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context,
                        "Dnevni/nedeljni/mesečni limit dostignut - 0 XP",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateStatusColor(TextView tv, TaskStatus status) {
        switch (status) {
            case ACTIVE:   tv.setTextColor(Color.parseColor("#27AE60")); break;
            case DONE:     tv.setTextColor(Color.GRAY);                  break;
            case CANCELLED:tv.setTextColor(Color.RED);                   break;
            case PAUSED:   tv.setTextColor(Color.parseColor("#F39C12")); break;
            case UPCOMING: tv.setTextColor(Color.parseColor("#9B59B6")); break;
            case FAILED:   tv.setTextColor(Color.parseColor("#E74C3C")); break;
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

    private int getCategoryColor(String categoryId) {
        for (Category c : categories) {
            if (c.getId().equals(categoryId)) {
                try {
                    return Color.parseColor(c.getColorHex());
                } catch (Exception e) {
                    return Color.WHITE;
                }
            }
        }
        return Color.WHITE;
    }

    // ===================================================
    // VIEW HOLDER
    // ===================================================

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.card.MaterialCardView cardView;
        TextView tvTitle, tvTime, tvXp, tvNextOccurrence, tvStatus;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTime = itemView.findViewById(R.id.tvTaskTime);
            tvXp = itemView.findViewById(R.id.tvTaskXp);
            tvNextOccurrence = itemView.findViewById(R.id.tvNextOccurrence);
            tvStatus = itemView.findViewById(R.id.tvTaskStatus);
            cardView = (com.google.android.material.card.MaterialCardView) itemView;
        }
    }
}