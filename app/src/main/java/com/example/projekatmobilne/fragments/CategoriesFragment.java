package com.example.projekatmobilne.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.activities.TasksActivity;
import com.example.projekatmobilne.adapters.CategoryAdapter;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CategoriesFragment extends Fragment {

    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;
    private String selectedColor = "#607D8B";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // Setup RecyclerView
        RecyclerView rv = view.findViewById(R.id.recyclerViewCategories);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CategoryAdapter(
                category -> {
                    // Običan klik -> otvori TasksActivity
                    Intent intent = new Intent(getActivity(), TasksActivity.class);
                    intent.putExtra("CATEGORY_ID", category.getId());
                    intent.putExtra("CATEGORY_NAME", category.getName());
                    startActivity(intent);
                },
                category -> {
                    // Dugi klik -> brisanje
                    showDeleteConfirmationDialog(category);
                }
        );
        rv.setAdapter(adapter);

        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        // Observe data
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            adapter.setCategories(categories);
        });

        // FAB dugme
        FloatingActionButton fab = view.findViewById(R.id.btnAddCategory);
        fab.setOnClickListener(v -> showAddCategoryDialog());

        return view;
    }

    private void showDeleteConfirmationDialog(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Brisanje kategorije")
                .setMessage("Da li ste sigurni da želite da obrišete kategoriju '" + category.getName() + "'?")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    viewModel.deleteCategory(category.getId());
                    Toast.makeText(getContext(), "Kategorija obrisana", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nova Kategorija");

        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(getContext());
        inputName.setHint("Naziv kategorije");
        inputName.setSingleLine(true);
        mainLayout.addView(inputName);

        TextView label = new TextView(getContext());
        label.setText("\nIzaberi boju:");
        mainLayout.addView(label);

        GridLayout colorGrid = new GridLayout(getContext());
        colorGrid.setColumnCount(7);

        String[] colors = {
                "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4",
                "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFC107", "#FF5722"
        };

        int size = (int) (33 * getResources().getDisplayMetrics().density);

        for (String color : colors) {
            View colorDot = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(10, 10, 10, 10);
            colorDot.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(Color.parseColor(color));
            dot.setStroke(3, Color.LTGRAY);
            colorDot.setBackground(dot);

            colorDot.setOnClickListener(v -> {
                selectedColor = color;
                for (int i = 0; i < colorGrid.getChildCount(); i++) {
                    ((android.graphics.drawable.GradientDrawable) colorGrid.getChildAt(i).getBackground()).setStroke(3, Color.LTGRAY);
                }
                dot.setStroke(8, Color.BLACK);
            });

            colorGrid.addView(colorDot);
        }

        mainLayout.addView(colorGrid);
        builder.setView(mainLayout);

        builder.setPositiveButton("Dodaj", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            if (!name.isEmpty()) {
                viewModel.insertCategory(name, selectedColor);
                Toast.makeText(getContext(), "Dodato: " + name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Ime ne sme biti prazno!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Otkaži", null);
        builder.show();
    }
}