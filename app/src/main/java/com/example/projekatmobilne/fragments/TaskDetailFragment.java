package com.example.projekatmobilne.fragments;

import android.app.AlertDialog;
import android.content.Intent;
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
import com.example.projekatmobilne.activities.AddTaskActivity;
import com.example.projekatmobilne.adapters.DateStatusAdapter;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.services.TaskService;
import com.example.projekatmobilne.services.TaskValidationService;
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
    private TaskService taskService;
    private TaskValidationService validationService;

    private Task currentTask;
    private View rootView;
    private List<Category> allCategories;

    private DateStatusAdapter dateStatusAdapter;
    private RecyclerView rvDateStatuses;

    // ===================================================
    // FACTORY
    // ===================================================

    public static TaskDetailFragment newInstance(Task task) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK, task);
        fragment.setArguments(args);
        Log.d("TASK_DETAIL_FRAGMENT", "newInstance() za task: " + task.getTitle());
        return fragment;
    }

    // ===================================================
    // LIFECYCLE
    // ===================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
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

        taskViewModel      = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel  = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        taskService        = new TaskService();
        validationService  = new TaskValidationService();

        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            allCategories = categories;
            updateCategoryName();
        });

        // Setup RecyclerView za datume
        rvDateStatuses = rootView.findViewById(R.id.rvDateStatuses);
        rvDateStatuses.setLayoutManager(new LinearLayoutManager(getContext()));

        dateStatusAdapter = new DateStatusAdapter(
                // Callback 1: Promena statusa (DOPUNJEN SA VALIDACIJOM)
                (dateTimestamp, newStatus) -> {
                    Log.d(TAG, "Pokušaj promene statusa | Datum: " + new Date(dateTimestamp) + " | Novi status: " + newStatus);

                    // Dobij trenutni status za taj datum
                    TaskStatus currentStatus = taskService.getStatusForDate(currentTask, dateTimestamp);

                    // VALIDACIJA PRE PROMENE STATUSA
                    String validationError = taskService.canChangeStatus(currentTask, dateTimestamp, currentStatus, newStatus);
                    if (validationError != null) {
                        Toast.makeText(getContext(), validationError, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Validacija NIJE prošla: " + validationError);
                        return; // Blokira promenu
                    }

                    Log.d(TAG, "Validacija prošla, menjam status");
                    Log.d(TAG, "Mapa PRE: " + currentTask.getOccurrenceStatuses());

                    taskService.setStatusForDate(currentTask, dateTimestamp, newStatus);

                    Log.d(TAG, "Mapa POSLE: " + currentTask.getOccurrenceStatuses());

                    taskViewModel.updateTask(currentTask);
                    dateStatusAdapter.setData(currentTask);

                    // Ako je DONE → dodaj XP
                    if (newStatus == TaskStatus.DONE) {
                        awardXPForTask(currentTask);
                    }

                    Toast.makeText(getContext(), "Status promenjen u " + newStatus.name(), Toast.LENGTH_SHORT).show();
                },
                // Callback 2: Uklanjanje datuma (ostaje isto)
                (dateTimestamp) -> {
                    Log.d(TAG, "Uklanjam datum + buduće");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    String dateStr = sdf.format(new Date(dateTimestamp));

                    taskViewModel.removeSingleOccurrence(currentTask, dateTimestamp,
                            new TaskViewModel.RemoveOccurrenceCallback() {
                                @Override
                                public void onSuccess(int removedCount, boolean taskDeleted) {
                                    Log.d(TAG, "Uklonjeno " + removedCount + " datuma");

                                    if (taskDeleted) {
                                        Toast.makeText(getContext(),
                                                "Zadatak je obrisan (nema preostalih termina)",
                                                Toast.LENGTH_LONG).show();
                                        dismiss();
                                    } else {
                                        dateStatusAdapter.setData(currentTask);
                                        String msg = removedCount == 1
                                                ? "Termin " + dateStr + " uklonjen"
                                                : "Uklonjeno " + removedCount + " termina";
                                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Greška pri uklanjanju: " + error);
                                    Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                }
        );
        rvDateStatuses.setAdapter(dateStatusAdapter);

        // Button listeneri
        rootView.findViewById(R.id.btnDeleteTask).setOnClickListener(v -> confirmDeletion());
        rootView.findViewById(R.id.btnCloseDetail).setOnClickListener(v -> dismiss());
        rootView.findViewById(R.id.btnEditTask).setOnClickListener(v -> handleEditClick());

        updateUI();
        Log.d(TAG, "onCreateView() završen");
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setDimAmount(0.6f);
        }
        Log.d(TAG, "onStart() - Dialog se prikazuje");
    }

    // ===================================================
    // EDIT LOGIKA
    // ===================================================

    private void handleEditClick() {
        // Sva validacija ide kroz servis — jedna linija umesto 50
        String blockReason = validationService.canEdit(currentTask);

        if (blockReason != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("❌ Izmena zabranjena")
                    .setMessage(blockReason)
                    .setPositiveButton("U REDU", null)
                    .show();
            Log.d(TAG, "EDIT ZABRANJEN: " + blockReason);
            return;
        }

        Log.d(TAG, "Edit dozvoljen za task: " + currentTask.getTitle());
        Intent intent = new Intent(getActivity(), AddTaskActivity.class);
        intent.putExtra("TASK_ID", currentTask.getId());
        startActivity(intent);
        dismiss();
    }

    // ===================================================
    // DELETE LOGIKA
    // ===================================================


    private void confirmDeletion() {
        if (currentTask == null) return;

        String blockReason = validationService.canDelete(currentTask);

        if (blockReason != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("❌ Brisanje zabranjeno")
                    .setMessage(blockReason)
                    .setPositiveButton("U REDU", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            Log.d(TAG, "DELETE ZABRANJEN: " + blockReason);
            return;
        }

        String message;
        if (currentTask.getFrequencyType() == FrequencyType.ONE_TIME) {
            message = "Da li ste sigurni da želite da obrišete ovaj zadatak?";
        } else {
            int totalDates = currentTask.getRecurringDates() != null
                    ? currentTask.getRecurringDates().size() : 0;
            message = "Da li ste sigurni da želite da obrišete ovaj zadatak?\n\nBiće obrisano "
                    + totalDates + " planiranih termina.";
        }

        new AlertDialog.Builder(getContext())
                .setTitle("⚠️ Potvrda brisanja")
                .setMessage(message)
                .setPositiveButton("OBRIŠI", (dialog, which) -> {
                    Log.d(TAG, "DELETE POTVRĐEN: " + currentTask.getTitle());
                    taskViewModel.deleteTaskSmart(currentTask, new TaskViewModel.DeleteSmartCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(getContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                        @Override public void onBlocked(String reason) { /* ne može se desiti, već provereno */ }
                        @Override public void onError(String error) {
                            Log.e(TAG, "Greška pri brisanju: " + error);
                        }
                    });
                })
                .setNegativeButton("OTKAŽI", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // ===================================================
    // UI UPDATE
    // ===================================================

    private void updateCategoryName() {
        if (rootView == null || currentTask == null || allCategories == null) return;

        TextView tvCat = rootView.findViewById(R.id.tvDetailCategory);
        for (Category c : allCategories) {
            if (c.getId().equals(currentTask.getCategoryId())) {
                tvCat.setText(c.getName());
                Log.d(TAG, "Kategorija: " + c.getName());
                return;
            }
        }
        tvCat.setText("Ostalo");
    }

    private void updateUI() {
        if (currentTask == null || rootView == null) return;
        Log.d(TAG, "updateUI()");

        ((TextView) rootView.findViewById(R.id.tvDetailTitle)).setText(currentTask.getTitle());
        ((TextView) rootView.findViewById(R.id.tvDetailDesc)).setText(currentTask.getDescription());
        ((TextView) rootView.findViewById(R.id.tvDetailXp)).setText("+" + currentTask.getTotalXp() + " XP");

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        ((TextView) rootView.findViewById(R.id.tvDetailExecutionTime))
                .setText(timeFormat.format(new Date(currentTask.getExecutionTime())));

        ((TextView) rootView.findViewById(R.id.tvDetailCategory)).setText("Učitavanje...");
        ((TextView) rootView.findViewById(R.id.tvDetailDifficulty))
                .setText(translateDifficulty(currentTask.getDifficulty().name()));
        ((TextView) rootView.findViewById(R.id.tvDetailImportance))
                .setText(translateImportance(currentTask.getImportance().name()));

        String freqLabel = currentTask.getFrequencyType() == FrequencyType.ONE_TIME
                ? "Jednokratno" : "Ponavljajuće";
        ((TextView) rootView.findViewById(R.id.tvDetailFrequency)).setText(freqLabel);

        dateStatusAdapter.setData(currentTask);

        TextView tvDateHeader = rootView.findViewById(R.id.tvDateStatusHeader);
        tvDateHeader.setText(currentTask.getFrequencyType() == FrequencyType.ONE_TIME
                ? "DATUM I STATUS:" : "PLANIRANI DATUMI I STATUSI:");

        Log.d(TAG, "UI potpuno ažuriran");
    }

    // ===================================================
    // PREVODI
    // ===================================================

    private String translateDifficulty(String diff) {
        switch (diff) {
            case "VERY_EASY": return "Veoma lako";
            case "EASY":      return "Lako";
            case "MEDIUM":    return "Srednje";
            case "HARD":      return "Teško";
            case "EXTREME":   return "Ekstremno";
            default:          return diff;
        }
    }

    private String translateImportance(String imp) {
        switch (imp) {
            case "LOW":      return "Nisko";
            case "NORMAL":   return "Normalno";
            case "HIGH":     return "Važno";
            case "CRITICAL": return "Kritično";
            case "SPECIAL":  return "Specijalno";
            default:         return imp;
        }
    }

    // Nova helper metoda (dodaj na kraj klase)
    private void awardXPForTask(Task task) {
        String userId = new com.example.projekatmobilne.utils.SharedPrefsManager(requireContext()).getUserId();

        if (userId != null) {
            com.example.projekatmobilne.services.XPTrackingService xpTracker =
                    new com.example.projekatmobilne.services.XPTrackingService(requireContext());

            com.example.projekatmobilne.services.XPTrackingService.XPResult result =
                    xpTracker.calculateAwardedXP(task.getDifficulty(), task.getImportance());

            if (result.hasEarnedXP()) {
                com.example.projekatmobilne.viewModels.UserViewModel userViewModel =
                        new ViewModelProvider(requireActivity()).get(com.example.projekatmobilne.viewModels.UserViewModel.class);

                userViewModel.addXP(userId, result.totalXP);

                Toast.makeText(getContext(),
                        "+" + result.getBreakdown(),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(),
                        "Dnevni/nedeljni/mesečni limit dostignut - 0 XP",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}