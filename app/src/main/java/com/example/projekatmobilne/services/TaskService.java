package com.example.projekatmobilne.services;

import android.util.Log;

import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.RepeatUnit;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskService {

    private static final String TAG = "TASK_SERVICE";
    private static final long THREE_DAYS_IN_MILLIS = 3 * 24 * 60 * 60 * 1000L;

    // ===================================================
    // DATUM HELPER
    // ===================================================

    public static String timestampToDateKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    public static long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getStartOfToday() {
        return getStartOfDay(System.currentTimeMillis());
    }

    // ===================================================
    // STATUS LOGIKA
    // ===================================================

    public TaskStatus getStatusForDate(Task task, long timestamp) {
        String dateKey = timestampToDateKey(timestamp);

        if (task.getOccurrenceStatuses().containsKey(dateKey)) {
            return TaskStatus.valueOf(task.getOccurrenceStatuses().get(dateKey));
        }

        return task.getStatus() != null ? task.getStatus() : TaskStatus.ACTIVE;
    }

    public void setStatusForDate(Task task, long timestamp, TaskStatus newStatus) {
        String dateKey = timestampToDateKey(timestamp);
        task.getOccurrenceStatuses().put(dateKey, newStatus.name());

        Log.d(TAG, "setStatusForDate():");
        Log.d(TAG, "  - Date Key: " + dateKey);
        Log.d(TAG, "  - New Status: " + newStatus);
        Log.d(TAG, "  - Mapa posle izmene: " + task.getOccurrenceStatuses());
    }

    public TaskStatus calculateInitialStatus(FrequencyType freqType, long executionTime) {
        if (freqType == FrequencyType.RECURRING) {
            return TaskStatus.ACTIVE;
        }

        long startOfToday = getStartOfToday();
        long startOfTaskDay = getStartOfDay(executionTime);

        return startOfTaskDay > startOfToday ? TaskStatus.UPCOMING : TaskStatus.ACTIVE;
    }

    // ===================================================
    // VALIDACIJA PROMENE STATUSA (Zadaci 1, 2, 3)
    // ===================================================

    /**
     * Validira da li je dozvoljena promena statusa.
     * @return poruku greške ako nije dozvoljeno, null ako jeste
     */
    public String canChangeStatus(Task task, long dateContext, TaskStatus currentStatus, TaskStatus newStatus) {
        long now = System.currentTimeMillis();

        // CANCELLED taskovi ne mogu menjati status
        if (currentStatus == TaskStatus.CANCELLED) {
            return "Ne možete menjati status otkazanog zadatka.";
        }

        // FAILED taskovi ne mogu menjati status
        if (currentStatus == TaskStatus.FAILED) {
            return "Ne možete menjati status zadatka koji je označen kao NEUSPEŠAN.";
        }

        // DONE taskovi ne mogu menjati status
        if (currentStatus == TaskStatus.DONE) {
            return "Ne možete menjati status zadatka koji je označen kao ZAVRŠEN.";
        }



        // Zadatak 1: Samo ACTIVE može prelaziti u DONE/CANCELLED/PAUSED
        if (currentStatus != TaskStatus.ACTIVE &&
                (newStatus == TaskStatus.DONE || newStatus == TaskStatus.CANCELLED || newStatus == TaskStatus.PAUSED)) {
            return "Samo aktivni zadaci mogu biti označeni kao urađeni, otkazani ili pauzirani.";
        }

        // Zadatak 3: DONE samo ako je executionTime prošao
        if (newStatus == TaskStatus.DONE) {
            if (dateContext > now) {
                return "Ne možete označiti zadatak kao urađen pre nego što nastupi vreme izvršavanja.";
            }
        }

        // Zadatak 2: Ne možemo menjati status zadataka starijih od 3 dana
        long threeDaysAgo = now - THREE_DAYS_IN_MILLIS;
        if (dateContext < threeDaysAgo && currentStatus == TaskStatus.ACTIVE) {
            return "Ne možete menjati status zadatka starijeg od 3 dana. Automatski je označen kao NEUSPEŠAN.";
        }

        return null; // Validacija prošla
    }

    // ===================================================
    // AUTOMATSKO OZNAČAVANJE OVERDUE TASKOVA (Zadatak 2)
    // ===================================================

    /**
     * Prolazi kroz sve taskove i označava prekoračene (>3 dana) ACTIVE taskove kao FAILED.
     * @return lista taskova koji su izmenjeni (za batch update u bazi)
     */
    public List<Task> validateAndUpdateOverdueTasks(List<Task> tasks) {
        List<Task> modifiedTasks = new ArrayList<>();
        long now = System.currentTimeMillis();
        long threeDaysAgo = now - THREE_DAYS_IN_MILLIS;

        for (Task task : tasks) {
            boolean wasModified = false;

            if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
                // Jednokratni task
                TaskStatus currentStatus = task.getStatus();
                long executionTime = task.getExecutionTime();

                if (currentStatus == TaskStatus.ACTIVE && executionTime < threeDaysAgo) {
                    task.setStatus(TaskStatus.FAILED);
                    wasModified = true;
                    Log.d(TAG, "Task '" + task.getTitle() + "' označen kao FAILED (one-time)");
                }

            } else {
                // Recurring task — proveravamo svaki datum posebno
                if (task.getRecurringDates() != null) {
                    for (Long timestamp : task.getRecurringDates()) {
                        String dateKey = timestampToDateKey(timestamp);
                        TaskStatus dateStatus = getStatusForDate(task, timestamp);

                        if (dateStatus == TaskStatus.ACTIVE && timestamp < threeDaysAgo) {
                            setStatusForDate(task, timestamp, TaskStatus.FAILED);
                            wasModified = true;
                            Log.d(TAG, "Task '" + task.getTitle() + "' datum " + dateKey + " označen kao FAILED");
                        }
                    }
                }
            }

            if (wasModified) {
                modifiedTasks.add(task);
            }
        }

        Log.d(TAG, "Označeno " + modifiedTasks.size() + " taskova kao FAILED");
        return modifiedTasks;
    }

    // ===================================================
    // RECURRING DATES LOGIKA
    // ===================================================

    public List<Long> generateRecurringDates(long start, long end, RepeatUnit unit, int interval) {
        List<Long> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(start);

        while (calendar.getTimeInMillis() <= end) {
            dates.add(calendar.getTimeInMillis());
            switch (unit) {
                case DAY:   calendar.add(Calendar.DAY_OF_YEAR, interval); break;
                case WEEK:  calendar.add(Calendar.WEEK_OF_YEAR, interval); break;
                case MONTH: calendar.add(Calendar.MONTH, interval); break;
                case YEAR:  calendar.add(Calendar.YEAR, interval); break;
            }
            if (interval <= 0) break;
        }

        return dates;
    }

    public int removeSingleOccurrence(Task task, long dateToRemove) {
        if (task.getRecurringDates() == null || task.getRecurringDates().isEmpty()) return 0;

        int removedCount = 0;
        List<Long> toRemove = new ArrayList<>();

        for (Long date : task.getRecurringDates()) {
            if (date >= dateToRemove) {
                toRemove.add(date);
            }
        }

        for (Long date : toRemove) {
            if (task.getRecurringDates().remove(date)) {
                removedCount++;
                String dateKey = timestampToDateKey(date);
                task.getOccurrenceStatuses().remove(dateKey);
                Log.d(TAG, "Uklonjen datum: " + dateKey);
            }
        }

        Log.d(TAG, "Ukupno uklonjeno: " + removedCount + " datuma");
        Log.d(TAG, "Preostalo datuma: " + task.getRecurringDates().size());

        return removedCount;
    }

    public void updateFutureOccurrenceTimes(Task task, int newHour, int newMinute) {
        if (task.getRecurringDates() == null) return;

        long now = System.currentTimeMillis();

        for (int i = 0; i < task.getRecurringDates().size(); i++) {
            long date = task.getRecurringDates().get(i);

            if (date < now) continue;

            String dateKey = timestampToDateKey(date);
            boolean isDone = "DONE".equals(task.getOccurrenceStatuses().get(dateKey));
            if (isDone) continue;

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date);
            cal.set(Calendar.HOUR_OF_DAY, newHour);
            cal.set(Calendar.MINUTE, newMinute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            task.getRecurringDates().set(i, cal.getTimeInMillis());
        }

        Log.d(TAG, "Ažurirano vreme za buduće datume: " + newHour + ":" + newMinute);
    }

    public long mergeDateTime(long datePart, long timePart) {
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTimeInMillis(datePart);

        Calendar timeCal = Calendar.getInstance();
        timeCal.setTimeInMillis(timePart);

        dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
        dateCal.set(Calendar.SECOND, 0);
        dateCal.set(Calendar.MILLISECOND, 0);

        return dateCal.getTimeInMillis();
    }

    // ===================================================
    // QUERY HELPER METODE
    // ===================================================

    public long getDateContext(Task task) {
        if (task.getFrequencyType() != FrequencyType.RECURRING) {
            return task.getExecutionTime();
        }

        long startOfToday = getStartOfToday();
        Long nextDate = task.getNextOccurrence(startOfToday);
        return nextDate != null ? nextDate : task.getExecutionTime();
    }

    public boolean isEmpty(Task task) {
        if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
            return false;
        }
        return task.getRecurringDates() == null || task.getRecurringDates().isEmpty();
    }

    public boolean isTaskInPast(Task task, long startOfToday) {
        if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
            return task.getExecutionTime() < startOfToday;
        }

        List<Long> recurringDates = task.getRecurringDates();
        if (recurringDates == null || recurringDates.isEmpty()) {
            return task.getExecutionTime() < startOfToday;
        }

        long lastOccurrence = recurringDates.get(recurringDates.size() - 1);
        return lastOccurrence < startOfToday;
    }
}