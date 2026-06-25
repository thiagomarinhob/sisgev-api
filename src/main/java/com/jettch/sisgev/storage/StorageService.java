package com.jettch.sisgev.storage;

import com.jettch.sisgev.shared.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * BE-14 — Abstração de storage de objetos (MinIO / S3-compatível).
 * O binário NÃO vai para o banco; só a key/URL são persistidas (§11.3, RN-023).
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    public StoredFile upload(String key, byte[] content, String contentType) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(new ByteArrayInputStream(content), (long) content.length, -1L)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
            return new StoredFile(key, publicUrl(key));
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Falha ao armazenar arquivo: " + e.getMessage());
        }
    }

    public String publicUrl(String key) {
        String base = properties.getPublicUrl() != null ? properties.getPublicUrl() : properties.getEndpoint();
        return base.replaceAll("/+$", "") + "/" + properties.getBucket() + "/" + key;
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(properties.getBucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
        }
    }
}
