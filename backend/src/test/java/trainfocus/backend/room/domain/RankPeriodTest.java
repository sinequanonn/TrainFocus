package trainfocus.backend.room.domain;

import org.junit.jupiter.api.Test;
import trainfocus.backend.common.exception.BusinessException;
import trainfocus.backend.common.exception.ErrorCode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankPeriodTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 3);

    @Test
    void DAY_그날_0시부터_다음날_0시() {
        assertThat(RankPeriod.DAY.startOf(DATE)).isEqualTo(LocalDateTime.of(2026, 6, 3, 0, 0));
        assertThat(RankPeriod.DAY.endOf(DATE)).isEqualTo(LocalDateTime.of(2026, 6, 4, 0, 0));
    }

    @Test
    void WEEK_일요일_시작_7일_범위() {
        LocalDateTime start = RankPeriod.WEEK.startOf(DATE);
        assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(start).isBeforeOrEqualTo(DATE.atStartOfDay());
        assertThat(RankPeriod.WEEK.endOf(DATE)).isEqualTo(start.plusWeeks(1));
    }

    @Test
    void MONTH_1일부터_다음달_1일() {
        assertThat(RankPeriod.MONTH.startOf(DATE)).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0));
        assertThat(RankPeriod.MONTH.endOf(DATE)).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
    }

    @Test
    void of_대소문자_무관_파싱() {
        assertThat(RankPeriod.of("day")).isEqualTo(RankPeriod.DAY);
        assertThat(RankPeriod.of("WEEK")).isEqualTo(RankPeriod.WEEK);
        assertThat(RankPeriod.of("Month")).isEqualTo(RankPeriod.MONTH);
    }

    @Test
    void of_잘못된_값이면_COMMON_INVALID_PARAMETER() {
        assertThatThrownBy(() -> RankPeriod.of("year"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER));
    }

    @Test
    void of_null이면_예외() {
        assertThatThrownBy(() -> RankPeriod.of(null)).isInstanceOf(BusinessException.class);
    }
}
