package trainfocus.backend.session.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import trainfocus.backend.session.domain.FocusSession;
import trainfocus.backend.session.domain.FocusSessionStatus;
import trainfocus.backend.user.domain.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    boolean existsByUserAndStatusIn(User user, Collection<FocusSessionStatus> status);

    Optional<FocusSession> findFirstByUserAndStatusIn(User user, Collection<FocusSessionStatus> statuses);

    @EntityGraph(attributePaths = {"departureStation", "arrivalStation"})
    Page<FocusSession> findByUserAndStatusIn(User user, Collection<FocusSessionStatus> statues, Pageable pageable);

    @EntityGraph(attributePaths = {"legs", "departureStation", "arrivalStation"})
    @Query("SELECT fs from FocusSession fs where fs.id = :id")
    Optional<FocusSession> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "departureStation", "arrivalStation"})
    @Query(value = """
            SELECT fs FROM FocusSession fs
            WHERE fs.status IN :statuses
            ORDER BY fs.startedAt ASC
            """,
            countQuery = """
                    SELECT COUNT(fs) FROM FocusSession fs
                    WHERE fs.status IN :statuses
                    """)
    Page<FocusSession> findActiveForAdmin(
            @Param("statuses") Collection<FocusSessionStatus> statuses,
            Pageable pageable);

    long countByStatusIn(Collection<FocusSessionStatus> statuses);

    long countByStartedAtGreaterThanEqual(LocalDateTime startedAtFrom);

    @Query("SELECT fs FROM FocusSession fs " +
            "JOIN FETCH fs.departureStation " +
            "JOIN FETCH fs.arrivalStation " +
            "WHERE fs.user.id IN :userIds AND fs.status IN :statuses")
    List<FocusSession> findActiveByUserIds(@Param("userIds") List<Long> userIds,
                                           @Param("statuses") Collection<FocusSessionStatus> statuses);

    @Query("""
            SELECT FUNCTION('DATE', fs.startedAt) AS date,
                   COUNT(fs) AS sessionCount,
                   SUM(CASE WHEN fs.status = :completed THEN 1 ELSE 0 END) AS arrivedCount,
                   COALESCE(SUM(fs.focusSeconds), 0) AS runSeconds
            FROM FocusSession fs
            WHERE fs.user = :user
              AND fs.status IN :statuses
              AND fs.startedAt >= :from AND fs.startedAt < :to
            GROUP BY FUNCTION('DATE', fs.startedAt)
            ORDER BY FUNCTION('DATE', fs.startedAt)
            """)
    List<CalendarDayProjection> aggregateDailyFocus(
            @Param("user") User user,
            @Param("statuses") Collection<FocusSessionStatus> statuses,
            @Param("completed") FocusSessionStatus completed,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @EntityGraph(attributePaths = {"departureStation", "arrivalStation"})
    @Query("""
            SELECT fs FROM FocusSession fs
            WHERE fs.user = :user
              AND fs.status IN :statuses
              AND fs.startedAt >= :from AND fs.startedAt < :to
            ORDER BY fs.startedAt ASC
            """)
    List<FocusSession> findSessionsBetween(
            @Param("user") User user,
            @Param("statuses") Collection<FocusSessionStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(fs.focusSeconds), 0)
            FROM FocusSession fs
            WHERE fs.user = :user
              AND fs.status IN :statuses
              AND fs.startedAt >= :from AND fs.startedAt < :to
            """)
    long sumFocusSecondsBetween(
            @Param("user") User user,
            @Param("statuses") Collection<FocusSessionStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT fs.user.id AS userId, COALESCE(SUM(fs.focusSeconds), 0) AS runSeconds
            FROM FocusSession fs
            WHERE fs.user.id IN :userIds AND fs.status IN :statuses
              AND fs.startedAt >= :from AND fs.startedAt < :to
            GROUP BY fs.user.id
            """)
    List<RoomRankingProjection> sumFocusByUsers(
            @Param("userIds") List<Long> userIds,
            @Param("statuses") Collection<FocusSessionStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
