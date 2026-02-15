package com.example.projekatmobilne.repositories;

import com.example.projekatmobilne.enums.Difficulty;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.Importance;
import com.example.projekatmobilne.enums.RepeatUnit;
import com.example.projekatmobilne.models.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    private final FirebaseFirestore db;

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // GET ALL TASKS (za specifičnog usera)
    public void getAllTasks(String userId, TasksCallback callback) {
        db.collection("tasks")
                .whereEqualTo("userId", userId)
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

    // INSERT TASK
    public void insertTask(Task task, InsertCallback callback) {
        db.collection("tasks")
                .add(task.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // UPDATE TASK
    public void updateTask(Task task, UpdateCallback callback) {
        db.collection("tasks").document(task.getId())
                .set(task.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // DELETE TASK
    public void deleteTask(String taskId, DeleteCallback callback) {
        db.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Helper: Firestore Document → Task objekat
    private Task documentToTask(QueryDocumentSnapshot doc) {
        Task task = new Task();
        task.setId(doc.getId());
        task.setUserId(doc.getString("userId"));
        task.setCategoryId(doc.getString("categoryId"));
        task.setTitle(doc.getString("title"));
        task.setDescription(doc.getString("description"));
        task.setFrequencyType(FrequencyType.valueOf(doc.getString("frequencyType")));
        task.setRepeatInterval(doc.getLong("repeatInterval") != null ? doc.getLong("repeatInterval").intValue() : null);

        String repeatUnitStr = doc.getString("repeatUnit");
        task.setRepeatUnit(repeatUnitStr != null ? RepeatUnit.valueOf(repeatUnitStr) : null);

        task.setRepeatStartDate(doc.getLong("repeatStartDate"));
        task.setRepeatEndDate(doc.getLong("repeatEndDate"));
        task.setExecutionTime(doc.getLong("executionTime"));
        task.setDifficulty(Difficulty.valueOf(doc.getString("difficulty")));
        task.setImportance(Importance.valueOf(doc.getString("importance")));
        task.setTotalXp(doc.getLong("totalXp").intValue());
        task.setCompleted(doc.getBoolean("completed"));
        return task;
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