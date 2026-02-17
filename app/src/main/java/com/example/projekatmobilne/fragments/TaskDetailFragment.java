package com.example.projekatmobilne.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.adapters.DateStatusAdapter;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.repositories.TaskRepository;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.example.projekatmobilne.viewModels.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskDetailFragment extends DialogFragment {

    private static final String ARG_TASK = "task_obj";
    private static final String TAG = "TASK_DETAIL_FRAGMENT";

    private TaskViewModel taskViewModel;
    private CategoryViewModel categoryViewModel;
    private Task currentTask;

    private View rootView;
    private List<Category> allCategories;

    // >>> NOVO: Adapter za datume <
    private DateStatusAdapter dateStatusAdapter;
    private RecyclerView rvDateStatuses;

    public static TaskDetailFragment newInstance(Task task) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK, task);
        fragment.setArguments(args);
        Log.d("TASK_DETAIL_FRAGMENT", "newInstance() pozvan za task: " + task.getTitle());
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() - Inflating layout...");
        rootView = inflater.inflate(R.layout.fragment_task_detail, container, false);

        if (getArguments() != null) {
            currentTask = (Task) getArguments().getSerializable(ARG_TASK);
            Log.d(TAG, "Task učitan: " + (currentTask != null ? currentTask.getTitle() : "NULL"));
        }

        if (currentTask == null) {
            Toast.makeText(getContext(), "Greška: Task nije pronađen", Toast.LENGTH_SHORT).show();
            dismiss();
            return rootView;
        }

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // Observer postavljen samo JEDNOM
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            allCategories = categories;
            updateCategoryName();
        });

        // >>> SETUP RECYCLERVIEW ZA DATUME <
        rvDateStatuses = rootView.findViewById(R.id.rvDateStatuses);
        rvDateStatuses.setLayoutManager(new LinearLayoutManager(getContext()));

        dateStatusAdapter = new DateStatusAdapter(
                // CALLBACK 1: Promena statusa (ostaje isti)
                (dateTimestamp, newStatus) -> {
                    Log.d(TAG, "=== Status promenjen iz DateStatusAdapter ===");
                    Log.d(TAG, "Datum: " + new Date(dateTimestamp));
                    Log.d(TAG, "Novi status: " + newStatus);
                    Log.d(TAG, "Mapa PRE: " + currentTask.getOccurrenceStatuses());

                    currentTask.setStatusForDate(dateTimestamp, newStatus);

                    Log.d(TAG, "Mapa POSLE: " + currentTask.getOccurrenceStatuses());

                    taskViewModel.updateTask(currentTask);
                    dateStatusAdapter.setData(currentTask);
                    Toast.makeText(getContext(), "Status promenjen u " + newStatus.name(), Toast.LENGTH_SHORT).show();
                },
                // CALLBACK 2: Uklanjanje datuma (AŽURIRAN)
                (dateTimestamp) -> {
                    Log.d(TAG, "=== Uklanjam datum + buduće ===");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    String dateStr = sdf.format(new Date(dateTimestamp));

                    taskViewModel.removeSingleOccurrence(currentTask, dateTimestamp, new TaskViewModel.RemoveOccurrenceCallback() {
                        @Override
                        public void onSuccess(int removedCount, boolean taskDeleted) {
                            Log.d(TAG, "Uspešno uklonjeno " + removedCount + " datuma");

                            if (taskDeleted) {
                                Toast.makeText(getContext(), "Zadatak je obrisan (nema preostalih termina)", Toast.LENGTH_LONG).show();
                                dismiss();
                            } else {
                                dateStatusAdapter.setData(currentTask);

                                String message;
                                if (removedCount == 1) {
                                    message = "Termin " + dateStr + " uklonjen";
                                } else {
                                    message = "Uklonjeno " + removedCount + " termina";
                                }
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Greška pri uklanjanju datuma: " + error);
                            Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
        );

        rvDateStatuses.setAdapter(dateStatusAdapter);

        // Button listeners
        rootView.findViewById(R.id.btnDeleteTask).setOnClickListener(view -> confirmDeletion());
        rootView.findViewById(R.id.btnCloseDetail).setOnClickListener(view -> dismiss());
        rootView.findViewById(R.id.btnEditTask).setOnClickListener(view -> {
            Toast.makeText(getContext(), "Izmena dolazi uskoro!", Toast.LENGTH_SHORT).show();
        });

        updateUI();

        Log.d(TAG, "onCreateView() završen");
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setDimAmount(0.6f);
        }
        Log.d(TAG, "onStart() - Dialog se prikazuje");
    }

    private void updateCategoryName() {
        if (rootView == null || currentTask == null || allCategories == null) return;

        TextView tvCat = rootView.findViewById(R.id.tvDetailCategory);
        for (Category c : allCategories) {
            if (c.getId().equals(currentTask.getCategoryId())) {
                tvCat.setText(c.getName());
                Log.d(TAG, "Kategorija ažurirana: " + c.getName());
                return;
            }
        }
        tvCat.setText("Ostalo");
    }

    private void updateUI() {
        if (currentTask == null || rootView == null) return;

        Log.d(TAG, "updateUI() - Popunjavam polja...");

        // Osnovne informacije
        ((TextView)rootView.findViewById(R.id.tvDetailTitle)).setText(currentTask.getTitle());
        ((TextView)rootView.findViewById(R.id.tvDetailDesc)).setText(currentTask.getDescription());
        ((TextView)rootView.findViewById(R.id.tvDetailXp)).setText("+" + currentTask.getTotalXp() + " XP");

        // Vreme izvršavanja (HH:mm)
        TextView tvExecTime = rootView.findViewById(R.id.tvDetailExecutionTime);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvExecTime.setText(timeFormat.format(new Date(currentTask.getExecutionTime())));

        // Kategorija (biće ažurirana preko observer-a)
        TextView tvCat = rootView.findViewById(R.id.tvDetailCategory);
        tvCat.setText("Učitavanje...");

        // Težina, Bitnost, Učestalost
        ((TextView)rootView.findViewById(R.id.tvDetailDifficulty)).setText(translateDifficulty(currentTask.getDifficulty().name()));
        ((TextView)rootView.findViewById(R.id.tvDetailImportance)).setText(translateImportance(currentTask.getImportance().name()));

        String freqLabel = currentTask.getFrequencyType() == FrequencyType.ONE_TIME ? "Jednokratno" : "Ponavljajuće";
        ((TextView)rootView.findViewById(R.id.tvDetailFrequency)).setText(freqLabel);

        // >>> NOVO: Popuni adapter sa datumima <
        dateStatusAdapter.setData(currentTask);

        // >>> NOVO: Sakrij RecyclerView ako je jednokratni task (opciono) <
        TextView tvDateHeader = rootView.findViewById(R.id.tvDateStatusHeader);
        if (currentTask.getFrequencyType() == FrequencyType.ONE_TIME) {
            tvDateHeader.setText("DATUM I STATUS:");
        } else {
            tvDateHeader.setText("PLANIRANI DATUMI I STATUSI:");
        }

        Log.d(TAG, "UI potpuno ažuriran");
    }

    private void confirmDeletion() {
        if (currentTask == null) return;

        // >>> PROVERA: Da li ima DONE datume? <
        if (currentTask.hasAnyCompletedOccurrences()) {
            int completedCount = currentTask.getCompletedCount();

            // ❌ ZABRANJEN DELETE
            new AlertDialog.Builder(getContext())
                    .setTitle("❌ Brisanje zabranjeno")
                    .setMessage("Ne možete obrisati zadatak koji ima završene termine (" + completedCount + " urađeno).\n\nOvo čuva vašu istoriju i zarađeni XP.")
                    .setPositiveButton("U REDU", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            Log.d(TAG, "DELETE ZABRANJEN - Task ima " + completedCount + " DONE datuma");
            return;
        }

        // ✅ DOZVOLJENO - Prikaži potvrdu
        String message;
        if (currentTask.getFrequencyType() == FrequencyType.ONE_TIME) {
            message = "Da li ste sigurni da želite da obrišete ovaj zadatak?";
        } else {
            int totalDates = currentTask.getRecurringDates() != null ? currentTask.getRecurringDates().size() : 0;
            message = "Da li ste sigurni da želite da obrišete ovaj zadatak?\n\nBiće obrisano " + totalDates + " planiranih termina.";
        }

        new AlertDialog.Builder(getContext())
                .setTitle("⚠️ Potvrda brisanja")
                .setMessage(message)
                .setPositiveButton("OBRIŠI", (dialog, which) -> {
                    Log.d(TAG, "DELETE POTVRĐEN - Brišem task: " + currentTask.getTitle());
                    taskViewModel.deleteTask(currentTask.getId());
                    Toast.makeText(getContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .setNegativeButton("OTKAŽI", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // PREVODI
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