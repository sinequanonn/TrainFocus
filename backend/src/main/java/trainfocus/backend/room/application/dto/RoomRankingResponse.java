package trainfocus.backend.room.application.dto;

import trainfocus.backend.room.domain.RankPeriod;
import trainfocus.backend.room.domain.RoomUser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record RoomRankingResponse(
        LocalDate date,
        String period,
        List<Entry> entries
) {
    public record Entry(int rank, Long userId, String nickname, long runSeconds) {
    }

    public static RoomRankingResponse of(
            LocalDate date,
            RankPeriod period,
            List<RoomUser> members,
            Map<Long, Long> secondsByUser
    ) {
        List<RoomUser> sorted = members.stream()
                .sorted((a, b) -> Long.compare(
                        secondsByUser.getOrDefault(b.getUser().getId(), 0L),
                        secondsByUser.getOrDefault(a.getUser().getId(), 0L)))
                .toList();

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            var u = sorted.get(i).getUser();
            entries.add(new Entry(
                    i + 1,
                    u.getId(),
                    u.getNickname(),
                    secondsByUser.getOrDefault(u.getId(), 0L)
            ));
        }
        return new RoomRankingResponse(date, period.name().toLowerCase(), entries);
    }
}
