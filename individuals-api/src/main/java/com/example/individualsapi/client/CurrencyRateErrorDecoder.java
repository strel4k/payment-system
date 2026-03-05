package com.example.individualsapi.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class CurrencyRateErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());
        log.error("Currency rate service error: method={}, status={}", methodKey, status);

        return switch (status) {
            case NOT_FOUND -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Exchange rate not found for requested currency pair");
            case BAD_REQUEST -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid currency code provided");
            default -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Currency rate service unavailable");
        };
    }
}