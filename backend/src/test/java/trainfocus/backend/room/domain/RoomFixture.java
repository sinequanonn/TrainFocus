package trainfocus.backend.room.domain;

import org.springframework.test.util.ReflectionTestUtils;

public class RoomFixture {

    public static Room withName(String name) {
        return Room.create(name, "CODE0001");
    }

    public static Room of(Long id, String name, String code) {
        Room room = Room.create(name, code);
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }

    public static Room withId(Long id) {
        return of(id, "방" + id, String.format("CODE%04d", id));
    }
}
