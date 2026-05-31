package trainfocus.backend.room.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trainfocus.backend.common.domain.BaseEntity;

@Getter
@Entity
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(unique = true, length = 8, nullable = false)
    private String code;

    private Room(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public static Room create(String name, String code) {
        return new Room(name, code);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void reIssueCode(String newCode) {
        this.code = newCode;
    }
}
