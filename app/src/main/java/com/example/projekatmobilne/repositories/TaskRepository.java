package com.example.projekatmobilne.repositories;

import android.util.Log;

import com.example.projekatmobilne.enums.Difficulty;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.Importance;
import com.example.projekatmobilne.enums.RepeatUnit;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {

    private static final String TAG = "TASK_REPO";
    private static final String COLLECTION = "tasks";

    private final FirebaseFirestore db;

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ===================================================
    // CRUD OPERACIJE
    // ===================================================

    public void getAllTasks(String userId, TasksCallback callback) {
        Log.d(TAG, "getAllTasks() za userId: " + userId);

        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Učitano dokumenata: " + querySnapshot.size());
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            tasks.add(parseDocument(doc));
                        } catch (Exception e) {
                            Log.e(TAG, "Greška pri parsiranju dokumenta " + doc.getId() + ": " + e.getMessage());
                        }
                    }
                    callback.onTasksLoaded(tasks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllTasks() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void getTaskById(String taskId, TaskByIdCallback callback) {
        Log.d(TAG, "getTaskById() za ID: " + taskId);

        db.collection(COLLECTION)
                .document(taskId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        try {
                            Task task = parseDocument(doc);
                            Log.d(TAG, "Task pronađen: " + task.getTitle());
                            callback.onTaskLoaded(task);
                        } catch (Exception e) {
                            Log.e(TAG, "Greška pri parsiranju: " + e.getMessage());
                            callback.onError(e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Task sa ID " + taskId + " ne postoji!");
                        callback.onError("Task nije pronađen");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getTaskById() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void getTasksByCategory(String userId, String categoryId, TasksCallback callback) {
        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            tasks.add(parseDocument(doc));
                        } catch (Exception e) {
                            Log.e(TAG, "Greška pri parsiranju: " + e.getMessage());
                        }
                    }
                    callback.onTasksLoaded(tasks);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void insertTask(Task task, InsertCallback callback) {
        db.collection(COLLECTION)
                .add(task.toMap())
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Task dodat sa ID: " + docRef.getId());
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "insertTask() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void updateTask(Task task, UpdateCallback callback) {
        Log.d(TAG, "updateTask() ID: " + task.getId() + " | " + task.getTitle());

        db.collection(COLLECTION).document(task.getId())
                .set(task.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task uspešno ažuriran");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateTask() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void deleteTask(String taskId, DeleteCallback callback) {
        db.collection(COLLECTION).document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task obrisan: " + taskId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteTask() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ===================================================
    // FIRESTORE PARSIRANJE - JEDNA METODA ZA SVE
    // ===================================================

    /**
     * Parsira bilo koji Firestore dokument u Task objekat.
     * Radi i sa DocumentSnapshot i sa QueryDocumentSnapshot
     * jer QueryDocumentSnapshot extends DocumentSnapshot.
     */
    private Task parseDocument(DocumentSnapshot doc) {
        Task task = new Task();

        task.setId(doc.getId());
        task.setUserId(doc.getString("userId"));
        task.setCategoryId(doc.getString("categoryId"));
        task.setTitle(doc.getString("title"));
        task.setDescription(doc.getString("description"));
        task.setRepeatStartDate(doc.getLong("repeatStartDate"));
        task.setRepeatEndDate(doc.getLong("repeatEndDate"));

        Long xpLong = doc.getLong("totalXp");
        task.setTotalXp(xpLong != null ? xpLong.intValue() : 0);

        Long execTime = doc.getLong("executionTime");
        task.setExecutionTime(execTime != null ? execTime : 0L);

        Long intervalLong = doc.getLong("repeatInterval");
        task.setRepeatInterval(intervalLong != null ? intervalLong.intValue() : null);

        // Enumi — svaki u svom try/catch da jedan ne blokira ostale
        try {
            String freq = doc.getString("frequencyType");
            task.setFrequencyType(freq != null ? FrequencyType.valueOf(freq) : FrequencyType.ONE_TIME);
        } catch (Exception e) { Log.e(TAG, "Greška parsiranja frequencyType: " + e.getMessage()); }

        try {
            String diff = doc.getString("difficulty");
            if (diff != null) task.setDifficulty(Difficulty.valueOf(diff));
        } catch (Exception e) { Log.e(TAG, "Greška parsiranja difficulty: " + e.getMessage()); }

        try {
            String imp = doc.getString("importance");
            if (imp != null) task.setImportance(Importance.valueOf(imp));
        } catch (Exception e) { Log.e(TAG, "Greška parsiranja importance: " + e.getMessage()); }

        try {
            String unit = doc.getString("repeatUnit");
            if (unit != null) task.setRepeatUnit(RepeatUnit.valueOf(unit));
        } catch (Exception e) { Log.e(TAG, "Greška parsiranja repeatUnit: " + e.getMessage()); }

        try {
            String statusStr = doc.getString("status");
            task.setStatus(statusStr != null ? TaskStatus.valueOf(statusStr) : TaskStatus.ACTIVE);
        } catch (Exception e) {
            task.setStatus(TaskStatus.ACTIVE);
            Log.e(TAG, "Greška parsiranja status: " + e.getMessage());
        }

        // Lista datuma
        try {
            List<Long> dates = (List<Long>) doc.get("recurringDates");
            if (dates != null) task.setRecurringDates(dates);
        } catch (Exception e) { Log.e(TAG, "Greška parsiranja recurringDates: " + e.getMessage()); }

        // OccurrenceStatuses mapa
        try {
            Map<String, Object> raw = (Map<String, Object>) doc.get("occurrenceStatuses");
            if (raw != null) {
                Map<String, String> statuses = new HashMap<>();
                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    statuses.put(entry.getKey(), entry.getValue().toString());
                }
                task.setOccurrenceStatuses(statuses);
                Log.d(TAG, "Učitano " + statuses.size() + " statusa za: " + task.getTitle());
            } else {
                task.setOccurrenceStatuses(new HashMap<>());
            }
        } catch (Exception e) { Log.e(TAG, "Greška parsiranja occurrenceStatuses: " + e.getMessage()); }

        return task;
    }

    // ===================================================
    // CALLBACK INTERFEJSI
    // ===================================================

    public interface TasksCallback {
        void onTasksLoaded(List<Task> tasks);
        void onError(String error);
    }

    public interface TaskByIdCallback {
        void onTaskLoaded(Task task);
        void onError(String error);
    }

    public interface InsertCallback {
        void onSuccess(String taskId);
        void onError(String error);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}