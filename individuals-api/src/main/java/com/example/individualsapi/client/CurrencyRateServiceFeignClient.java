package com.example.individualsapi.client;

import com.example.individualsapi.client.dto.currencyrate.RateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "currency-rate-service",
        url = "${currency-rate-service.base-url}",
        configuration = CurrencyRateServiceFeignConfig.class
)
public interface CurrencyRateServiceFeignClient {

    @GetMapping("/api/v1/rates")
    RateResponse getRate(@RequestParam("from") String from,
                         @RequestParam("to") String to);

    @GetMapping("/api/v1/rates")
    RateResponse getRateAt(@RequestParam("from") String from,
                           @RequestParam("to") String to,
                           @RequestParam("timestamp") String timestamp);
}