package trainfocus.backend.room.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import trainfocus.backend.common.exception.BusinessException;
import trainfocus.backend.common.exception.ErrorCode;
import trainfocus.backend.room.application.dto.RoomRankingResponse;
import trainfocus.backend.room.application.dto.RoomUserLiveResponse;
import trainfocus.backend.room.domain.*;
import trainfocus.backend.room.domain.repository.RoomRepository;
import trainfocus.backend.room.domain.repository.RoomUserRepository;
import trainfocus.backend.session.domain.FocusSession;
import trainfocus.backend.session.domain.repository.FocusSessionRepository;
import trainfocus.backend.session.domain.repository.RoomRankingProjection;
import trainfocus.backend.station.domain.Station;
import trainfocus.backend.station.domain.StationFixture;
import trainfocus.backend.user.domain.User;
import trainfocus.backend.user.domain.UserFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    RoomRepository roomRepository;
    @Mock
    RoomUserRepository roomUserRepository;
    @Mock
    FocusSessionRepository focusSessionRepository;
    @Mock
    CodeGenerator codeGenerator;

    @InjectMocks
    RoomService roomService;

    private User owner;
    private User member;
    private User outsider;

    @BeforeEach
    void setUp() {
        owner = UserFixture.withId(1L);
        member = UserFixture.withId(2L);
        outsider = UserFixture.withId(99L);
    }

    // ===================== create =====================

    @Test
    void create_성공() {
        given(codeGenerator.issue()).willReturn("ABCD1234");
        given(roomRepository.existsByCode("ABCD1234")).willReturn(false);
        given(roomRepository.save(any(Room.class))).willAnswer(inv -> inv.getArgument(0));
        given(roomUserRepository.save(any(RoomUser.class))).willAnswer(inv -> inv.getArgument(0));

        RoomUser result = roomService.create("방", owner);

        assertThat(result.isOwner()).isTrue();
        assertThat(result.getRoom().getName()).isEqualTo("방");
        assertThat(result.getRoom().getCode()).isEqualTo("ABCD1234");
    }

    @Test
    void create_코드_충돌_retry_후_성공() {
        given(codeGenerator.issue())
                .willReturn("DUP00001")
                .willReturn("OK000001");
        given(roomRepository.existsByCode("DUP00001")).willReturn(true);
        given(roomRepository.existsByCode("OK000001")).willReturn(false);
        given(roomRepository.save(any(Room.class))).willAnswer(inv -> inv.getArgument(0));
        given(roomUserRepository.save(any(RoomUser.class))).willAnswer(inv -> inv.getArgument(0));

        RoomUser result = roomService.create("방", owner);

        assertThat(result.getRoom().getCode()).isEqualTo("OK000001");
    }

    @Test
    void create_5회_모두_코드_충돌이면_ROOM_CODE_ISSUE_FAIL() {
        given(codeGenerator.issue()).willReturn("DUP00001");
        given(roomRepository.existsByCode("DUP00001")).willReturn(true);

        assertThatThrownBy(() -> roomService.create("방", owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_CODE_ISSUE_FAIL));
    }

    @Test
    void create_저장_시_DataIntegrityViolation이면_ROOM_CODE_ISSUE_FAIL() {
        given(codeGenerator.issue()).willReturn("ABCD1234");
        given(roomRepository.existsByCode("ABCD1234")).willReturn(false);
        given(roomRepository.save(any(Room.class))).willThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> roomService.create("방", owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_CODE_ISSUE_FAIL));
    }

    // ===================== findMyRooms =====================

    @Test
    void findMyRooms_성공() {
        Room a = RoomFixture.of(1L, "방A", "CODEAAAA");
        Room b = RoomFixture.of(2L, "방B", "CODEBBBB");
        given(roomUserRepository.findAllByUserId(owner.getId()))
                .willReturn(List.of(
                        RoomUserFixture.owner(10L, a, owner),
                        RoomUserFixture.member(11L, b, owner)
                ));

        List<RoomUser> result = roomService.findMyRooms(owner);

        assertThat(result).hasSize(2);
    }

    // ===================== findRoomDetail =====================

    @Test
    void findRoomDetail_성공() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser membership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(membership));

        RoomUser result = roomService.findRoomDetail(1L, owner);

        assertThat(result).isEqualTo(membership);
    }

    @Test
    void findRoomDetail_멤버_아니면_ROOM_FORBIDDEN_NOT_MEMBER() {
        given(roomUserRepository.findByRoomIdAndUserId(1L, outsider.getId()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.findRoomDetail(1L, outsider))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));
    }

    // ===================== findRoomMembers =====================

    @Test
    void findRoomMembers_성공() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        given(roomUserRepository.existsByRoomIdAndUserId(1L, owner.getId())).willReturn(true);
        given(roomUserRepository.findAllByRoomId(1L))
                .willReturn(List.of(
                        RoomUserFixture.owner(10L, room, owner),
                        RoomUserFixture.member(11L, room, member)
                ));

        List<RoomUser> result = roomService.findRoomMembers(1L, owner);

        assertThat(result).hasSize(2);
    }

    @Test
    void findRoomMembers_멤버_아니면_ROOM_FORBIDDEN_NOT_MEMBER() {
        given(roomUserRepository.existsByRoomIdAndUserId(1L, outsider.getId())).willReturn(false);

        assertThatThrownBy(() -> roomService.findRoomMembers(1L, outsider))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));
    }

    // ===================== findRoomLive =====================

    @Test
    void findRoomLive_활성_세션_포함된_응답() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        Station dep = StationFixture.of(10L, "강남");
        Station arr = StationFixture.of(20L, "서울역");
        FocusSession session = FocusSession.createNewFocusSession(member, dep, arr, 30, 0, LocalDateTime.now());

        given(roomUserRepository.existsByRoomIdAndUserId(1L, owner.getId())).willReturn(true);
        given(roomUserRepository.findAllByRoomId(1L))
                .willReturn(List.of(
                        RoomUserFixture.owner(10L, room, owner),
                        RoomUserFixture.member(11L, room, member)
                ));
        given(focusSessionRepository.findActiveByUserIds(any(), any()))
                .willReturn(List.of(session));

        List<RoomUserLiveResponse> result = roomService.findRoomLive(1L, owner);

        assertThat(result).hasSize(2);
        RoomUserLiveResponse memberLive = result.stream()
                .filter(r -> r.userId().equals(member.getId()))
                .findFirst().orElseThrow();
        assertThat(memberLive.session()).isNotNull();
        RoomUserLiveResponse ownerLive = result.stream()
                .filter(r -> r.userId().equals(owner.getId()))
                .findFirst().orElseThrow();
        assertThat(ownerLive.session()).isNull();
    }

    @Test
    void findRoomLive_멤버_아니면_ROOM_FORBIDDEN_NOT_MEMBER() {
        given(roomUserRepository.existsByRoomIdAndUserId(1L, outsider.getId())).willReturn(false);

        assertThatThrownBy(() -> roomService.findRoomLive(1L, outsider))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));
    }

    // ===================== joinByCode =====================

    @Test
    void joinByCode_성공() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        given(roomRepository.findByCode("ABCD1234")).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomIdAndUserId(1L, member.getId())).willReturn(false);
        given(roomUserRepository.countByRoomId(1L)).willReturn(2L);
        given(roomUserRepository.save(any(RoomUser.class))).willAnswer(inv -> inv.getArgument(0));

        RoomUser result = roomService.joinByCode("ABCD1234", member);

        assertThat(result.isOwner()).isFalse();
        assertThat(result.getRoom()).isEqualTo(room);
    }

    @Test
    void joinByCode_없는_코드면_ROOM_CODE_INVALID() {
        given(roomRepository.findByCode("BADCODE0")).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.joinByCode("BADCODE0", member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_CODE_INVALID));
    }

    @Test
    void joinByCode_이미_멤버면_ROOM_ALREADY_MEMBER() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        given(roomRepository.findByCode("ABCD1234")).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomIdAndUserId(1L, member.getId())).willReturn(true);

        assertThatThrownBy(() -> roomService.joinByCode("ABCD1234", member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_ALREADY_MEMBER));
    }

    @Test
    void joinByCode_인원_가득_차면_ROOM_FULL() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        given(roomRepository.findByCode("ABCD1234")).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomIdAndUserId(1L, member.getId())).willReturn(false);
        given(roomUserRepository.countByRoomId(1L)).willReturn(6L);

        assertThatThrownBy(() -> roomService.joinByCode("ABCD1234", member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FULL));
    }

    @Test
    void joinByCode_저장_DataIntegrityViolation이면_ROOM_ALREADY_MEMBER() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        given(roomRepository.findByCode("ABCD1234")).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomIdAndUserId(1L, member.getId())).willReturn(false);
        given(roomUserRepository.countByRoomId(1L)).willReturn(2L);
        given(roomUserRepository.save(any(RoomUser.class)))
                .willThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> roomService.joinByCode("ABCD1234", member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_ALREADY_MEMBER));
    }

    // ===================== leave =====================

    @Test
    void leave_성공_멤버() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser membership = RoomUserFixture.member(11L, room, member);
        given(roomUserRepository.findByRoomIdAndUserId(1L, member.getId()))
                .willReturn(Optional.of(membership));

        roomService.leave(1L, member);

        then(roomUserRepository).should().delete(membership);
    }

    @Test
    void leave_방장은_ROOM_OWNER_CANNOT_LEAVE() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> roomService.leave(1L, owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_OWNER_CANNOT_LEAVE));
    }

    @Test
    void leave_멤버_아니면_ROOM_FORBIDDEN_NOT_MEMBER() {
        given(roomUserRepository.findByRoomIdAndUserId(1L, outsider.getId()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.leave(1L, outsider))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));
    }

    // ===================== kickMember =====================

    @Test
    void kickMember_성공() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        RoomUser target = RoomUserFixture.member(11L, room, member);

        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));
        given(roomUserRepository.findByRoomIdAndUserId(1L, member.getId()))
                .willReturn(Optional.of(target));

        roomService.kickMember(1L, member.getId(), owner);

        then(roomUserRepository).should().delete(target);
    }

    @Test
    void kickMember_방장_본인_강퇴_시도() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> roomService.kickMember(1L, owner.getId(), owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_OWNER_CANNOT_LEAVE));
    }

    @Test
    void kickMember_방장_아니면_ROOM_FORBIDDEN_NOT_OWNER() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser memberMembership = RoomUserFixture.member(11L, room, member);
        given(roomUserRepository.findByRoomIdAndUserId(1L, member.getId()))
                .willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> roomService.kickMember(1L, outsider.getId(), member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER));
    }

    @Test
    void kickMember_대상이_없으면_ROOM_MEMBER_NOT_FOUND() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));
        given(roomUserRepository.findByRoomIdAndUserId(1L, outsider.getId()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.kickMember(1L, outsider.getId(), owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND));
    }

    // ===================== transferOwner =====================

    @Test
    void transferOwner_성공_역할_swap() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        RoomUser memberMembership = RoomUserFixture.member(11L, room, member);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));
        given(roomUserRepository.findByRoomIdAndUserId(1L, member.getId()))
                .willReturn(Optional.of(memberMembership));

        roomService.transferOwner(1L, member.getId(), owner);

        assertThat(ownerMembership.isOwner()).isFalse();
        assertThat(memberMembership.isOwner()).isTrue();
    }

    @Test
    void transferOwner_본인_대상이면_ROOM_OWNER_TRANSFER_SELF() {
        assertThatThrownBy(() -> roomService.transferOwner(1L, owner.getId(), owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_OWNER_TRANSFER_SELF));
    }

    @Test
    void transferOwner_방장_아니면_ROOM_FORBIDDEN_NOT_OWNER() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser memberMembership = RoomUserFixture.member(11L, room, member);
        given(roomUserRepository.findByRoomIdAndUserId(1L, member.getId()))
                .willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> roomService.transferOwner(1L, outsider.getId(), member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER));
    }

    @Test
    void transferOwner_대상이_없으면_ROOM_MEMBER_NOT_FOUND() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));
        given(roomUserRepository.findByRoomIdAndUserId(1L, outsider.getId()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.transferOwner(1L, outsider.getId(), owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND));
    }

    // ===================== reIssueCode =====================

    @Test
    void reIssueCode_성공() {
        Room room = RoomFixture.of(1L, "방", "OLDCODE1");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));
        given(codeGenerator.issue()).willReturn("NEWCODE1");
        given(roomRepository.existsByCode("NEWCODE1")).willReturn(false);

        RoomUser result = roomService.reIssueCode(1L, owner);

        assertThat(result.getRoom().getCode()).isEqualTo("NEWCODE1");
    }

    @Test
    void reIssueCode_방장_아니면_ROOM_FORBIDDEN_NOT_OWNER() {
        Room room = RoomFixture.of(1L, "방", "OLDCODE1");
        RoomUser memberMembership = RoomUserFixture.member(11L, room, member);
        given(roomUserRepository.findByRoomIdAndUserId(1L, member.getId()))
                .willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> roomService.reIssueCode(1L, member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER));
    }

    @Test
    void reIssueCode_flush_시_DataIntegrityViolation이면_ROOM_CODE_ISSUE_FAIL() {
        Room room = RoomFixture.of(1L, "방", "OLDCODE1");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));
        given(codeGenerator.issue()).willReturn("NEWCODE1");
        given(roomRepository.existsByCode("NEWCODE1")).willReturn(false);
        willThrow(DataIntegrityViolationException.class).given(roomRepository).flush();

        assertThatThrownBy(() -> roomService.reIssueCode(1L, owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_CODE_ISSUE_FAIL));
    }

    // ===================== rename =====================

    @Test
    void rename_성공() {
        Room room = RoomFixture.of(1L, "기존이름", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));

        RoomUser result = roomService.rename(1L, "새이름", owner);

        assertThat(result.getRoom().getName()).isEqualTo("새이름");
    }

    @Test
    void rename_방장_아니면_ROOM_FORBIDDEN_NOT_OWNER() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser memberMembership = RoomUserFixture.member(11L, room, member);
        given(roomUserRepository.findByRoomIdAndUserId(1L, member.getId()))
                .willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> roomService.rename(1L, "새이름", member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER));
    }

    // ===================== deleteRoom =====================

    @Test
    void deleteRoom_성공() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));
        given(roomUserRepository.countByRoomId(1L)).willReturn(1L);

        roomService.deleteRoom(1L, owner);

        then(roomUserRepository).should().deleteByRoomId(1L);
        then(roomRepository).should().deleteById(1L);
    }

    @Test
    void deleteRoom_방장_아니면_ROOM_FORBIDDEN_NOT_OWNER() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser memberMembership = RoomUserFixture.member(11L, room, member);
        given(roomUserRepository.findByRoomIdAndUserId(1L, member.getId()))
                .willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> roomService.deleteRoom(1L, member))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER));
    }

    @Test
    void deleteRoom_다른_멤버_있으면_ROOM_NOT_EMPTY() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        RoomUser ownerMembership = RoomUserFixture.owner(10L, room, owner);
        given(roomUserRepository.findByRoomIdAndUserId(1L, owner.getId()))
                .willReturn(Optional.of(ownerMembership));
        given(roomUserRepository.countByRoomId(1L)).willReturn(2L);

        assertThatThrownBy(() -> roomService.deleteRoom(1L, owner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_NOT_EMPTY));
    }

    // ===================== findRanking =====================

    private RoomRankingProjection rankRow(Long userId, long seconds) {
        return new RoomRankingProjection() {
            @Override public Long getUserId() { return userId; }
            @Override public long getRunSeconds() { return seconds; }
        };
    }

    @Test
    void findRanking_집계로_순위_응답() {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        given(roomUserRepository.existsByRoomIdAndUserId(1L, owner.getId())).willReturn(true);
        given(roomUserRepository.findWithUserByRoomId(1L)).willReturn(List.of(
                RoomUserFixture.owner(10L, room, owner),
                RoomUserFixture.member(11L, room, member)
        ));
        given(focusSessionRepository.sumFocusByUsers(any(), any(), any(), any()))
                .willReturn(List.of(rankRow(member.getId(), 300L), rankRow(owner.getId(), 100L)));

        RoomRankingResponse res = roomService.findRanking(
                1L, owner, LocalDate.of(2026, 6, 3), RankPeriod.WEEK);

        assertThat(res.entries()).hasSize(2);
        assertThat(res.entries().get(0).userId()).isEqualTo(member.getId());
        assertThat(res.entries().get(0).rank()).isEqualTo(1);
        assertThat(res.entries().get(1).userId()).isEqualTo(owner.getId());
    }

    @Test
    void findRanking_멤버_아니면_ROOM_FORBIDDEN_NOT_MEMBER() {
        given(roomUserRepository.existsByRoomIdAndUserId(1L, outsider.getId())).willReturn(false);

        assertThatThrownBy(() -> roomService.findRanking(
                1L, outsider, LocalDate.of(2026, 6, 3), RankPeriod.WEEK))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));
    }
}
