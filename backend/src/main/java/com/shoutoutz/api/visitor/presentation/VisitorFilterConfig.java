package com.shoutoutz.api.visitor.presentation;

import com.shoutoutz.api.visitor.VisitorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class VisitorFilterConfig {

    @Bean
    VisitorCookieFilter visitorCookieFilter(VisitorProperties visitorProperties) {
        return new VisitorCookieFilter(visitorProperties);
    }
}
