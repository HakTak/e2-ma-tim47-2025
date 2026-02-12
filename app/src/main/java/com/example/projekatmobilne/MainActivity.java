package com.example.projekatmobilne;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import Database.CategoryRepository;
import Database.TaskRepository;
import Model.Category;
import Model.Task;
import Enum.*;
import ViewModels.CategoryViewModel;

public class MainActivity extends AppCompatActivity {
    private CategoryViewModel categoryViewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // ✅ Test kategorija
        testCategoryDatabase();

        // ✅ Test taskova
        testTaskDatabase();

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        // 👀 Posmatraj promene i ispisuj u Logcat
        categoryViewModel.getAllCategories().observe(this, categories -> {
            for (Category c : categories) {
                Log.d("VM_CATEGORY", "Kategorija: " + c.getName() + " (" + c.getColorHex() + ")");
            }
        });
        // ✅ Dodaj kategoriju
        categoryViewModel.insertCategory("Učenje", "#0000FF");


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void testCategoryDatabase() {
        CategoryRepository categoryRepo = new CategoryRepository(this);
        categoryRepo.open();
        long catId = categoryRepo.insertCategory("Zdravlje", "#00FF00");
        Log.d("DB_TEST", "Nova kategorija ID = " + catId);
        List<Category> allCats = categoryRepo.getAllCategories();
        for (Category c : allCats) {
            Log.d("DB_TEST", "Kategorija: id=" + c.getId() + ", name=" + c.getName());
        }
        categoryRepo.close();
    }
    private void testTaskDatabase() {
        TaskRepository taskRepo = new TaskRepository(this);
        taskRepo.open();
        Task task = new Task(
                1, // categoryId
                "Trčanje ujutru",
                "Lagano kardio vežbanje",
                FrequencyType.RECURRING,
                1,
                RepeatUnit.DAY,
                System.currentTimeMillis(),
                System.currentTimeMillis() + 604800000L, // završava za 7 dana
                System.currentTimeMillis() + 3600000L,   // 1h od sada izvršenje
                Difficulty.EASY,
                Importance.NORMAL
        );
        long newTaskId = taskRepo.insertTask(task);
        Log.d("DB_TEST", "Novi task ID = " + newTaskId);
        List<Task> tasks = taskRepo.getAllTasks();
        for (Task t : tasks) {
            Log.d("DB_TEST", "Task: id=" + t.getId() + ", title=" + t.getTitle() + ", XP=" + t.getTotalXp());
        }
        taskRepo.close();
    }
}