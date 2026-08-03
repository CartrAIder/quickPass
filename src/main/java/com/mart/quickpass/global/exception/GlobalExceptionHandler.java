package com.mart.quickpass.global.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 모든 업무 예외는 생성 시점에 ErrorCode를 받는다.
     * 따라서 새 업무 예외가 이 기반 클래스를 상속하면 code와 HTTP 상태를 빼먹을 수 없다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ErrorResponse.of(errorCode.name(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR.name(),
                        "입력값이 올바르지 않습니다.",
                        details
                ));
    }

    /** JSON 문법 오류나 타입 변환 불가 요청도 Boot 기본 형식 대신 공통 계약으로 반환한다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.MALFORMED_REQUEST.httpStatus())
                .body(ErrorResponse.of(ErrorCode.MALFORMED_REQUEST.name(), "요청 본문 형식이 올바르지 않습니다."));
    }

    /** 쿼리 파라미터와 경로 변수의 검증/타입 오류도 요청 오류로 통일한다. */
    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequestParameter(Exception e) {
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), "요청 파라미터가 올바르지 않습니다."));
    }

    /** 내부 상세 정보는 로그에만 남기고, 프론트에는 안전하고 안정적인 코드만 노출한다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("처리되지 않은 API 예외", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR.name(), "서버 내부 오류가 발생했습니다."));
    }
}
