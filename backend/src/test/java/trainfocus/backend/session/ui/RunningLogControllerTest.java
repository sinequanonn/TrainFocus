package trainfocus.backend.session.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trainfocus.backend.auth.firebase.FirebaseAuthClient;
import trainfocus.backend.auth.firebase.FirebaseUserInfo;
import trainfocus.backend.session.application.RunningLogService;
import trainfocus.backend.session.application.dto.RunningLogCalendarResponse;
import trainfocus.backend.session.application.dto.RunningLogDailyResponse;
import trainfocus.backend.session.application.dto.RunningLogPeriodResponse;
import trainfocus.backend.station.application.dto.StationResponse;
import trainfocus.backend.user.application.UserService;
import trainfocus.backend.user.domain.User;
import trainfocus.backend.user.domain.UserFixture;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RunningLogController.class)
class RunningLogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RunningLogService runningLogService;

    @MockitoBean
    FirebaseAuthClient firebaseAuthClient;

    @MockitoBean
    UserService userService;

    private static final String BEARER = "Bearer valid-token";

    @BeforeEach
    void setUp() {
        User user = UserFixture.withId(1L);
        given(firebaseAuthClient.verifyToken("valid-token"))
                .willReturn(new FirebaseUserInfo("uid-1", "1@test.com", "테스터"));
        given(userService.findByFirebaseUid("uid-1")).willReturn(user);
    }

    // ===================== GET /api/sessions/log/calendar =====================

    @Test
    void 달력_조회_200() throws Exception {
        RunningLogCalendarResponse response = new RunningLogCalendarResponse(
                2026, 6,
                List.of(new RunningLogCalendarResponse.CalendarDay(
                        LocalDate.of(2026, 6, 10), 3, 2, 240)));
        given(runningLogService.getCalendar(any(User.class), eq(2026), eq(6)))
                .willReturn(response);

        mockMvc.perform(get("/api/sessions/log/calendar")
                        .header("Authorization", BEARER)
                        .param("year", "2026")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(6))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-06-10"))
                .andExpect(jsonPath("$.data.days[0].sessionCount").value(3))
                .andExpect(jsonPath("$.data.days[0].arrivedCount").value(2))
                .andExpect(jsonPath("$.data.days[0].runSeconds").value(240));
    }

    @Test
    void 달력_필수_파라미터_누락_400() throws Exception {
        mockMvc.perform(get("/api/sessions/log/calendar")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_PARAMETER"));
    }

    // ===================== GET /api/sessions/log/daily =====================

    @Test
    void 그날_조회_200() throws Exception {
        RunningLogDailyResponse response = new RunningLogDailyResponse(
                LocalDate.of(2026, 6, 10), 150, 1, 2,
                List.of(new RunningLogDailyResponse.Ticket(
                        100L,
                        new StationResponse(10L, "강남", null, null),
                        new StationResponse(20L, "서울역", null, null),
                        60, 60, true)));
        given(runningLogService.getDaily(any(User.class), eq(LocalDate.of(2026, 6, 10))))
                .willReturn(response);

        mockMvc.perform(get("/api/sessions/log/daily")
                        .header("Authorization", BEARER)
                        .param("date", "2026-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runSeconds").value(150))
                .andExpect(jsonPath("$.data.arrivedCount").value(1))
                .andExpect(jsonPath("$.data.sessionCount").value(2))
                .andExpect(jsonPath("$.data.tickets[0].sessionId").value(100L))
                .andExpect(jsonPath("$.data.tickets[0].departure.id").value(10L))
                .andExpect(jsonPath("$.data.tickets[0].focusSeconds").value(60))
                .andExpect(jsonPath("$.data.tickets[0].completed").value(true));
    }

    @Test
    void 그날_날짜형식_오류_400() throws Exception {
        mockMvc.perform(get("/api/sessions/log/daily")
                        .header("Authorization", BEARER)
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_PARAMETER"));
    }

    // ===================== GET /api/sessions/log/period =====================

    @Test
    void 기간_조회_200() throws Exception {
        given(runningLogService.getPeriod(any(User.class), eq(LocalDate.of(2026, 6, 10))))
                .willReturn(new RunningLogPeriodResponse(600, 3600));

        mockMvc.perform(get("/api/sessions/log/period")
                        .header("Authorization", BEARER)
                        .param("date", "2026-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekRunSeconds").value(600))
                .andExpect(jsonPath("$.data.monthRunSeconds").value(3600));
    }

    // ===================== auth =====================

    @Test
    void 토큰_없이_요청하면_401() throws Exception {
        mockMvc.perform(get("/api/sessions/log/period")
                        .param("date", "2026-06-10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_MISSING"));
    }
}
