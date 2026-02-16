package com.example.projekatmobilne.adapters;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList = new ArrayList<>();
    private OnTaskStatusChangeListener statusListener;
    private OnTaskLongClickListener longClickListener;
    private OnTaskClickListener clickListener;
    private List<Category> categories = new ArrayList<>();

    public interface OnTaskStatusChangeListener {
        void onStatusChanged(Task task, TaskStatus newStatus);
    }

    public interface OnTaskLongClickListener {
        void onTaskLongClick(Task task);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskAdapter(OnTaskStatusChangeListener statusListener,
                       OnTaskLongClickListener longClickListener,
                       OnTaskClickListener clickListener) {
        this.statusListener = statusListener;
        this.longClickListener = longClickListener;
        this.clickListener = clickListener;
    }

    public void setTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

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

        // Formatiranje vremena
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(task.getExecutionTime())));

        // Logika za sledeće ponavljanje
        if (task.getFrequencyType() == FrequencyType.RECURRING) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);

            Long nextDate = task.getNextOccurrence(cal.getTimeInMillis());
            if (nextDate != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
                holder.tvNextOccurrence.setText("Sledeće: " + dateFormat.format(new Date(nextDate)));
                holder.tvNextOccurrence.setVisibility(View.VISIBLE);
            } else {
                holder.tvNextOccurrence.setText("Završeno");
                holder.tvNextOccurrence.setVisibility(View.VISIBLE);
            }
        } else {
            holder.tvNextOccurrence.setVisibility(View.GONE);
        }

        // --- NOVI DEO: PRIKAZ STATUSA ---
        if (task.getStatus() != null) {
            holder.tvStatus.setText(task.getStatus().name());
            updateStatusColor(holder.tvStatus, task.getStatus());
        }

        // Klik na status otvara meni za promenu (umesto checkboxa)
        holder.tvStatus.setOnClickListener(v -> showStatusMenu(v, task));

        // Vizuelni efekat: Precrtan tekst ako je DONE
        if (task.getStatus() == TaskStatus.DONE) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1.0f);
        }

        // Postojeći click listeneri
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onTaskLongClick(task);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTaskClick(task);
        });


        // Pronađi boju kategorije
        String categoryColor = "#FFFFFF"; // Default bela
        for (Category c : categories) {
            if (c.getId().equals(task.getCategoryId())) {
                categoryColor = c.getColorHex(); // Uzimamo boju iz modela kategorije
                break;
            }
        }

        holder.cardView.setCardBackgroundColor(Color.parseColor(categoryColor));
    }


    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }
    // Pomoćna metoda za Popup meni statusa
    private void showStatusMenu(View view, Task task) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add("Postavi: AKTIVAN");
        popup.getMenu().add("Postavi: URAĐENO");
        popup.getMenu().add("Postavi: OTKAZANO");

        if (task.getFrequencyType() == FrequencyType.RECURRING) {
            popup.getMenu().add("Postavi: PAUZIRANO");
        }

        popup.setOnMenuItemClickListener(item -> {
            TaskStatus nextStatus;
            String choice = item.getTitle().toString();

            if (choice.contains("AKTIVAN")) nextStatus = TaskStatus.ACTIVE;
            else if (choice.contains("URAĐENO")) nextStatus = TaskStatus.DONE;
            else if (choice.contains("OTKAZANO")) nextStatus = TaskStatus.CANCELLED;
            else nextStatus = TaskStatus.PAUSED;

            if (statusListener != null) statusListener.onStatusChanged(task, nextStatus);
            return true;
        });
        popup.show();
    }

    // Pomoćna metoda za boje statusa
    private void updateStatusColor(TextView tv, TaskStatus status) {
        switch (status) {
            case ACTIVE: tv.setTextColor(Color.parseColor("#27AE60")); break;
            case DONE: tv.setTextColor(Color.GRAY); break;
            case CANCELLED: tv.setTextColor(Color.RED); break;
            case PAUSED: tv.setTextColor(Color.parseColor("#F39C12")); break;
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

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
            cardView = (com.google.android.material.card.MaterialCardView) itemView;// U XML-u promeni cbCompleted u tvTaskStatus (TextView)
        }
    }
}