package com.mart.quickpass.global.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다: " + userId);
    }
}
