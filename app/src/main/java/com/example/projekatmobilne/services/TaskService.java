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

    // ===================================================
    // DATUM HELPER
    // ===================================================

    /**
     * Konvertuj timestamp u String ključ formata YYYY-MM-DD
     * PUBLIC STATIC - koristi se i u ostalim servisima
     */
    public static String timestampToDateKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Početak dana (00:00:00) za dati timestamp
     */
    public static long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Početak današnjeg dana (00:00:00)
     */
    public static long getStartOfToday() {
        return getStartOfDay(System.currentTimeMillis());
    }

    // ===================================================
    // STATUS LOGIKA
    // ===================================================

    /**
     * Dobij status za specifičan datum.
     * Ako nema specifičnog statusa, vraća globalni status taska.
     */
    public TaskStatus getStatusForDate(Task task, long timestamp) {
        String dateKey = timestampToDateKey(timestamp);

        if (task.getOccurrenceStatuses().containsKey(dateKey)) {
            return TaskStatus.valueOf(task.getOccurrenceStatuses().get(dateKey));
        }

        return task.getStatus() != null ? task.getStatus() : TaskStatus.ACTIVE;
    }

    /**
     * Postavi status za specifičan datum
     */
    public void setStatusForDate(Task task, long timestamp, TaskStatus newStatus) {
        String dateKey = timestampToDateKey(timestamp);
        task.getOccurrenceStatuses().put(dateKey, newStatus.name());

        Log.d(TAG, "setStatusForDate():");
        Log.d(TAG, "  - Date Key: " + dateKey);
        Log.d(TAG, "  - New Status: " + newStatus);
        Log.d(TAG, "  - Mapa posle izmene: " + task.getOccurrenceStatuses());
    }

    /**
     * Odredi inicijalni status novog taska
     */
    public TaskStatus calculateInitialStatus(FrequencyType freqType, long executionTime) {
        if (freqType == FrequencyType.RECURRING) {
            return TaskStatus.ACTIVE;
        }

        long startOfToday = getStartOfToday();
        long startOfTaskDay = getStartOfDay(executionTime);

        return startOfTaskDay > startOfToday ? TaskStatus.UPCOMING : TaskStatus.ACTIVE;
    }

    // ===================================================
    // RECURRING DATES LOGIKA
    // ===================================================

    /**
     * Generiši listu datuma za recurring task
     */


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

    /**
     * Ukloni datum + sve datume posle njega iz recurring niza.
     * @return broj uklonjenih datuma
     */
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

    /**
     * Ažuriraj vreme izvršavanja za sve buduće ACTIVE datume recurring taska
     */
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

    /**
     * Spoji izabrani datum i izabrano vreme u jedan timestamp
     */
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

    /**
     * Odredi dateContext za prikaz taska (koristi se u adapterima)
     * Za recurring: sledeći datum od danas
     * Za one-time: executionTime
     */
    public long getDateContext(Task task) {
        if (task.getFrequencyType() != FrequencyType.RECURRING) {
            return task.getExecutionTime();
        }

        long startOfToday = getStartOfToday();
        Long nextDate = task.getNextOccurrence(startOfToday);
        return nextDate != null ? nextDate : task.getExecutionTime();
    }

    /**
     * Provera da li task ima preostalih datuma
     */
    public boolean isEmpty(Task task) {
        if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
            return false;
        }
        return task.getRecurringDates() == null || task.getRecurringDates().isEmpty();
    }

    /**
     * Provera da li je task u prošlosti
     */
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