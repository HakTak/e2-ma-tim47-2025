package com.example.projekatmobilne.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.Database.TaskRepository;
import com.example.projekatmobilne.Model.Task;

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
        try {
            executorService.execute(() -> {
                repository.open();
                List<Task> all = repository.getAllTasks();
                tasksLiveData.postValue(all);
            });
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            repository.close();
        }

    }

    // =====================================================
    // Dodaj novi task
    // =====================================================
    public void insertTask(Task task) {

        try {
            executorService.execute(() -> {
                repository.open();
                repository.insertTask(task);

                loadAllTasks(); // automatski osveži listu
            });
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            repository.close();
        }


    }

    // =====================================================
    // Ažuriraj postojeći task
    // =====================================================
    public void updateTask(Task task) {

        try {
            executorService.execute(() -> {
                repository.open();
                repository.updateTask(task);

                loadAllTasks();
            });
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            repository.close();
        }


    }

    // =====================================================
    // Označi task kao završen
    // =====================================================
    public void markAsCompleted(long taskId) {

        try {
            executorService.execute(() -> {
                repository.open();
                repository.markAsCompleted(taskId);

                loadAllTasks();
            });
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            repository.close();
        }


    }

    // =====================================================
    // Obriši task
    // =====================================================
    public void deleteTask(long taskId) {

        try {
            executorService.execute(() -> {
                repository.open();
                repository.deleteTask(taskId);

                loadAllTasks();
            });
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            repository.close();
        }


    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
