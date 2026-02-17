package com.example.projekatmobilne.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.enums.*;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.R;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.example.projekatmobilne.viewModels.TaskViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class AddTaskActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etRepeatInterval;
    private Spinner spinnerCategory, spinnerDifficulty, spinnerImportance, spinnerFrequency, spinnerRepeatUnit;
    private LinearLayout layoutRecurringOptions;
    private TextView tvSelectedTime;
    private Button btnPickTime, btnSaveTask;

    private CategoryViewModel categoryViewModel;
    private TaskViewModel taskViewModel;
    private SharedPrefsManager prefsManager;
    private List<Category> allCategories = new ArrayList<>();

    private long selectedExecutionTime = System.currentTimeMillis();

    private Button btnPickStartDate, btnPickEndDate;
    private TextView tvStartDate, tvEndDate;
    private Long repeatStartDate = null;
    private Long repeatEndDate = null;

    // NOVO: Polja za jednokratni datum
    private Button btnPickDate;
    private long selectedDate = System.currentTimeMillis();
    private boolean isEditMode = false;
    private Task editingTask  = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        initViews();

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        prefsManager = new SharedPrefsManager(this);

        loadCategoriesIntoSpinner();

        // IZMENJENO: Logika za prikazivanje/skrivanje polja zavisno od učestalosti
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

        // Listeneri za datume ponavljanja
        btnPickStartDate.setOnClickListener(v -> showRecurringDatePicker(true));
        btnPickEndDate.setOnClickListener(v -> showRecurringDatePicker(false));

        // NOVO: Listener za datum jednokratnog zadatka
        btnPickDate.setOnClickListener(v -> showSingleTaskDatePicker());

        // >>> PROVERA: Da li je EDIT MODE? <
        String taskId = getIntent().getStringExtra("TASK_ID");
        if (taskId != null) {
            isEditMode = true;
            loadTaskForEditing(taskId);
        } else {
            isEditMode = false;
        }

        // Postavi dugme tekst
        btnSaveTask.setText(isEditMode ? "SAČUVAJ IZMENE" : "DODAJ ZADATAK");

    }

    private void initViews() {
        etTitle = findViewById(R.id.etTaskTitle);
        etDescription = findViewById(R.id.etTaskDescription);
        etRepeatInterval = findViewById(R.id.etRepeatInterval);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        spinnerImportance = findViewById(R.id.spinnerImportance);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        spinnerRepeatUnit = findViewById(R.id.spinnerRepeatUnit);
        layoutRecurringOptions = findViewById(R.id.layoutRecurringOptions);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnPickStartDate = findViewById(R.id.btnPickStartDate);
        btnPickEndDate = findViewById(R.id.btnPickEndDate);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);

        // NOVO
        btnPickDate = findViewById(R.id.btnPickDate);
    }

    // NOVO: Picker za jednokratni zadatak
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

    private void loadTaskForEditing(String taskId) {
        taskViewModel.getTaskById(taskId, new TaskViewModel.TaskByIdCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                editingTask = task;
                Log.d("ADD_TASK", "Task učitan za editovanje: " + task.getTitle());

                // >>> ČEKAJ DA SE KATEGORIJE UČITAJU PA TEK ONDA POPUNI POLJA <
                if (allCategories.isEmpty()) {
                    // Kategorije još nisu učitane - sačekaj observer
                    categoryViewModel.getAllCategories().observe(AddTaskActivity.this, categories -> {
                        if (categories != null && !categories.isEmpty()) {
                            allCategories = categories;
                            populateFields(editingTask);
                            disableNonEditableFields();
                        }
                    });
                } else {
                    // Kategorije su već tu
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

        // Postavi vreme
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(task.getExecutionTime());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        tvSelectedTime.setText("Vreme: " + hour + ":" + (minute < 10 ? "0" + minute : minute));
        selectedExecutionTime = task.getExecutionTime();

        // Postavi težinu
        spinnerDifficulty.setSelection(getDifficultyIndex(task.getDifficulty()));

        // Postavi bitnost
        spinnerImportance.setSelection(getImportanceIndex(task.getImportance()));

        // Postavi kategoriju
        for (int i = 0; i < allCategories.size(); i++) {
            if (allCategories.get(i).getId().equals(task.getCategoryId())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private int getDifficultyIndex(Difficulty diff) {
        switch (diff) {
            case VERY_EASY: return 0;
            case EASY: return 1;
            case HARD: return 2;
            case EXTREME: return 3;
            default: return 0;
        }
    }

    private int getImportanceIndex(Importance imp) {
        switch (imp) {
            case NORMAL: return 0;
            case IMPORTANT: return 1;
            case EXTREME: return 2;
            case SPECIAL: return 3;
            default: return 0;
        }
    }

    private void disableNonEditableFields() {
        // Disable učestalost (ne može se menjati ONE_TIME ↔ RECURRING)
        spinnerFrequency.setEnabled(false);

        // Disable recurring opcije
        layoutRecurringOptions.setVisibility(View.GONE);
        btnPickDate.setVisibility(View.GONE);

        // Kategoriju možeš dozvoliti ili ne (po želji)
        // spinnerCategory.setEnabled(false);
    }

    private void loadCategoriesIntoSpinner() {
        categoryViewModel.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                this.allCategories = categories;
                List<String> categoryNames = new ArrayList<>();
                for (Category c : categories) {
                    categoryNames.add(c.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
            }
        });
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

    private void saveTask() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (title.isEmpty() || allCategories.isEmpty()) {
            Toast.makeText(this, "Naslov i kategorija su obavezni!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode) {
            // >>> EDIT MODE <
            updateExistingTask();
            return;
        }

        String userId = prefsManager.getUserId();
        if(userId == null) return;

        Category selectedCat = allCategories.get(spinnerCategory.getSelectedItemPosition());
        Difficulty diff = Difficulty.valueOf(spinnerDifficulty.getSelectedItem().toString());
        Importance imp = Importance.valueOf(spinnerImportance.getSelectedItem().toString());
        FrequencyType freqType = (spinnerFrequency.getSelectedItemPosition() == 0) ? FrequencyType.ONE_TIME : FrequencyType.RECURRING;

        List<Long> calculatedDates = new ArrayList<>();
        Integer interval = null;
        RepeatUnit unit = null;
        Long finalStartDate = null;
        Long finalEndDate = null;

        // NOVO: Finalni timestamp koji spaja izabrani Datum i izabrano Vreme
        long finalMergedTimestamp;

        if (freqType == FrequencyType.RECURRING) {
            // Logika za PONAVLJAJUĆE (već postoji)
            String intervalStr = etRepeatInterval.getText().toString();
            interval = intervalStr.isEmpty() ? 1 : Integer.parseInt(intervalStr);
            unit = RepeatUnit.valueOf(spinnerRepeatUnit.getSelectedItem().toString().toUpperCase());
            finalStartDate = repeatStartDate;
            finalEndDate = repeatEndDate;

            if (finalStartDate == null || finalEndDate == null) {
                Toast.makeText(this, "Izaberite opseg datuma!", Toast.LENGTH_SHORT).show();
                return;
            }
            calculatedDates = generateRecurringDates(repeatStartDate, repeatEndDate, unit, interval);
            Collections.sort(calculatedDates);
            finalMergedTimestamp = selectedExecutionTime; // Kod ponavljajućih koristimo time picker direktno
        } else {
            // Logika za JEDNOKRATNE (IZMENJENO)
            Calendar finalCal = Calendar.getInstance();
            finalCal.setTimeInMillis(selectedDate); // Postavi izabrani dan

            Calendar timeCal = Calendar.getInstance();
            timeCal.setTimeInMillis(selectedExecutionTime); // Izvuci sate i minute iz time pickera

            finalCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            finalCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            finalCal.set(Calendar.SECOND, 0);
            finalCal.set(Calendar.MILLISECOND, 0);

            finalMergedTimestamp = finalCal.getTimeInMillis();
            calculatedDates.add(finalMergedTimestamp);
        }

        // NOVO: Određivanje inicijalnog statusa (Zadatak 1)
        TaskStatus status = TaskStatus.ACTIVE;
        if (freqType == FrequencyType.ONE_TIME) {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar taskDay = Calendar.getInstance();
            taskDay.setTimeInMillis(finalMergedTimestamp);
            taskDay.set(Calendar.HOUR_OF_DAY, 0);
            taskDay.set(Calendar.MINUTE, 0);
            taskDay.set(Calendar.SECOND, 0);
            taskDay.set(Calendar.MILLISECOND, 0);

            if (taskDay.after(today)) {
                status = TaskStatus.UPCOMING;
            }
        }

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
                finalMergedTimestamp, // Spojeno vreme i datum
                diff,
                imp,
                calculatedDates
        );

        // Postavljamo izračunati status
        newTask.setStatus(status);

        taskViewModel.insertTask(newTask);
        Toast.makeText(this, "Zadatak sačuvan!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateExistingTask() {
        // Ažuriraj samo dozvoljena polja
        editingTask.setTitle(etTitle.getText().toString().trim());
        editingTask.setDescription(etDescription.getText().toString().trim());

        // Ažuriraj vreme (samo HH:mm)
        Calendar newTimeCal = Calendar.getInstance();
        newTimeCal.setTimeInMillis(selectedExecutionTime);

        Calendar oldTimeCal = Calendar.getInstance();
        oldTimeCal.setTimeInMillis(editingTask.getExecutionTime());
        oldTimeCal.set(Calendar.HOUR_OF_DAY, newTimeCal.get(Calendar.HOUR_OF_DAY));
        oldTimeCal.set(Calendar.MINUTE, newTimeCal.get(Calendar.MINUTE));

        editingTask.setExecutionTime(oldTimeCal.getTimeInMillis());

        // Ažuriraj težinu i bitnost
        editingTask.setDifficulty(Difficulty.valueOf(spinnerDifficulty.getSelectedItem().toString()));
        editingTask.setImportance(Importance.valueOf(spinnerImportance.getSelectedItem().toString()));

        // Ponovo izračunaj XP
        editingTask.setTotalXp(editingTask.getDifficulty().getXp() + editingTask.getImportance().getXp());

        // Sačuvaj u bazi
        taskViewModel.updateTask(editingTask);
        Toast.makeText(this, "Izmene sačuvane!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private List<Long> generateRecurringDates(long start, long end, RepeatUnit unit, int interval) {
        List<Long> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(start);
        while (calendar.getTimeInMillis() <= end) {
            dates.add(calendar.getTimeInMillis());
            switch (unit) {
                case DAY: calendar.add(Calendar.DAY_OF_YEAR, interval); break;
                case WEEK: calendar.add(Calendar.WEEK_OF_YEAR, interval); break;
                case MONTH: calendar.add(Calendar.MONTH, interval); break;
                case YEAR: calendar.add(Calendar.YEAR, interval); break;
            }
            if (interval <= 0) break;
        }
        return dates;
    }
}