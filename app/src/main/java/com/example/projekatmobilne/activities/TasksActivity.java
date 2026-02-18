package com.example.projekatmobilne.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.adapters.TaskAdapter;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.fragments.TaskDetailFragment;
import com.example.projekatmobilne.models.Boss;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.services.TaskService;
import com.example.projekatmobilne.services.TaskValidationService;
import com.example.projekatmobilne.viewModels.BossViewModel;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.example.projekatmobilne.viewModels.TaskViewModel;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private TaskViewModel taskViewModel;
    private TaskService taskService;
    private TaskValidationService validationService;

    private String categoryId;
    private List<Task> masterTaskList = new ArrayList<>();
    private int currentFilterMode = 0; // 0: Svi, 1: Jednokratni, 2: Ponavljajući
    private TaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        /*// ===== PRIVREMENI TEST - OBRIŠI POSLE =====
        BossViewModel bossViewModel = new ViewModelProvider(this).get(BossViewModel.class);

        bossViewModel.ensureBossExistsForLevel(1, new BossViewModel.BossReadyCallback() {
            @Override
            public void onReady(Boss boss) {
                Log.d("BOSS_TEST", "Boss spreman! Level: " + boss.getLevel()
                        + " | HP: " + boss.getMaxHp()
                        + " | Coins: " + boss.getCoins()
                        + " | ID: " + boss.getId());
                Toast.makeText(TasksActivity.this,
                        "Boss level 1 kreiran! HP: " + boss.getMaxHp(),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                Log.e("BOSS_TEST", "Greška: " + error);
                Toast.makeText(TasksActivity.this, "Greška: " + error, Toast.LENGTH_LONG).show();
            }
        });
       // ===== KRAJ TESTA =====*/

        // 1. Inicijalizacija
        categoryId = getIntent().getStringExtra("CATEGORY_ID");
        taskViewModel   = new ViewModelProvider(this).get(TaskViewModel.class);
        taskService      = new TaskService();
        validationService = new TaskValidationService();

        // 2. Setup RecyclerView
        RecyclerView rv = findViewById(R.id.recyclerViewTasks);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TaskAdapter(
                (task, newStatus, dateContext) -> {
                    Log.d("TASKS_ACTIVITY", "Status promenjen u " + newStatus + " za datum: " + dateContext);
                    taskService.setStatusForDate(task, dateContext, newStatus);
                    taskViewModel.updateTask(task);
                    Toast.makeText(this, "Status promenjen u " + newStatus.name(), Toast.LENGTH_SHORT).show();
                },
                task -> showDeleteTaskDialog(task),
                task -> openTaskDetail(task)
        );
        rv.setAdapter(adapter);

        ImageButton btnFilter = findViewById(R.id.btnFilterTasks);
        btnFilter.setOnClickListener(v -> showFilterMenu(v));

        // 3. Observer
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                masterTaskList = tasks;
                applyFilter();
                Log.d("UI_DEBUG", "Observer aktivan! Broj taskova: " + adapter.getItemCount());
                if (adapter.getItemCount() > 0) {
                    rv.setVisibility(View.VISIBLE);
                }
            } else {
                Log.d("UI_DEBUG", "Observer primio NULL listu");
            }
        });

        // 4. Učitaj taskove
        taskViewModel.loadAllTasks();

        // 5. FAB
        findViewById(R.id.btnAddTask).setOnClickListener(v -> {
            Intent intent = new Intent(TasksActivity.this, AddTaskActivity.class);
            intent.putExtra("CATEGORY_ID", categoryId);
            startActivity(intent);
        });

        // 6. Kategorije za boje
        CategoryViewModel categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        categoryViewModel.getAllCategories().observe(this, cats -> {
            if (cats != null) adapter.setCategories(cats);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("TASKS_ACTIVITY", "onResume() - osvežavam listu");
        taskViewModel.loadAllTasks();
    }

    // ===================================================
    // DELETE DIALOG
    // ===================================================

    private void showDeleteTaskDialog(Task task) {
        String blockReason = validationService.canDelete(task);

        if (blockReason != null) {
            new AlertDialog.Builder(this)
                    .setTitle("❌ Brisanje zabranjeno")
                    .setMessage(blockReason)
                    .setPositiveButton("U REDU", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            Log.d("TASKS_ACTIVITY", "DELETE ZABRANJEN: " + blockReason);
            return;
        }

        String message;
        if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
            message = "Da li ste sigurni da želite da obrišete '" + task.getTitle() + "'?";
        } else {
            int totalDates = task.getRecurringDates() != null ? task.getRecurringDates().size() : 0;
            message = "Da li ste sigurni da želite da obrišete '" + task.getTitle() + "'?\n\nBiće obrisano " + totalDates + " planiranih termina.";
        }

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Potvrda brisanja")
                .setMessage(message)
                .setPositiveButton("OBRIŠI", (dialog, which) -> {
                    taskViewModel.deleteTaskSmart(task, new TaskViewModel.DeleteSmartCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(TasksActivity.this, "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onBlocked(String reason) { /* neće se desiti, već smo proverili */ }
                        @Override public void onError(String error) {
                            Log.e("TASKS_ACTIVITY", "Greška pri brisanju: " + error);
                        }
                    });
                })
                .setNegativeButton("OTKAŽI", null)
                .setIcon(R.drawable.avatar_1)
                .show();
    }

    // ===================================================
    // NAVIGACIJA
    // ===================================================

    private void openTaskDetail(Task task) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
        fragment.show(getSupportFragmentManager(), "task_detail");
    }

    // ===================================================
    // FILTER
    // ===================================================

    private void showFilterMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add(0, 0, 0, "Prikaži sve");
        popup.getMenu().add(0, 1, 1, "Samo jednokratni");
        popup.getMenu().add(0, 2, 2, "Samo ponavljajući");

        popup.setOnMenuItemClickListener(item -> {
            Log.d("FILTER_DEBUG", "Kliknut meni ID: " + item.getItemId());
            currentFilterMode = item.getItemId();
            applyFilter();
            return true;
        });
        popup.show();
    }

    private void applyFilter() {
        if (adapter == null) {
            Log.e("FILTER_DEBUG", "Adapter je NULL!");
            return;
        }

        Log.d("FILTER_DEBUG", "--- START FILTRIRANJA ---");

        long startOfToday = TaskService.getStartOfToday();
        List<Task> filteredList = new ArrayList<>();

        for (Task task : masterTaskList) {
            FrequencyType type = task.getFrequencyType();

            if (taskService.isTaskInPast(task, startOfToday)) {
                continue;
            }

            if (currentFilterMode == 0) {
                filteredList.add(task);
            } else if (currentFilterMode == 1 && type == FrequencyType.ONE_TIME) {
                filteredList.add(task);
            } else if (currentFilterMode == 2 && type == FrequencyType.RECURRING) {
                filteredList.add(task);
            }
        }

        Log.d("FILTER_DEBUG", "Kraj. Prikazujem: " + filteredList.size() + " od ukupno " + masterTaskList.size());
        adapter.setTasks(filteredList);
    }
}