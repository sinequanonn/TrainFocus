package trainfocus.backend.common.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import trainfocus.backend.common.exception.BusinessException;
import trainfocus.backend.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        fe.getRejectedValue(),
                        fe.getDefaultMessage()))
                .toList();
        ErrorResponse response = new ErrorResponse(
                ErrorCode.COMMON_VALIDATION_FAILED.getCode(),
                ErrorCode.COMMON_VALIDATION_FAILED.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(ErrorCode.COMMON_VALIDATION_FAILED.getStatus()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                ErrorCode.COMMON_INVALID_PARAMETER.getCode(),
                ErrorCode.COMMON_INVALID_PARAMETER.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.COMMON_INVALID_PARAMETER.getStatus()).body(response);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequestParameter(
            Exception e, HttpServletRequest request) {
        log.warn("잘못된 요청 파라미터: {}", e.getMessage());
        ErrorResponse response = new ErrorResponse(
                ErrorCode.COMMON_INVALID_PARAMETER.getCode(),
                ErrorCode.COMMON_INVALID_PARAMETER.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.COMMON_INVALID_PARAMETER.getStatus()).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                ErrorCode.COMMON_METHOD_NOT_ALLOWED.getCode(),
                ErrorCode.COMMON_METHOD_NOT_ALLOWED.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.COMMON_METHOD_NOT_ALLOWED.getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {
        log.error("예상치 못한 오류 발생", e);
        ErrorResponse response = new ErrorResponse(
                ErrorCode.COMMON_INTERNAL_ERROR.getCode(),
                ErrorCode.COMMON_INTERNAL_ERROR.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.COMMON_INTERNAL_ERROR.getStatus()).body(response);
    }
}
