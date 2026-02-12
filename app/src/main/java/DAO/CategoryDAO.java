package DAO;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import Model.Category;

@Dao
public class CategoryDAO    {
    @Insert
    public void insert(Category category) {

    }

    @Query("SELECT * FROM categories")
    public List<Category> getAll() {
        return null;
    }

    @Delete
    void delete(Category category) {

    }
}
