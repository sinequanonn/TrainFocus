package trainfocus.backend.session.application.dto;

import trainfocus.backend.session.domain.FocusSession;
import trainfocus.backend.session.domain.FocusSessionStatus;
import trainfocus.backend.station.application.dto.StationResponse;

import java.time.LocalDate;
import java.util.List;

public record RunningLogDailyResponse(
        LocalDate date,
        long runSeconds,
        long arrivedCount,
        int sessionCount,
        List<Ticket> tickets
) {
    public record Ticket(
            Long sessionId,
            StationResponse departure,
            StationResponse arrival,
            int focusSeconds,
            int targetSeconds,
            boolean completed
    ) {
        public static Ticket from(FocusSession fs) {
            return new Ticket(
                    fs.getId(),
                    StationResponse.from(fs.getDepartureStation()),
                    StationResponse.from(fs.getArrivalStation()),
                    fs.getFocusSeconds() == null ? 0 : fs.getFocusSeconds(),
                    fs.totalTargetSeconds(),
                    fs.getStatus() == FocusSessionStatus.COMPLETED
            );
        }
    }

    public static RunningLogDailyResponse of(LocalDate date, List<FocusSession> sessions) {
        List<Ticket> tickets = sessions.stream().map(Ticket::from).toList();
        long runSeconds = tickets.stream().mapToLong(Ticket::focusSeconds).sum();
        long arrivedCount = tickets.stream().filter(Ticket::completed).count();
        return new RunningLogDailyResponse(date, runSeconds, arrivedCount, tickets.size(), tickets);
    }
}
