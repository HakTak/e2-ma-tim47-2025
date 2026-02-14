package com.example.projekatmobilne.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.example.projekatmobilne.Enum.Difficulty;
import com.example.projekatmobilne.Enum.FrequencyType;
import com.example.projekatmobilne.Enum.Importance;
import com.example.projekatmobilne.Enum.RepeatUnit;
import com.example.projekatmobilne.Model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    private final AppDataBaseHelper dbHelper;
    private SQLiteDatabase database;

    public TaskRepository(Context context) {
        dbHelper = new AppDataBaseHelper(context);
    }

    // Otvori/zatvori konekciju
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // =====================================================
    // INSERT - dodaj novi task
    // =====================================================
    public long insertTask(Task task) {
        database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(AppDataBaseHelper.COLUMN_TITLE, task.getTitle());
        values.put(AppDataBaseHelper.COLUMN_DESCRIPTION, task.getDescription());
        values.put(AppDataBaseHelper.COLUMN_CATEGORY_ID, task.getCategoryId());
        values.put(AppDataBaseHelper.COLUMN_FREQUENCY_TYPE, task.getFrequencyType().name());
        values.put(AppDataBaseHelper.COLUMN_REPEAT_INTERVAL, task.getRepeatInterval());
        values.put(AppDataBaseHelper.COLUMN_REPEAT_UNIT, task.getRepeatUnit() != null ? task.getRepeatUnit().name() : null);
        values.put(AppDataBaseHelper.COLUMN_REPEAT_START, task.getRepeatStartDate() != null ? String.valueOf(task.getRepeatStartDate()) : null);
        values.put(AppDataBaseHelper.COLUMN_REPEAT_END, task.getRepeatEndDate() != null ? String.valueOf(task.getRepeatEndDate()) : null);
        values.put(AppDataBaseHelper.COLUMN_EXECUTION_TIME, task.getExecutionTime());
        values.put(AppDataBaseHelper.COLUMN_DIFFICULTY, task.getDifficulty().name());
        values.put(AppDataBaseHelper.COLUMN_IMPORTANCE, task.getImportance().name());
        values.put(AppDataBaseHelper.COLUMN_TOTAL_XP, task.getTotalXp());
        values.put(AppDataBaseHelper.COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(AppDataBaseHelper.COLUMN_CREATION_DATE, String.valueOf(System.currentTimeMillis()));

        long newRowId = database.insert(AppDataBaseHelper.TABLE_TASKS, null, values);

        return newRowId;
    }

    // =====================================================
    // GET ALL - svi taskovi
    // =====================================================
    public List<Task> getAllTasks() {
        List<Task> taskList = new ArrayList<>();
        database = dbHelper.getReadableDatabase();

        Cursor cursor = database.query(
                AppDataBaseHelper.TABLE_TASKS,
                null,
                null,
                null,
                null,
                null,
                AppDataBaseHelper.COLUMN_EXECUTION_TIME + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                taskList.add(task);
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();

        return taskList;
    }

    // =====================================================
    // GET BY ID
    // =====================================================
    public Task getTaskById(long id) {
        Task task = null;
        database = dbHelper.getReadableDatabase();

        String selection = AppDataBaseHelper.COLUMN_TASK_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = database.query(
                AppDataBaseHelper.TABLE_TASKS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            task = cursorToTask(cursor);
        }

        if (cursor != null) cursor.close();

        return task;
    }

    // =====================================================
    // UPDATE - ažuriranje taska (npr. promena statusa)
    // =====================================================
    public int updateTask(Task task) {
        database = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(AppDataBaseHelper.COLUMN_TITLE, task.getTitle());
        values.put(AppDataBaseHelper.COLUMN_DESCRIPTION, task.getDescription());
        values.put(AppDataBaseHelper.COLUMN_CATEGORY_ID, task.getCategoryId());
        values.put(AppDataBaseHelper.COLUMN_EXECUTION_TIME, task.getExecutionTime());
        values.put(AppDataBaseHelper.COLUMN_FREQUENCY_TYPE, task.getFrequencyType().name());
        values.put(AppDataBaseHelper.COLUMN_REPEAT_INTERVAL, task.getRepeatInterval());
        values.put(AppDataBaseHelper.COLUMN_REPEAT_UNIT, task.getRepeatUnit() != null ? task.getRepeatUnit().name() : null);
        values.put(AppDataBaseHelper.COLUMN_REPEAT_START, task.getRepeatStartDate() != null ? String.valueOf(task.getRepeatStartDate()) : null);
        values.put(AppDataBaseHelper.COLUMN_REPEAT_END, task.getRepeatEndDate() != null ? String.valueOf(task.getRepeatEndDate()) : null);
        values.put(AppDataBaseHelper.COLUMN_DIFFICULTY, task.getDifficulty().name());
        values.put(AppDataBaseHelper.COLUMN_IMPORTANCE, task.getImportance().name());
        values.put(AppDataBaseHelper.COLUMN_TOTAL_XP, task.getTotalXp());
        values.put(AppDataBaseHelper.COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);

        String whereClause = AppDataBaseHelper.COLUMN_TASK_ID + " = ?";
        String[] whereArgs = {String.valueOf(task.getId())};

        int rowsAffected = database.update(AppDataBaseHelper.TABLE_TASKS, values, whereClause, whereArgs);

        return rowsAffected;
    }

    // =====================================================
    // DELETE - brisanje taska po id-u
    // =====================================================
    public int deleteTask(long id) {
        database = dbHelper.getWritableDatabase();
        String whereClause = AppDataBaseHelper.COLUMN_TASK_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        int rowsDeleted = database.delete(AppDataBaseHelper.TABLE_TASKS, whereClause, whereArgs);

        return rowsDeleted;
    }

    // =====================================================
    // UPDATE STATUS - markiraj kao završen
    // =====================================================
    public int markAsCompleted(long id) {
        database = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppDataBaseHelper.COLUMN_COMPLETED, 1);

        String where = AppDataBaseHelper.COLUMN_TASK_ID + " = ?";
        String[] args = {String.valueOf(id)};

        int rows = database.update(AppDataBaseHelper.TABLE_TASKS, values, where, args);

        return rows;
    }

    // =====================================================
    // Privatna pomoćna metoda: konverzija Cursor → Task
    // =====================================================
    private Task cursorToTask(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_TASK_ID));
        long categoryId = cursor.getLong(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_CATEGORY_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_TITLE));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_DESCRIPTION));

        FrequencyType freqType = FrequencyType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_FREQUENCY_TYPE)));
        Integer interval = cursor.isNull(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_REPEAT_INTERVAL)) ? null : cursor.getInt(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_REPEAT_INTERVAL));
        RepeatUnit repeatUnit = null;
        String repeatUnitStr = cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_REPEAT_UNIT));
        if (repeatUnitStr != null) repeatUnit = RepeatUnit.valueOf(repeatUnitStr);

        Long repeatStart = cursor.isNull(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_REPEAT_START)) ? null : Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_REPEAT_START)));
        Long repeatEnd = cursor.isNull(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_REPEAT_END)) ? null : Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_REPEAT_END)));
        long executionTime = cursor.getLong(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_EXECUTION_TIME));

        Difficulty difficulty = Difficulty.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_DIFFICULTY)));
        Importance importance = Importance.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_IMPORTANCE)));
        int totalXp = cursor.getInt(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_TOTAL_XP));

        boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_COMPLETED)) == 1;

        Task task = new Task(
                categoryId,
                title,
                description,
                freqType,
                interval,
                repeatUnit,
                repeatStart,
                repeatEnd,
                executionTime,
                difficulty,
                importance
        );
        task.setId(id);
        task.setCompleted(completed);

        return task;
    }
}

