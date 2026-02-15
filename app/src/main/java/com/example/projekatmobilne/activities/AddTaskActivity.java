package com.example.projekatmobilne.activities;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
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
    private long selectedExecutionTime = System.currentTimeMillis(); // Default je sad
    private Button btnPickStartDate, btnPickEndDate;
    private TextView tvStartDate, tvEndDate;
    private Long repeatStartDate = null;
    private Long repeatEndDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        initViews();

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        prefsManager = new SharedPrefsManager(this);

        // 1. Popuni spinner kategorijama iz baze
        loadCategoriesIntoSpinner();

        // 2. Logika za prikaz/skrivanje opcija ponavljanja
        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                // Pozicija 1 je "Ponavljajuće" u strings.xml
                layoutRecurringOptions.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // 3. Time Picker Dijalog
        btnPickTime.setOnClickListener(v -> showTimePicker());

        // 4. SAVE DUGME
        btnSaveTask.setOnClickListener(v -> saveTask());

        btnPickStartDate.setOnClickListener(v -> showDatePicker(true));
        btnPickEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar c = Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, year, month, day) -> {
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
    }

    private void loadCategoriesIntoSpinner() {
        categoryViewModel.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                this.allCategories = categories;
                List<String> categoryNames = new ArrayList<>();
                for (Category c : categories) {
                    categoryNames.add(c.getName());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, categoryNames);
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
            tvSelectedTime.setText("Izabrano: " + hour + ":" + (minute < 10 ? "0" + minute : minute));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void saveTask() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (title.isEmpty() || allCategories.isEmpty()) {
            Toast.makeText(this, "Naslov i kategorija su obavezni!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mapiranje Spinnera na tvoje Enume
        Category selectedCat = allCategories.get(spinnerCategory.getSelectedItemPosition());
        String userId = prefsManager.getUserId();
        if(userId == null){
            Toast.makeText(this, "Greska: niste ulogovani!", Toast.LENGTH_SHORT).show();
            return;
        }
        Difficulty diff = Difficulty.valueOf(spinnerDifficulty.getSelectedItem().toString());
        Importance imp = Importance.valueOf(spinnerImportance.getSelectedItem().toString());

        // Učestalost (0 = Jednokratno, 1 = Ponavljajuće)
        FrequencyType freqType = (spinnerFrequency.getSelectedItemPosition() == 0) ?
                FrequencyType.ONE_TIME : FrequencyType.RECURRING;

        List<Long> calculatedDates = new ArrayList<>();
        Integer interval = null;
        RepeatUnit unit = null;
        Long finalStartDate = null;
        Long finalEndDate = null;

        if (freqType == FrequencyType.RECURRING) {
            String intervalStr = etRepeatInterval.getText().toString();
            interval = intervalStr.isEmpty() ? 1 : Integer.parseInt(intervalStr);

            // Pazi: Spinner entries moraju odgovarati Enum imenima (npr. DAY, WEEK)
            unit = RepeatUnit.valueOf(spinnerRepeatUnit.getSelectedItem().toString().toUpperCase());

            finalStartDate = repeatStartDate;
            finalEndDate = repeatEndDate;

            if (finalStartDate == null || finalEndDate == null) {
                Toast.makeText(this, "Izaberite period ponavljanja!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generisanje liste datuma
            calculatedDates = generateRecurringDates(repeatStartDate, repeatEndDate, unit, interval);

            Collections.sort(calculatedDates);

        }else{
            // Za jednokratne zadatke, lista sadrži samo izabrano vreme izvršenja
            calculatedDates.add(selectedExecutionTime);
        }

        Task newTask = new Task(
                prefsManager.getUserId(),
                allCategories.get(spinnerCategory.getSelectedItemPosition()).getId(),
                title,
                etDescription.getText().toString().trim(),
                freqType,
                interval,
                unit,
                finalStartDate,
                finalEndDate,
                selectedExecutionTime,
                Difficulty.valueOf(spinnerDifficulty.getSelectedItem().toString()),
                Importance.valueOf(spinnerImportance.getSelectedItem().toString()),
                calculatedDates
        );

        taskViewModel.insertTask(newTask);
        Toast.makeText(this, "Zadatak sačuvan!", Toast.LENGTH_SHORT).show();
        finish(); // Zatvara AddTaskActivity i vraća nas nazad
    }

    private List<Long> generateRecurringDates(long start, long end, RepeatUnit unit, int interval) {
        List<Long> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(start);

        // Prvi datum je uvek datum početka (ako je unutar opsega)
        while (calendar.getTimeInMillis() <= end) {
            dates.add(calendar.getTimeInMillis());

            // Pomeranje kalendara unapred za zadati interval i jedinicu
            switch (unit) {
                case DAY:
                    calendar.add(Calendar.DAY_OF_YEAR, interval);
                    break;
                case WEEK:
                    calendar.add(Calendar.WEEK_OF_YEAR, interval);
                    break;
                case MONTH:
                    calendar.add(Calendar.MONTH, interval);
                    break;
                case YEAR:
                    calendar.add(Calendar.YEAR, interval);
                    break;
            }

            // Sigurnosna kočnica - sprečava beskonačne petlje ako je interval 0 ili negativan
            if (interval <= 0) break;
        }
        return dates;
    }
}
