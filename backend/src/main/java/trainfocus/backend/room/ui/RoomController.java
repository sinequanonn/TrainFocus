package trainfocus.backend.room.ui;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trainfocus.backend.auth.ui.LoginUser;
import trainfocus.backend.common.ui.ApiResponse;
import trainfocus.backend.room.application.RoomService;
import trainfocus.backend.room.application.dto.*;
import trainfocus.backend.room.domain.RankPeriod;
import trainfocus.backend.user.domain.User;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> create(
            @LoginUser User user,
            @Valid @RequestBody RoomCreateRequest request
    ) {
        RoomResponse response = RoomResponse.from(roomService.create(request.name(), user));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> findMyRooms(@LoginUser User user) {
        List<RoomResponse> response = roomService.findMyRooms(user).stream()
                .map(RoomResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> findRoomDetail(
            @LoginUser User user,
            @PathVariable Long roomId
    ) {
        RoomResponse response = RoomResponse.from(roomService.findRoomDetail(roomId, user));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{roomId}/users")
    public ResponseEntity<ApiResponse<List<RoomUserResponse>>> findRoomMembers(
            @LoginUser User user,
            @PathVariable Long roomId
    ) {
        List<RoomUserResponse> response = roomService.findRoomMembers(roomId, user).stream()
                .map(RoomUserResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{roomId}/live")
    public ResponseEntity<ApiResponse<List<RoomUserLiveResponse>>> findRoomLive(
            @LoginUser User user,
            @PathVariable Long roomId
    ) {
        List<RoomUserLiveResponse> response = roomService.findRoomLive(roomId, user);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<RoomResponse>> join(
            @LoginUser User user,
            @Valid @RequestBody RoomJoinRequest request
    ) {
        RoomResponse response = RoomResponse.from(roomService.joinByCode(request.code(), user));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/{roomId}/users/me")
    public ResponseEntity<ApiResponse<Void>> leave(
            @LoginUser User user,
            @PathVariable Long roomId
    ) {
        roomService.leave(roomId, user);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @DeleteMapping("/{roomId}/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @LoginUser User user,
            @PathVariable Long roomId,
            @PathVariable Long userId
    ) {
        roomService.kickMember(roomId, userId, user);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PutMapping("/{roomId}/owner")
    public ResponseEntity<ApiResponse<Void>> transferOwner(
            @LoginUser User user,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomOwnerTransferRequest request
    ) {
        roomService.transferOwner(roomId, request.targetUserId(), user);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PatchMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> rename(
            @LoginUser User user,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomRenameRequest request
    ) {
        RoomResponse response = RoomResponse.from(roomService.rename(roomId, request.name(), user));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{roomId}/code/reset")
    public ResponseEntity<ApiResponse<RoomResponse>> reIssueCode(
            @LoginUser User user,
            @PathVariable Long roomId
    ) {
        RoomResponse response = RoomResponse.from(roomService.reIssueCode(roomId, user));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @LoginUser User user,
            @PathVariable Long roomId
    ) {
        roomService.deleteRoom(roomId, user);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @GetMapping("/{roomId}/ranking")
    public ResponseEntity<ApiResponse<RoomRankingResponse>> ranking(
            @LoginUser User user,
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String period
    ) {
        RoomRankingResponse response =
                roomService.findRanking(roomId, user, date, RankPeriod.of(period));
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
