package com.example.projekatmobilne.viewModels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.repositories.CategoryRepository;
import com.example.projekatmobilne.services.CategoryService;
import com.example.projekatmobilne.utils.SharedPrefsManager;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private static final String TAG = "CATEGORY_VIEWMODEL";

    private final CategoryRepository repository;
    private final CategoryService categoryService;
    private final SharedPrefsManager prefsManager;
    private final MutableLiveData<List<Category>> categoriesLiveData = new MutableLiveData<>();

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        this.repository = new CategoryRepository();
        this.categoryService = new CategoryService();
        this.prefsManager = new SharedPrefsManager(application);
        loadAllCategories();
    }

    // ===================================================
    // LOAD
    // ===================================================

    public MutableLiveData<List<Category>> getAllCategories() {
        return categoriesLiveData;
    }

    public void loadAllCategories() {
        String userId = prefsManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "UserID je NULL u SharedPrefs!");
            return;
        }

        repository.getAllCategories(userId, new CategoryRepository.CategoriesCallback() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                categoriesLiveData.postValue(categories);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "loadAllCategories() greška: " + error);
            }
        });
    }

    // ===================================================
    // INSERT
    // ===================================================

    public void insertCategory(String name, String colorHex) {
        String userId = prefsManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "UserID je NULL - ne mogu dodati kategoriju");
            return;
        }

        // Kreiranje objekta kroz servis
        Category category = categoryService.createCategory(userId, name, colorHex);

        repository.insertCategory(category, new CategoryRepository.InsertCallback() {
            @Override
            public void onSuccess(String categoryId) {
                Log.d(TAG, "Kategorija dodata: " + categoryId);
                loadAllCategories();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "insertCategory() greška: " + error);
            }
        });
    }

    // ===================================================
    // DELETE
    // ===================================================

    public void deleteCategory(String categoryId) {
        repository.deleteCategory(categoryId, new CategoryRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Kategorija obrisana: " + categoryId);
                loadAllCategories();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "deleteCategory() greška: " + error);
            }
        });
    }
}