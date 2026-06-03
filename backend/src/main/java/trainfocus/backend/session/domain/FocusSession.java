package trainfocus.backend.session.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trainfocus.backend.common.domain.BaseEntity;
import trainfocus.backend.common.exception.BusinessException;
import trainfocus.backend.common.exception.ErrorCode;
import trainfocus.backend.station.domain.Station;
import trainfocus.backend.user.domain.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "focus_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FocusSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_station_id", nullable = false)
    private Station departureStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_station_id", nullable = false)
    private Station arrivalStation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private FocusSessionStatus status;

    @Column(nullable = false)
    private Integer baseDurationMinutes;

    @Column(nullable = false)
    private Integer delayMinutes;

    @Column(nullable = false)
    private Integer totalTargetMinutes;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime plannedEndAt;

    private LocalDateTime endedAt;

    @Column
    private Integer focusSeconds;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("legNumber ASC")
    private final List<Leg> legs = new ArrayList<>();

    public static FocusSession createNewFocusSession(User user, Station departure, Station arrival,
                                                     int baseDurationMinutes, int delayMinutes,
                                                     LocalDateTime startedAt) {
        return new FocusSession(user, departure, arrival,
                baseDurationMinutes, delayMinutes, startedAt);
    }

    private FocusSession(User user, Station departure, Station arrival,
                         int baseDurationMinutes, int delayMinutes,
                         LocalDateTime startedAt) {
        if (departure.getId() != null
                && departure.getId().equals(arrival.getId())) {
            throw new BusinessException(ErrorCode.ROUTE_SAME_STATION);
        }
        if (baseDurationMinutes <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        if (delayMinutes < 0) {
            throw new BusinessException(ErrorCode.SESSION_DELAY_NEGATIVE);
        }
        int totalMinutes = baseDurationMinutes + delayMinutes;
        this.user = user;
        this.departureStation = departure;
        this.arrivalStation = arrival;
        this.status = FocusSessionStatus.RUNNING;
        this.baseDurationMinutes = baseDurationMinutes;
        this.delayMinutes = delayMinutes;
        this.totalTargetMinutes = totalMinutes;
        this.startedAt = startedAt;
        this.plannedEndAt = startedAt.plusMinutes(totalMinutes);
        this.legs.add(Leg.start(this, 1, startedAt));
    }

    public void pause(LocalDateTime now) {
        if (this.status.isEnded()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        if (this.status == FocusSessionStatus.PAUSED) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_PAUSED);
        }
        currentLeg().end(now);
        this.status = FocusSessionStatus.PAUSED;
    }

    public void resume(LocalDateTime now) {
        if (this.status.isEnded()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        if (this.status == FocusSessionStatus.RUNNING) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_RUNNING);
        }
        this.legs.add(Leg.start(this, this.legs.size() + 1, now));
        this.status = FocusSessionStatus.RUNNING;
    }

    public void complete(LocalDateTime now) {
        if (this.status.isEnded()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        if (this.status == FocusSessionStatus.RUNNING) {
            currentLeg().end(now);
        }
        if (accumulatedSeconds() < totalTargetSeconds()) {
            throw new BusinessException(ErrorCode.SESSION_TARGET_NOT_REACHED);
        }
        this.status = FocusSessionStatus.COMPLETED;
        this.endedAt = now;
        this.focusSeconds = accumulatedSeconds();
        this.user.updateDepartureStation(this.arrivalStation);
    }

    public void abort(LocalDateTime now) {
        if (this.status.isEnded()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        if (this.status == FocusSessionStatus.RUNNING) {
            currentLeg().end(now);
        }
        this.status = FocusSessionStatus.ABORTED;
        this.endedAt = now;
        this.focusSeconds = accumulatedSeconds();
    }

    public int totalTargetSeconds() {
        return this.totalTargetMinutes * 60;
    }

    public int accumulatedSeconds(LocalDateTime now) {
        int total = 0;
        for (Leg leg : this.legs) {
            if (leg.isRunning()) {
                total += leg.elapsedSeconds(now);
            } else {
                total += leg.getDurationSeconds();
            }
        }
        return total;
    }

    /** 종료된 leg만 합산 */
    public int accumulatedSeconds() {
        return this.legs.stream()
                .filter(leg -> !leg.isRunning())
                .mapToInt(Leg::getDurationSeconds)
                .sum();
    }

    public int remainingSeconds(LocalDateTime now) {
        return Math.max(0, totalTargetSeconds() - accumulatedSeconds(now));
    }

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }

    public List<Leg> getLegs() {
        return Collections.unmodifiableList(this.legs);
    }

    public void autoCompleteIfTargetReached(LocalDateTime now) {
        if (this.status != FocusSessionStatus.RUNNING) {
            return;
        }

        if (this.legs.isEmpty()) {
            return;
        }
        int endedAccumulated = accumulatedSeconds();
        int remainingTargetSeconds = totalTargetSeconds() - endedAccumulated;
        LocalDateTime currentLegStart = currentLeg().getStartedAt();

        LocalDateTime reachTime = remainingTargetSeconds <= 0
                ? currentLegStart
                : currentLegStart.plusSeconds(remainingTargetSeconds);

        if (now.isBefore(reachTime)) {
            return;
        }
        currentLeg().end(reachTime);
        this.status = FocusSessionStatus.COMPLETED;
        this.endedAt = reachTime;
        this.focusSeconds = accumulatedSeconds();
        this.user.updateDepartureStation(this.arrivalStation);
    }

    private Leg currentLeg() {
        return this.legs.getLast();
    }
}
