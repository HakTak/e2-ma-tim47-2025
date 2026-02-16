package com.example.projekatmobilne.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.SlotViewHolder> {

    private List<Task> tasks = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public CalendarTaskAdapter(OnTaskClickListener listener) {
        this.listener = listener;
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
        holder.tvStatus.setText(task.getStatus().name());

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

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
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