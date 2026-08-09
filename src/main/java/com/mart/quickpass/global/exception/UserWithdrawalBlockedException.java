package com.mart.quickpass.global.exception;

public class UserWithdrawalBlockedException extends BusinessException {
    public UserWithdrawalBlockedException() {
        super(ErrorCode.USER_WITHDRAWAL_BLOCKED, "처리 중인 주문, 결제 또는 환불이 있어 탈퇴할 수 없습니다.");
    }
}
