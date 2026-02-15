package com.example.projekatmobilne.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.adapters.TaskAdapter;
import com.example.projekatmobilne.fragments.TaskDetailFragment;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.R;
import com.example.projekatmobilne.viewModels.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private TaskViewModel taskViewModel;
    private String categoryId;

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

        TaskAdapter adapter = new TaskAdapter(
                (task, isChecked) -> {
                    task.setCompleted(isChecked);
                    taskViewModel.updateTask(task);
                },
                task -> showDeleteTaskDialog(task),
                task -> openTaskDetail(task) // DODAJ OVAJ TREĆI PARAMETAR
        );
        rv.setAdapter(adapter);

        // 3. PRVO postavi posmatrača (Observer)
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                android.util.Log.d("UI_DEBUG", "Observer aktivan! Broj taskova za prikaz: " + tasks.size());
                adapter.setTasks(tasks);
                // Dodatni test: ako je lista i dalje prazna na ekranu, proveri visinu RV
                if (tasks.size() > 0) {
                    rv.setVisibility(android.view.View.VISIBLE);
                }
            } else {
                android.util.Log.d("UI_DEBUG", "Observer primio NULL listu");
            }
        });

        // 4. TEK SAD pokreni učitavanje
        taskViewModel.loadAllTasks();

        // 5. FAB
        findViewById(R.id.btnAddTask).setOnClickListener(v -> {
            Intent intent = new Intent(TasksActivity.this, AddTaskActivity.class);
            intent.putExtra("CATEGORY_ID", categoryId);
            startActivity(intent);
        });
    }

    // DODATA METODA KOJA JE NEDOSTAJALA
    private void showDeleteTaskDialog(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Obriši zadatak")
                .setMessage("Da li ste sigurni da želite da obrišete '" + task.getTitle() + "'?")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    taskViewModel.deleteTask(task.getId());
                    Toast.makeText(this, "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Otkaži", null)
                .setIcon(android.R.drawable.ic_delete)
                .show();
    }

    private void openTaskDetail(Task task) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out) // Lepa animacija
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null) // Omogućava da se vratiš nazad na dugme 'back'
                .commit();
    }
}