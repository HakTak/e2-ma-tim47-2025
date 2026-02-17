package com.example.projekatmobilne.viewModels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.repositories.TaskRepository;
import com.example.projekatmobilne.services.TaskService;
import com.example.projekatmobilne.services.TaskValidationService;
import com.example.projekatmobilne.utils.SharedPrefsManager;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private static final String TAG = "TASK_VIEWMODEL";

    private final TaskRepository repository;
    private final TaskService taskService;
    private final TaskValidationService validationService;
    private final SharedPrefsManager prefsManager;

    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();

    public TaskViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TaskRepository();
        this.taskService = new TaskService();
        this.validationService = new TaskValidationService();
        this.prefsManager = new SharedPrefsManager(application);
    }

    // ===================================================
    // LOAD
    // ===================================================

    public MutableLiveData<List<Task>> getAllTasks() {
        return tasksLiveData;
    }

    public void loadAllTasks() {
        String userId = prefsManager.getUserId();
        Log.d(TAG, "loadAllTasks() userId: " + userId);

        if (userId == null) {
            Log.e(TAG, "UserID je NULL u SharedPrefs!");
            return;
        }

        repository.getAllTasks(userId, new TaskRepository.TasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                Log.d(TAG, "Učitano taskova: " + tasks.size());
                tasksLiveData.postValue(tasks);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "loadAllTasks() greška: " + error);
            }
        });
    }

    public void loadTasksByCategory(String categoryId) {
        String userId = prefsManager.getUserId();
        if (userId == null || categoryId == null) return;

        repository.getTasksByCategory(userId, categoryId, new TaskRepository.TasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                tasksLiveData.postValue(tasks);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "loadTasksByCategory() greška: " + error);
            }
        });
    }

    public void getTaskById(String taskId, TaskByIdCallback callback) {
        repository.getTaskById(taskId, new TaskRepository.TaskByIdCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                callback.onTaskLoaded(task);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ===================================================
    // INSERT / UPDATE
    // ===================================================

    public void insertTask(Task task) {
        repository.insertTask(task, new TaskRepository.InsertCallback() {
            @Override
            public void onSuccess(String taskId) {
                Log.d(TAG, "Task dodat: " + taskId);
                loadAllTasks();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "insertTask() greška: " + error);
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
                Log.e(TAG, "updateTask() greška: " + error);
            }
        });
    }

    // ===================================================
    // DELETE
    // ===================================================

    /**
     * Briše task uz validaciju — ne može se obrisati task koji ima DONE datume.
     * @param callback vraća grešku ako brisanje nije dozvoljeno
     */
    public void deleteTaskSmart(Task task, DeleteSmartCallback callback) {
        String validationError = validationService.canDelete(task);
        if (validationError != null) {
            callback.onBlocked(validationError);
            return;
        }

        repository.deleteTask(task.getId(), new TaskRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Task obrisan: " + task.getId());
                loadAllTasks();
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "deleteTaskSmart() greška: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * Ukloni datum + sve datume posle njega iz recurring taska.
     * Ako task ostane prazan, briše se kompletno.
     */
    public void removeSingleOccurrence(Task task, long dateToRemove, RemoveOccurrenceCallback callback) {
        int removedCount = taskService.removeSingleOccurrence(task, dateToRemove);

        if (removedCount == 0) {
            callback.onError("Datum nije pronađen u listi");
            return;
        }

        if (taskService.isEmpty(task)) {
            Log.d(TAG, "Task prazan — brišem kompletno");
            repository.deleteTask(task.getId(), new TaskRepository.DeleteCallback() {
                @Override
                public void onSuccess() {
                    loadAllTasks();
                    callback.onSuccess(removedCount, true);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        } else {
            Log.d(TAG, "Ažuriram task posle uklanjanja " + removedCount + " datuma");
            repository.updateTask(task, new TaskRepository.UpdateCallback() {
                @Override
                public void onSuccess() {
                    loadAllTasks();
                    callback.onSuccess(removedCount, false);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        }
    }

    // ===================================================
    // CALLBACK INTERFEJSI
    // ===================================================

    public interface TaskByIdCallback {
        void onTaskLoaded(Task task);
        void onError(String error);
    }

    public interface DeleteSmartCallback {
        void onSuccess();
        void onBlocked(String reason);
        void onError(String error);
    }

    public interface RemoveOccurrenceCallback {
        void onSuccess(int removedCount, boolean taskDeleted);
        void onError(String error);
    }
}