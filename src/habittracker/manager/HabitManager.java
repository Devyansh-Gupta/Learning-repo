package habittracker.manager;

import habittracker.db.HabitRepository;
import habittracker.model.*;
import habittracker.exceptions.*;
import habittracker.interfaces.Categorizable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic manager for habit operations.
 * Demonstrates Generics usage with HabitManager<T extends Habit>.
 */
public class HabitManager<T extends Habit> {
    private List<T> habits;
    private HabitRepository repository;
    private User currentUser;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public HabitManager(User user, HabitRepository repository) {
        this.currentUser = user;
        this.repository = repository;
        this.habits = new ArrayList<>();
    }

    /**
     * Loads habits from database into memory.
     */
    public void loadHabits() throws DatabaseException {
        List<Habit> loadedHabits = repository.getHabitsByUser(currentUser.getId());
        habits.clear();
        for (Habit habit : loadedHabits) {
            @SuppressWarnings("unchecked")
            T typedHabit = (T) habit;
            habits.add(typedHabit);
        }
    }

    /**
     * Adds a new habit.
     */
    public void addHabit(T habit) throws InvalidHabitException, DatabaseException {
        if (habit.getName() == null || habit.getName().isBlank()) {
            throw new InvalidHabitException("Habit name cannot be blank");
        }

        habit.setUserId(currentUser.getId());
        habit.setCreatedAt(LocalDate.now().format(DATE_FORMAT));

        int habitId = repository.insertHabit(habit);
        habit.setId(habitId);

        Streak streak = repository.getStreak(habitId);
        habit.setStreak(streak);

        habits.add(habit);
    }

    /**
     * Removes a habit by ID.
     */
    public void removeHabit(int habitId) throws HabitNotFoundException, DatabaseException {
        repository.deleteHabit(habitId, currentUser.getId());
        habits.removeIf(h -> h.getId() == habitId);
    }

    /**
     * Marks a habit as complete for today.
     */
    public void markComplete(int habitId)
            throws HabitNotFoundException, InvalidHabitException, DatabaseException {

        T habit = findHabitById(habitId);
        if (habit == null) {
            throw new HabitNotFoundException("Habit not found: " + habitId);
        }

        String today = LocalDate.now().format(DATE_FORMAT);

        habit.markComplete();
        repository.logCompletion(habitId, today);

        Streak streak = habit.getStreak();
        if (streak != null) {
            streak.updateStreak(today);
            repository.updateStreak(streak);
        }
    }

    /**
     * Gets all habits.
     */
    public List<T> getAllHabits() {
        return new ArrayList<>(habits);
    }

    /**
     * Generic filter method - demonstrates Generics feature.
     * Filters habits by category.
     */
    public <T extends Habit> List<T> filterByCategory(List<T> habitList, Category category) {
        return habitList.stream()
            .filter(h -> h.getCategory() == category)
            .collect(Collectors.toList());
    }

    /**
     * Gets habits that have reminders set.
     */
    public List<T> getHabitsWithReminders() {
        return habits.stream()
            .filter(h -> h.getReminderTime() != null && !h.getReminderTime().isBlank())
            .collect(Collectors.toList());
    }

    /**
     * Finds a habit by its ID.
     */
    public T findHabitById(int habitId) {
        return habits.stream()
            .filter(h -> h.getId() == habitId)
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if a habit is completed today.
     */
    public boolean isCompletedToday(int habitId) throws DatabaseException {
        return repository.isCompletedToday(habitId);
    }

    /**
     * Gets the current user.
     */
    public User getCurrentUser() {
        return currentUser;
    }
}
