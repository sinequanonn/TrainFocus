package trainfocus.backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // AUTH
    AUTH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다. 다시 로그인해주세요."),
    AUTH_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일 정보가 필요합니다. Google 계정 권한을 확인해주세요."),
    AUTH_FIREBASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "인증 서비스에 일시적 문제가 발생했습니다."),
    AUTH_FORBIDDEN_ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다."),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "중복된 닉네임이 존재합니다."),
    USER_DEPARTURE_STATION_NOT_SET(HttpStatus.UNPROCESSABLE_ENTITY, "출발역이 설정되지 않았습니다."),
    USER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 사용자입니다."),

    // COMMON
    COMMON_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    COMMON_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 올바르지 않습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청 방식입니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // STATION
    STATION_NOT_FOUND(HttpStatus.NOT_FOUND, "역을 찾을 수 없습니다."),
    STATION_NAME_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 역 이름입니다."),
    STATION_COORDINATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "좌표가 한반도 범위를 벗어났습니다."),

    // ROUTE
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 노선이 등록되어 있지 않습니다."),
    ROUTE_SAME_STATION(HttpStatus.BAD_REQUEST, "출발역과 도착역이 같을 수 없습니다."),
    ROUTE_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 노선입니다."),
    ROUTE_DURATION_NOT_POSITIVE(HttpStatus.BAD_REQUEST, "노선 소요 시간은 양수여야 합니다."),

    // SESSION
    SESSION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 활성화된 집중 세션이 존재합니다."),
    SESSION_DELAY_NEGATIVE(HttpStatus.BAD_REQUEST, "지연시간이 음수가 될 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."),
    SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "타인의 세션에 접근할 수 없습니다."),
    SESSION_ALREADY_PAUSED(HttpStatus.UNPROCESSABLE_ENTITY, "이미 하차 상태입니다."),
    SESSION_ALREADY_RUNNING(HttpStatus.UNPROCESSABLE_ENTITY, "이미 진행 중입니다."),
    SESSION_ALREADY_ENDED(HttpStatus.UNPROCESSABLE_ENTITY, "이미 종료된 세션입니다."),
    SESSION_TARGET_NOT_REACHED(HttpStatus.UNPROCESSABLE_ENTITY, "목표 시간에 도달하지 않았습니다."),

    // ROOM
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."),
    ROOM_CODE_INVALID(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    ROOM_CODE_ISSUE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드 생성에 실패했습니다."),
    ROOM_ALREADY_MEMBER(HttpStatus.CONFLICT, "이미 참여 중인 방입니다."),
    ROOM_FORBIDDEN_NOT_OWNER(HttpStatus.FORBIDDEN, "해당 작업에 권한이 없습니다."),
    ROOM_FORBIDDEN_NOT_MEMBER(HttpStatus.FORBIDDEN, "방에 참여한 사용자만 접근할 수 있습니다."),
    ROOM_OWNER_CANNOT_LEAVE(HttpStatus.UNPROCESSABLE_ENTITY, "방장은 양도 후 나갈 수 있습니다."),
    ROOM_OWNER_TRANSFER_SELF(HttpStatus.CONFLICT, "본인에게 방장을 양도할 수 없습니다."),
    ROOM_FULL(HttpStatus.CONFLICT, "방 인원이 가득 찼습니다."),
    ROOM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "방 멤버를 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}
