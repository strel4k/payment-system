package com.example.webhookcollector.config;

import com.example.webhookcollector.security.WebhookSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebhookSecurityProperties.class)
public class AppConfig {
}
