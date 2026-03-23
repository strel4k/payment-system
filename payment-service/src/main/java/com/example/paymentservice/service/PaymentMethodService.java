package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentMethodResponse;
import com.example.paymentservice.mapper.PaymentMethodMapper;
import com.example.paymentservice.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getPaymentMethods(String currencyCode, String countryCode) {
        log.info("Fetching payment methods for currency={} country={}", currencyCode, countryCode);

        List<PaymentMethodResponse> result = paymentMethodRepository
                .findActiveByCurrencyAndCountry(currencyCode, countryCode)
                .stream()
                .map(paymentMethodMapper::toDto)
                .toList();

        log.info("Found {} payment methods for currency={} country={}", result.size(), currencyCode, countryCode);
        return result;
    }
}