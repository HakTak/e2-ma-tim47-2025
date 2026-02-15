package com.example.projekatmobilne.viewModels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.repositories.TaskRepository;
import com.example.projekatmobilne.utils.SharedPrefsManager;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final SharedPrefsManager prefsManager;
    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();

    public TaskViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TaskRepository();
        this.prefsManager = new SharedPrefsManager(application);
        loadAllTasks();
    }

    public MutableLiveData<List<Task>> getAllTasks() {
        return tasksLiveData;
    }

    public void loadAllTasks() {
        String userId = prefsManager.getUserId();
        if (userId != null) {
            repository.getAllTasks(userId, new TaskRepository.TasksCallback() {
                @Override
                public void onTasksLoaded(List<Task> tasks) {
                    tasksLiveData.postValue(tasks);
                }

                @Override
                public void onError(String error) {
                    // Handle error
                }
            });
        }
    }

    public void insertTask(Task task) {
        repository.insertTask(task, new TaskRepository.InsertCallback() {
            @Override
            public void onSuccess(String taskId) {
                loadAllTasks();
            }

            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }

    public void updateTask(Task task) {
        repository.updateTask(task, new TaskRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                loadAllTasks();
            }

            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }

    public void deleteTask(String taskId) {
        repository.deleteTask(taskId, new TaskRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                loadAllTasks();
            }

            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }
}