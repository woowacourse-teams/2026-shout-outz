package com.shoutoutz.api.common.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerUiController {

    @GetMapping({"/docs", "/docs/"})
    public String redirectToSwaggerUi() {
        return "redirect:/docs/swaggerui/index.html";
    }
}
