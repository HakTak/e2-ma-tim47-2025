package com.example.projekatmobilne.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.adapters.TaskAdapter;
import com.example.projekatmobilne.enums.FrequencyType;
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

    private List<Task> masterTaskList = new ArrayList<>(); // Čuva sve taskove iz baze
    private int currentFilterMode = 0; // 0: Svi, 1: Jednokratni, 2: Ponavljajući
    private TaskAdapter adapter; // Izvuci adapter kao polje klase

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
                (task, newStatus) -> {
                    task.setStatus(newStatus); // Postavljamo novi status
                    taskViewModel.updateTask(task); // Šaljemo u bazu
                    Toast.makeText(this, "Status promenjen u " + newStatus.name(), Toast.LENGTH_SHORT).show();
                },
                task -> showDeleteTaskDialog(task),
                task -> openTaskDetail(task)
        );
        rv.setAdapter(adapter);

        ImageButton btnFilter = findViewById(R.id.btnFilterTasks);
        btnFilter.setOnClickListener(v -> showFilterMenu(v));

        // 3. PRVO postavi posmatrača (Observer)
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                masterTaskList = tasks; // Sačuvaj originalnu listu
                applyFilter(); // Primeni trenutno aktivni filter
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

    // 5. Metoda za prikaz Popup menija
    private void showFilterMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add(0, 0, 0, "Prikaži sve");
        popup.getMenu().add(0, 1, 1, "Samo jednokratni");
        popup.getMenu().add(0, 2, 2, "Samo ponavljajući");

        popup.setOnMenuItemClickListener(item -> {
            Log.d("FILTER_DEBUG", "Kliknut meni ID: " + item.getItemId()); // DODAJ OVAJ LOG
            currentFilterMode = item.getItemId();
            applyFilter();
            return true;
        });
        popup.show();
    }

    // 6. Glavna logika za filtriranje
    private void applyFilter() {

        if (adapter == null) {
            Log.e("FILTER_DEBUG", "Adapter je NULL!");
            return; // Sigurnosni prekid
        }
        Log.d("FILTER_DEBUG", "--- START FILTRIRANJA ---");
        Log.d("FILTER_DEBUG", "Trenutni mod filtera: " + currentFilterMode);
        Log.d("FILTER_DEBUG", "Ukupno u master listi: " + masterTaskList.size());

        Log.d("FILTER_DEBUG", "Pokrećem filter. Mod: " + currentFilterMode + ", Ukupno taskova u master listi: " + masterTaskList.size());

        List<Task> filteredList = new ArrayList<>();

        for (Task task : masterTaskList) {
            // LOGUJEMO SVAKI TASK DA VIDIMO ŠTA JE U NJEMU
            //Log.d("FILTER_DEBUG", "Proveravam task: " + task.getTitle() + " | Tip: " + task.getFrequencyType());
            FrequencyType type = task.getFrequencyType();
            Log.d("FILTER_DEBUG", "Task: " + task.getTitle() + " | Tip u objektu: " + (type != null ? type.name() : "NULL"));
            if (currentFilterMode == 0) {
                // Prikaži sve
                filteredList.add(task);
            } else if (currentFilterMode == 1) {
                // Samo jednokratni
                if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
                    filteredList.add(task);
                }
            } else if (currentFilterMode == 2) {
                // Samo ponavljajući
                if (task.getFrequencyType() == FrequencyType.RECURRING) {
                    filteredList.add(task);
                }
            }
        }
        Log.d("FILTER_DEBUG", "Kraj. Filtrirano za prikaz: " + filteredList.size());
        Log.d("FILTER_DEBUG", "--- END FILTRIRANJA ---");
        adapter.setTasks(filteredList);

        // Opciono: Toast poruka da korisnik zna šta vidi
        String msg = currentFilterMode == 0 ? "Svi zadaci" :
                (currentFilterMode == 1 ? "Jednokratni zadaci" : "Ponavljajući zadaci");
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}