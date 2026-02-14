package com.example.projekatmobilne;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projekatmobilne.Adapter.CategoryAdapter;
import com.example.projekatmobilne.ViewModels.CategoryViewModel;
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
        adapter = new CategoryAdapter();
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
            // Za test: Dodajemo nasumičnu kategoriju
            viewModel.insertCategory("Nova " + (int)(Math.random()*100), "#FF5733");
        });
    }
}