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
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.services.StatisticsService;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.UserViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
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
    private LineChart lineChartXP;
    private PieChart pieChartTasks;
    private UserViewModel userViewModel;
    private SharedPrefsManager prefsManager;
    private StatisticsService statisticsService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        // Initialize views
        tvActiveDays = view.findViewById(R.id.tvActiveDays);
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak);
        tvMissionsStarted = view.findViewById(R.id.tvMissionsStarted);
        tvMissionsCompleted = view.findViewById(R.id.tvMissionsCompleted);
        lineChartXP = view.findViewById(R.id.lineChartXP);
        pieChartTasks = view.findViewById(R.id.pieChartTasks);

        prefsManager = new SharedPrefsManager(requireContext());
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        statisticsService = new StatisticsService();

        // Load user data
        String userId = prefsManager.getUserId();
        if (userId != null) {
            userViewModel.loadUser(userId);
        }

        // Observe user data
        userViewModel.userData.observe(getViewLifecycleOwner(), this::displayStatistics);

        return view;
    }

    private void displayStatistics(User user) {
        // ===== AKTIVNI DANI (koristi StatisticsService) =====
        tvActiveDays.setText(statisticsService.formatActiveDays(user.getActiveDays()));

        // ===== NAJDUŽI NIZ (koristi StatisticsService) =====
        tvLongestStreak.setText(statisticsService.formatLongestStreak(user.getLongestStreak()));

        // ===== XP GRAFIKON (koristi StatisticsService) =====
        setupXPLineChart(user.getXpHistory());

        // ===== ZADACI DONUT CHART (koristi StatisticsService) =====
        setupTasksPieChart(user);

        // ===== SPECIJALNE MISIJE =====
        tvMissionsStarted.setText("0");
        tvMissionsCompleted.setText("0");
    }

    private void setupXPLineChart(Map<String, Integer> xpHistory) {
        // ===== KORISTI StatisticsService ZA PRIPREMU PODATAKA =====
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

        LineData lineData = new LineData(dataSet);
        lineChartXP.setData(lineData);

        // Styling
        lineChartXP.getDescription().setEnabled(false);
        lineChartXP.setDrawGridBackground(false);
        lineChartXP.getAxisRight().setEnabled(false);
        lineChartXP.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChartXP.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChartXP.getXAxis().setGranularity(1f);
        lineChartXP.getXAxis().setGranularityEnabled(true);
        lineChartXP.animateX(1000);
        lineChartXP.invalidate();
    }

    private void setupTasksPieChart(User user) {
        // ===== KORISTI StatisticsService ZA PRIPREMU PODATAKA =====
        Map<String, Integer> taskData = statisticsService.prepareTasksPieChartData(user);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(taskData.get("completed"), "Urađeni"));
        entries.add(new PieEntry(taskData.get("unfinished"), "Neurađeni"));
        entries.add(new PieEntry(taskData.get("cancelled"), "Otkazani"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                getResources().getColor(R.color.chart_completed, null),
                getResources().getColor(R.color.chart_unfinished, null),
                getResources().getColor(R.color.chart_cancelled, null)
        );
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieChartTasks.setData(pieData);

        // Styling
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
}