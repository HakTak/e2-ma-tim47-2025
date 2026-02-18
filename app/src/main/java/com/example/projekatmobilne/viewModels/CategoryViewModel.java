package com.example.projekatmobilne.viewModels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.repositories.CategoryRepository;
import com.example.projekatmobilne.repositories.TaskRepository;
import com.example.projekatmobilne.services.CategoryService;
import com.example.projekatmobilne.utils.SharedPrefsManager;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private static final String TAG = "CATEGORY_VIEWMODEL";

    private final CategoryRepository repository;
    private final TaskRepository taskRepository;
    private final CategoryService categoryService;
    private final SharedPrefsManager prefsManager;
    private final MutableLiveData<List<Category>> categoriesLiveData = new MutableLiveData<>();

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        this.repository = new CategoryRepository();
        this.taskRepository = new TaskRepository();
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
            Log.e(TAG, "UserID je NULL!");
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
            Log.e(TAG, "UserID je NULL!");
            return;
        }

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
    // UPDATE BOJE
    // ===================================================

    public String updateCategoryColor(Category category,
                                      String newColorHex,
                                      CategoryRepository.UpdateCallback callback) {

        List<Category> currentList = categoriesLiveData.getValue();

        if (currentList != null &&
                !categoryService.isColorAvailable(newColorHex, category.getId(), currentList)) {
            return "Ova boja je već zauzeta drugom kategorijom. Izaberite drugu boju.";
        }

        repository.updateCategoryColor(category.getId(), newColorHex,
                new CategoryRepository.UpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Boja ažurirana za: " + category.getName());
                        loadAllCategories();
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "updateCategoryColor() greška: " + error);
                        callback.onError(error);
                    }
                });

        return null;
    }

    // ===================================================
    // DELETE
    // ===================================================

    /**
     * Briše kategoriju uz validaciju — ne može se obrisati
     * ako ima aktivnih taskova koji joj pripadaju.
     */
    public void deleteCategory(Category category, DeleteCategoryCallback callback) {
        String userId = prefsManager.getUserId();
        if (userId == null) {
            callback.onError("Korisnik nije prijavljen");
            return;
        }

        // Prvo učitaj sve taskove pa provjeri validaciju
        taskRepository.getAllTasks(userId, new TaskRepository.TasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                // Validacija kroz servis
                String blockReason = categoryService.canDeleteCategory(category, tasks);

                if (blockReason != null) {
                    callback.onBlocked(blockReason);
                    return;
                }

                // Validacija prošla — briši
                repository.deleteCategory(category.getId(),
                        new CategoryRepository.DeleteCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Kategorija obrisana: " + category.getId());
                                loadAllCategories();
                                callback.onSuccess();
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "deleteCategory() greška: " + error);
                                callback.onError(error);
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Greška pri učitavanju taskova: " + error);
                callback.onError("Greška pri proveri zadataka: " + error);
            }
        });
    }

    public String validateColorUnique(String colorHex) {
        List<Category> current = categoriesLiveData.getValue();
        if (current != null && !categoryService.isColorAvailable(colorHex, null, current)) {
            return "Ova boja je već zauzeta drugom kategorijom. Izaberite drugu boju.";
        }
        return null;
    }

    // ===================================================
    // CALLBACK
    // ===================================================

    public interface DeleteCategoryCallback {
        void onSuccess();
        void onBlocked(String reason);
        void onError(String error);
    }
}