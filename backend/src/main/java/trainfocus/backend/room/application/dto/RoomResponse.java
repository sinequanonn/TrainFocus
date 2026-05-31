package trainfocus.backend.room.application.dto;

import trainfocus.backend.room.domain.RoomUser;
import trainfocus.backend.room.domain.RoomUserRole;

public record RoomResponse(
        Long id,
        String name,
        String code,
        RoomUserRole role
) {
    public static RoomResponse from(RoomUser membership) {
        return new RoomResponse(
                membership.getRoom().getId(),
                membership.getRoom().getName(),
                membership.getRoom().getCode(),
                membership.getRole()
        );
    }
}
