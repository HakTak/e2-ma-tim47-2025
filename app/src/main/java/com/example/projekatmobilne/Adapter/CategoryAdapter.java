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