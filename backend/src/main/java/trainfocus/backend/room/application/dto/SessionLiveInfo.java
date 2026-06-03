package trainfocus.backend.room.application.dto;

import trainfocus.backend.session.domain.FocusSession;
import trainfocus.backend.station.application.dto.StationResponse;

import java.time.LocalDateTime;

public record SessionLiveInfo(
        Long sessionId,
        String status,
        StationResponse departure,
        StationResponse arrival,
        Integer totalTargetSeconds,
        Integer accumulatedSeconds,
        LocalDateTime startedAt,
        LocalDateTime plannedEndAt
) {
    public static SessionLiveInfo from(FocusSession session, LocalDateTime now) {
        return new SessionLiveInfo(
                session.getId(),
                session.getStatus().name(),
                StationResponse.from(session.getDepartureStation()),
                StationResponse.from(session.getArrivalStation()),
                session.totalTargetSeconds(),
                session.accumulatedSeconds(now),
                session.getStartedAt(),
                session.getPlannedEndAt()
        );
    }
}
