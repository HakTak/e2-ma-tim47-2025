package com.example.projekatmobilne.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projekatmobilne.Model.Category;
import com.example.projekatmobilne.R;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> categoryList = new ArrayList<>();

    // 1. Definišemo dva interfejsa
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public interface OnCategoryLongClickListener {
        void onCategoryLongClick(Category category);
    }

    private final OnCategoryClickListener clickListener;
    private final OnCategoryLongClickListener longClickListener;

    // 2. Ažuriramo konstruktor da prima DVA argumenta
    public CategoryAdapter(OnCategoryClickListener clickListener, OnCategoryLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setCategories(List<Category> categories) {
        this.categoryList = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvName.setText(category.getName());

        try {
            holder.vColor.setBackgroundColor(Color.parseColor(category.getColorHex()));
        } catch (Exception e) {
            holder.vColor.setBackgroundColor(Color.GRAY);
        }

        // 3. Postavljamo običan klik
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCategoryClick(category);
            }
        });

        // 4. Postavljamo dugi klik
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCategoryLongClick(category);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() { return categoryList.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        View vColor;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            vColor = itemView.findViewById(R.id.viewCategoryColor);
        }
    }
}