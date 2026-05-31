package trainfocus.backend.room.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoomJoinRequest(
        @NotBlank
        @Pattern(regexp = "[A-Z0-9]{8}", message = "초대 코드는 형식이 적절하지 않습니다.")
        String code
) {
}
