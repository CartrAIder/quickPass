package com.mart.quickpass.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerProductImageTest {

    @Test
    void oversizedMultipartRequestUsesProductImageErrorContract() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleOversizedProductImage(
                new MaxUploadSizeExceededException(5L * 1024 * 1024)
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_PRODUCT_IMAGE");
        assertThat(response.getBody().message()).isEqualTo("이미지 파일은 5MB를 초과할 수 없습니다.");
    }
}
