package com.healthvault.platform.storage;

import com.healthvault.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Real object storage over the S3 API. Active only under the {@code s3} profile; otherwise
 * {@link LocalStorageService} handles storage so local and CI builds need no credentials.
 * <p>
 * The same client talks to AWS S3 and to MinIO/LocalStack: point {@code storage.s3.endpoint} at
 * the MinIO container for local dev, or drop it for real AWS. Path-style access is on because
 * MinIO does not do virtual-host-style buckets.
 */
@Service("s3StorageService")
@Profile("s3")
public class S3StorageService implements StorageService {

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageService(
            @Value("${storage.s3.bucket}") String bucket,
            @Value("${storage.s3.region}") String region,
            @Value("${storage.s3.endpoint}") String endpoint,
            @Value("${storage.s3.access-key}") String accessKey,
            @Value("${storage.s3.secret-key}") String secretKey) {
        this.bucket = bucket;
        var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        var uri = URI.create(endpoint);
        this.client = S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(uri)
                .credentialsProvider(creds)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(uri)
                .credentialsProvider(creds)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override
    public String put(byte[] content, String contentType) {
        LocalDate today = LocalDate.now();
        String key = "%04d/%02d/%02d/%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID());
        try {
            client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(content));
        } catch (Exception e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "storage_error", "could not store file");
        }
        return key;
    }

    @Override
    public byte[] get(String storageKey) {
        try {
            ResponseBytes<?> res = client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(storageKey).build());
            return res.asByteArray();
        } catch (Exception e) {
            throw ApiException.notFound("object not found");
        }
    }

    @Override
    public String presignedGetUrl(String storageKey, long ttlSeconds) {
        var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(ttlSeconds))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(storageKey).build())
                .build());
        return presigned.url().toString();
    }
}
