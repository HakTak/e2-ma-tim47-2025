package com.example.projekatmobilne.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.adapters.CalendarTaskAdapter;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.utils.EventDecorator;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.example.projekatmobilne.viewModels.TaskViewModel;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.util.*;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private RecyclerView rvTasks;
    private CalendarTaskAdapter calendarAdapter;
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

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // >>> IZMENJENO: Dodaj statusChangeListener callback <
        calendarAdapter = new CalendarTaskAdapter(
                task -> openTaskDetail(task),
                () -> getSelectedDateTimestamp(),
                (task, newStatus, dateContext) -> {
                    // >>> CALLBACK ZA PROMENU STATUSA <
                    Log.d("CALENDAR_FRAGMENT", "=== Status promenjen ===");
                    Log.d("CALENDAR_FRAGMENT", "Task: " + task.getTitle());
                    Log.d("CALENDAR_FRAGMENT", "Datum: " + new Date(dateContext));
                    Log.d("CALENDAR_FRAGMENT", "Novi status: " + newStatus);
                    Log.d("CALENDAR_FRAGMENT", "Mapa PRE: " + task.getOccurrenceStatuses());

                    // >>> POSTAVI STATUS ZA DATUM <
                    task.setStatusForDate(dateContext, newStatus);

                    Log.d("CALENDAR_FRAGMENT", "Mapa POSLE: " + task.getOccurrenceStatuses());

                    // >>> AŽURIRAJ U BAZI <
                    taskViewModel.updateTask(task);
                    Toast.makeText(getContext(), "Status promenjen u " + newStatus.name(), Toast.LENGTH_SHORT).show();
                }
        );
        rvTasks.setAdapter(calendarAdapter);

        // Listener za promenu dana
        calendarView.setOnDateChangedListener((widget, date, selected) -> filterTasksForDate(date));

        // Selektuj današnji datum po defaultu
        calendarView.setSelectedDate(CalendarDay.today());

        loadData();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("CALENDAR_DEBUG", "onResume() - osvežavam taskove");
        taskViewModel.loadAllTasks();
    }
    private long getSelectedDateTimestamp() {
        CalendarDay selectedDay = calendarView.getSelectedDate();
        if (selectedDay == null) {
            selectedDay = CalendarDay.today();
        }

        LocalDate ld = selectedDay.getDate();
        return ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void loadData() {
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            this.allCategories = categories;

            taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
                Log.d("CALENDAR_DEBUG", "Stiglo taskova: " + tasks.size());
                this.allTasks = tasks;

                decorateCalendar();
                filterTasksForDate(calendarView.getSelectedDate());
            });
            taskViewModel.loadAllTasks();
        });
    }

    private void decorateCalendar() {
        if (allTasks == null || allTasks.isEmpty()) return;
        calendarView.removeDecorators();

        Map<CalendarDay, Set<Integer>> dayToColors = new HashMap<>();

        for (Task task : allTasks) {
            int color = Color.parseColor(getCatColor(task.getCategoryId()));
            List<CalendarDay> days = new ArrayList<>();

            if (task.getFrequencyType() == FrequencyType.RECURRING && task.getRecurringDates() != null) {
                for (Long ts : task.getRecurringDates()) days.add(convertToCalendarDay(ts));
            } else {
                days.add(convertToCalendarDay(task.getExecutionTime()));
            }

            for (CalendarDay day : days) {
                if (!dayToColors.containsKey(day)) dayToColors.put(day, new HashSet<>());
                dayToColors.get(day).add(color);
            }
        }

        Map<List<Integer>, HashSet<CalendarDay>> colorsToDaysGroup = new HashMap<>();

        for (Map.Entry<CalendarDay, Set<Integer>> entry : dayToColors.entrySet()) {
            List<Integer> sortedColors = new ArrayList<>(entry.getValue());
            Collections.sort(sortedColors);

            if (!colorsToDaysGroup.containsKey(sortedColors)) {
                colorsToDaysGroup.put(sortedColors, new HashSet<>());
            }
            colorsToDaysGroup.get(sortedColors).add(entry.getKey());
        }

        for (Map.Entry<List<Integer>, HashSet<CalendarDay>> entry : colorsToDaysGroup.entrySet()) {
            calendarView.addDecorator(new EventDecorator(entry.getKey(), entry.getValue()));
        }

        calendarView.invalidateDecorators();
    }

    private void filterTasksForDate(CalendarDay selectedDay) {
        if (selectedDay == null) return;
        List<Task> dailyTasks = new ArrayList<>();

        for (Task t : allTasks) {
            boolean isToday = false;
            if (t.getFrequencyType() == FrequencyType.RECURRING && t.getRecurringDates() != null) {
                for (Long ts : t.getRecurringDates()) {
                    if (convertToCalendarDay(ts).equals(selectedDay)) isToday = true;
                }
            } else {
                if (convertToCalendarDay(t.getExecutionTime()).equals(selectedDay)) isToday = true;
            }

            if (isToday) dailyTasks.add(t);
        }

        Collections.sort(dailyTasks, (t1, t2) -> Long.compare(t1.getExecutionTime(), t2.getExecutionTime()));

        calendarAdapter.setData(dailyTasks, allCategories);
        Log.d("CALENDAR_DEBUG", "Prikazujem " + dailyTasks.size() + " zadataka za " + selectedDay.toString());
    }

    private CalendarDay convertToCalendarDay(long timestamp) {
        LocalDate ld = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return CalendarDay.from(ld);
    }

    private String getCatColor(String id) {
        for (Category c : allCategories) {
            if (c.getId().equals(id)) return c.getColorHex();
        }
        return "#B2BEC3";
    }

    private void openTaskDetail(Task task) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
        fragment.show(getChildFragmentManager(), "task_detail");
    }
}