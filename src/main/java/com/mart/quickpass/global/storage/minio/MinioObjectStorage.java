package com.mart.quickpass.global.storage.minio;

import com.mart.quickpass.global.config.MinioProperties;
import com.mart.quickpass.global.exception.ProductImageUploadException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class MinioObjectStorage {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public void put(String objectKey, String contentType, InputStream inputStream, long size) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(inputStream, size, -1L)
                    .build());
        } catch (Exception e) {
            throw new ProductImageUploadException(e);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new ProductImageUploadException(e);
        }
    }

    public String publicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return stripTrailingSlash(properties.publicUrl())
                + "/" + properties.bucket()
                + "/" + objectKey;
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
