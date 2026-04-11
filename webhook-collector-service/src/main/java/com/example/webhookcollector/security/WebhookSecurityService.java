package com.example.webhookcollector.security;

import com.example.webhookcollector.exception.WebhookAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookSecurityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final WebhookSecurityProperties properties;

    public void validateToken(String incomingToken) {
        if (!properties.getToken().equals(incomingToken)) {
            log.warn("Webhook token validation failed: invalid token");
            throw new WebhookAuthenticationException("Invalid webhook token");
        }
    }

    public void verifyHmacSignature(String rawBody, String incomingSignature) {
        if (incomingSignature == null || incomingSignature.isBlank()) {
            log.warn("Webhook HMAC verification failed: missing signature header");
            throw new WebhookAuthenticationException("Missing X-Webhook-Signature header");
        }

        String expectedSignature = computeHmac(rawBody);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                incomingSignature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Webhook HMAC verification failed: signature mismatch");
            throw new WebhookAuthenticationException("Invalid webhook signature");
        }
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.getHmacSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return HexFormat.of().formatHex(
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC signature", e);
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }
}