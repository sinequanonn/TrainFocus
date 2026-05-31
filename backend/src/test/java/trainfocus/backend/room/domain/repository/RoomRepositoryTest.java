package trainfocus.backend.room.domain.repository;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoomRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    RoomRepository roomRepository;

    @Test
    void code로_방_조회_성공() {
        roomRepository.save(Room.create("방A", "ABCD1234"));

        Optional<Room> found = roomRepository.findByCode("ABCD1234");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("방A");
    }

    @Test
    void 없는_code면_Optional_empty() {
        Optional<Room> found = roomRepository.findByCode("ZZZZ9999");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByCode_존재하면_true() {
        roomRepository.save(Room.create("방", "ABCD1234"));

        assertThat(roomRepository.existsByCode("ABCD1234")).isTrue();
    }

    @Test
    void existsByCode_없으면_false() {
        assertThat(roomRepository.existsByCode("ZZZZ9999")).isFalse();
    }

    @Test
    void code_중복_저장_시_무결성_예외() {
        roomRepository.save(Room.create("방A", "SAME1234"));
        roomRepository.flush();

        assertThatThrownBy(() -> {
            roomRepository.save(Room.create("방B", "SAME1234"));
            roomRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 방_저장_시_생성일_수정일_자동_설정() {
        Room saved = roomRepository.save(Room.create("방", "ABCD1234"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
