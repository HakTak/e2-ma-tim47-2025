package com.example.projekatmobilne.utils;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.util.HashSet;
import java.util.List;

public class EventDecorator implements DayViewDecorator {
    private final int[] colors;
    private final HashSet<CalendarDay> dates;

    public EventDecorator(List<Integer> colors, HashSet<CalendarDay> dates) {
        this.colors = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            this.colors[i] = colors.get(i);
        }
        this.dates = dates;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // Koristimo naš novi MultipleDotSpan
        view.addSpan(new MultipleDotSpan(7, colors));
    }
}