package trainfocus.backend.room.application.dto;

import jakarta.validation.constraints.NotNull;

public record RoomOwnerTransferRequest(
        @NotNull Long targetUserId
) {
}
