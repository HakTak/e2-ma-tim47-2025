package com.example.projekatmobilne.viewModels;

import android.app.Application;
import android.util.Log;

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
        //loadAllTasks();
    }

    public MutableLiveData<List<Task>> getAllTasks() {
        return tasksLiveData;
    }

    public void loadAllTasks() {
        String userId = prefsManager.getUserId();
        android.util.Log.d("VIEWMODEL_DEBUG", "Dobijen userId iz Prefs: " + userId);

        if (userId != null) {
            repository.getAllTasks(userId, new TaskRepository.TasksCallback() {
                @Override
                public void onTasksLoaded(List<Task> tasks) {
                    android.util.Log.d("VIEWMODEL_DEBUG", "Stiglo taskova u ViewModel: " + tasks.size());
                    tasksLiveData.postValue(tasks);
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("VIEWMODEL_DEBUG", "Greška u ViewModelu: " + error);
                }
            });
        } else {
            android.util.Log.e("VIEWMODEL_DEBUG", "UserID je NULL u SharedPrefs!");
        }
    }

    public void loadTasksByCategory(String categoryId) {
        String userId = prefsManager.getUserId();
        if (userId != null && categoryId != null) {
            repository.getTasksByCategory(userId, categoryId, new TaskRepository.TasksCallback() {
                @Override
                public void onTasksLoaded(List<Task> tasks) {
                    tasksLiveData.postValue(tasks);
                }

                @Override
                public void onError(String error) {
                    // Loguj grešku da vidiš šta se dešava
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