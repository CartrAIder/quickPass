package com.mart.quickpass.product.service;

import com.mart.quickpass.global.exception.InvalidProductImageException;
import com.mart.quickpass.global.exception.ProductNotFoundException;
import com.mart.quickpass.global.storage.minio.MinioObjectStorage;
import com.mart.quickpass.product.dto.ProductImageUploadResponse;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final ProductRepository productRepository;
    private final MinioObjectStorage objectStorage;

    @Transactional
    public ProductImageUploadResponse upload(String barcode, MultipartFile image, boolean replace) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ProductNotFoundException(barcode));

        if (product.getImageKey() != null && !replace) {
            return new ProductImageUploadResponse(objectStorage.publicUrl(product.getImageKey()), false);
        }

        validate(image);

        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(image.getContentType());
        String newImageKey = "products/" + product.getBarcode() + "/" + UUID.randomUUID() + "." + extension;
        String previousImageKey = product.getImageKey();

        try {
            objectStorage.put(newImageKey, image.getContentType(), image.getInputStream(), image.getSize());
        } catch (IOException e) {
            throw new InvalidProductImageException("이미지 파일을 읽을 수 없습니다.");
        }

        product.changeImageKey(newImageKey);
        try {
            productRepository.saveAndFlush(product);
        } catch (RuntimeException e) {
            deleteOrphanQuietly(newImageKey);
            throw e;
        }
        deletePreviousImageAfterCommit(previousImageKey);

        return new ProductImageUploadResponse(objectStorage.publicUrl(newImageKey), true);
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidProductImageException("이미지 파일이 비어 있습니다.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidProductImageException("이미지 파일은 5MB를 초과할 수 없습니다.");
        }

        String contentType = image.getContentType();
        if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType) || !hasValidSignature(image, contentType)) {
            throw new InvalidProductImageException(
                    "지원하지 않는 이미지 형식입니다. JPEG, PNG, WebP만 업로드할 수 있습니다.");
        }
    }

    private boolean hasValidSignature(MultipartFile image, String contentType) {
        try {
            byte[] bytes = image.getBytes();
            return switch (contentType) {
                case "image/jpeg" -> bytes.length >= 3
                        && unsigned(bytes[0]) == 0xFF
                        && unsigned(bytes[1]) == 0xD8
                        && unsigned(bytes[2]) == 0xFF;
                case "image/png" -> bytes.length >= 8
                        && unsigned(bytes[0]) == 0x89
                        && bytes[1] == 0x50
                        && bytes[2] == 0x4E
                        && bytes[3] == 0x47
                        && bytes[4] == 0x0D
                        && bytes[5] == 0x0A
                        && bytes[6] == 0x1A
                        && bytes[7] == 0x0A;
                case "image/webp" -> bytes.length >= 12
                        && bytes[0] == 'R'
                        && bytes[1] == 'I'
                        && bytes[2] == 'F'
                        && bytes[3] == 'F'
                        && bytes[8] == 'W'
                        && bytes[9] == 'E'
                        && bytes[10] == 'B'
                        && bytes[11] == 'P';
                default -> false;
            };
        } catch (IOException e) {
            throw new InvalidProductImageException("이미지 파일을 읽을 수 없습니다.");
        }
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    private void deletePreviousImageAfterCommit(String previousImageKey) {
        if (previousImageKey == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            objectStorage.delete(previousImageKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    objectStorage.delete(previousImageKey);
                } catch (RuntimeException e) {
                    log.warn("교체된 상품 이미지 삭제에 실패했습니다. objectKey={}", previousImageKey, e);
                }
            }
        });
    }

    private void deleteOrphanQuietly(String imageKey) {
        try {
            objectStorage.delete(imageKey);
        } catch (RuntimeException cleanupException) {
            log.warn("DB 반영 실패 후 미사용 상품 이미지 정리에 실패했습니다. objectKey={}", imageKey, cleanupException);
        }
    }
}
