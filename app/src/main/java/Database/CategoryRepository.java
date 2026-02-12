package Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

//import com.example.myhobitapplication.models.Category;

import java.util.ArrayList;
import java.util.List;

import Model.Category;

public class CategoryRepository {

    private final AppDataBaseHelper dbHelper;
    private SQLiteDatabase database;

    public CategoryRepository(Context context) {
        dbHelper = new AppDataBaseHelper(context);
    }

    // Otvaranje i zatvaranje konekcije nad bazom
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // =====================================================
    // INSERT - dodavanje nove kategorije
    // =====================================================
    public long insertCategory(String name, String colorHex) {
        database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(AppDataBaseHelper.COLUMN_CATEGORY_NAME, name);
        values.put(AppDataBaseHelper.COLUMN_CATEGORY_COLOR, colorHex);

        long newRowId = database.insert(AppDataBaseHelper.TABLE_CATEGORIES, null, values);
        database.close();
        return newRowId;
    }

    // =====================================================
    // GET ALL - dohvat svih kategorija
    // =====================================================
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        database = dbHelper.getReadableDatabase();

        Cursor cursor = database.query(
                AppDataBaseHelper.TABLE_CATEGORIES,
                new String[]{
                        AppDataBaseHelper.COLUMN_CATEGORY_ID_PK,
                        AppDataBaseHelper.COLUMN_CATEGORY_NAME,
                        AppDataBaseHelper.COLUMN_CATEGORY_COLOR
                },
                null,
                null,
                null,
                null,
                AppDataBaseHelper.COLUMN_CATEGORY_NAME + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_CATEGORY_ID_PK));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_CATEGORY_NAME));
                String color = cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_CATEGORY_COLOR));

                Category category = new Category(name, color);
                category.setId(id);
                categories.add(category);
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();
        database.close();
        return categories;
    }

    // =====================================================
    // GET BY ID - dohvat jedne kategorije po ID-u
    // =====================================================
    public Category getCategoryById(long id) {
        Category category = null;
        database = dbHelper.getReadableDatabase();

        String selection = AppDataBaseHelper.COLUMN_CATEGORY_ID_PK + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = database.query(
                AppDataBaseHelper.TABLE_CATEGORIES,
                new String[]{
                        AppDataBaseHelper.COLUMN_CATEGORY_ID_PK,
                        AppDataBaseHelper.COLUMN_CATEGORY_NAME,
                        AppDataBaseHelper.COLUMN_CATEGORY_COLOR
                },
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_CATEGORY_NAME));
            String color = cursor.getString(cursor.getColumnIndexOrThrow(AppDataBaseHelper.COLUMN_CATEGORY_COLOR));
            category = new Category(name, color);
            category.setId(id);
        }

        if (cursor != null) cursor.close();
        database.close();
        return category;
    }

    // =====================================================
    // UPDATE - ažuriranje postojeće kategorije
    // =====================================================
    public int updateCategory(Category category) {
        database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(AppDataBaseHelper.COLUMN_CATEGORY_NAME, category.getName());
        values.put(AppDataBaseHelper.COLUMN_CATEGORY_COLOR, category.getColorHex());

        String whereClause = AppDataBaseHelper.COLUMN_CATEGORY_ID_PK + " = ?";
        String[] whereArgs = {String.valueOf(category.getId())};

        int rowsAffected = database.update(AppDataBaseHelper.TABLE_CATEGORIES, values, whereClause, whereArgs);
        database.close();
        return rowsAffected;
    }

    // =====================================================
    // DELETE - brisanje po ID-u
    // =====================================================
    public int deleteCategory(long id) {
        database = dbHelper.getWritableDatabase();

        String whereClause = AppDataBaseHelper.COLUMN_CATEGORY_ID_PK + " = ?";
        String[] whereArgs = {String.valueOf(id)};

        int rowsDeleted = database.delete(AppDataBaseHelper.TABLE_CATEGORIES, whereClause, whereArgs);
        database.close();
        return rowsDeleted;
    }
}
