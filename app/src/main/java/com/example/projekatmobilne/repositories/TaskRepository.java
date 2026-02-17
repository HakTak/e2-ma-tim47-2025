package com.example.projekatmobilne.repositories;

import android.util.Log;
import com.example.projekatmobilne.enums.*;
import com.example.projekatmobilne.models.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;

public class TaskRepository {

    private final FirebaseFirestore db;

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // GET ALL TASKS (za specifičnog usera)
    public void getAllTasks(String userId, TasksCallback callback) {
        Log.d("FIRESTORE_RETIREVE", "Pokrećem query za userId: " + userId);

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("FIRESTORE_RETIREVE", "Uspeh! Broj dokumenata: " + querySnapshot.size());
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Task task = documentToTask(doc);
                            tasks.add(task);
                        } catch (Exception e) {
                            Log.e("FIRESTORE_RETIREVE", "Greška pri konverziji dokumenta: " + e.getMessage());
                        }
                    }
                    callback.onTasksLoaded(tasks);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_RETIREVE", "Greška u query-ju: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // INSERT TASK
    public void insertTask(Task task, InsertCallback callback) {
        db.collection("tasks")
                .add(task.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // UPDATE TASK
    public void updateTask(Task task, UpdateCallback callback) {
        Map<String, Object> taskMap = task.toMap();
        // >>> DODAJ LOG DA VIDIŠ ŠTA SE ŠALJE <
        Log.d("REPO_UPDATE", "===== UPDATING TASK =====");
        Log.d("REPO_UPDATE", "Task ID: " + task.getId());
        Log.d("REPO_UPDATE", "Task Title: " + task.getTitle());
        Log.d("REPO_UPDATE", "occurrenceStatuses u mapi: " + taskMap.get("occurrenceStatuses"));
        Log.d("REPO_UPDATE", "=========================");

        db.collection("tasks").document(task.getId())
                .set(taskMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("REPO_UPDATE", "✅ Task uspešno ažuriran u Firestore!");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("REPO_UPDATE", "❌ Greška pri ažuriranju: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // DELETE TASK
    public void deleteTask(String taskId, DeleteCallback callback) {
        db.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // >>> AŽURIRANA METODA: Helper: Firestore Document → Task objekat <
    private Task documentToTask(QueryDocumentSnapshot doc) {
        Task task = new Task();
        task.setId(doc.getId());
        task.setUserId(doc.getString("userId"));
        task.setCategoryId(doc.getString("categoryId"));
        task.setTitle(doc.getString("title"));
        task.setDescription(doc.getString("description"));

        // BROJEVI
        Long xpLong = doc.getLong("totalXp");
        task.setTotalXp(xpLong != null ? xpLong.intValue() : 0);

        Long execTime = doc.getLong("executionTime");
        task.setExecutionTime(execTime != null ? execTime : 0L);

        // DATUMI PONAVLJANJA
        task.setRepeatStartDate(doc.getLong("repeatStartDate"));
        task.setRepeatEndDate(doc.getLong("repeatEndDate"));

        Long intervalLong = doc.getLong("repeatInterval");
        task.setRepeatInterval(intervalLong != null ? intervalLong.intValue() : null);

        // ENUMI
        try {
            String freq = doc.getString("frequencyType");
            if (freq != null) {
                task.setFrequencyType(FrequencyType.valueOf(freq));
            } else {
                Log.e("REPO_DEBUG", "Task " + task.getTitle() + " nema frequencyType u bazi!");
                task.setFrequencyType(FrequencyType.ONE_TIME);
            }

            String diff = doc.getString("difficulty");
            if (diff != null) task.setDifficulty(Difficulty.valueOf(diff));

            String imp = doc.getString("importance");
            if (imp != null) task.setImportance(Importance.valueOf(imp));

            String unit = doc.getString("repeatUnit");
            if (unit != null) task.setRepeatUnit(RepeatUnit.valueOf(unit));

            List<Long> dates = (List<Long>) doc.get("recurringDates");
            if (dates != null) {
                task.setRecurringDates(dates);
            }

            String statusStr = doc.getString("status");
            if (statusStr != null) {
                task.setStatus(TaskStatus.valueOf(statusStr));
            } else {
                task.setStatus(TaskStatus.ACTIVE);
            }

            // >>> NOVO: Učitaj occurrenceStatuses mapu iz Firestore-a <
            Map<String, Object> occurrenceStatusesRaw = (Map<String, Object>) doc.get("occurrenceStatuses");
            if (occurrenceStatusesRaw != null) {
                Map<String, String> occurrenceStatuses = new HashMap<>();
                for (Map.Entry<String, Object> entry : occurrenceStatusesRaw.entrySet()) {
                    occurrenceStatuses.put(entry.getKey(), entry.getValue().toString());
                }
                task.setOccurrenceStatuses(occurrenceStatuses);
                Log.d("REPO_DEBUG", "Učitano " + occurrenceStatuses.size() + " specific statuses za task: " + task.getTitle());
            } else {
                task.setOccurrenceStatuses(new HashMap<>()); // Prazna mapa ako ne postoji
                Log.d("REPO_DEBUG", "Nema specific statuses za task: " + task.getTitle());
            }

        } catch (Exception e) {
            Log.e("REPO_ERROR", "Greška kod Enuma: " + e.getMessage());
        }

        return task;
    }

    public void getTasksByCategory(String userId, String categoryId, TasksCallback callback) {
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = documentToTask(doc);
                        tasks.add(task);
                    }
                    callback.onTasksLoaded(tasks);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // CALLBACKS
    public interface TasksCallback {
        void onTasksLoaded(List<Task> tasks);
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