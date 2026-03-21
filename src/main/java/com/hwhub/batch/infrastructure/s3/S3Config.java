package com.hwhub.batch.infrastructure.s3;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(AwsS3Properties.class)
public class S3Config {

  private final AwsS3Properties awsS3Properties;

  public S3Config(AwsS3Properties awsS3Properties) {
    this.awsS3Properties = awsS3Properties;
  }

  @Bean
  S3Client s3Client() {
    String region =
        awsS3Properties.getRegion() != null ? awsS3Properties.getRegion() : "ap-northeast-1";

    S3Configuration serviceConfig =
        S3Configuration.builder()
            .pathStyleAccessEnabled(awsS3Properties.isPathStyleAccessEnabled())
            .build();

    S3ClientBuilder builder =
        S3Client.builder().region(Region.of(region)).serviceConfiguration(serviceConfig);

    String endpoint = awsS3Properties.getEndpoint();
    if (endpoint != null && !endpoint.isBlank()) {
      // LocalStack 用
      AwsBasicCredentials credentials =
          AwsBasicCredentials.create(
              awsS3Properties.getAccessKey(), awsS3Properties.getSecretKey());
      builder =
          builder
              .endpointOverride(URI.create(endpoint))
              .credentialsProvider(StaticCredentialsProvider.create(credentials));
    } else {
      // ECS / 本番: Task Role などのデフォルトチェーン
      builder = builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
    }

    return builder.build();
  }
}
