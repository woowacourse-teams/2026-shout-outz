package com.shoutoutz.api.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyCollection;

import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAvatarUrlResolverTest {

    private static final Instant AUTHENTICATED_AT = Instant.parse("2026-09-30T00:00:00Z");

    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    @Test
    void usesGithubAvatarWhenCustomImageIsMissing() {
        String githubAvatarUrl = "https://avatars.githubusercontent.com/u/1";
        given(oauthAccountRepository.findByUserIdAndProvider(1L, OAuthProvider.GITHUB))
                .willReturn(Optional.of(oauthAccount(1L, githubAvatarUrl)));

        UserAvatarUrlResolver resolver = new UserAvatarUrlResolver(
                mediaUrlResolver,
                oauthAccountRepository
        );

        assertThat(resolver.resolve(1L, null)).isEqualTo(githubAvatarUrl);
    }

    @Test
    void prefersCustomImageOverGithubAvatar() {
        URI mediaUrl = URI.create("https://cdn.example.com/avatar-10");
        given(mediaUrlResolver.resolve(10L)).willReturn(mediaUrl);

        UserAvatarUrlResolver resolver = new UserAvatarUrlResolver(
                mediaUrlResolver,
                oauthAccountRepository
        );

        assertThat(resolver.resolve(1L, 10L)).isEqualTo(mediaUrl.toString());
        verify(oauthAccountRepository, org.mockito.Mockito.never())
                .findByUserIdAndProvider(1L, OAuthProvider.GITHUB);
    }

    @Test
    void resolvesBatchWithCustomImagePriorityAndGithubFallback() {
        String githubAvatarUrl = "https://avatars.githubusercontent.com/u/2";
        given(mediaUrlResolver.resolveAll(java.util.Set.of(10L)))
                .willReturn(Map.of(10L, URI.create("https://cdn.example.com/avatar-10")));
        given(oauthAccountRepository.findAllByUserIdsAndProvider(
                anyCollection(),
                org.mockito.ArgumentMatchers.eq(OAuthProvider.GITHUB)
        )).willReturn(List.of(
                oauthAccount(1L, "https://avatars.githubusercontent.com/u/1"),
                oauthAccount(2L, githubAvatarUrl)
        ));

        UserAvatarUrlResolver resolver = new UserAvatarUrlResolver(
                mediaUrlResolver,
                oauthAccountRepository
        );

        Map<Long, String> urls = resolver.resolveAll(List.of(
                new UserAvatarUrlResolver.AvatarReference(1L, 10L),
                new UserAvatarUrlResolver.AvatarReference(2L, null)
        ));

        assertThat(urls).containsExactlyInAnyOrderEntriesOf(Map.of(
                1L, "https://cdn.example.com/avatar-10",
                2L, githubAvatarUrl
        ));
    }

    private OAuthAccount oauthAccount(long userId, String avatarUrl) {
        return OAuthAccount.initialize(
                userId,
                OAuthProvider.GITHUB,
                String.valueOf(userId),
                avatarUrl,
                AUTHENTICATED_AT
        );
    }
}
