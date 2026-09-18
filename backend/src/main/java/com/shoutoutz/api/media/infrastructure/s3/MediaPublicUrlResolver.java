package com.shoutoutz.api.media.infrastructure.s3;

import com.shoutoutz.api.media.infrastructure.config.CloudFrontProperties;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 논리 미디어 키를 CloudFront 공개 URL로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class MediaPublicUrlResolver {

    private final CloudFrontProperties cloudFrontProperties;
    private final S3MediaStorage s3MediaStorage;

    public URI resolve(String logicalKey) {
        String actualKey = s3MediaStorage.actualKey(logicalKey);
        return cloudFrontProperties.publicBaseUri().resolve(actualKey);
    }
}
