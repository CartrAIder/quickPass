package com.mart.quickpass.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        List<String> details
) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(null, message, List.of());
    }

    public static ErrorResponse of(String message, List<String> details) {
        return new ErrorResponse(null, message, details);
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }
}
