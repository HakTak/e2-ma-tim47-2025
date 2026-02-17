package com.example.projekatmobilne.repositories;

import android.util.Log;

import com.example.projekatmobilne.models.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    private static final String TAG = "CATEGORY_REPO";
    private static final String COLLECTION = "categories";

    private final FirebaseFirestore db;

    public CategoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ===================================================
    // CRUD
    // ===================================================

    public void getAllCategories(String userId, CategoriesCallback callback) {
        db.collection(COLLECTION)
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
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllCategories() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void insertCategory(Category category, InsertCallback callback) {
        db.collection(COLLECTION)
                .add(category.toMap())
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Kategorija dodata: " + docRef.getId());
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "insertCategory() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void updateCategoryColor(String categoryId, String newColorHex, UpdateCallback callback) {
        db.collection(COLLECTION).document(categoryId)
                .update("colorHex", newColorHex)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Boja kategorije ažurirana: " + categoryId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateCategoryColor() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void deleteCategory(String categoryId, DeleteCallback callback) {
        db.collection(COLLECTION).document(categoryId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Kategorija obrisana: " + categoryId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteCategory() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ===================================================
    // CALLBACKS
    // ===================================================

    public interface CategoriesCallback {
        void onCategoriesLoaded(List<Category> categories);
        void onError(String error);
    }

    public interface InsertCallback {
        void onSuccess(String categoryId);
        void onError(String error);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}