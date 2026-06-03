package trainfocus.backend.session.domain.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import trainfocus.backend.common.config.JpaConfig;
import trainfocus.backend.session.domain.FocusSession;
import trainfocus.backend.session.domain.FocusSessionStatus;
import trainfocus.backend.station.domain.Station;
import trainfocus.backend.station.domain.StationFixture;
import trainfocus.backend.user.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FocusSessionRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    FocusSessionRepository focusSessionRepository;

    @Autowired
    EntityManager entityManager;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 9, 10, 0);

    @Test
    void existsByUserAndStatusIn_활성_세션_존재_여부() {
        // given
        User user = persistUser("uid-1");
        Station dep = persistStation("강남");
        Station arr = persistStation("서울역");
        focusSessionRepository.save(
                FocusSession.createNewFocusSession(user, dep, arr, 30, 0, NOW)
        );

        // when
        boolean exists = focusSessionRepository.existsByUserAndStatusIn(
                user, List.of(FocusSessionStatus.RUNNING, FocusSessionStatus.PAUSED)
        );

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserAndStatusIn_종료된_세션은_제외() {
        // given
        User user = persistUser("uid-1");
        Station dep = persistStation("강남");
        Station arr = persistStation("서울역");
        FocusSession session = FocusSession.createNewFocusSession(user, dep, arr, 1, 0, NOW);
        session.complete(NOW.plusSeconds(60));
        focusSessionRepository.save(session);

        // when
        boolean exists = focusSessionRepository.existsByUserAndStatusIn(
                user, List.of(FocusSessionStatus.RUNNING, FocusSessionStatus.PAUSED)
        );

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void findFirstByUserAndStatusIn_활성_세션_조회() {
        // given
        User user = persistUser("uid-1");
        Station dep = persistStation("강남");
        Station arr = persistStation("서울역");
        FocusSession active = focusSessionRepository.save(
                FocusSession.createNewFocusSession(user, dep, arr, 30, 0, NOW)
        );

        // when
        Optional<FocusSession> found = focusSessionRepository.findFirstByUserAndStatusIn(
                user, List.of(FocusSessionStatus.RUNNING, FocusSessionStatus.PAUSED)
        );

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(active.getId());
    }

    @Test
    void findByUserAndStatusIn_페이지_조회() {
        // given
        User user = persistUser("uid-1");
        Station dep = persistStation("강남");
        Station arr = persistStation("서울역");
        for (int i = 0; i < 3; i++) {
            FocusSession s = FocusSession.createNewFocusSession(user, dep, arr, 1, 0, NOW.plusMinutes(i));
            s.complete(NOW.plusMinutes(i).plusSeconds(60));
            focusSessionRepository.save(s);
        }

        // when
        Page<FocusSession> page = focusSessionRepository.findByUserAndStatusIn(
                user,
                List.of(FocusSessionStatus.COMPLETED, FocusSessionStatus.ABORTED),
                PageRequest.of(0, 2)
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void findDetailById_legs_포함하여_조회() {
        // given
        User user = persistUser("uid-1");
        Station dep = persistStation("강남");
        Station arr = persistStation("서울역");
        FocusSession session = FocusSession.createNewFocusSession(user, dep, arr, 30, 0, NOW);
        session.pause(NOW.plusSeconds(60));
        session.resume(NOW.plusSeconds(120));
        FocusSession saved = focusSessionRepository.save(session);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<FocusSession> found = focusSessionRepository.findDetailById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getLegs()).hasSize(2);
        assertThat(found.get().getDepartureStation().getName()).isEqualTo("강남");
    }

    @Test
    void findDetailById_없으면_Optional_empty() {
        Optional<FocusSession> found = focusSessionRepository.findDetailById(9999L);
        assertThat(found).isEmpty();
    }

    @Test
    void aggregateDailyFocus_날짜별_집중시간_집계() {
        // given
        User user = persistUser("uid-1");
        Station dep = persistStation("강남");
        Station arr = persistStation("서울역");
        saveCompleted(user, dep, arr, LocalDateTime.of(2026, 6, 10, 9, 0), 1);   // focus 60s
        saveCompleted(user, dep, arr, LocalDateTime.of(2026, 6, 10, 10, 0), 2);  // focus 120s
        saveAborted(user, dep, arr, LocalDateTime.of(2026, 6, 10, 11, 0), 60);   // focus 60s, 중단
        saveCompleted(user, dep, arr, LocalDateTime.of(2026, 6, 15, 9, 0), 1);   // 다른 날
        entityManager.flush();
        entityManager.clear();

        // when
        List<CalendarDayProjection> rows = focusSessionRepository.aggregateDailyFocus(
                user,
                List.of(FocusSessionStatus.COMPLETED, FocusSessionStatus.ABORTED),
                FocusSessionStatus.COMPLETED,
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 7, 1, 0, 0)
        );

        // then
        assertThat(rows).hasSize(2);
        CalendarDayProjection day10 = rows.stream()
                .filter(r -> r.getDate().equals(LocalDate.of(2026, 6, 10)))
                .findFirst().orElseThrow();
        assertThat(day10.getSessionCount()).isEqualTo(3);
        assertThat(day10.getArrivedCount()).isEqualTo(2);
        assertThat(day10.getRunSeconds()).isEqualTo(240);
    }

    @Test
    void findSessionsBetween_그날_종료세션_시간순() {
        // given
        User user = persistUser("uid-1");
        Station dep = persistStation("강남");
        Station arr = persistStation("서울역");
        saveCompleted(user, dep, arr, LocalDateTime.of(2026, 6, 10, 9, 0), 1);
        saveCompleted(user, dep, arr, LocalDateTime.of(2026, 6, 10, 10, 0), 2);
        saveCompleted(user, dep, arr, LocalDateTime.of(2026, 6, 11, 9, 0), 1);   // 범위 밖
        entityManager.flush();
        entityManager.clear();

        // when
        List<FocusSession> sessions = focusSessionRepository.findSessionsBetween(
                user,
                List.of(FocusSessionStatus.COMPLETED, FocusSessionStatus.ABORTED),
                LocalDateTime.of(2026, 6, 10, 0, 0),
                LocalDateTime.of(2026, 6, 11, 0, 0)
        );

        // then
        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0).getFocusSeconds()).isEqualTo(60);
        assertThat(sessions.get(1).getFocusSeconds()).isEqualTo(120);
    }

    @Test
    void sumFocusSecondsBetween_범위_집중시간_합() {
        // given
        User user = persistUser("uid-1");
        Station dep = persistStation("강남");
        Station arr = persistStation("서울역");
        saveCompleted(user, dep, arr, LocalDateTime.of(2026, 6, 10, 9, 0), 1);   // 60
        saveCompleted(user, dep, arr, LocalDateTime.of(2026, 6, 10, 10, 0), 2);  // 120
        saveAborted(user, dep, arr, LocalDateTime.of(2026, 6, 10, 11, 0), 60);   // 60
        entityManager.flush();
        entityManager.clear();

        // when
        long sum = focusSessionRepository.sumFocusSecondsBetween(
                user,
                List.of(FocusSessionStatus.COMPLETED, FocusSessionStatus.ABORTED),
                LocalDateTime.of(2026, 6, 10, 0, 0),
                LocalDateTime.of(2026, 6, 11, 0, 0)
        );

        // then
        assertThat(sum).isEqualTo(240);
    }

    private void saveCompleted(User u, Station dep, Station arr, LocalDateTime start, int baseMinutes) {
        FocusSession s = FocusSession.createNewFocusSession(u, dep, arr, baseMinutes, 0, start);
        s.complete(start.plusSeconds(baseMinutes * 60L));
        focusSessionRepository.save(s);
    }

    private void saveAborted(User u, Station dep, Station arr, LocalDateTime start, int focusSeconds) {
        FocusSession s = FocusSession.createNewFocusSession(u, dep, arr, 5, 0, start);
        s.abort(start.plusSeconds(focusSeconds));
        focusSessionRepository.save(s);
    }

    private User persistUser(String uid) {
        User user = User.createNewUser(uid, uid + "@b.com", "이름" + uid);
        entityManager.persist(user);
        return user;
    }

    private Station persistStation(String name) {
        Station station = StationFixture.withName(name);
        entityManager.persist(station);
        return station;
    }
}
