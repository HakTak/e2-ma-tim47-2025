package com.example.projekatmobilne.viewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.projekatmobilne.database.CategoryRepository;
import com.example.projekatmobilne.models.Category;

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository repository;
    private final MutableLiveData<List<Category>> categoriesLiveData = new MutableLiveData<>();
    private final ExecutorService executorService;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
        executorService = Executors.newSingleThreadExecutor();
        loadAllCategories();
    }

    // =====================================================
    // Dohvatanje svih kategorija
    // =====================================================
    public LiveData<List<Category>> getAllCategories() {
        return categoriesLiveData;
    }

    public void loadAllCategories() {
        executorService.execute(() -> {
            try {
                repository.open();
                List<Category> categories = repository.getAllCategories();
                categoriesLiveData.postValue(categories);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                repository.close();
            }
        });
    }

    // =====================================================
    // Dodaj novu kategoriju
    // =====================================================
    public void insertCategory(String name, String colorHex) {
        executorService.execute(() -> {
            try {
                repository.open();
                repository.insertCategory(name, colorHex);
                // Nakon inserta odmah učitavamo sve ponovo
                List<Category> categories = repository.getAllCategories();
                categoriesLiveData.postValue(categories);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                repository.close();
            }
        });
    }

    // =====================================================
    // Ažuriraj postojeću kategoriju
    // =====================================================
    public void updateCategory(Category category) {
        executorService.execute(() -> {
            try{
                repository.open();
                repository.updateCategory(category);
              // repository.close();
                loadAllCategories();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                repository.close();
            }

        });
    }

    // =====================================================
    // Obriši kategoriju
    // =====================================================
    public void deleteCategory(long id) {
        executorService.execute(() -> {
            try{
                repository.open();
                repository.deleteCategory(id);

                loadAllCategories();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                repository.close();
            }

        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
