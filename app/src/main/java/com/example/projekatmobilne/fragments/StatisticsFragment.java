package com.example.projekatmobilne.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.CategoryRepository;
import com.example.projekatmobilne.repositories.TaskRepository;
import com.example.projekatmobilne.services.StatisticsService;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.UserViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private TextView tvActiveDays, tvLongestStreak, tvMissionsStarted, tvMissionsCompleted;
    private TextView tvCategoryPlaceholder, tvDifficultyPlaceholder, tvDifficultyLabel;
    private LineChart lineChartXP, lineChartDifficulty;
    private PieChart pieChartTasks;
    private BarChart barChartCategories;

    private UserViewModel userViewModel;
    private SharedPrefsManager prefsManager;
    private StatisticsService statisticsService;
    private TaskRepository taskRepository;
    private CategoryRepository categoryRepository;

    private User currentUser;
    private List<Task> currentTasks = new ArrayList<>();
    private List<Category> currentCategories = new ArrayList<>();

    private boolean userLoaded = false;
    private boolean tasksLoaded = false;
    private boolean categoriesLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        initViews(view);

        prefsManager = new SharedPrefsManager(requireContext());
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        statisticsService = new StatisticsService();
        taskRepository = new TaskRepository();
        categoryRepository = new CategoryRepository();

        String userId = prefsManager.getUserId();
        if (userId != null) {
            userViewModel.loadUser(userId);
            loadTasks(userId);
            loadCategories(userId);
        }

        userViewModel.userData.observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            userLoaded = true;
            tryDisplayAll();
        });

        return view;
    }

    private void initViews(View view) {
        tvActiveDays = view.findViewById(R.id.tvActiveDays);
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak);
        tvMissionsStarted = view.findViewById(R.id.tvMissionsStarted);
        tvMissionsCompleted = view.findViewById(R.id.tvMissionsCompleted);
        tvCategoryPlaceholder = view.findViewById(R.id.tvCategoryPlaceholder);
        tvDifficultyPlaceholder = view.findViewById(R.id.tvDifficultyPlaceholder);
        tvDifficultyLabel = view.findViewById(R.id.tvDifficultyLabel);
        lineChartXP = view.findViewById(R.id.lineChartXP);
        lineChartDifficulty = view.findViewById(R.id.lineChartDifficulty);
        pieChartTasks = view.findViewById(R.id.pieChartTasks);
        barChartCategories = view.findViewById(R.id.barChartCategories);
    }

    private void loadTasks(String userId) {
        taskRepository.getAllTasks(userId, new TaskRepository.TasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                if (!isAdded()) return;
                currentTasks = tasks;
                tasksLoaded = true;
                tryDisplayAll();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                currentTasks = new ArrayList<>();
                tasksLoaded = true;
                tryDisplayAll();
            }
        });
    }

    private void loadCategories(String userId) {
        categoryRepository.getAllCategories(userId, new CategoryRepository.CategoriesCallback() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                if (!isAdded()) return;
                currentCategories = categories;
                categoriesLoaded = true;
                tryDisplayAll();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                currentCategories = new ArrayList<>();
                categoriesLoaded = true;
                tryDisplayAll();
            }
        });
    }

    private void tryDisplayAll() {
        if (!userLoaded || !tasksLoaded || !categoriesLoaded) return;
        if (currentUser == null) return;
        displayStatistics();
    }

    private void displayStatistics() {
        tvActiveDays.setText(statisticsService.formatActiveDays(currentUser.getActiveDays()));

        // Longest streak iz taskova (ispravna implementacija prema specifikaciji)
        int longestStreak = statisticsService.calculateLongestStreakFromTasks(currentTasks);
        tvLongestStreak.setText(statisticsService.formatLongestStreak(longestStreak));

        setupXPLineChart(currentUser.getXpHistory());
        setupTasksPieChart();
        setupBarChart();
        setupDifficultyLineChart();
        tvMissionsStarted.setText("0");
        tvMissionsCompleted.setText("0");
    }

    // ===================================================
    // XP LINE CHART
    // ===================================================

    private void setupXPLineChart(Map<String, Integer> xpHistory) {
        Map<String, Object> chartData = statisticsService.prepareXPChartData(xpHistory);
        List<Integer> xpValues = (List<Integer>) chartData.get("values");
        List<String> labels = (List<String>) chartData.get("labels");

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < xpValues.size(); i++) {
            entries.add(new Entry(i, xpValues.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP");
        dataSet.setColor(getResources().getColor(R.color.chart_xp, null));
        dataSet.setCircleColor(getResources().getColor(R.color.chart_xp, null));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.chart_xp, null));
        dataSet.setFillAlpha(50);

        lineChartXP.setData(new LineData(dataSet));
        styleLineChart(lineChartXP, labels);
    }

    // ===================================================
    // PIE CHART (DONUT) - sa aktivnim zadacima
    // ===================================================

    private void setupTasksPieChart() {
        Map<String, Integer> taskData = statisticsService.prepareTasksPieChartData(
                currentUser, currentTasks);

        int completed = taskData.get("completed");
        int unfinished = taskData.get("unfinished");
        int cancelled = taskData.get("cancelled");
        int active = taskData.get("active");

        if (completed == 0 && unfinished == 0 && cancelled == 0 && active == 0) {
            pieChartTasks.setVisibility(View.GONE);
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (completed > 0) {
            entries.add(new PieEntry(completed, "Urađeni"));
            colors.add(getResources().getColor(R.color.chart_completed, null));
        }
        if (unfinished > 0) {
            entries.add(new PieEntry(unfinished, "Neurađeni"));
            colors.add(getResources().getColor(R.color.chart_unfinished, null));
        }
        if (cancelled > 0) {
            entries.add(new PieEntry(cancelled, "Otkazani"));
            colors.add(getResources().getColor(R.color.chart_cancelled, null));
        }
        if (active > 0) {
            entries.add(new PieEntry(active, "Aktivni"));
            colors.add(Color.parseColor("#2196F3")); // plava za aktivne
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        pieChartTasks.setData(new PieData(dataSet));
        pieChartTasks.setVisibility(View.VISIBLE);
        pieChartTasks.getDescription().setEnabled(false);
        pieChartTasks.setDrawHoleEnabled(true);
        pieChartTasks.setHoleColor(Color.WHITE);
        pieChartTasks.setHoleRadius(40f);
        pieChartTasks.setTransparentCircleRadius(45f);
        pieChartTasks.setCenterText("Zadaci");
        pieChartTasks.setCenterTextSize(18f);
        pieChartTasks.animateY(1000);
        pieChartTasks.invalidate();
    }

    // ===================================================
    // BAR CHART - po kategoriji
    // ===================================================

    private void setupBarChart() {
        if (currentCategories.isEmpty()) {
            barChartCategories.setVisibility(View.GONE);
            if (tvCategoryPlaceholder != null)
                tvCategoryPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        Map<String, Integer> data = statisticsService.prepareBarChartData(
                currentTasks, currentCategories);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int index = 0;
        for (Category cat : currentCategories) {
            int count = data.containsKey(cat.getId()) ? data.get(cat.getId()) : 0;
            entries.add(new BarEntry(index, count));
            labels.add(cat.getName());
            try {
                colors.add(Color.parseColor(cat.getColorHex()));
            } catch (Exception e) {
                colors.add(Color.GRAY);
            }
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Završeni zadaci");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        barChartCategories.setData(barData);
        barChartCategories.setVisibility(View.VISIBLE);
        if (tvCategoryPlaceholder != null)
            tvCategoryPlaceholder.setVisibility(View.GONE);

        barChartCategories.getDescription().setEnabled(false);
        barChartCategories.setFitBars(true);
        barChartCategories.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartCategories.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartCategories.getXAxis().setGranularity(1f);
        barChartCategories.getXAxis().setGranularityEnabled(true);
        barChartCategories.getAxisRight().setEnabled(false);
        barChartCategories.getAxisLeft().setAxisMinimum(0f);
        barChartCategories.getLegend().setEnabled(false);
        barChartCategories.animateY(1000);
        barChartCategories.invalidate();
    }

    // ===================================================
    // DIFFICULTY LINE CHART
    // ===================================================

    private void setupDifficultyLineChart() {
        // Uvijek prikaži difficulty label
        String difficultyLabel = statisticsService.getAverageDifficultyLabel(currentTasks);
        if (!difficultyLabel.isEmpty() && tvDifficultyLabel != null) {
            tvDifficultyLabel.setText(difficultyLabel);
            tvDifficultyLabel.setVisibility(View.VISIBLE);
        }

        if (currentTasks.isEmpty()) {
            lineChartDifficulty.setVisibility(View.GONE);
            if (tvDifficultyPlaceholder != null)
                tvDifficultyPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        Map<String, Object> chartData = statisticsService.prepareDifficultyChartData(currentTasks);
        List<Float> values = (List<Float>) chartData.get("values");
        List<String> labels = (List<String>) chartData.get("labels");

        boolean hasData = false;
        for (Float v : values) {
            if (v > 0) { hasData = true; break; }
        }

        if (!hasData) {
            lineChartDifficulty.setVisibility(View.GONE);
            if (tvDifficultyPlaceholder != null)
                tvDifficultyPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new Entry(i, values.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Prosječan XP");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setCircleColor(Color.parseColor("#FF9800"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(11f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FF9800"));
        dataSet.setFillAlpha(40);

        lineChartDifficulty.setData(new LineData(dataSet));
        lineChartDifficulty.setVisibility(View.VISIBLE);
        if (tvDifficultyPlaceholder != null)
            tvDifficultyPlaceholder.setVisibility(View.GONE);

        styleLineChart(lineChartDifficulty, labels);
    }

    // ===================================================
    // HELPER
    // ===================================================

    private void styleLineChart(LineChart chart, List<String> labels) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setGranularityEnabled(true);
        chart.animateX(1000);
        chart.invalidate();
    }
}