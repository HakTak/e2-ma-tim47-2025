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
    // 1. Definišemo interfejs za dugi klik
    public interface OnCategoryLongClickListener {
        void onCategoryLongClick(Category category);
    }
    private OnCategoryLongClickListener longClickListener;

    // 2. Konstruktor koji prima listener
    public CategoryAdapter(OnCategoryLongClickListener listener) {
        this.longClickListener = listener;
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

        // 3. Postavljamo Long Click na ceo red (itemView)
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCategoryLongClick(category);
            }
            return true; // true znači da smo "konzumirali" klik i da se običan klik neće desiti
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