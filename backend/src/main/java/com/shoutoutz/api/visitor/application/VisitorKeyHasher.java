package com.shoutoutz.api.visitor.application;

import com.shoutoutz.api.visitor.VisitorProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * 방문자 식별 쿠키 값을 저장용 해시로 바꾼다.
 * 서버 비밀키를 섞은 HMAC-SHA256 을 써서, DB 값만으로는 쿠키 값과 대조할 수 없게 한다.
 * Mac은 스레드 안전하지 않으므로, 요청마다 새로 만든다.
 */
@Component
public class VisitorKeyHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec secretKey;

    public VisitorKeyHasher(VisitorProperties properties) {
        this.secretKey = new SecretKeySpec(
                properties.hashSecret().getBytes(StandardCharsets.UTF_8),
                ALGORITHM
        );
        createMac();
    }

    public String hash(String visitorId) {
        Objects.requireNonNull(visitorId, "방문자 식별값이 없습니다.");
        byte[] digest = createMac().doFinal(visitorId.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    /**
     * 생성 시점에도 한 번 호출해, 알고리즘이나 키에 문제가 있으면 기동할 때 실패하게 한다.
     */
    private Mac createMac() {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);
            return mac;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("방문자 식별값 해시를 준비할 수 없습니다.", e);
        }
    }
}
