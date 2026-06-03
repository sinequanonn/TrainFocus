package trainfocus.backend.session.domain.repository;

import java.time.LocalDate;

public interface CalendarDayProjection {
    LocalDate getDate();

    long getSessionCount();

    long getArrivedCount();

    long getRunSeconds();
}
