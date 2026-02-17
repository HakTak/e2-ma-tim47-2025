package com.example.projekatmobilne.services;

import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Task;

public class TaskValidationService {

    // ===================================================
    // VALIDACIJA BRISANJA
    // ===================================================

    public String canDelete(Task task) {
        // CANCELLED taskovi ne mogu biti obrisani
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return "Ne možete obrisati otkazani zadatak.\n\nOvo čuva vašu istoriju.";
        }

        // FAILED taskovi ne mogu biti obrisani
        if (task.getStatus() == TaskStatus.FAILED) {
            return "Ne možete obrisati zadatak koji je označen kao NEUSPEŠAN.\n\nOvo čuva vašu istoriju.";
        }

        if (hasAnyCompletedOccurrences(task)) {
            int count = getCompletedCount(task);
            return "Ne možete obrisati zadatak koji ima završene termine ("
                    + count + " urađeno).\n\nOvo čuva vašu istoriju i zarađeni XP.";
        }
        return null;
    }

    public String canDeleteOccurrence(long timestamp, TaskStatus status) {
        // CANCELLED datumi ne mogu biti obrisani
        if (status == TaskStatus.CANCELLED) {
            String dateKey = TaskService.timestampToDateKey(timestamp);
            return "Ne možete obrisati otkazani termin ("
                    + dateKey + ").\n\nOvo čuva vašu istoriju.";
        }

        // FAILED datumi ne mogu biti obrisani
        if (status == TaskStatus.FAILED) {
            String dateKey = TaskService.timestampToDateKey(timestamp);
            return "Ne možete obrisati neuspešan termin ("
                    + dateKey + ").\n\nOvo čuva vašu istoriju.";
        }

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

    public String canEdit(Task task) {
        // CANCELLED taskovi ne mogu biti editovani
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return "Ne možete menjati otkazani zadatak.\n\nOvo čuva vašu istoriju.";
        }

        // FAILED taskovi ne mogu biti editovani
        if (task.getStatus() == TaskStatus.FAILED) {
            return "Ne možete menjati zadatak koji je označen kao NEUSPEŠAN.\n\nOvo čuva vašu istoriju.";
        }

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
    // HELPER METODE
    // ===================================================

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

    public boolean hasFutureActiveDates(Task task) {
        if (task.getRecurringDates() == null) return false;

        long startOfToday = TaskService.getStartOfToday();

        for (Long timestamp : task.getRecurringDates()) {
            long startOfDay = TaskService.getStartOfDay(timestamp);
            boolean isFuture = startOfDay >= startOfToday;

            String dateKey = TaskService.timestampToDateKey(timestamp);
            String statusStr = task.getOccurrenceStatuses().get(dateKey);

            // Proveravamo da status nije DONE, FAILED, ili CANCELLED
            boolean isReadOnly = "DONE".equals(statusStr) ||
                    "FAILED".equals(statusStr) ||
                    "CANCELLED".equals(statusStr);

            if (isFuture && !isReadOnly) {
                return true;
            }
        }

        return false;
    }
}