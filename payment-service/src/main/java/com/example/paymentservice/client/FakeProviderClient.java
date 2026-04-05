package com.example.paymentservice.client;

import com.example.paymentservice.client.dto.FppTransactionRequest;
import com.example.paymentservice.client.dto.FppTransactionResponse;
import com.example.paymentservice.config.FakeProviderProperties;
import com.example.paymentservice.exception.PaymentProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;

@Slf4j
@Component
public class FakeProviderClient {

    private static final String TRANSACTIONS_PATH = "/api/v1/transactions";

    private final RestTemplate restTemplate;
    private final FakeProviderProperties properties;

    public FakeProviderClient(RestTemplate restTemplate, FakeProviderProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public FppTransactionResponse createTransaction(BigDecimal amount,
                                                    String currency,
                                                    String method) {
        String url = properties.getBaseUrl() + TRANSACTIONS_PATH;
        log.info("Calling FPP: POST {} amount={} currency={} method={}", url, amount, currency, method);

        FppTransactionRequest request = new FppTransactionRequest(
                amount.doubleValue(),
                currency,
                method,
                null,
                null
        );

        HttpHeaders headers = buildHeaders();
        HttpEntity<FppTransactionRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<FppTransactionResponse> response = restTemplate.postForEntity(
                    url, entity, FppTransactionResponse.class
            );
            FppTransactionResponse body = response.getBody();
            if (body == null) {
                throw new PaymentProviderException(
                        "Payment provider returned empty response",
                        HttpStatus.BAD_GATEWAY
                );
            }
            log.info("FPP response: transactionId={} status={}", body.id(), body.status());
            return body;

        } catch (HttpClientErrorException e) {
            log.error("FPP client error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentProviderException(
                    "Payment provider rejected the request: " + e.getResponseBodyAsString(),
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    e
            );
        } catch (HttpServerErrorException e) {
            log.error("FPP server error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentProviderException(
                    "Payment provider internal error: " + e.getResponseBodyAsString(),
                    HttpStatus.BAD_GATEWAY,
                    e
            );
        } catch (Exception e) {
            log.error("FPP unexpected error: {}", e.getMessage(), e);
            throw new PaymentProviderException(
                    "Payment provider unavailable: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY,
                    e
            );
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader());
        return headers;
    }

    private String buildBasicAuthHeader() {
        String credentials = properties.getUsername() + ":" + properties.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}