package trainfocus.backend.room.application.dto;

import trainfocus.backend.room.domain.RoomUser;
import trainfocus.backend.room.domain.RoomUserRole;
import trainfocus.backend.session.domain.FocusSession;

import java.time.LocalDateTime;

public record RoomUserLiveResponse(
        Long userId,
        String nickname,
        RoomUserRole role,
        SessionLiveInfo session
) {
    public static RoomUserLiveResponse from(RoomUser membership,
                                            FocusSession session,
                                            LocalDateTime now) {
        return new RoomUserLiveResponse(
                membership.getUser().getId(),
                membership.getUser().getNickname(),
                membership.getRole(),
                session != null ? SessionLiveInfo.from(session, now) : null
        );
    }
}
