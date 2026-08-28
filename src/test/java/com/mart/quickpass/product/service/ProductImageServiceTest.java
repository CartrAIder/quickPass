package com.mart.quickpass.product.service;

import com.mart.quickpass.global.exception.InvalidProductImageException;
import com.mart.quickpass.global.storage.minio.MinioObjectStorage;
import com.mart.quickpass.product.dto.ProductImageUploadResponse;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductImageServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final MinioObjectStorage objectStorage = mock(MinioObjectStorage.class);
    private final ProductImageService productImageService = new ProductImageService(productRepository, objectStorage);

    @Test
    void uploadStoresImageUnderBarcodeAndSavesOnlyObjectKey() throws Exception {
        Product product = product(null);
        MockMultipartFile image = png();
        when(productRepository.findByBarcode("8800000000001")).thenReturn(Optional.of(product));
        when(objectStorage.publicUrl(any())).thenAnswer(invocation ->
                "http://localhost:9000/product-images/" + invocation.getArgument(0));

        ProductImageUploadResponse response = productImageService.upload("8800000000001", image, false);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorage).put(keyCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("image/png"), any(InputStream.class),
                org.mockito.ArgumentMatchers.eq(8L));
        assertThat(keyCaptor.getValue()).matches("products/8800000000001/[0-9a-f-]{36}\\.png");
        assertThat(product.getImageKey()).isEqualTo(keyCaptor.getValue());
        assertThat(response.imageUrl()).endsWith(keyCaptor.getValue());
        assertThat(response.uploaded()).isTrue();
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void existingImageIsSkippedUnlessReplacementWasRequested() {
        Product product = product("products/8800000000001/old.png");
        when(productRepository.findByBarcode("8800000000001")).thenReturn(Optional.of(product));
        when(objectStorage.publicUrl(product.getImageKey())).thenReturn("http://images/old.png");

        ProductImageUploadResponse response = productImageService.upload("8800000000001", png(), false);

        assertThat(response.uploaded()).isFalse();
        assertThat(response.imageUrl()).isEqualTo("http://images/old.png");
        verify(objectStorage, never()).put(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void replacementUploadsAndThenDeletesPreviousImage() {
        Product product = product("products/8800000000001/old.png");
        when(productRepository.findByBarcode("8800000000001")).thenReturn(Optional.of(product));

        productImageService.upload("8800000000001", png(), true);

        verify(productRepository).saveAndFlush(product);
        verify(objectStorage).delete("products/8800000000001/old.png");
        assertThat(product.getImageKey()).isNotEqualTo("products/8800000000001/old.png");
    }

    @Test
    void uploadRejectsEmptyFile() {
        when(productRepository.findByBarcode("8800000000001")).thenReturn(Optional.of(product(null)));
        MockMultipartFile image = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> productImageService.upload("8800000000001", image, false))
                .isInstanceOf(InvalidProductImageException.class)
                .hasMessage("이미지 파일이 비어 있습니다.");
    }

    @Test
    void uploadRejectsUnsupportedContentType() {
        when(productRepository.findByBarcode("8800000000001")).thenReturn(Optional.of(product(null)));
        MockMultipartFile image = new MockMultipartFile("image", "script.svg", "image/svg+xml", new byte[]{1});

        assertThatThrownBy(() -> productImageService.upload("8800000000001", image, false))
                .isInstanceOf(InvalidProductImageException.class)
                .hasMessage("지원하지 않는 이미지 형식입니다. JPEG, PNG, WebP만 업로드할 수 있습니다.");
    }

    private Product product(String imageKey) {
        return Product.builder()
                .barcode("8800000000001")
                .name("서울우유")
                .price(3_000)
                .category(ProductCategory.DAIRY)
                .status(ProductStatus.ON_SALE)
                .imageKey(imageKey)
                .build();
    }

    private MockMultipartFile png() {
        return new MockMultipartFile(
                "image", "milk.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );
    }
}
