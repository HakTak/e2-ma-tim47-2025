package com.example.projekatmobilne.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.enums.FrequencyType;
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

    // Interfejsi za komunikaciju sa Activity-jem
    public interface OnTaskStatusChangeListener {
        void onStatusChanged(Task task, boolean isCompleted);
    }

    public interface OnTaskLongClickListener {
        void onTaskLongClick(Task task);
    }

    private OnTaskClickListener clickListener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskAdapter(OnTaskStatusChangeListener statusListener,
                       OnTaskLongClickListener longClickListener,
                       OnTaskClickListener clickListener) { // DODATO
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

        // Formatiranje vremena (long -> HH:mm)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(task.getExecutionTime())));

        if (task.getFrequencyType() == FrequencyType.RECURRING) {
            // Uzimamo trenutno vreme (početak dana) za poređenje
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
            // Ako je jednokratni, sakrivamo polje
            holder.tvNextOccurrence.setVisibility(View.GONE);
        }

        // Checkbox status (bez okidanja listenera dok setujemo inicijalno)
        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(task.isCompleted());

        // Vizuelni efekat: Precrtan tekst ako je task završen
        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1.0f);
        }

        // Listener za promenu statusa (Checkbox)
        holder.cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (statusListener != null) {
                statusListener.onStatusChanged(task, isChecked);
            }
        });

        // Listener za brisanje (Dugi klik)
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onTaskLongClick(task);
            }
            return true;
        });

        // DODAJ KLIK NA CELU STAVKU
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvXp, tvNextOccurrence;
        CheckBox cbCompleted;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTime = itemView.findViewById(R.id.tvTaskTime);
            tvXp = itemView.findViewById(R.id.tvTaskXp);
            tvNextOccurrence = itemView.findViewById(R.id.tvNextOccurrence); // DODATO
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
        }
    }


}