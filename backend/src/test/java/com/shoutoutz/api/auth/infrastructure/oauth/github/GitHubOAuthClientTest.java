package com.shoutoutz.api.auth.infrastructure.oauth.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class GitHubOAuthClientTest {

    private static final String TOKEN_URI = "https://github.com/login/oauth/access_token";
    private static final String USER_URI = "https://api.github.com/user";

    @Test
    @DisplayName("Authorization Code를 교환하고 GitHub 사용자 신원을 조회한다")
    void fetchesGitHubIdentity() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GitHubOAuthProperties properties = new GitHubOAuthProperties(
                "test-client-id",
                "test-client-secret",
                URI.create("http://localhost:8080/login/oauth2/code/github")
        );
        GitHubOAuthClient client = new GitHubOAuthClient(restClientBuilder, properties);

        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("client_id", "test-client-id");
        tokenRequest.add("client_secret", "test-client-secret");
        tokenRequest.add("code", "authorization-code");
        tokenRequest.add("redirect_uri", "http://localhost:8080/login/oauth2/code/github");
        tokenRequest.add("code_verifier", "code-verifier");

        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(POST))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().formData(tokenRequest))
                .andRespond(withSuccess(
                        "{\"access_token\":\"github-access-token\"}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo(USER_URI))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer github-access-token"))
                .andRespond(withSuccess(
                        """
                                {
                                  "id": 12345678,
                                  "login": "sangjun",
                                  "avatar_url": "https://avatars.githubusercontent.com/u/12345678"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        OAuthIdentity identity = client.fetchIdentity(
                "authorization-code",
                "code-verifier"
        );

        assertThat(identity.provider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(identity.providerAccountId()).isEqualTo("12345678");
        assertThat(identity.avatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/12345678");
        server.verify();
    }
}
