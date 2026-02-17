package com.example.projekatmobilne.services;

import com.example.projekatmobilne.models.Category;

public class CategoryService {

    private static final String TAG = "CATEGORY_SERVICE";

    // ===================================================
    // FACTORY METODA
    // ===================================================

    /**
     * Kreiraj novi Category objekat
     */
    public Category createCategory(String userId, String name, String colorHex) {
        return new Category(userId, name, colorHex);
    }
}