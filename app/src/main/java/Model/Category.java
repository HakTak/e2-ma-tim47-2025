package Model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String colorHex; // Čuvamo boju kao npr "#FF0000"

    public Category(String name, String colorHex) {
        this.name = name;
        this.colorHex = colorHex;
    }
}