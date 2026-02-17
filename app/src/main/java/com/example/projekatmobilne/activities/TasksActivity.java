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

import com.example.projekatmobilne.adapters.TaskAdapter;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.fragments.TaskDetailFragment;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.R;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.example.projekatmobilne.viewModels.TaskViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private TaskViewModel taskViewModel;
    private String categoryId;
    private List<Task> masterTaskList = new ArrayList<>();
    private int currentFilterMode = 0; // 0: Svi, 1: Jednokratni, 2: Ponavljajući
    private TaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        // 1. Inicijalizacija
        categoryId = getIntent().getStringExtra("CATEGORY_ID");
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // 2. Setup RecyclerView
        RecyclerView rv = findViewById(R.id.recyclerViewTasks);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TaskAdapter(
                // >>> IZMENJENO: Dodaj dateContext parametar <
                (task, newStatus, dateContext) -> {
                    Log.d("TASKS_ACTIVITY", "Status promenjen u " + newStatus + " za datum: " + dateContext);

                    // >>> NOVO: Postavi status za specifičan datum <
                    task.setStatusForDate(dateContext, newStatus);

                    // Ažuriraj u bazi
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
                Log.d("UI_DEBUG", "Observer aktivan! Broj taskova nakon filtriranja: " + adapter.getItemCount());

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
            if (cats != null) {
                adapter.setCategories(cats);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("TASKS_ACTIVITY", "onResume() - osvežavam listu");
        taskViewModel.loadAllTasks();
    }

    private void showDeleteTaskDialog(Task task) {
        // >>> PROVERA: Da li ima DONE datume? <
        if (task.hasAnyCompletedOccurrences()) {
            int completedCount = task.getCompletedCount();

            // ❌ ZABRANJEN DELETE
            new AlertDialog.Builder(this)
                    .setTitle("❌ Brisanje zabranjeno")
                    .setMessage("Ne možete obrisati zadatak '" + task.getTitle() + "' koji ima završene termine (" + completedCount + " urađeno).\n\nOvo čuva vašu istoriju i zarađeni XP.")
                    .setPositiveButton("U REDU", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            Log.d("TASKS_ACTIVITY", "DELETE ZABRANJEN - Task ima " + completedCount + " DONE datuma");
            return;
        }

        // ✅ DOZVOLJENO - Prikaži potvrdu
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
                    Log.d("TASKS_ACTIVITY", "DELETE POTVRĐEN - Brišem task: " + task.getTitle());
                    taskViewModel.deleteTask(task.getId());
                    Toast.makeText(this, "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("OTKAŽI", null)
                .setIcon(R.drawable.avatar_1)
                .show();
    }

    private void openTaskDetail(Task task) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
        fragment.show(getSupportFragmentManager(), "task_detail");
    }

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

        // VREMENSKI ŠTIT: Početak današnjeg dana (00:00:00)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfToday = cal.getTimeInMillis();

        List<Task> filteredList = new ArrayList<>();

        for (Task task : masterTaskList) {
            FrequencyType type = task.getFrequencyType();

            // 1. PROVERA VREMENSKOG ŠTITA
            if (isTaskInPast(task, startOfToday)) {
                continue;
            }

            // 2. FILTRIRANJE PO TIPU
            if (currentFilterMode == 0) {
                filteredList.add(task);
            } else if (currentFilterMode == 1) {
                if (type == FrequencyType.ONE_TIME) {
                    filteredList.add(task);
                }
            } else if (currentFilterMode == 2) {
                if (type == FrequencyType.RECURRING) {
                    filteredList.add(task);
                }
            }
        }

        Log.d("FILTER_DEBUG", "Kraj. Prikazujem: " + filteredList.size() + " od ukupno " + masterTaskList.size());
        adapter.setTasks(filteredList);
    }

    private boolean isTaskInPast(Task task, long startOfToday) {
        if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
            return task.getExecutionTime() < startOfToday;
        } else {
            List<Long> recurringDates = task.getRecurringDates();
            if (recurringDates == null || recurringDates.isEmpty()) {
                return task.getExecutionTime() < startOfToday;
            }
            long lastOccurrence = recurringDates.get(recurringDates.size() - 1);
            return lastOccurrence < startOfToday;
        }
    }
}