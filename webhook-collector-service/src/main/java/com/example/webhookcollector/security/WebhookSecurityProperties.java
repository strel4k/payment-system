package com.example.webhookcollector.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webhook.security")
public class WebhookSecurityProperties {

    private String token;
    private String hmacSecret;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }
}