package com.example.projekatmobilne.services;

import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Task;

import java.util.Calendar;

public class TaskValidationService {

    private static final String TAG = "TASK_VALIDATION";

    // ===================================================
    // VALIDACIJA BRISANJA
    // ===================================================

    /**
     * Da li task može biti obrisan?
     * @return poruku greške ako ne može, null ako može
     */
    public String canDelete(Task task) {
        if (hasAnyCompletedOccurrences(task)) {
            int count = getCompletedCount(task);
            return "Ne možete obrisati zadatak koji ima završene termine ("
                    + count + " urađeno).\n\nOvo čuva vašu istoriju i zarađeni XP.";
        }
        return null;
    }

    /**
     * Da li pojedinačni datum recurring taska može biti obrisan?
     * @return poruku greške ako ne može, null ako može
     */
    public String canDeleteOccurrence(long timestamp, TaskStatus status) {
        if (status == TaskStatus.DONE) {
            String dateKey = TaskService.timestampToDateKey(timestamp);
            return "Ne možete obrisati završen termin ("
                    + dateKey + ").\n\nOvo čuva vašu istoriju i zarađeni XP.";
        }
        return null;
    }

    // ===================================================
    // VALIDACIJA EDITOVANJA
    // ===================================================

    /**
     * Da li task može biti editovan?
     * @return poruku greške ako ne može, null ako može
     */
    public String canEdit(Task task) {
        if (task.getFrequencyType() == FrequencyType.ONE_TIME) {

            if (task.getStatus() == TaskStatus.DONE) {
                return "Ne možete menjati završen zadatak.\n\nOvo čuva vašu istoriju.";
            }

            long startOfToday = TaskService.getStartOfToday();
            long startOfTaskDay = TaskService.getStartOfDay(task.getExecutionTime());

            if (startOfTaskDay < startOfToday) {
                return "Ne možete menjati zadatak koji je vremenski prošao.\n\nDatum zadatka je bio pre današnjeg dana.";
            }

        } else {
            if (!hasFutureActiveDates(task)) {
                return "Ne možete menjati ovaj zadatak.\n\nSvi termini su završeni ili vremenski prošli.";
            }
        }

        return null;
    }

    // ===================================================
    // QUERY HELPER METODE
    // ===================================================

    /**
     * Da li task ima bar jedan DONE datum?
     */
    public boolean hasAnyCompletedOccurrences(Task task) {
        if (task.getOccurrenceStatuses() == null || task.getOccurrenceStatuses().isEmpty()) {
            return task.getStatus() == TaskStatus.DONE;
        }

        for (String statusStr : task.getOccurrenceStatuses().values()) {
            if ("DONE".equals(statusStr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Broj završenih datuma
     */
    public int getCompletedCount(Task task) {
        if (task.getOccurrenceStatuses() == null) return 0;

        int count = 0;
        for (String statusStr : task.getOccurrenceStatuses().values()) {
            if ("DONE".equals(statusStr)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Da li recurring task ima bar jedan budući ACTIVE datum?
     */
    public boolean hasFutureActiveDates(Task task) {
        if (task.getRecurringDates() == null) return false;

        long startOfToday = TaskService.getStartOfToday();

        for (Long timestamp : task.getRecurringDates()) {
            long startOfDay = TaskService.getStartOfDay(timestamp);
            boolean isFuture = startOfDay >= startOfToday;

            String dateKey = TaskService.timestampToDateKey(timestamp);
            boolean isDone = "DONE".equals(task.getOccurrenceStatuses().get(dateKey));

            if (isFuture && !isDone) {
                return true;
            }
        }

        return false;
    }
}