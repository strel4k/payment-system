package com.example.webhookcollector.security;

import com.example.webhookcollector.exception.WebhookAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookSecurityServiceTest {

    @Mock
    private WebhookSecurityProperties properties;

    @InjectMocks
    private WebhookSecurityService securityService;

    private static final String VALID_TOKEN = "test-secret-token";
    private static final String HMAC_SECRET = "test-hmac-secret";
    private static final String BODY        = "{\"type\":\"PAYMENT_STATUS_UPDATED\"}";

    @BeforeEach
    void setUp() {
        when(properties.getToken()).thenReturn(VALID_TOKEN);
        when(properties.getHmacSecret()).thenReturn(HMAC_SECRET);
    }

    // ── validateToken ─────────────────────────────────────────────

    @Test
    @DisplayName("validateToken — верный токен → без исключений")
    void validateToken_validToken_noException() {
        assertThatCode(() -> securityService.validateToken(VALID_TOKEN))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateToken — неверный токен → WebhookAuthenticationException")
    void validateToken_invalidToken_throwsException() {
        assertThatThrownBy(() -> securityService.validateToken("wrong-token"))
                .isInstanceOf(WebhookAuthenticationException.class)
                .hasMessage("Invalid webhook token");
    }

    @Test
    @DisplayName("validateToken — null токен → WebhookAuthenticationException")
    void validateToken_nullToken_throwsException() {
        assertThatThrownBy(() -> securityService.validateToken(null))
                .isInstanceOf(WebhookAuthenticationException.class);
    }

    // ── verifyHmacSignature ───────────────────────────────────────

    @Test
    @DisplayName("verifyHmacSignature — верная подпись → без исключений")
    void verifyHmacSignature_validSignature_noException() {
        String validSignature = computeExpectedHmac(BODY, HMAC_SECRET);

        assertThatCode(() -> securityService.verifyHmacSignature(BODY, validSignature))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verifyHmacSignature — неверная подпись → WebhookAuthenticationException")
    void verifyHmacSignature_invalidSignature_throwsException() {
        assertThatThrownBy(() -> securityService.verifyHmacSignature(BODY, "invalid-signature"))
                .isInstanceOf(WebhookAuthenticationException.class)
                .hasMessage("Invalid webhook signature");
    }

    @Test
    @DisplayName("verifyHmacSignature — null подпись → WebhookAuthenticationException")
    void verifyHmacSignature_nullSignature_throwsException() {
        assertThatThrownBy(() -> securityService.verifyHmacSignature(BODY, null))
                .isInstanceOf(WebhookAuthenticationException.class)
                .hasMessage("Missing X-Webhook-Signature header");
    }

    @Test
    @DisplayName("verifyHmacSignature — пустая подпись → WebhookAuthenticationException")
    void verifyHmacSignature_blankSignature_throwsException() {
        assertThatThrownBy(() -> securityService.verifyHmacSignature(BODY, "   "))
                .isInstanceOf(WebhookAuthenticationException.class)
                .hasMessage("Missing X-Webhook-Signature header");
    }

    @Test
    @DisplayName("verifyHmacSignature — подпись от другого тела → WebhookAuthenticationException")
    void verifyHmacSignature_signatureForDifferentBody_throwsException() {
        String signatureForOtherBody = computeExpectedHmac("{\"type\":\"OTHER\"}", HMAC_SECRET);

        assertThatThrownBy(() -> securityService.verifyHmacSignature(BODY, signatureForOtherBody))
                .isInstanceOf(WebhookAuthenticationException.class)
                .hasMessage("Invalid webhook signature");
    }

    // ── helper ────────────────────────────────────────────────────

    private String computeExpectedHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}