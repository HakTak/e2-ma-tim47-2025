package com.example.projekatmobilne.viewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.database.TaskRepository;
import com.example.projekatmobilne.models.Task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();
    private final ExecutorService executorService;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        executorService = Executors.newSingleThreadExecutor();
        loadAllTasks();
    }

    // =====================================================
    // Dohvati sve taskove (učitavanje iz baze)
    // =====================================================
    public LiveData<List<Task>> getAllTasks() {
        return tasksLiveData;
    }

    public void loadAllTasks() {
        executorService.execute(() -> {
            repository.open();
            List<Task> all = repository.getAllTasks();
            repository.close();
            tasksLiveData.postValue(all);
        });
    }

    // =====================================================
    // Dodaj novi task
    // =====================================================
    public void insertTask(Task task) {
        executorService.execute(() -> {
            repository.open();
            repository.insertTask(task);
            repository.close();
            loadAllTasks(); // automatski osveži listu
        });
    }

    // =====================================================
    // Ažuriraj postojeći task
    // =====================================================
    public void updateTask(Task task) {
        executorService.execute(() -> {
            repository.open();
            repository.updateTask(task);
            repository.close();
            loadAllTasks();
        });
    }

    // =====================================================
    // Označi task kao završen
    // =====================================================
    public void markAsCompleted(long taskId) {
        executorService.execute(() -> {
            repository.open();
            repository.markAsCompleted(taskId);
            repository.close();
            loadAllTasks();
        });
    }

    // =====================================================
    // Obriši task
    // =====================================================
    public void deleteTask(long taskId) {
        executorService.execute(() -> {
            repository.open();
            repository.deleteTask(taskId);
            repository.close();
            loadAllTasks();
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
