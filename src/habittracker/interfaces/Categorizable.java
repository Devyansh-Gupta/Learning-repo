package habittracker.interfaces;

import habittracker.model.Category;

/**
 * Interface for categorizable habits.
 */
public interface Categorizable {
    Category getCategory();
    void setCategory(Category category);
}
