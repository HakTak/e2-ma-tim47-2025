package com.example.projekatmobilne.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.Difficulty;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.Importance;
import com.example.projekatmobilne.enums.RepeatUnit;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.services.TaskService;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.example.projekatmobilne.viewModels.TaskViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class AddTaskActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etRepeatInterval;
    private Spinner spinnerCategory, spinnerDifficulty, spinnerImportance,
            spinnerFrequency, spinnerRepeatUnit;
    private LinearLayout layoutRecurringOptions;
    private TextView tvSelectedTime;
    private Button btnPickTime, btnSaveTask;
    private Button btnPickStartDate, btnPickEndDate;
    private TextView tvStartDate, tvEndDate;
    private Button btnPickDate;

    private CategoryViewModel categoryViewModel;
    private TaskViewModel taskViewModel;
    private TaskService taskService;
    private SharedPrefsManager prefsManager;

    private List<Category> allCategories = new ArrayList<>();

    private long selectedExecutionTime = System.currentTimeMillis();
    private Long repeatStartDate = null;
    private Long repeatEndDate   = null;
    private long selectedDate    = System.currentTimeMillis();

    private boolean isEditMode  = false;
    private Task editingTask    = null;

    // ===================================================
    // LIFECYCLE
    // ===================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        initViews();

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        taskViewModel     = new ViewModelProvider(this).get(TaskViewModel.class);
        taskService       = new TaskService();
        prefsManager      = new SharedPrefsManager(this);

        loadCategoriesIntoSpinner();

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos == 1) { // Ponavljajuće
                    layoutRecurringOptions.setVisibility(View.VISIBLE);
                    btnPickDate.setVisibility(View.GONE);
                } else { // Jednokratno
                    layoutRecurringOptions.setVisibility(View.GONE);
                    btnPickDate.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnSaveTask.setOnClickListener(v -> saveTask());
        btnPickStartDate.setOnClickListener(v -> showRecurringDatePicker(true));
        btnPickEndDate.setOnClickListener(v -> showRecurringDatePicker(false));
        btnPickDate.setOnClickListener(v -> showSingleTaskDatePicker());

        String taskId = getIntent().getStringExtra("TASK_ID");
        if (taskId != null) {
            isEditMode = true;
            loadTaskForEditing(taskId);
        }

        btnSaveTask.setText(isEditMode ? "SAČUVAJ IZMENE" : "DODAJ ZADATAK");
    }

    // ===================================================
    // INIT
    // ===================================================

    private void initViews() {
        etTitle               = findViewById(R.id.etTaskTitle);
        etDescription         = findViewById(R.id.etTaskDescription);
        etRepeatInterval      = findViewById(R.id.etRepeatInterval);
        spinnerCategory       = findViewById(R.id.spinnerCategory);
        spinnerDifficulty     = findViewById(R.id.spinnerDifficulty);
        spinnerImportance     = findViewById(R.id.spinnerImportance);
        spinnerFrequency      = findViewById(R.id.spinnerFrequency);
        spinnerRepeatUnit     = findViewById(R.id.spinnerRepeatUnit);
        layoutRecurringOptions = findViewById(R.id.layoutRecurringOptions);
        tvSelectedTime        = findViewById(R.id.tvSelectedTime);
        btnPickTime           = findViewById(R.id.btnPickTime);
        btnSaveTask           = findViewById(R.id.btnSaveTask);
        btnPickStartDate      = findViewById(R.id.btnPickStartDate);
        btnPickEndDate        = findViewById(R.id.btnPickEndDate);
        tvStartDate           = findViewById(R.id.tvStartDate);
        tvEndDate             = findViewById(R.id.tvEndDate);
        btnPickDate           = findViewById(R.id.btnPickDate);
    }

    // ===================================================
    // DATE/TIME PICKERI
    // ===================================================

    private void showSingleTaskDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);
            selectedDate = selected.getTimeInMillis();
            btnPickDate.setText("Datum: " + day + "." + (month + 1) + "." + year + ".");
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showRecurringDatePicker(boolean isStartDate) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, day);

            long time = c.getTimeInMillis();
            String dateStr = day + "." + (month + 1) + "." + year + ".";

            if (isStartDate) {
                repeatStartDate = time;
                tvStartDate.setText(dateStr);
            } else {
                repeatEndDate = time;
                tvEndDate.setText(dateStr);
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);
            selectedExecutionTime = c.getTimeInMillis();
            tvSelectedTime.setText("Vreme: " + hour + ":" + (minute < 10 ? "0" + minute : minute));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    // ===================================================
    // EDIT MODE
    // ===================================================

    private void loadTaskForEditing(String taskId) {
        taskViewModel.getTaskById(taskId, new TaskViewModel.TaskByIdCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                editingTask = task;
                Log.d("ADD_TASK", "Task učitan za editovanje: " + task.getTitle());

                if (allCategories.isEmpty()) {
                    categoryViewModel.getAllCategories().observe(AddTaskActivity.this, categories -> {
                        if (categories != null && !categories.isEmpty()) {
                            allCategories = categories;
                            populateFields(editingTask);
                            disableNonEditableFields();
                        }
                    });
                } else {
                    populateFields(task);
                    disableNonEditableFields();
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ADD_TASK", "Greška: " + error);
                Toast.makeText(AddTaskActivity.this, "Greška: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateFields(Task task) {
        etTitle.setText(task.getTitle());
        etDescription.setText(task.getDescription());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(task.getExecutionTime());
        int hour   = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        tvSelectedTime.setText("Vreme: " + hour + ":" + (minute < 10 ? "0" + minute : minute));
        selectedExecutionTime = task.getExecutionTime();

        spinnerDifficulty.setSelection(getDifficultyIndex(task.getDifficulty()));
        spinnerImportance.setSelection(getImportanceIndex(task.getImportance()));

        for (int i = 0; i < allCategories.size(); i++) {
            if (allCategories.get(i).getId().equals(task.getCategoryId())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void disableNonEditableFields() {
        // Disable učestalost (ne može se menjati ONE_TIME ↔ RECURRING)
        spinnerFrequency.setEnabled(false);

        // Disable recurring opcije
        layoutRecurringOptions.setVisibility(View.GONE);
        btnPickDate.setVisibility(View.GONE);

        // Kategorija je sada editabilna — ne disable-ujemo je
    }

    private int getDifficultyIndex(Difficulty diff) {
        switch (diff) {
            case VERY_EASY: return 0;
            case EASY:      return 1;
            case HARD:      return 2;
            case EXTREME:   return 3;
            default:        return 0;
        }
    }

    private int getImportanceIndex(Importance imp) {
        switch (imp) {
            case NORMAL:    return 0;
            case IMPORTANT: return 1;
            case EXTREME:   return 2;
            case SPECIAL:   return 3;
            default:        return 0;
        }
    }

    // ===================================================
    // CATEGORIES
    // ===================================================

    private void loadCategoriesIntoSpinner() {
        categoryViewModel.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                this.allCategories = categories;
                List<String> names = new ArrayList<>();
                for (Category c : categories) names.add(c.getName());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, names);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
            }
        });
    }

    // ===================================================
    // SAVE / UPDATE
    // ===================================================

    private void saveTask() {
        String title = etTitle.getText().toString().trim();
        String desc  = etDescription.getText().toString().trim();

        if (title.isEmpty() || allCategories.isEmpty()) {
            Toast.makeText(this, "Naslov i kategorija su obavezni!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode) {
            updateExistingTask();
            return;
        }

        String userId = prefsManager.getUserId();
        if (userId == null) return;

        Category selectedCat = allCategories.get(spinnerCategory.getSelectedItemPosition());
        Difficulty diff = Difficulty.valueOf(spinnerDifficulty.getSelectedItem().toString());
        Importance imp  = Importance.valueOf(spinnerImportance.getSelectedItem().toString());
        FrequencyType freqType = (spinnerFrequency.getSelectedItemPosition() == 0)
                ? FrequencyType.ONE_TIME : FrequencyType.RECURRING;

        List<Long> calculatedDates = new ArrayList<>();
        Integer interval     = null;
        RepeatUnit unit      = null;
        Long finalStartDate  = null;
        Long finalEndDate    = null;
        long finalMergedTimestamp;

        if (freqType == FrequencyType.RECURRING) {
            String intervalStr = etRepeatInterval.getText().toString();
            interval = intervalStr.isEmpty() ? 1 : Integer.parseInt(intervalStr);
            unit = RepeatUnit.valueOf(spinnerRepeatUnit.getSelectedItem().toString().toUpperCase());
            finalStartDate = repeatStartDate;
            finalEndDate   = repeatEndDate;

            if (finalStartDate == null || finalEndDate == null) {
                Toast.makeText(this, "Izaberite opseg datuma!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generiši datume kroz servis (ne lokalno)
            calculatedDates = taskService.generateRecurringDates(
                    repeatStartDate, repeatEndDate, unit, interval);
            Collections.sort(calculatedDates);

            finalMergedTimestamp = selectedExecutionTime;
        } else {
            // Spoji datum i vreme kroz servis
            finalMergedTimestamp = taskService.mergeDateTime(selectedDate, selectedExecutionTime);
            calculatedDates.add(finalMergedTimestamp);
        }

        // Odredi inicijalni status kroz servis
        TaskStatus status = taskService.calculateInitialStatus(freqType, finalMergedTimestamp);

        Task newTask = new Task(
                userId,
                selectedCat.getId(),
                title,
                desc,
                freqType,
                interval,
                unit,
                finalStartDate,
                finalEndDate,
                finalMergedTimestamp,
                diff,
                imp,
                calculatedDates
        );
        newTask.setStatus(status);

        taskViewModel.insertTask(newTask);
        Toast.makeText(this, "Zadatak sačuvan!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateExistingTask() {
        // Ažuriraj metapodatke — važe za ceo task
        editingTask.setTitle(etTitle.getText().toString().trim());
        editingTask.setDescription(etDescription.getText().toString().trim());

        // Ažuriraj kategoriju na izabranu iz spinnera
        Category selectedCat = allCategories.get(spinnerCategory.getSelectedItemPosition());
        editingTask.setCategoryId(selectedCat.getId());

        // Ažuriraj težinu i bitnost
        editingTask.setDifficulty(Difficulty.valueOf(spinnerDifficulty.getSelectedItem().toString()));
        editingTask.setImportance(Importance.valueOf(spinnerImportance.getSelectedItem().toString()));
        editingTask.setTotalXp(editingTask.getDifficulty().getXp() + editingTask.getImportance().getXp());

        if (editingTask.getFrequencyType() == FrequencyType.ONE_TIME) {
            // Jednokratni: spoji stari datum sa novim vremenom
            long updatedTime = taskService.mergeDateTime(
                    editingTask.getExecutionTime(), selectedExecutionTime);
            editingTask.setExecutionTime(updatedTime);

        } else {
            // Recurring: ažuriraj vreme SAMO za buduće ACTIVE datume
            Calendar newTimeCal = Calendar.getInstance();
            newTimeCal.setTimeInMillis(selectedExecutionTime);
            int newHour   = newTimeCal.get(Calendar.HOUR_OF_DAY);
            int newMinute = newTimeCal.get(Calendar.MINUTE);

            taskService.updateFutureOccurrenceTimes(editingTask, newHour, newMinute);

            // Ažuriraj i executionTime na tasku (referentno vreme)
            long updatedTime = taskService.mergeDateTime(
                    editingTask.getExecutionTime(), selectedExecutionTime);
            editingTask.setExecutionTime(updatedTime);
        }

        taskViewModel.updateTask(editingTask);
        Toast.makeText(this, "Izmene sačuvane!", Toast.LENGTH_SHORT).show();
        finish();
    }
}