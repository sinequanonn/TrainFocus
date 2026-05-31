package trainfocus.backend.room.application.dto;

import trainfocus.backend.room.domain.RoomUser;
import trainfocus.backend.room.domain.RoomUserRole;

public record RoomUserResponse(
        Long userId,
        String nickname,
        RoomUserRole role
) {
    public static RoomUserResponse from(RoomUser membership) {
        return new RoomUserResponse(
                membership.getUser().getId(),
                membership.getUser().getNickname(),
                membership.getRole()
        );
    }
}
