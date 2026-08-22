package com.mart.quickpass.product.service;

import com.mart.quickpass.global.config.MinioProperties;
import com.mart.quickpass.global.exception.InvalidProductImageException;
import com.mart.quickpass.global.exception.ProductImageUploadException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductImageServiceTest {

    private final MinioClient minioClient = mock(MinioClient.class);
    private final MinioProperties properties = new MinioProperties(
            "http://minio:9000",
            "http://localhost:9000",
            "admin",
            "password",
            "product-images"
    );
    private final ProductImageService productImageService = new ProductImageService(minioClient, properties);

    @Test
    void uploadStoresImageInProductPrefixAndReturnsPublicUrl() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "milk.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        String imageUrl = productImageService.upload(image);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();
        assertThat(args.bucket()).isEqualTo("product-images");
        assertThat(args.object()).matches("products/[0-9a-f-]{36}\\.png");
        assertThat(args.contentType()).hasToString("image/png");
        assertThat(imageUrl).isEqualTo("http://localhost:9000/product-images/" + args.object());
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile image = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> productImageService.upload(image))
                .isInstanceOf(InvalidProductImageException.class)
                .hasMessage("이미지 파일이 비어 있습니다.");
    }

    @Test
    void uploadRejectsUnsupportedContentType() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "script.svg", "image/svg+xml", new byte[]{1}
        );

        assertThatThrownBy(() -> productImageService.upload(image))
                .isInstanceOf(InvalidProductImageException.class)
                .hasMessage("지원하지 않는 이미지 형식입니다. JPEG, PNG, WebP만 업로드할 수 있습니다.");
    }

    @Test
    void uploadRejectsFileLargerThanFiveMegabytes() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "large.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> productImageService.upload(image))
                .isInstanceOf(InvalidProductImageException.class)
                .hasMessage("이미지 파일은 5MB를 초과할 수 없습니다.");
    }

    @Test
    void uploadConvertsMinioFailureToBusinessException() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "milk.webp", "image/webp",
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}
        );
        doThrow(new RuntimeException("minio unavailable"))
                .when(minioClient).putObject(any(PutObjectArgs.class));

        assertThatThrownBy(() -> productImageService.upload(image))
                .isInstanceOf(ProductImageUploadException.class)
                .hasMessage("상품 이미지 저장에 실패했습니다.")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
