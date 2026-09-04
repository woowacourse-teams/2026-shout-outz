package com.shoutoutz.api.common.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SwaggerUiController.class)
class SwaggerUiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redirectsDocsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/docs/swaggerui/index.html"));
    }
}
