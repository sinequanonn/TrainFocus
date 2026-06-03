package trainfocus.backend.room.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trainfocus.backend.common.domain.BaseEntity;
import trainfocus.backend.user.domain.User;

@Getter
@Entity
@Table(
        name = "room_users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_users_room_user",
                columnNames = {"room_id", "user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private RoomUserRole role;

    private RoomUser(Room room, User user, RoomUserRole role) {
        this.room = room;
        this.user = user;
        this.role = role;
    }

    public static RoomUser createOwner(Room room, User user) {
        return new RoomUser(room, user, RoomUserRole.OWNER);
    }

    public static RoomUser createMember(Room room, User user) {
        return new RoomUser(room, user, RoomUserRole.MEMBER);
    }

    public boolean isOwner() {
        return role == RoomUserRole.OWNER;
    }

    public void promoteToOwner() {
        this.role = RoomUserRole.OWNER;
    }

    public void demoteToMember() {
        this.role = RoomUserRole.MEMBER;
    }
}
