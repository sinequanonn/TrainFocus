package trainfocus.backend.room.domain;

import org.junit.jupiter.api.Test;
import trainfocus.backend.user.domain.User;
import trainfocus.backend.user.domain.UserFixture;

import static org.assertj.core.api.Assertions.assertThat;

class RoomUserTest {

    @Test
    void 방장으로_생성_성공() {
        Room room = RoomFixture.withName("방");
        User user = UserFixture.withId(1L);

        RoomUser owner = RoomUser.createOwner(room, user);

        assertThat(owner.getRole()).isEqualTo(RoomUserRole.OWNER);
        assertThat(owner.isOwner()).isTrue();
    }

    @Test
    void 멤버로_생성_성공() {
        Room room = RoomFixture.withName("방");
        User user = UserFixture.withId(1L);

        RoomUser member = RoomUser.createMember(room, user);

        assertThat(member.getRole()).isEqualTo(RoomUserRole.MEMBER);
        assertThat(member.isOwner()).isFalse();
    }

    @Test
    void 멤버를_방장으로_승급_성공() {
        Room room = RoomFixture.withName("방");
        User user = UserFixture.withId(1L);
        RoomUser member = RoomUser.createMember(room, user);

        member.promoteToOwner();

        assertThat(member.isOwner()).isTrue();
    }

    @Test
    void 방장을_멤버로_강등_성공() {
        Room room = RoomFixture.withName("방");
        User user = UserFixture.withId(1L);
        RoomUser owner = RoomUser.createOwner(room, user);

        owner.demoteToMember();

        assertThat(owner.isOwner()).isFalse();
    }
}
