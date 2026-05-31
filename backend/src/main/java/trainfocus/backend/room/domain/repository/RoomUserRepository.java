package trainfocus.backend.room.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trainfocus.backend.room.domain.RoomUser;

import java.util.List;
import java.util.Optional;

public interface RoomUserRepository extends JpaRepository<RoomUser, Long> {

    List<RoomUser> findAllByUserId(Long userId);

    List<RoomUser> findAllByRoomId(Long roomId);

    Optional<RoomUser> findByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomId(Long roomId);

    void deleteByRoomId(Long roomId);
}
