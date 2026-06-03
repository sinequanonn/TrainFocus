package trainfocus.backend.room.domain;

import trainfocus.backend.common.exception.BusinessException;
import trainfocus.backend.common.exception.ErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public enum RankPeriod {
    DAY, WEEK, MONTH;

    public static RankPeriod of(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
    }

    public LocalDateTime startOf(LocalDate date) {
        return switch (this) {
            case DAY -> date.atStartOfDay();
            case WEEK -> date.minusDays(date.getDayOfWeek().getValue() % 7).atStartOfDay();
            case MONTH -> date.withDayOfMonth(1).atStartOfDay();
        };
    }

    public LocalDateTime endOf(LocalDate date) {
        return switch (this) {
            case DAY -> startOf(date).plusDays(1);
            case WEEK -> startOf(date).plusWeeks(1);
            case MONTH -> startOf(date).plusMonths(1);
        };
    }
}
