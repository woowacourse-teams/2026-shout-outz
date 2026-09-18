package com.shoutoutz.api.adminbootstrap.domain;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AdminBootstrapCode {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String CODE_CONTEXT = "shoutoutz-admin-bootstrap-v1";

    private AdminBootstrapCode() {
    }

    public static boolean matches(String requestedCode, String bucket) {
        if (requestedCode == null || requestedCode.isBlank()
                || bucket == null || bucket.isBlank()) {
            return false;
        }

        byte[] expectedCode = derive(bucket);
        byte[] requestedCodeBytes = requestedCode.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedCode, requestedCodeBytes);
    }

    private static byte[] derive(String bucket) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    bucket.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return HexFormat.of()
                    .formatHex(mac.doFinal(CODE_CONTEXT.getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("관리자 권한 부여 코드를 생성할 수 없습니다.", exception);
        }
    }
}
