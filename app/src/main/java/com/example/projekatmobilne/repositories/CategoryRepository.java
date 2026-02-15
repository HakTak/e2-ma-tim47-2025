package com.example.projekatmobilne.repositories;

import com.example.projekatmobilne.models.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    private final FirebaseFirestore db;

    public CategoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // GET ALL CATEGORIES (za specifičnog usera)
    public void getAllCategories(String userId, CategoriesCallback callback) {
        db.collection("categories")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Category> categories = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Category cat = doc.toObject(Category.class);
                        cat.setId(doc.getId());
                        categories.add(cat);
                    }
                    callback.onCategoriesLoaded(categories);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // INSERT CATEGORY
    public void insertCategory(Category category, InsertCallback callback) {
        db.collection("categories")
                .add(category.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // DELETE CATEGORY
    public void deleteCategory(String categoryId, DeleteCallback callback) {
        db.collection("categories").document(categoryId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // CALLBACKS
    public interface CategoriesCallback {
        void onCategoriesLoaded(List<Category> categories);
        void onError(String error);
    }

    public interface InsertCallback {
        void onSuccess(String categoryId);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}