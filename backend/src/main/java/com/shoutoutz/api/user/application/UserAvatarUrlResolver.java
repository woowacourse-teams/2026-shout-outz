package com.shoutoutz.api.user.application;

import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자 프로필 이미지의 공개 URL을 공통 규칙으로 해석한다.
 *
 * <p>직접 업로드한 이미지가 있으면 그 URL을 우선하고, 없으면 GitHub OAuth 계정의
 * 아바타 URL을 기본 이미지로 사용한다. 외부 URL은 프로필이나 미디어 컬럼에 저장하지
 * 않고 조회 시점에만 fallback으로 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class UserAvatarUrlResolver {

    private final MediaUrlResolver mediaUrlResolver;
    private final OAuthAccountRepository oauthAccountRepository;

    public String resolve(Long userId, Long avatarImageId) {
        if (userId == null) {
            return null;
        }

        URI mediaUrl = mediaUrlResolver.resolve(avatarImageId);
        if (mediaUrl != null) {
            return mediaUrl.toString();
        }
        var oauthAccount = oauthAccountRepository.findByUserIdAndProvider(
                userId,
                OAuthProvider.GITHUB
        );
        return oauthAccount == null
                ? null
                : oauthAccount.map(OAuthAccount::getProviderAvatarUrl).orElse(null);
    }

    public Map<Long, String> resolveAll(Collection<AvatarReference> references) {
        if (references == null || references.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> avatarImageIdsByUserId = new LinkedHashMap<>();
        for (AvatarReference reference : references) {
            if (reference == null || reference.userId() == null) {
                continue;
            }
            Long currentImageId = avatarImageIdsByUserId.get(reference.userId());
            if (!avatarImageIdsByUserId.containsKey(reference.userId())
                    || currentImageId == null) {
                avatarImageIdsByUserId.put(reference.userId(), reference.avatarImageId());
            }
        }
        if (avatarImageIdsByUserId.isEmpty()) {
            return Map.of();
        }

        Set<Long> mediaIds = avatarImageIdsByUserId.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, URI> resolvedMediaUrls = mediaIds.isEmpty()
                ? Map.of()
                : mediaUrlResolver.resolveAll(mediaIds);
        Map<Long, URI> mediaUrls = resolvedMediaUrls == null ? Map.of() : resolvedMediaUrls;
        List<OAuthAccount> oauthAccounts = oauthAccountRepository
                .findAllByUserIdsAndProvider(avatarImageIdsByUserId.keySet(), OAuthProvider.GITHUB);
        Map<Long, String> safeGithubAvatarUrls = (oauthAccounts == null ? List.<OAuthAccount>of() : oauthAccounts)
                .stream()
                .filter(account -> account.getProviderAvatarUrl() != null)
                .collect(Collectors.toMap(
                        OAuthAccount::getUserId,
                        OAuthAccount::getProviderAvatarUrl,
                        (first, ignored) -> first
                ));

        Map<Long, String> resolvedUrls = new LinkedHashMap<>();
        avatarImageIdsByUserId.forEach((userId, avatarImageId) -> {
            URI mediaUrl = avatarImageId == null ? null : mediaUrls.get(avatarImageId);
            if (mediaUrl != null) {
                resolvedUrls.put(userId, mediaUrl.toString());
                return;
            }
            String githubAvatarUrl = safeGithubAvatarUrls.get(userId);
            if (githubAvatarUrl != null) {
                resolvedUrls.put(userId, githubAvatarUrl);
            }
        });
        return Map.copyOf(resolvedUrls);
    }

    public record AvatarReference(Long userId, Long avatarImageId) {
    }
}
