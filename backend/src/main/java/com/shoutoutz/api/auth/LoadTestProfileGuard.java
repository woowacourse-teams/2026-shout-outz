package com.shoutoutz.api.auth;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * [부하 테스트 전용] prod와 loadtest 프로필이 함께 켜지면 앱 시작을 실패시킨다.
 * loadtest 프로필의 로그인 API는 handle만으로 로그인되므로 prod에서 켜지면 안 된다.
 * 전역 지연 초기화가 켜져도 시작 시점에 만들어지도록 @Lazy(false)를 둔다.
 */
@Lazy(false)
@Profile("loadtest & prod")
@Component
public class LoadTestProfileGuard {

    public LoadTestProfileGuard() {
        throw new IllegalStateException("prod와 loadtest 프로필은 함께 활성화할 수 없습니다.");
    }
}
