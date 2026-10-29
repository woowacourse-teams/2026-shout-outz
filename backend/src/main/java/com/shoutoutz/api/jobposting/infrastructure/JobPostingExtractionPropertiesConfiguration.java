package com.shoutoutz.api.jobposting.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JobPostingExtractionProperties.class)
public class JobPostingExtractionPropertiesConfiguration {
}
