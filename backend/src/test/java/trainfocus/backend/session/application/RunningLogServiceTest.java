package trainfocus.backend.session.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trainfocus.backend.session.application.dto.RunningLogCalendarResponse;
import trainfocus.backend.session.application.dto.RunningLogDailyResponse;
import trainfocus.backend.session.application.dto.RunningLogPeriodResponse;
import trainfocus.backend.session.domain.FocusSession;
import trainfocus.backend.session.domain.FocusSessionStatus;
import trainfocus.backend.session.domain.repository.CalendarDayProjection;
import trainfocus.backend.session.domain.repository.FocusSessionRepository;
import trainfocus.backend.station.domain.Station;
import trainfocus.backend.station.domain.StationFixture;
import trainfocus.backend.user.domain.User;
import trainfocus.backend.user.domain.UserFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RunningLogServiceTest {

    @Mock
    FocusSessionRepository sessionRepository;

    @InjectMocks
    RunningLogService runningLogService;

    private static final List<FocusSessionStatus> ENDED =
            List.of(FocusSessionStatus.COMPLETED, FocusSessionStatus.ABORTED);

    private final User user = UserFixture.withId(1L);
    private final Station gangnam = StationFixture.of(10L, "강남");
    private final Station seoul = StationFixture.of(20L, "서울역");

    // ===================== getCalendar =====================

    @Test
    void 달력_그달_범위로_집계_조회() {
        given(sessionRepository.aggregateDailyFocus(
                eq(user), eq(ENDED), eq(FocusSessionStatus.COMPLETED),
                eq(LocalDateTime.of(2026, 6, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 7, 1, 0, 0))))
                .willReturn(List.of(
                        new FakeDay(LocalDate.of(2026, 6, 10), 3, 2, 240)));

        RunningLogCalendarResponse response = runningLogService.getCalendar(user, 2026, 6);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(6);
        assertThat(response.days()).hasSize(1);
        RunningLogCalendarResponse.CalendarDay day = response.days().get(0);
        assertThat(day.date()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(day.sessionCount()).isEqualTo(3);
        assertThat(day.arrivedCount()).isEqualTo(2);
        assertThat(day.runSeconds()).isEqualTo(240);
    }

    @Test
    void 달력_운행_없으면_빈_목록() {
        given(sessionRepository.aggregateDailyFocus(
                eq(user), eq(ENDED), eq(FocusSessionStatus.COMPLETED),
                eq(LocalDateTime.of(2026, 2, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 3, 1, 0, 0))))
                .willReturn(List.of());

        RunningLogCalendarResponse response = runningLogService.getCalendar(user, 2026, 2);

        assertThat(response.days()).isEmpty();
    }

    // ===================== getDaily =====================

    @Test
    void 그날_종료세션_승차권_요약() {
        LocalDate date = LocalDate.of(2026, 6, 10);
        given(sessionRepository.findSessionsBetween(
                eq(user), eq(ENDED),
                eq(date.atStartOfDay()),
                eq(date.plusDays(1).atStartOfDay())))
                .willReturn(List.of(
                        completed(date.atTime(9, 0), 1),    // focus 60s, target 60s, 완주
                        aborted(date.atTime(11, 0), 90)));  // focus 90s, 미완주

        RunningLogDailyResponse response = runningLogService.getDaily(user, date);

        assertThat(response.date()).isEqualTo(date);
        assertThat(response.sessionCount()).isEqualTo(2);
        assertThat(response.arrivedCount()).isEqualTo(1);
        assertThat(response.runSeconds()).isEqualTo(150);
        assertThat(response.tickets()).hasSize(2);

        RunningLogDailyResponse.Ticket first = response.tickets().get(0);
        assertThat(first.departure().id()).isEqualTo(10L);
        assertThat(first.arrival().id()).isEqualTo(20L);
        assertThat(first.focusSeconds()).isEqualTo(60);
        assertThat(first.targetSeconds()).isEqualTo(60);
        assertThat(first.completed()).isTrue();
        assertThat(response.tickets().get(1).completed()).isFalse();
    }

    @Test
    void 그날_운행_없으면_0() {
        LocalDate date = LocalDate.of(2026, 6, 10);
        given(sessionRepository.findSessionsBetween(
                eq(user), eq(ENDED),
                eq(date.atStartOfDay()),
                eq(date.plusDays(1).atStartOfDay())))
                .willReturn(List.of());

        RunningLogDailyResponse response = runningLogService.getDaily(user, date);

        assertThat(response.runSeconds()).isZero();
        assertThat(response.arrivedCount()).isZero();
        assertThat(response.sessionCount()).isZero();
        assertThat(response.tickets()).isEmpty();
    }

    // ===================== getPeriod =====================

    @Test
    void 기간_선택일이_속한_주와_달_집계() {
        LocalDate date = LocalDate.of(2026, 6, 10); // 수요일
        LocalDateTime weekFrom = LocalDateTime.of(2026, 6, 7, 0, 0);  // 직전 일요일
        LocalDateTime monthFrom = LocalDateTime.of(2026, 6, 1, 0, 0);

        given(sessionRepository.sumFocusSecondsBetween(
                eq(user), eq(ENDED), eq(weekFrom), eq(weekFrom.plusWeeks(1))))
                .willReturn(600L);
        given(sessionRepository.sumFocusSecondsBetween(
                eq(user), eq(ENDED), eq(monthFrom), eq(monthFrom.plusMonths(1))))
                .willReturn(3600L);

        RunningLogPeriodResponse response = runningLogService.getPeriod(user, date);

        assertThat(response.weekRunSeconds()).isEqualTo(600);
        assertThat(response.monthRunSeconds()).isEqualTo(3600);
    }

    @Test
    void 기간_일요일은_그날이_주_시작() {
        LocalDate sunday = LocalDate.of(2026, 6, 7); // 일요일
        LocalDateTime weekFrom = LocalDateTime.of(2026, 6, 7, 0, 0);
        LocalDateTime monthFrom = LocalDateTime.of(2026, 6, 1, 0, 0);

        given(sessionRepository.sumFocusSecondsBetween(
                eq(user), eq(ENDED), eq(weekFrom), eq(weekFrom.plusWeeks(1))))
                .willReturn(100L);
        given(sessionRepository.sumFocusSecondsBetween(
                eq(user), eq(ENDED), eq(monthFrom), eq(monthFrom.plusMonths(1))))
                .willReturn(200L);

        RunningLogPeriodResponse response = runningLogService.getPeriod(user, sunday);

        assertThat(response.weekRunSeconds()).isEqualTo(100);
        assertThat(response.monthRunSeconds()).isEqualTo(200);
    }

    // ===================== helpers =====================

    private FocusSession completed(LocalDateTime start, int baseMinutes) {
        FocusSession s = FocusSession.createNewFocusSession(user, gangnam, seoul, baseMinutes, 0, start);
        s.complete(start.plusSeconds(baseMinutes * 60L));
        return s;
    }

    private FocusSession aborted(LocalDateTime start, int focusSeconds) {
        FocusSession s = FocusSession.createNewFocusSession(user, gangnam, seoul, 5, 0, start);
        s.abort(start.plusSeconds(focusSeconds));
        return s;
    }

    private record FakeDay(LocalDate date, long sessionCount, long arrivedCount, long runSeconds)
            implements CalendarDayProjection {
        @Override public LocalDate getDate() { return date; }
        @Override public long getSessionCount() { return sessionCount; }
        @Override public long getArrivedCount() { return arrivedCount; }
        @Override public long getRunSeconds() { return runSeconds; }
    }
}
