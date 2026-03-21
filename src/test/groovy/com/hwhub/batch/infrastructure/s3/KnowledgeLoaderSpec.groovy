package com.hwhub.batch.infrastructure.s3

import org.springframework.test.util.ReflectionTestUtils
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import spock.lang.Specification

class KnowledgeLoaderSpec extends Specification {

    S3Client s3Client = Mock()
    KnowledgeLoader loader

    def setup() {
        loader = new KnowledgeLoader(s3Client)
        ReflectionTestUtils.setField(loader, "bucket", "test-bucket")
        ReflectionTestUtils.setField(loader, "faqKey", "faq.txt")
        ReflectionTestUtils.setField(loader, "howtoKey", "howto.txt")
    }

    private ResponseBytes<GetObjectResponse> responseBytes(String content) {
        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content.getBytes("UTF-8"))
    }

    // ==================================
    // load - FAQ + howto 結合
    // ==================================

    def "FAQとhowtoを結合して返す"() {
        when:
        def result = loader.load()

        then:
        1 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == "faq.txt" }) >> responseBytes("FAQの内容")
        1 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == "howto.txt" }) >> responseBytes("ハウツーの内容")
        result == "FAQの内容\n\n---\n\nハウツーの内容"
    }

    def "FAQとhowtoの先頭・末尾の空白はstripされる"() {
        when:
        def result = loader.load()

        then:
        1 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == "faq.txt" }) >> responseBytes("  FAQ  ")
        1 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == "howto.txt" }) >> responseBytes("  howto  ")
        result == "FAQ  \n\n---\n\n  howto"
    }

    // ==================================
    // load - NoSuchKeyException フォールバック
    // ==================================

    def "FAQファイルが存在しない場合は空文字として扱い、howtoのみ返す"() {
        when:
        def result = loader.load()

        then:
        1 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == "faq.txt" }) >> {
            throw NoSuchKeyException.builder().build()
        }
        1 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == "howto.txt" }) >> responseBytes("ハウツー")
        result == "---\n\nハウツー"
    }

    def "howtoファイルが存在しない場合は空文字として扱い、FAQのみ返す"() {
        when:
        def result = loader.load()

        then:
        1 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == "faq.txt" }) >> responseBytes("FAQ")
        1 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == "howto.txt" }) >> {
            throw NoSuchKeyException.builder().build()
        }
        result == "FAQ\n\n---"
    }

    def "FAQとhowtoの両方が存在しない場合は区切り文字のみ返す"() {
        when:
        def result = loader.load()

        then:
        2 * s3Client.getObjectAsBytes(_) >> { throw NoSuchKeyException.builder().build() }
        result == "---"
    }

    // ==================================
    // load - bucketが正しく使用されていること
    // ==================================

    def "GetObjectRequestに正しいbucketが設定される"() {
        when:
        loader.load()

        then:
        2 * s3Client.getObjectAsBytes({ GetObjectRequest r -> r.bucket() == "test-bucket" }) >> responseBytes("")
    }
}
