package trainfocus.backend.session.application.dto;

public record RunningLogPeriodResponse(
        long weekRunSeconds,
        long monthRunSeconds
) {
}
