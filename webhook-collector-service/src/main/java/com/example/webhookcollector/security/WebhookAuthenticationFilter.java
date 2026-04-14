package com.example.webhookcollector.security;

import com.example.webhookcollector.exception.WebhookAuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class WebhookAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER     = "X-Webhook-Token";
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String WEBHOOK_PATH     = "/api/v1/webhooks/";

    private final WebhookSecurityService securityService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(WEBHOOK_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token     = request.getHeader(TOKEN_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String rawBody = new String(
                StreamUtils.copyToByteArray(cachedRequest.getInputStream()),
                StandardCharsets.UTF_8
        );

        try {
            securityService.validateToken(token);
            securityService.verifyHmacSignature(rawBody, signature);
        } catch (WebhookAuthenticationException ex) {
            log.warn("Webhook authentication failed: {}", ex.getMessage());
            sendUnauthorized(response, ex.getMessage());
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Map.of("error", "UNAUTHORIZED", "message", message));
    }
}