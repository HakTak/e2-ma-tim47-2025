package com.example.projekatmobilne.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.example.projekatmobilne.viewModels.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_TASK = "task_obj";
    private TaskViewModel taskViewModel;
    private CategoryViewModel categoryViewModel; // DODATO
    private Task currentTask;

    public static TaskDetailFragment newInstance(Task task) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK, task);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_detail, container, false);

        currentTask = (Task) getArguments().getSerializable(ARG_TASK);

        // INICIJALIZACIJA VIEWMODEL-A
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class); // INICIJALIZOVANO

        // Povezivanje akcija
        v.findViewById(R.id.layoutStatusClick).setOnClickListener(view -> showStatusPopup(view));
        v.findViewById(R.id.btnDeleteTask).setOnClickListener(view -> confirmDeletion());
        v.findViewById(R.id.btnCloseDetail).setOnClickListener(view -> dismissFragment());
        v.findViewById(R.id.btnEditTask).setOnClickListener(view -> {
            Toast.makeText(getContext(), "Otvaranje ekrana za izmenu...", Toast.LENGTH_SHORT).show();
        });

        // Popunjavanje podataka
        updateUI(v);

        return v;
    }

    private void updateUI(View v) {
        if (currentTask == null || v == null) return;

        // 1. Osnovno
        ((TextView)v.findViewById(R.id.tvDetailTitle)).setText(currentTask.getTitle());
        ((TextView)v.findViewById(R.id.tvDetailDesc)).setText(currentTask.getDescription());
        ((TextView)v.findViewById(R.id.tvDetailXp)).setText("+" + currentTask.getTotalXp() + " XP");

        // 2. Status
        TextView tvStatus = v.findViewById(R.id.tvDetailStatus);
        tvStatus.setText(translateStatus(currentTask.getStatus()));
        tvStatus.setTextColor(getStatusColor(currentTask.getStatus()));

        // 3. Vreme izvršavanja (HH:mm)
        TextView tvExecTime = v.findViewById(R.id.tvDetailExecutionTime);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvExecTime.setText(timeFormat.format(new Date(currentTask.getExecutionTime())));

        // 4. Planirani datumi ponavljanja
        TextView tvDates = v.findViewById(R.id.tvDetailDates);
        if (currentTask.getRecurringDates() != null && !currentTask.getRecurringDates().isEmpty()) {
            StringBuilder sb = new StringBuilder("Planirani termini:\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            for (Long timestamp : currentTask.getRecurringDates()) {
                sb.append("• ").append(dateFormat.format(new Date(timestamp))).append("  ");
            }
            tvDates.setText(sb.toString());
        } else {
            tvDates.setText("Jednokratni zadatak");
        }

        // 5. Naziv Kategorije (Traženje imena preko ID-ja)
        TextView tvCat = v.findViewById(R.id.tvDetailCategory);
        tvCat.setText("Učitavanje...");
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                for (Category c : categories) {
                    if (c.getId().equals(currentTask.getCategoryId())) {
                        tvCat.setText(c.getName());
                        return;
                    }
                }
                tvCat.setText("Ostalo");
            }
        });

        // 6. Težina, Bitnost i Učestalost
        ((TextView)v.findViewById(R.id.tvDetailDifficulty)).setText(translateDifficulty(currentTask.getDifficulty().name()));
        ((TextView)v.findViewById(R.id.tvDetailImportance)).setText(translateImportance(currentTask.getImportance().name()));

        String freq = currentTask.getFrequencyType() == FrequencyType.ONE_TIME ? "Jednokratno" : "Ponavljajuće";
        ((TextView)v.findViewById(R.id.tvDetailFrequency)).setText(freq);
    }

    private void showStatusPopup(View view) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenu().add("Postavi: AKTIVAN");
        popup.getMenu().add("Postavi: URAĐENO");
        popup.getMenu().add("Postavi: OTKAZANO");
        if (currentTask.getFrequencyType() == FrequencyType.RECURRING) {
            popup.getMenu().add("Postavi: PAUZIRANO");
        }

        popup.setOnMenuItemClickListener(item -> {
            TaskStatus newStatus;
            String choice = item.getTitle().toString();
            if (choice.contains("AKTIVAN")) newStatus = TaskStatus.ACTIVE;
            else if (choice.contains("URAĐENO")) newStatus = TaskStatus.DONE;
            else if (choice.contains("OTKAZANO")) newStatus = TaskStatus.CANCELLED;
            else newStatus = TaskStatus.PAUSED;

            currentTask.setStatus(newStatus);
            taskViewModel.updateTask(currentTask);
            updateUI(getView());
            return true;
        });
        popup.show();
    }

    private void confirmDeletion() {
        new AlertDialog.Builder(getContext())
                .setTitle("Brisanje zadatka")
                .setMessage("Da li ste sigurni?")
                .setPositiveButton("Da", (dialog, which) -> {
                    taskViewModel.deleteTask(currentTask.getId());
                    dismissFragment();
                })
                .setNegativeButton("Ne", null).show();
    }

    private void dismissFragment() {
        getParentFragmentManager().beginTransaction().remove(this).commit();
    }

    // PREVODI
    private String translateStatus(TaskStatus status) {
        if (status == null) return "AKTIVAN";
        switch (status) {
            case DONE: return "URAĐENO";
            case PAUSED: return "PAUZIRANO";
            case CANCELLED: return "OTKAZANO";
            default: return "AKTIVAN";
        }
    }

    private int getStatusColor(TaskStatus status) {
        if (status == null) return Color.parseColor("#27AE60");
        switch (status) {
            case DONE: return Color.GRAY;
            case CANCELLED: return Color.RED;
            case PAUSED: return Color.parseColor("#F39C12");
            default: return Color.parseColor("#27AE60");
        }
    }

    private String translateDifficulty(String diff) {
        switch (diff) {
            case "VERY_EASY": return "Veoma lako";
            case "EASY": return "Lako";
            case "MEDIUM": return "Srednje";
            case "HARD": return "Teško";
            case "EXTREME": return "Ekstremno";
            default: return diff;
        }
    }

    private String translateImportance(String imp) {
        switch (imp) {
            case "LOW": return "Nisko";
            case "NORMAL": return "Normalno";
            case "HIGH": return "Važno";
            case "CRITICAL": return "Kritično";
            case "SPECIAL": return "Specijalno";
            default: return imp;
        }
    }
}