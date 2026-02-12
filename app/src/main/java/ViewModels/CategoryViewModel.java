package ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import Database.CategoryRepository;
import Model.Category;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CategoryViewModel
 * Služi kao posrednik između UI sloja i baze (CategoryRepository).
 * Omogućava asinhroni CRUD pristup nad kategorijama.
 */
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
            repository.open();
            List<Category> categories = repository.getAllCategories();
            repository.close();
            categoriesLiveData.postValue(categories);
        });
    }

    // =====================================================
    // Dodaj novu kategoriju
    // =====================================================
    public void insertCategory(String name, String colorHex) {
        executorService.execute(() -> {
            repository.open();
            repository.insertCategory(name, colorHex);
            repository.close();
            loadAllCategories();  // automatski refresh da UI vidi promenu
        });
    }

    // =====================================================
    // Ažuriraj postojeću kategoriju
    // =====================================================
    public void updateCategory(Category category) {
        executorService.execute(() -> {
            repository.open();
            repository.updateCategory(category);
            repository.close();
            loadAllCategories();
        });
    }

    // =====================================================
    // Obriši kategoriju
    // =====================================================
    public void deleteCategory(long id) {
        executorService.execute(() -> {
            repository.open();
            repository.deleteCategory(id);
            repository.close();
            loadAllCategories();
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
