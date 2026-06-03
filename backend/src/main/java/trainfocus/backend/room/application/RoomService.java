package trainfocus.backend.room.application;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trainfocus.backend.common.exception.BusinessException;
import trainfocus.backend.common.exception.ErrorCode;
import trainfocus.backend.room.application.dto.RoomRankingResponse;
import trainfocus.backend.room.application.dto.RoomUserLiveResponse;
import trainfocus.backend.room.domain.CodeGenerator;
import trainfocus.backend.room.domain.RankPeriod;
import trainfocus.backend.room.domain.Room;
import trainfocus.backend.room.domain.RoomUser;
import trainfocus.backend.room.domain.repository.RoomRepository;
import trainfocus.backend.room.domain.repository.RoomUserRepository;
import trainfocus.backend.session.domain.FocusSession;
import trainfocus.backend.session.domain.FocusSessionStatus;
import trainfocus.backend.session.domain.repository.FocusSessionRepository;
import trainfocus.backend.session.domain.repository.RoomRankingProjection;
import trainfocus.backend.user.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private static final List<FocusSessionStatus> ENDED_STATUSES =
            List.of(FocusSessionStatus.COMPLETED, FocusSessionStatus.ABORTED);
    private static final int MAX_MEMBERS_PER_ROOM = 6;

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final CodeGenerator codeGenerator;

    @Transactional
    public RoomUser create(String name, User user) {
        String code = issueCode();
        try {
            Room room = Room.create(name, code);
            Room savedRoom = roomRepository.save(room);
            return roomUserRepository.save(RoomUser.createOwner(savedRoom, user));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ROOM_CODE_ISSUE_FAIL);
        }
    }

    public List<RoomUser> findMyRooms(User user) {
        return roomUserRepository.findAllByUserId(user.getId());
    }

    public RoomUser findRoomDetail(Long roomId, User user) {
        return roomUserRepository.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));
    }

    public List<RoomUser> findRoomMembers(Long roomId, User user) {
        verifyMembership(roomId, user.getId());
        return roomUserRepository.findAllByRoomId(roomId);
    }

    public List<RoomUserLiveResponse> findRoomLive(Long roomId, User user) {
        verifyMembership(roomId, user.getId());
        List<RoomUser> users = roomUserRepository.findAllByRoomId(roomId);
        List<Long> userIds = users.stream()
                .map(ru -> ru.getUser().getId())
                .toList();

        Map<Long, FocusSession> sessionByUserId = focusSessionRepository
                .findActiveByUserIds(
                        userIds,
                        List.of(FocusSessionStatus.RUNNING, FocusSessionStatus.PAUSED)
                )
                .stream()
                .collect(Collectors.toMap(fs -> fs.getUser().getId(), fs -> fs));
        LocalDateTime now = LocalDateTime.now();

        return users.stream()
                .map(ru -> RoomUserLiveResponse.from(
                        ru,
                        sessionByUserId.get(ru.getUser().getId()),
                        now
                ))
                .toList();
    }

    @Transactional
    public RoomUser joinByCode(String code, User user) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_CODE_INVALID));
        if (roomUserRepository.existsByRoomIdAndUserId(room.getId(), user.getId())) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_MEMBER);
        }
        if (roomUserRepository.countByRoomId(room.getId()) >= MAX_MEMBERS_PER_ROOM) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }
        try {
            return roomUserRepository.save(RoomUser.createMember(room, user));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_MEMBER);
        }
    }

    @Transactional
    public void leave(Long roomId, User user) {
        RoomUser membership = roomUserRepository.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));

        if (membership.isOwner()) {
            throw new BusinessException(ErrorCode.ROOM_OWNER_CANNOT_LEAVE);
        }
        roomUserRepository.delete(membership);
    }

    @Transactional
    public void kickMember(Long roomId, Long targetUserId, User user) {
        verifyOwnership(roomId, user.getId());
        if (targetUserId.equals(user.getId())) {
            throw new BusinessException(ErrorCode.ROOM_OWNER_CANNOT_LEAVE);
        }
        RoomUser target = roomUserRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_MEMBER_NOT_FOUND));
        roomUserRepository.delete(target);
    }

    @Transactional
    public void transferOwner(Long roomId, Long targetUserId, User user) {
        if (targetUserId.equals(user.getId())) {
            throw new BusinessException(ErrorCode.ROOM_OWNER_TRANSFER_SELF);
        }

        RoomUser owner = verifyOwnership(roomId, user.getId());
        RoomUser target = roomUserRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_MEMBER_NOT_FOUND));
        owner.demoteToMember();
        target.promoteToOwner();
    }

    @Transactional
    public RoomUser rename(Long roomId, String name, User user) {
        RoomUser owner = verifyOwnership(roomId, user.getId());
        owner.getRoom().rename(name);
        return owner;
    }

    @Transactional
    public RoomUser reIssueCode(Long roomId, User user) {
        RoomUser owner = verifyOwnership(roomId, user.getId());
        String newCode = issueCode();
        owner.getRoom().reIssueCode(newCode);

        try {
            roomRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ROOM_CODE_ISSUE_FAIL);
        }
        return owner;
    }

    @Transactional
    public void deleteRoom(Long roomId, User user) {
        verifyOwnership(roomId, user.getId());
        if (roomUserRepository.countByRoomId(roomId) > 1) {
            throw new BusinessException(ErrorCode.ROOM_NOT_EMPTY);
        }
        roomUserRepository.deleteByRoomId(roomId);
        roomUserRepository.flush();
        roomRepository.deleteById(roomId);
    }

    public RoomRankingResponse findRanking(Long roomId, User user, LocalDate date, RankPeriod period) {
        verifyMembership(roomId, user.getId());
        List<RoomUser> members = roomUserRepository.findWithUserByRoomId(roomId);

        List<Long> userIds = members.stream().map(ru -> ru.getUser().getId()).toList();
        Map<Long, Long> secondsByUser = focusSessionRepository.sumFocusByUsers(
                        userIds, ENDED_STATUSES, period.startOf(date), period.endOf(date))
                .stream()
                .collect(Collectors.toMap(
                        RoomRankingProjection::getUserId,
                        RoomRankingProjection::getRunSeconds));

        return RoomRankingResponse.of(date, period, members, secondsByUser);
    }

    private RoomUser verifyOwnership(Long roomId, Long userId) {
        RoomUser roomUser = roomUserRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));
        if (!roomUser.isOwner()) {
            throw new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER);
        }
        return roomUser;
    }

    private String issueCode() {
        for (int i = 0; i < 5; i++) {
            String code = codeGenerator.issue();
            if (!roomRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.ROOM_CODE_ISSUE_FAIL);
    }

    private void verifyMembership(Long roomId, Long userId) {
        if (!roomUserRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER);
        }
    }
}
