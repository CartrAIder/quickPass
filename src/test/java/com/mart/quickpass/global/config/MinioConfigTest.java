package com.mart.quickpass.global.config;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MinioConfigTest {

    @Test
    void createsMinioClientFromProperties() {
        MinioProperties properties = new MinioProperties(
                "http://localhost:9000",
                "http://cdn.quickpass.example",
                "admin",
                "password",
                "product-images"
        );

        MinioClient minioClient = new MinioConfig().minioClient(properties);

        assertThat(minioClient).isNotNull();
    }
}
