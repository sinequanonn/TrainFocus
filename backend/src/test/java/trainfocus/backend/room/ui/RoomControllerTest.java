package trainfocus.backend.room.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trainfocus.backend.auth.firebase.FirebaseAuthClient;
import trainfocus.backend.auth.firebase.FirebaseUserInfo;
import trainfocus.backend.common.exception.BusinessException;
import trainfocus.backend.common.exception.ErrorCode;
import trainfocus.backend.room.application.RoomService;
import trainfocus.backend.room.application.dto.RoomCreateRequest;
import trainfocus.backend.room.application.dto.RoomJoinRequest;
import trainfocus.backend.room.application.dto.RoomOwnerTransferRequest;
import trainfocus.backend.room.application.dto.RoomRenameRequest;
import trainfocus.backend.room.application.dto.RoomUserLiveResponse;
import trainfocus.backend.room.domain.Room;
import trainfocus.backend.room.domain.RoomFixture;
import trainfocus.backend.room.domain.RoomUserFixture;
import trainfocus.backend.room.domain.RoomUserRole;
import trainfocus.backend.user.application.UserService;
import trainfocus.backend.user.domain.User;
import trainfocus.backend.user.domain.UserFixture;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RoomService roomService;

    @MockitoBean
    FirebaseAuthClient firebaseAuthClient;

    @MockitoBean
    UserService userService;

    private static final String BEARER = "Bearer valid-token";

    private User loginUser;

    @BeforeEach
    void setUp() {
        loginUser = UserFixture.withId(1L);
        given(firebaseAuthClient.verifyToken("valid-token"))
                .willReturn(new FirebaseUserInfo("uid-1", "1@test.com", "테스터"));
        given(userService.findByFirebaseUid("uid-1")).willReturn(loginUser);
    }

    // ===================== POST /api/rooms =====================

    @Test
    void 방_생성_201() throws Exception {
        Room room = RoomFixture.of(1L, "스터디방", "ABCD1234");
        given(roomService.create(eq("스터디방"), any(User.class)))
                .willReturn(RoomUserFixture.owner(10L, room, loginUser));

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomCreateRequest("스터디방"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("스터디방"))
                .andExpect(jsonPath("$.data.code").value("ABCD1234"))
                .andExpect(jsonPath("$.data.role").value("OWNER"));
    }

    @Test
    void 방_생성_시_이름_누락_400() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"));
    }

    // ===================== GET =====================

    @Test
    void 내_방_목록_200() throws Exception {
        Room a = RoomFixture.of(1L, "방A", "CODEAAAA");
        Room b = RoomFixture.of(2L, "방B", "CODEBBBB");
        given(roomService.findMyRooms(any()))
                .willReturn(List.of(
                        RoomUserFixture.owner(10L, a, loginUser),
                        RoomUserFixture.member(11L, b, loginUser)
                ));

        mockMvc.perform(get("/api/rooms/me")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data[1].role").value("MEMBER"));
    }

    @Test
    void 방_상세_200() throws Exception {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        given(roomService.findRoomDetail(eq(1L), any()))
                .willReturn(RoomUserFixture.owner(10L, room, loginUser));

        mockMvc.perform(get("/api/rooms/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.code").value("ABCD1234"));
    }

    @Test
    void 방_상세_멤버_아니면_403() throws Exception {
        given(roomService.findRoomDetail(eq(1L), any()))
                .willThrow(new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_MEMBER));

        mockMvc.perform(get("/api/rooms/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ROOM_FORBIDDEN_NOT_MEMBER"));
    }

    @Test
    void 방_멤버_목록_200() throws Exception {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        User other = UserFixture.withId(2L);
        given(roomService.findRoomMembers(eq(1L), any()))
                .willReturn(List.of(
                        RoomUserFixture.owner(10L, room, loginUser),
                        RoomUserFixture.member(11L, room, other)
                ));

        mockMvc.perform(get("/api/rooms/1/users")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void 방_라이브_조회_200() throws Exception {
        given(roomService.findRoomLive(eq(1L), any()))
                .willReturn(List.of(
                        new RoomUserLiveResponse(1L, "테스터", RoomUserRole.OWNER, null)
                ));

        mockMvc.perform(get("/api/rooms/1/live")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(1L))
                .andExpect(jsonPath("$.data[0].session").doesNotExist());
    }

    // ===================== POST /api/rooms/join =====================

    @Test
    void 방_입장_성공_200() throws Exception {
        Room room = RoomFixture.of(1L, "방", "ABCD1234");
        given(roomService.joinByCode(eq("ABCD1234"), any()))
                .willReturn(RoomUserFixture.member(11L, room, loginUser));

        mockMvc.perform(post("/api/rooms/join")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomJoinRequest("ABCD1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.role").value("MEMBER"));
    }

    @Test
    void 방_입장_시_코드_형식_위반_400() throws Exception {
        mockMvc.perform(post("/api/rooms/join")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomJoinRequest("badcode"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void 방_입장_시_코드_무효이면_404() throws Exception {
        given(roomService.joinByCode(any(), any()))
                .willThrow(new BusinessException(ErrorCode.ROOM_CODE_INVALID));

        mockMvc.perform(post("/api/rooms/join")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomJoinRequest("ABCD1234"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ROOM_CODE_INVALID"));
    }

    @Test
    void 방_입장_시_이미_멤버이면_409() throws Exception {
        given(roomService.joinByCode(any(), any()))
                .willThrow(new BusinessException(ErrorCode.ROOM_ALREADY_MEMBER));

        mockMvc.perform(post("/api/rooms/join")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomJoinRequest("ABCD1234"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROOM_ALREADY_MEMBER"));
    }

    // ===================== DELETE /users/me =====================

    @Test
    void 방_나가기_200() throws Exception {
        mockMvc.perform(delete("/api/rooms/1/users/me")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk());
    }

    @Test
    void 방_나가기_방장이면_422() throws Exception {
        willThrow(new BusinessException(ErrorCode.ROOM_OWNER_CANNOT_LEAVE))
                .given(roomService).leave(eq(1L), any());

        mockMvc.perform(delete("/api/rooms/1/users/me")
                        .header("Authorization", BEARER))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ROOM_OWNER_CANNOT_LEAVE"));
    }

    // ===================== DELETE /users/{userId} =====================

    @Test
    void 멤버_강퇴_200() throws Exception {
        mockMvc.perform(delete("/api/rooms/1/users/2")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk());
    }

    @Test
    void 멤버_강퇴_방장_아니면_403() throws Exception {
        willThrow(new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER))
                .given(roomService).kickMember(eq(1L), eq(2L), any());

        mockMvc.perform(delete("/api/rooms/1/users/2")
                        .header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ROOM_FORBIDDEN_NOT_OWNER"));
    }

    // ===================== PUT /owner =====================

    @Test
    void 방장_양도_200() throws Exception {
        mockMvc.perform(put("/api/rooms/1/owner")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomOwnerTransferRequest(2L))))
                .andExpect(status().isOk());
    }

    @Test
    void 방장_양도_본인_대상_409() throws Exception {
        willThrow(new BusinessException(ErrorCode.ROOM_OWNER_TRANSFER_SELF))
                .given(roomService).transferOwner(eq(1L), eq(1L), any());

        mockMvc.perform(put("/api/rooms/1/owner")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomOwnerTransferRequest(1L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROOM_OWNER_TRANSFER_SELF"));
    }

    // ===================== POST /code/reset =====================

    @Test
    void 코드_재발급_200() throws Exception {
        Room room = RoomFixture.of(1L, "방", "NEWCODE1");
        given(roomService.reIssueCode(eq(1L), any()))
                .willReturn(RoomUserFixture.owner(10L, room, loginUser));

        mockMvc.perform(post("/api/rooms/1/code/reset")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("NEWCODE1"));
    }

    @Test
    void 코드_재발급_방장_아니면_403() throws Exception {
        given(roomService.reIssueCode(eq(1L), any()))
                .willThrow(new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER));

        mockMvc.perform(post("/api/rooms/1/code/reset")
                        .header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ROOM_FORBIDDEN_NOT_OWNER"));
    }

    // ===================== DELETE /api/rooms/{id} =====================

    @Test
    void 방_삭제_200() throws Exception {
        mockMvc.perform(delete("/api/rooms/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk());
    }

    @Test
    void 방_삭제_방장_아니면_403() throws Exception {
        willThrow(new BusinessException(ErrorCode.ROOM_FORBIDDEN_NOT_OWNER))
                .given(roomService).deleteRoom(eq(1L), any());

        mockMvc.perform(delete("/api/rooms/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ROOM_FORBIDDEN_NOT_OWNER"));
    }

    // ===================== PATCH /api/rooms/{id} (rename) =====================

    @Test
    void 방_이름_수정_200() throws Exception {
        Room room = RoomFixture.of(1L, "새이름", "ABCD1234");
        given(roomService.rename(eq(1L), eq("새이름"), any()))
                .willReturn(RoomUserFixture.owner(10L, room, loginUser));

        mockMvc.perform(patch("/api/rooms/1")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomRenameRequest("새이름"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새이름"));
    }

    @Test
    void 방_이름_수정_시_이름_누락_400() throws Exception {
        mockMvc.perform(patch("/api/rooms/1")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"));
    }

    // ===================== auth =====================

    @Test
    void 토큰_없이_요청하면_401() throws Exception {
        mockMvc.perform(get("/api/rooms/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_MISSING"));
    }
}
