package habittracker.model;

import habittracker.exceptions.HabitNotFoundException;
import habittracker.exceptions.InvalidHabitException;

/**
 * Abstract base class for all habit types.
 * Provides common fields and encapsulation for habit data.
 */
public abstract class Habit {
    protected int id;
    protected int userId;
    protected String name;
    protected Category category;
    protected String reminderTime;
    protected String createdAt;

    protected Habit() {}

    protected Habit(int userId, String name, Category category, String createdAt) {
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.createdAt = createdAt;
    }

    protected Habit(int id, int userId, String name, Category category,
                    String reminderTime, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.reminderTime = reminderTime;
        this.createdAt = createdAt;
    }

    public abstract String getType();

    public abstract void markComplete() throws HabitNotFoundException, InvalidHabitException;

    public abstract Streak getStreak();

    public abstract void setStreak(Streak streak);

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        String reminder = (reminderTime != null) ? " @ " + reminderTime : "";
        return String.format("[%s] %s%s - %s", getType(), name, reminder, category);
    }
}
