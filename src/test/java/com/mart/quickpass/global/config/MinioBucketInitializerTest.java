package com.mart.quickpass.global.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioBucketInitializerTest {

    @Test
    void createsConfiguredBucketWhenItDoesNotExist() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        MinioProperties properties = properties();
        when(minioClient.bucketExists(org.mockito.ArgumentMatchers.any(BucketExistsArgs.class)))
                .thenReturn(false);

        new MinioBucketInitializer(minioClient, properties).run();

        ArgumentCaptor<MakeBucketArgs> captor = ArgumentCaptor.forClass(MakeBucketArgs.class);
        verify(minioClient).makeBucket(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("product-images");
        assertPublicReadPolicyApplied(minioClient);
    }

    @Test
    void doesNotCreateConfiguredBucketWhenItAlreadyExists() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        MinioProperties properties = properties();
        when(minioClient.bucketExists(org.mockito.ArgumentMatchers.any(BucketExistsArgs.class)))
                .thenReturn(true);

        new MinioBucketInitializer(minioClient, properties).run();

        verify(minioClient, never()).makeBucket(org.mockito.ArgumentMatchers.any(MakeBucketArgs.class));
        assertPublicReadPolicyApplied(minioClient);
    }

    private void assertPublicReadPolicyApplied(MinioClient minioClient) throws Exception {
        ArgumentCaptor<SetBucketPolicyArgs> captor = ArgumentCaptor.forClass(SetBucketPolicyArgs.class);
        verify(minioClient).setBucketPolicy(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("product-images");
        assertThat(captor.getValue().config())
                .contains("s3:GetObject")
                .contains("arn:aws:s3:::product-images/*");
    }

    private MinioProperties properties() {
        return new MinioProperties(
                "http://localhost:9000",
                "http://localhost:9000",
                "admin",
                "password",
                "product-images"
        );
    }
}
