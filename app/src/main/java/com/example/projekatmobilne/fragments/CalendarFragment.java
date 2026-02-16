package com.example.projekatmobilne.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.adapters.TaskAdapter;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.utils.EventDecorator;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.example.projekatmobilne.viewModels.TaskViewModel;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private TaskViewModel taskViewModel;
    private CategoryViewModel categoryViewModel;

    private List<Task> allTasks = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = v.findViewById(R.id.calendarView);
        rvTasks = v.findViewById(R.id.rvCalendarTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        // Zadatak 2: Ponovna upotreba adaptera i fragmenta detalja
        adapter = new TaskAdapter(
                (task, newStatus) -> {
                    task.setStatus(newStatus);
                    taskViewModel.updateTask(task);
                },
                task -> { /* brisanje po želji */ },
                this::openTaskDetail
        );
        rvTasks.setAdapter(adapter);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        calendarView.setOnDateChangedListener((widget, date, selected) -> filterTasksForDate(date));

        loadData();
        return v;
    }

    private void loadData() {
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            this.allCategories = categories;
            adapter.setCategories(categories);

            taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
                this.allTasks = tasks;
                decorateCalendar(); // Zadatak 1
                filterTasksForDate(calendarView.getSelectedDate());
            });
            taskViewModel.loadAllTasks();
        });
    }

    // Zadatak 1: Bojenje kalendara
    private void decorateCalendar() {
        calendarView.removeDecorators();
        Map<String, List<CalendarDay>> colorToDates = new HashMap<>();

        for (Task task : allTasks) {
            String color = "#B2BEC3";
            for (Category c : allCategories) {
                if (c.getId().equals(task.getCategoryId())) {
                    color = c.getColorHex();
                    break;
                }
            }

            List<CalendarDay> taskDays = new ArrayList<>();
            if (task.getFrequencyType() == FrequencyType.RECURRING) {
                for (Long time : task.getRecurringDates()) {
                    // KONVERZIJA IZ Long U CalendarDay (Verzija 2.0.1)
                    LocalDate ld = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
                    taskDays.add(CalendarDay.from(ld));
                }
            } else {
                // KONVERZIJA IZ Long U CalendarDay
                LocalDate ld = Instant.ofEpochMilli(task.getExecutionTime()).atZone(ZoneId.systemDefault()).toLocalDate();
                taskDays.add(CalendarDay.from(ld));
            }

            if (!colorToDates.containsKey(color)) colorToDates.put(color, new ArrayList<>());
            colorToDates.get(color).addAll(taskDays);
        }

        for (Map.Entry<String, List<CalendarDay>> entry : colorToDates.entrySet()) {
            calendarView.addDecorator(new EventDecorator(Color.parseColor(entry.getKey()), entry.getValue()));
        }
    }

    private void filterTasksForDate(CalendarDay day) {
        if (day == null) return;
        List<Task> filtered = new ArrayList<>();

        for (Task t : allTasks) {
            boolean matches = false;
            if (t.getFrequencyType() == FrequencyType.RECURRING) {
                for (Long time : t.getRecurringDates()) {
                    LocalDate ld = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
                    if (ld.equals(day.getDate())) matches = true;
                }
            } else {
                LocalDate ld = Instant.ofEpochMilli(t.getExecutionTime()).atZone(ZoneId.systemDefault()).toLocalDate();
                if (ld.equals(day.getDate())) matches = true;
            }
            if (matches) filtered.add(t);
        }
        adapter.setTasks(filtered);
    }

    private boolean isSameDay(long ts, CalendarDay d) {
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(ts);
        return cal.get(Calendar.YEAR) == d.getYear() && cal.get(Calendar.MONTH) == d.getMonth() && cal.get(Calendar.DAY_OF_MONTH) == d.getDay();
    }

    private void openTaskDetail(Task task) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
        fragment.show(getChildFragmentManager(), "task_detail");
    }
}