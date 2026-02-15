package com.example.projekatmobilne.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.projekatmobilne.R;

public class StatisticsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        TextView tvPlaceholder = view.findViewById(R.id.tvStatsPlaceholder);
        tvPlaceholder.setText(R.string.stats_coming_soon);

        return view;
    }
}