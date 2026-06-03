package trainfocus.backend.session.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trainfocus.backend.session.application.dto.RunningLogCalendarResponse;
import trainfocus.backend.session.application.dto.RunningLogDailyResponse;
import trainfocus.backend.session.application.dto.RunningLogPeriodResponse;
import trainfocus.backend.session.domain.FocusSession;
import trainfocus.backend.session.domain.FocusSessionStatus;
import trainfocus.backend.session.domain.repository.CalendarDayProjection;
import trainfocus.backend.session.domain.repository.FocusSessionRepository;
import trainfocus.backend.user.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningLogService {

    private static final List<FocusSessionStatus> ENDED_STATUSES =
            List.of(FocusSessionStatus.COMPLETED, FocusSessionStatus.ABORTED);

    private final FocusSessionRepository sessionRepository;

    public RunningLogCalendarResponse getCalendar(User user, int year, int month) {
        LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);
        List<CalendarDayProjection> rows = sessionRepository.aggregateDailyFocus(
                user, ENDED_STATUSES, FocusSessionStatus.COMPLETED, from, to
        );
        return RunningLogCalendarResponse.of(year, month, rows);
    }

    public RunningLogDailyResponse getDaily(User user, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        List<FocusSession> sessions = sessionRepository.findSessionsBetween(user, ENDED_STATUSES, from, to);
        return RunningLogDailyResponse.of(date, sessions);
    }

    public RunningLogPeriodResponse getPeriod(User user, LocalDate date) {
        LocalDate weekStart = date.minusDays(date.getDayOfWeek().getValue() % 7);
        LocalDateTime weekFrom = weekStart.atStartOfDay();
        LocalDateTime monthFrom = date.withDayOfMonth(1).atStartOfDay();

        long week = sessionRepository.sumFocusSecondsBetween(user,
                ENDED_STATUSES,
                weekFrom,
                weekFrom.plusWeeks(1));
        long month = sessionRepository.sumFocusSecondsBetween(user,
                ENDED_STATUSES,
                monthFrom,
                monthFrom.plusMonths(1));
        return new RunningLogPeriodResponse(week, month);
    }
}
