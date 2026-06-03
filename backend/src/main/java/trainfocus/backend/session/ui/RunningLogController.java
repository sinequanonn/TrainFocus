package trainfocus.backend.session.ui;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trainfocus.backend.auth.ui.LoginUser;
import trainfocus.backend.common.ui.ApiResponse;
import trainfocus.backend.session.application.RunningLogService;
import trainfocus.backend.session.application.dto.RunningLogCalendarResponse;
import trainfocus.backend.session.application.dto.RunningLogDailyResponse;
import trainfocus.backend.session.application.dto.RunningLogPeriodResponse;
import trainfocus.backend.user.domain.User;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions/log")
public class RunningLogController {

    private final RunningLogService runningLogService;

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<RunningLogCalendarResponse>> calendar(
            @LoginUser User user,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(ApiResponse.of(runningLogService.getCalendar(user, year, month)));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<RunningLogDailyResponse>> daily(
            @LoginUser User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.of(runningLogService.getDaily(user, date)));
    }

    @GetMapping("/period")
    public ResponseEntity<ApiResponse<RunningLogPeriodResponse>> period(
            @LoginUser User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.of(runningLogService.getPeriod(user, date)));
    }
}
