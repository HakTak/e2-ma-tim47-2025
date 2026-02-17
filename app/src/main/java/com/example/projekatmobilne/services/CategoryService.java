package com.example.projekatmobilne.services;

import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;

import java.util.List;

public class CategoryService {

    // ===================================================
    // FACTORY METODA
    // ===================================================

    public Category createCategory(String userId, String name, String colorHex) {
        return new Category(userId, name, colorHex);
    }

    // ===================================================
    // VALIDACIJA BOJE
    // ===================================================

    public boolean isColorAvailable(String colorHex,
                                    String excludeId,
                                    List<Category> allCategories) {
        for (Category c : allCategories) {
            if (c.getId().equals(excludeId)) continue;
            if (colorHex.equalsIgnoreCase(c.getColorHex())) return false;
        }
        return true;
    }

    // ===================================================
    // VALIDACIJA BRISANJA
    // ===================================================

    /**
     * Da li kategorija može biti obrisana?
     * @return poruku greške ako ne može, null ako može
     */
    public String canDeleteCategory(Category category, List<Task> allTasks) {
        int activeCount = 0;

        for (Task task : allTasks) {
            if (!category.getId().equals(task.getCategoryId())) continue;

            // Task pripada ovoj kategoriji — proveri da li je aktivan
            if (task.getStatus() != TaskStatus.DONE &&
                    task.getStatus() != TaskStatus.CANCELLED) {
                activeCount++;
            }
        }

        if (activeCount > 0) {
            return "Ne možete obrisati kategoriju '" + category.getName()
                    + "' jer ima " + activeCount + " aktivnih zadataka.\n\n"
                    + "Prvo završite ili otkažite sve zadatke u ovoj kategoriji.";
        }

        return null;
    }
}