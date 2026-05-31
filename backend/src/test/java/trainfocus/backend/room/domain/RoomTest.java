package trainfocus.backend.room.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RoomTest {
    @Test
    void 방_생성_성공() {
        Room room = Room.create("스터디방", "ABCD1234");

        assertThat(room.getName()).isEqualTo("스터디방");
        assertThat(room.getCode()).isEqualTo("ABCD1234");
    }

    @Test
    void 방_이름_수정_성공() {
        Room room = Room.create("기존이름", "ABCD1234");

        room.rename("새이름");

        assertThat(room.getName()).isEqualTo("새이름");
    }

    @Test
    void 초대_코드_재발급_성공() {
        Room room = Room.create("방", "OLDCODE1");

        room.reIssueCode("NEWCODE1");

        assertThat(room.getCode()).isEqualTo("NEWCODE1");
    }
}
