package com.example.projekatmobilne.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.example.projekatmobilne.adapters.CalendarTaskAdapter;
import com.example.projekatmobilne.enums.FrequencyType;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // Inicijalizacija tvog novog adaptera za slotove
        calendarAdapter = new CalendarTaskAdapter(task -> openTaskDetail(task));
        rvTasks.setAdapter(calendarAdapter);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // Listener za promenu dana
        calendarView.setOnDateChangedListener((widget, date, selected) -> filterTasksForDate(date));

        // Selektuj današnji datum po defaultu
        calendarView.setSelectedDate(CalendarDay.today());

        loadData();
        return v;
    }

    private void loadData() {
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            this.allCategories = categories;

            taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
                Log.d("CALENDAR_DEBUG", "Stiglo taskova: " + tasks.size());
                this.allTasks = tasks;

                decorateCalendar(); // Crtanje tačkica
                filterTasksForDate(calendarView.getSelectedDate()); // Osvežavanje liste slotova
            });
            taskViewModel.loadAllTasks();
        });
    }

    private void decorateCalendar() {
        if (allTasks == null || allTasks.isEmpty()) return;
        calendarView.removeDecorators();

        // 1. Mapa: za svaki dan čuvamo SET unikatnih boja (Set sprečava duple tačkice iste boje)
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

        // 2. Grupisanje dana koji imaju istu kombinaciju boja (radi performansi)
        Map<List<Integer>, HashSet<CalendarDay>> colorsToDaysGroup = new HashMap<>();

        for (Map.Entry<CalendarDay, Set<Integer>> entry : dayToColors.entrySet()) {
            List<Integer> sortedColors = new ArrayList<>(entry.getValue());
            Collections.sort(sortedColors); // Sortiramo da bi kombinacije bile uporedive

            if (!colorsToDaysGroup.containsKey(sortedColors)) {
                colorsToDaysGroup.put(sortedColors, new HashSet<>());
            }
            colorsToDaysGroup.get(sortedColors).add(entry.getKey());
        }

        // 3. Dodavanje dekoratora za svaku kombinaciju boja
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

        // Zadatak 1: SORTIRANJE PO VREMENU (Vremenski slotovi 00-24h)
        Collections.sort(dailyTasks, (t1, t2) -> Long.compare(t1.getExecutionTime(), t2.getExecutionTime()));

        calendarAdapter.setData(dailyTasks, allCategories);
        Log.d("CALENDAR_DEBUG", "Prikazujem " + dailyTasks.size() + " zadataka za " + selectedDay.toString());
    }

    // Pomoćna metoda za konverziju timestamp-a u CalendarDay (ThreeTenABP verzija)
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