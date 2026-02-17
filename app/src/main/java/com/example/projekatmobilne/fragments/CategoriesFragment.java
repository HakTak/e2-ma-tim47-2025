package com.example.projekatmobilne.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.EditText;
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
import com.example.projekatmobilne.repositories.CategoryRepository;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CategoriesFragment extends Fragment {

    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;
    private String selectedColor = "#607D8B";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        RecyclerView rv = view.findViewById(R.id.recyclerViewCategories);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CategoryAdapter(
                // Običan klik → otvori TasksActivity
                category -> {
                    Intent intent = new Intent(getActivity(), TasksActivity.class);
                    intent.putExtra("CATEGORY_ID", category.getId());
                    intent.putExtra("CATEGORY_NAME", category.getName());
                    startActivity(intent);
                },
                // Dugi klik → prikaži opcije (edit ili delete)
                category -> showCategoryOptionsDialog(category)
        );
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            adapter.setCategories(categories);
        });

        FloatingActionButton fab = view.findViewById(R.id.btnAddCategory);
        fab.setOnClickListener(v -> showAddCategoryDialog());

        return view;
    }

    // ===================================================
    // OPCIJE DIJALOG (edit ili delete)
    // ===================================================

    private void showCategoryOptionsDialog(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle(category.getName())
                .setItems(new String[]{"✏️ Promeni boju", "🗑️ Obriši"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditColorDialog(category);
                    } else {
                        showDeleteConfirmationDialog(category);
                    }
                })
                .show();
    }

    // ===================================================
    // EDIT BOJE DIJALOG
    // ===================================================

    private void showEditColorDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Promeni boju: " + category.getName());

        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 40, 50, 10);

        TextView label = new TextView(getContext());
        label.setText("Izaberi novu boju:");
        mainLayout.addView(label);

        // Čuvamo referencu na selectedColor lokalno za ovaj dijalog
        final String[] localSelectedColor = {category.getColorHex()};

        GridLayout colorGrid = buildColorGrid(localSelectedColor);
        mainLayout.addView(colorGrid);

        builder.setView(mainLayout);

        builder.setPositiveButton("SAČUVAJ", (dialog, which) -> {
            String newColor = localSelectedColor[0];

            // Validacija i update kroz ViewModel
            String error = viewModel.updateCategoryColor(
                    category,
                    newColor,
                    new CategoryRepository.UpdateCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(),
                                    "Boja kategorije ažurirana!",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String err) {
                            Toast.makeText(getContext(),
                                    "Greška: " + err,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

            // Ako validacija nije prošla, prikaži grešku
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("OTKAŽI", null);
        builder.show();
    }

    // ===================================================
    // DELETE DIJALOG
    // ===================================================

    private void showDeleteConfirmationDialog(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Brisanje kategorije")
                .setMessage("Da li ste sigurni da želite da obrišete kategoriju '"
                        + category.getName() + "'?")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    viewModel.deleteCategory(category, new CategoryViewModel.DeleteCategoryCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(),
                                    "Kategorija obrisana",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onBlocked(String reason) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("❌ Brisanje zabranjeno")
                                    .setMessage(reason)
                                    .setPositiveButton("U REDU", null)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(),
                                    "Greška: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    // ===================================================
    // ADD DIJALOG
    // ===================================================

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

        final String[] localSelectedColor = {selectedColor};
        GridLayout colorGrid = buildColorGrid(localSelectedColor);
        mainLayout.addView(colorGrid);

        builder.setView(mainLayout);

        builder.setPositiveButton("Dodaj", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            if (!name.isEmpty()) {
                selectedColor = localSelectedColor[0];
                viewModel.insertCategory(name, selectedColor);
                Toast.makeText(getContext(), "Dodato: " + name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Ime ne sme biti prazno!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Otkaži", null);
        builder.show();
    }

    // ===================================================
    // HELPER — Color Grid (deljeno između add i edit dijaloga)
    // ===================================================

    private GridLayout buildColorGrid(String[] selectedColorRef) {
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

            android.graphics.drawable.GradientDrawable dot =
                    new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(Color.parseColor(color));

            // Označi trenutno izabranu boju
            if (color.equalsIgnoreCase(selectedColorRef[0])) {
                dot.setStroke(8, Color.BLACK);
            } else {
                dot.setStroke(3, Color.LTGRAY);
            }

            colorDot.setBackground(dot);

            colorDot.setOnClickListener(v -> {
                selectedColorRef[0] = color;
                // Resetuj sve bordere pa označi izabranu
                for (int i = 0; i < colorGrid.getChildCount(); i++) {
                    ((android.graphics.drawable.GradientDrawable)
                            colorGrid.getChildAt(i).getBackground())
                            .setStroke(3, Color.LTGRAY);
                }
                dot.setStroke(8, Color.BLACK);
            });

            colorGrid.addView(colorDot);
        }

        return colorGrid;
    }
}