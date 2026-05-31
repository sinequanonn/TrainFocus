package trainfocus.backend.room.domain.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import trainfocus.backend.common.config.JpaConfig;
import trainfocus.backend.room.domain.Room;
import trainfocus.backend.room.domain.RoomUser;
import trainfocus.backend.user.domain.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoomUserRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    RoomUserRepository roomUserRepository;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void findAllByUserId_여러_방_조회() {
        User user = persistUser("uid-1");
        Room a = persistRoom("방A", "CODEAAAA");
        Room b = persistRoom("방B", "CODEBBBB");
        roomUserRepository.save(RoomUser.createOwner(a, user));
        roomUserRepository.save(RoomUser.createMember(b, user));

        List<RoomUser> result = roomUserRepository.findAllByUserId(user.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findAllByRoomId_여러_멤버_조회() {
        User u1 = persistUser("uid-1");
        User u2 = persistUser("uid-2");
        Room room = persistRoom("방", "CODE1111");
        roomUserRepository.save(RoomUser.createOwner(room, u1));
        roomUserRepository.save(RoomUser.createMember(room, u2));

        List<RoomUser> result = roomUserRepository.findAllByRoomId(room.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findByRoomIdAndUserId_성공() {
        User user = persistUser("uid-1");
        Room room = persistRoom("방", "CODE1111");
        roomUserRepository.save(RoomUser.createOwner(room, user));

        Optional<RoomUser> found = roomUserRepository.findByRoomIdAndUserId(room.getId(), user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().isOwner()).isTrue();
    }

    @Test
    void existsByRoomIdAndUserId_존재하면_true() {
        User user = persistUser("uid-1");
        Room room = persistRoom("방", "CODE1111");
        roomUserRepository.save(RoomUser.createOwner(room, user));

        assertThat(roomUserRepository.existsByRoomIdAndUserId(room.getId(), user.getId())).isTrue();
    }

    @Test
    void countByRoomId_멤버_수_반환() {
        User u1 = persistUser("uid-1");
        User u2 = persistUser("uid-2");
        Room room = persistRoom("방", "CODE1111");
        roomUserRepository.save(RoomUser.createOwner(room, u1));
        roomUserRepository.save(RoomUser.createMember(room, u2));

        assertThat(roomUserRepository.countByRoomId(room.getId())).isEqualTo(2);
    }

    @Test
    void deleteByRoomId_방_멤버_전체_삭제() {
        User u1 = persistUser("uid-1");
        User u2 = persistUser("uid-2");
        Room room = persistRoom("방", "CODE1111");
        roomUserRepository.save(RoomUser.createOwner(room, u1));
        roomUserRepository.save(RoomUser.createMember(room, u2));

        roomUserRepository.deleteByRoomId(room.getId());
        roomUserRepository.flush();

        assertThat(roomUserRepository.countByRoomId(room.getId())).isZero();
    }

    @Test
    void 같은_방_같은_유저_중복_저장_시_무결성_예외() {
        User user = persistUser("uid-1");
        Room room = persistRoom("방", "CODE1111");
        roomUserRepository.save(RoomUser.createOwner(room, user));
        roomUserRepository.flush();

        assertThatThrownBy(() -> {
            roomUserRepository.save(RoomUser.createMember(room, user));
            roomUserRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private User persistUser(String uid) {
        User user = User.createNewUser(uid, uid + "@b.com", "이름" + uid);
        entityManager.persist(user);
        return user;
    }

    private Room persistRoom(String name, String code) {
        return roomRepository.save(Room.create(name, code));
    }
}
