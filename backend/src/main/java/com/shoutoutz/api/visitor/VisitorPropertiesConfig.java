package com.shoutoutz.api.visitor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VisitorProperties.class)
class VisitorPropertiesConfig {
}
