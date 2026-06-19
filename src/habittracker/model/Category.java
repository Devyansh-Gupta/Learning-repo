package habittracker.model;

import habittracker.exceptions.InvalidHabitException;

/**
 * Enum representing habit categories.
 */
public enum Category {
    HEALTH, STUDY, FITNESS, FINANCE, OTHER;

    public static Category fromString(String value) throws InvalidHabitException {
        if (value == null || value.isBlank()) {
            throw new InvalidHabitException("Category cannot be blank");
        }
        try {
            return Category.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidHabitException("Invalid category: " + value);
        }
    }
}
