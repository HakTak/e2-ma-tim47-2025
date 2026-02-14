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
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.R;
import com.example.projekatmobilne.viewModels.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private TaskViewModel taskViewModel;
    private long categoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        // 1. Preuzimanje podataka iz Intenta
        categoryId = getIntent().getLongExtra("CATEGORY_ID", -1);
        String categoryName = getIntent().getStringExtra("CATEGORY_NAME");

        TextView tvTitle = findViewById(R.id.tvCategoryTasksTitle);
        tvTitle.setText("Zadaci: " + categoryName);

        // 2. Inicijalizacija ViewModela
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // 3. Setup RecyclerView i Adaptera
        RecyclerView rv = findViewById(R.id.recyclerViewTasks);
        rv.setLayoutManager(new LinearLayoutManager(this));

        TaskAdapter adapter = new TaskAdapter(
                (task, isChecked) -> {
                    // Update statusa (Checkbox)
                    task.setCompleted(isChecked);
                    taskViewModel.updateTask(task);
                },
                task -> {
                    // Pozivamo metodu za brisanje (koju dodajemo dole)
                    showDeleteTaskDialog(task);
                }
        );
        rv.setAdapter(adapter);

        // 4. Posmatranje podataka (Samo jedan observer je potreban)
        taskViewModel.getAllTasks().observe(this, tasks -> {
            List<Task> filteredTasks = new ArrayList<>();
            for (Task t : tasks) {
                // Filtriramo taskove tako da prikazujemo samo one iz ove kategorije
                if (t.getCategoryId() == categoryId) {
                    filteredTasks.add(t);
                }
            }
            adapter.setTasks(filteredTasks);
        });

        // 5. Dugme za dodavanje novog taska
        FloatingActionButton btnAdd = findViewById(R.id.btnAddTask);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(TasksActivity.this, AddTaskActivity.class);
            // Možemo proslediti categoryId da AddTaskActivity odmah zna u koju kategoriju da doda
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
}