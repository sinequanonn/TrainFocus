package trainfocus.backend.session.application.dto;

import trainfocus.backend.session.domain.repository.CalendarDayProjection;

import java.time.LocalDate;
import java.util.List;

public record RunningLogCalendarResponse(
        int year,
        int month,
        List<CalendarDay> days
) {
    public record CalendarDay(
            LocalDate date,
            long sessionCount,
            long arrivedCount,
            long runSeconds
    ) {
        public static CalendarDay from(CalendarDayProjection p) {
            return new CalendarDay(p.getDate(),
                    p.getSessionCount(),
                    p.getArrivedCount(),
                    p.getRunSeconds());
        }
    }

    public static RunningLogCalendarResponse of(int year, int month, List<CalendarDayProjection> rows) {
        return new RunningLogCalendarResponse(
                year,
                month,
                rows.stream()
                        .map(CalendarDay::from)
                        .toList());
    }
}
