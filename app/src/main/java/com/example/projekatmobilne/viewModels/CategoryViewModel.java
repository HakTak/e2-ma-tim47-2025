package com.example.projekatmobilne.viewModels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.repositories.CategoryRepository;
import com.example.projekatmobilne.utils.SharedPrefsManager;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository repository;
    private final SharedPrefsManager prefsManager;
    private final MutableLiveData<List<Category>> categoriesLiveData = new MutableLiveData<>();

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        this.repository = new CategoryRepository();
        this.prefsManager = new SharedPrefsManager(application);
        loadAllCategories();
    }

    public MutableLiveData<List<Category>> getAllCategories() {
        return categoriesLiveData;
    }

    public void loadAllCategories() {
        String userId = prefsManager.getUserId();
        if (userId != null) {
            repository.getAllCategories(userId, new CategoryRepository.CategoriesCallback() {
                @Override
                public void onCategoriesLoaded(List<Category> categories) {
                    categoriesLiveData.postValue(categories);
                }

                @Override
                public void onError(String error) {
                    // Handle error
                }
            });
        }
    }

    public void insertCategory(String name, String colorHex) {
        String userId = prefsManager.getUserId();
        Category category = new Category(userId, name, colorHex);

        repository.insertCategory(category, new CategoryRepository.InsertCallback() {
            @Override
            public void onSuccess(String categoryId) {
                loadAllCategories(); // Reload after insert
            }

            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }

    public void deleteCategory(String categoryId) {
        repository.deleteCategory(categoryId, new CategoryRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                loadAllCategories(); // Reload after delete
            }

            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }
}