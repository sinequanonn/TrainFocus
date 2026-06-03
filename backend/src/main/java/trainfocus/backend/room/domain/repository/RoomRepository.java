package trainfocus.backend.room.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trainfocus.backend.room.domain.Room;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);
}
