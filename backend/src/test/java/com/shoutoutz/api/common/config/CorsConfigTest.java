package com.shoutoutz.api.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
        controllers = CorsConfigTest.TestApi.class,
        properties = "cors.allowed-origin-patterns=http://localhost:5173,"
                + "https://shout-ou.tz,https://*.shout-ou.tz"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(CorsConfig.class)
class CorsConfigTest {

    private static final String API_PATH = "/api/test";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:5173",
            "https://shout-ou.tz",
            "https://dev.shout-ou.tz"
    })
    @DisplayName("설정된 Origin과 서브도메인 패턴의 CORS 사전 요청을 허용한다")
    void allowsConfiguredOriginPatterns(String origin) throws Exception {
        mockMvc.perform(corsPreflight(origin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        "true"
                ));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://attacker.example",
            "https://shout-ou.tz.attacker.example"
    })
    @DisplayName("허용 패턴에 일치하지 않는 Origin의 CORS 사전 요청을 거부한다")
    void rejectsUnknownOrigins(String origin) throws Exception {
        mockMvc.perform(corsPreflight(origin))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private static MockHttpServletRequestBuilder corsPreflight(String origin) {
        return options(API_PATH)
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name());
    }

    @RestController
    static class TestApi {

        @GetMapping(API_PATH)
        void get() {
        }
    }
}
