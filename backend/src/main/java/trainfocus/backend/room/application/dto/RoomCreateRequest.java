package trainfocus.backend.room.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
        @NotBlank
        @Size(max = 20)
        String name
) {
}
