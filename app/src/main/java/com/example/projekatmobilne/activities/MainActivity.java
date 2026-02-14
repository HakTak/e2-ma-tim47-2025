package com.example.projekatmobilne.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.adapters.CategoryAdapter;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.viewModels.CategoryViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Setup RecyclerView

        RecyclerView rv = findViewById(R.id.recyclerViewCategories);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(category -> {
            // Običan klik -> Otvara taskove
            Intent intent = new Intent(MainActivity.this, TasksActivity.class);
            intent.putExtra("CATEGORY_ID", category.getId());
            intent.putExtra("CATEGORY_NAME", category.getName());
            startActivity(intent);
        }, category -> {
            // Dugi klik -> Brisanje (ono što smo već uradili)
            showDeleteConfirmationDialog(category);
        });
        rv.setAdapter(adapter);

        // 2. Setup ViewModel
        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        // 3. Observe data
        viewModel.getAllCategories().observe(this, categories -> {
            adapter.setCategories(categories);
        });

        // 4. Click listener za dugme
        FloatingActionButton btn = findViewById(R.id.btnAddCategory);
        btn.setOnClickListener(v -> {
            // Poziva dijalog ispod da se unsese nova kategorija
            showAddCategoryDialog();
        });
    }

    private void showDeleteConfirmationDialog(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("Brisanje kategorije")
                .setMessage("Da li ste sigurni da želite da obrišete kategoriju '" + category.getName() + "'?")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    // POZIV VIEWMODEL-A
                    viewModel.deleteCategory(category.getId());
                    Toast.makeText(this, "Kategorija obrisana", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Otkaži", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private String selectedColor = "#607D8B"; //Def boja ako korisnik ne klikne nista ili uspe da upadne neka glupost
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Kategorija");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Naziv kategorije");
        inputName.setSingleLine(true);
        mainLayout.addView(inputName);

        TextView label = new TextView(this);
        label.setText("\nIzaberi boju:");
        mainLayout.addView(label);

        // FIX: Koristimo FlowLayout ili precizniji GridLayout
        GridLayout colorGrid = new GridLayout(this);
        colorGrid.setColumnCount(7);
        colorGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        String[] colors = {
                "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", // Red 1
                "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFC107", "#FF5722"  // Red 2
        };

        // Smanjili smo veličinu na 35dp da bi sigurno stalo
        int size = (int) (33 * getResources().getDisplayMetrics().density);

        for (String color : colors) {
            View colorDot = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(10, 10, 10, 10);
            colorDot.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(Color.parseColor(color));
            dot.setStroke(3, Color.LTGRAY); // Default ivica
            colorDot.setBackground(dot);

            colorDot.setOnClickListener(v -> {
                selectedColor = color;
                // Resetuj sve
                for (int i = 0; i < colorGrid.getChildCount(); i++) {
                    ((android.graphics.drawable.GradientDrawable) colorGrid.getChildAt(i).getBackground()).setStroke(3, Color.LTGRAY);
                }
                // Selektovan
                dot.setStroke(8, Color.BLACK);
            });

            colorGrid.addView(colorDot);
        }

        mainLayout.addView(colorGrid);
        builder.setView(mainLayout);

        builder.setPositiveButton("Dodaj", (dialog, which) -> {
            String name = inputName.getText().toString().trim();

            // DEBUG LOG: Ispisuje u konzolu šta je uneto
            android.util.Log.d("MOJA_APLIKACIJA", "Pokušaj unosa: " + name + " sa bojom: " + selectedColor);

            if (!name.isEmpty()) {
                viewModel.insertCategory(name, selectedColor);
                Toast.makeText(this, "Dodato: " + name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ime ne sme biti prazno!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Otkaži", null);
        builder.show();
    }
}