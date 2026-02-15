package com.example.projekatmobilne.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.viewModels.CategoryViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_TASK = "task_obj";
    private CategoryViewModel categoryViewModel;
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

        Task task = (Task) getArguments().getSerializable(ARG_TASK);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        TextView tvTitle = v.findViewById(R.id.tvDetailTitle);
        TextView tvDesc = v.findViewById(R.id.tvDetailDesc);
        TextView tvXp = v.findViewById(R.id.tvDetailXp);
        TextView tvFreq = v.findViewById(R.id.tvDetailFrequency);
        TextView tvDates = v.findViewById(R.id.tvDetailDates);
        Button btnClose = v.findViewById(R.id.btnCloseDetail);
        TextView tvStatus = v.findViewById(R.id.tvDetailStatus);
        TextView tvCategory = v.findViewById(R.id.tvDetailCategory);
        TextView tvDifficulty = v.findViewById(R.id.tvDetailDifficulty);
        TextView tvImportance = v.findViewById(R.id.tvDetailImportance);

        if (task != null) {
            tvTitle.setText(task.getTitle());
            tvDesc.setText(task.getDescription() != null && !task.getDescription().isEmpty() ? task.getDescription() : "Nema opisa");
            tvXp.setText("Vrednost: " + task.getTotalXp() + " XP");
            tvFreq.setText("Učestalost: " + task.getFrequencyType().name());
            tvDifficulty.setText("Težina: " + task.getDifficulty().name());
            tvImportance.setText("Bitnost: " + task.getImportance().name());
            String targetCategoryId = task.getCategoryId();
            tvCategory.setText("Kategorija: Učitavanje...");

            categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
                if (categories != null) {
                    for (Category cat : categories) {
                        if (cat.getId().equals(targetCategoryId)) {
                            tvCategory.setText("Kategorija: " + cat.getName());
                            return; // Prekidamo petlju čim nađemo podudaranje
                        }
                    }
                    // Ako petlja završi a ne nađe ID (npr. kategorija obrisana u međuvremenu)
                    tvCategory.setText("Kategorija: Nepoznato");
                }
            });

            // 2. Kategorija (ID za sada, jer model ima samo to)
            tvCategory.setText("Kategorija: " + task.getCategoryId());

            // 3. Status (Hardkodovano kako si tražio)
            tvStatus.setText("Status: AKTIVAN");
            tvStatus.setTextColor(Color.parseColor("#27AE60")); // Zelena boja
            // Prikaz svih datuma iz liste
            if (task.getRecurringDates() != null && !task.getRecurringDates().isEmpty()) {
                StringBuilder sb = new StringBuilder("Planirani datumi:\n");
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                for (Long d : task.getRecurringDates()) {
                    sb.append("• ").append(sdf.format(new Date(d))).append("\n");
                }
                tvDates.setText(sb.toString());
            } else {
                tvDates.setText("Nema dodatnih datuma.");
            }
        }

        btnClose.setOnClickListener(view -> {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        return v;
    }
}