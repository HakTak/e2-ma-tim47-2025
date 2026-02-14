package com.example.projekatmobilne.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDataBaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "MyHabitApp.db";
    public static final int DATABASE_VERSION = 1;

    // ===============================
    // TABLE TASKS (both one-time and recurring)
    // ===============================
    public static final String TABLE_TASKS = "tasks";

    public static final String COLUMN_TASK_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CATEGORY_ID = "category_id";

    public static final String COLUMN_FREQUENCY_TYPE = "frequency_type";    // ONE_TIME / RECURRING
    public static final String COLUMN_REPEAT_INTERVAL = "repeat_interval";
    public static final String COLUMN_REPEAT_UNIT = "repeat_unit";          // DAY / WEEK
    public static final String COLUMN_REPEAT_START = "repeat_start";        // date long/string
    public static final String COLUMN_REPEAT_END = "repeat_end";

    public static final String COLUMN_EXECUTION_TIME = "execution_time";

    public static final String COLUMN_DIFFICULTY = "difficulty";            // enum
    public static final String COLUMN_IMPORTANCE = "importance";            // enum
    public static final String COLUMN_TOTAL_XP = "total_xp";
    public static final String COLUMN_COMPLETED = "completed";              // 0/1 bool

    public static final String COLUMN_CREATION_DATE = "creation_date";

    // ===============================
    // TABLE CATEGORIES
    // ===============================
    public static final String TABLE_CATEGORIES = "categories";

    public static final String COLUMN_CATEGORY_ID_PK = "id";
    public static final String COLUMN_CATEGORY_NAME = "name";
    public static final String COLUMN_CATEGORY_COLOR = "color_hex";

    public AppDataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // =============== TABLE CATEGORIES =================
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COLUMN_CATEGORY_ID_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CATEGORY_NAME + " TEXT NOT NULL, " +
                COLUMN_CATEGORY_COLOR + " TEXT" +
                ");";
        db.execSQL(CREATE_CATEGORIES_TABLE);

        // =============== TABLE TASKS =================
        String CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_TASKS + " (" +
                COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_CATEGORY_ID + " INTEGER, " +

                COLUMN_FREQUENCY_TYPE + " TEXT, " +
                COLUMN_REPEAT_INTERVAL + " INTEGER, " +
                COLUMN_REPEAT_UNIT + " TEXT, " +
                COLUMN_REPEAT_START + " TEXT, " +
                COLUMN_REPEAT_END + " TEXT, " +

                COLUMN_EXECUTION_TIME + " INTEGER, " +

                COLUMN_DIFFICULTY + " TEXT, " +
                COLUMN_IMPORTANCE + " TEXT, " +
                COLUMN_TOTAL_XP + " INTEGER, " +
                COLUMN_COMPLETED + " INTEGER DEFAULT 0, " +
                COLUMN_CREATION_DATE + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID_PK + ")" +
                ");";
        db.execSQL(CREATE_TASKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop and recreate for upgrade simplicity
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }
}
