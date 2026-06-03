package trainfocus.backend.room.domain;

import org.springframework.test.util.ReflectionTestUtils;
import trainfocus.backend.user.domain.User;

public class RoomUserFixture {

    public static RoomUser owner(Long id, Room room, User user) {
        RoomUser ru = RoomUser.createOwner(room, user);
        ReflectionTestUtils.setField(ru, "id", id);
        return ru;
    }

    public static RoomUser member(Long id, Room room, User user) {
        RoomUser ru = RoomUser.createMember(room, user);
        ReflectionTestUtils.setField(ru, "id", id);
        return ru;
    }
}
