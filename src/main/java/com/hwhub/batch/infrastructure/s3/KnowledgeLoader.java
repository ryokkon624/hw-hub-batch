package com.hwhub.batch.infrastructure.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeLoader {

  private final S3Client s3Client;

  @Value("${hwhub.knowledge.s3.bucket}")
  private String bucket;

  @Value("${hwhub.knowledge.s3.faq-key}")
  private String faqKey;

  @Value("${hwhub.knowledge.s3.howto-key}")
  private String howtoKey;

  public String load() {
    String faq = readFile(faqKey);
    String howto = readFile(howtoKey);
    return (faq + "\n\n---\n\n" + howto).strip();
  }

  private String readFile(String key) {
    try {
      return s3Client
          .getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build())
          .asUtf8String();
    } catch (NoSuchKeyException e) {
      log.warn("S3ナレッジファイルが見つかりません: s3://{}/{}", bucket, key);
      return "";
    }
  }
}
