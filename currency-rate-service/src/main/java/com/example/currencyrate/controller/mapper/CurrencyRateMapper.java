package com.example.currencyrate.controller.mapper;

import com.example.currencyrate.controller.dto.CurrencyRateResponse;
import com.example.currencyrate.controller.dto.CurrencyResponse;
import com.example.currencyrate.controller.dto.RateProviderResponse;
import com.example.currencyrate.entity.ConversionRate;
import com.example.currencyrate.entity.Currency;
import com.example.currencyrate.entity.RateProvider;
import org.springframework.stereotype.Component;

@Component
public class CurrencyRateMapper {

    public CurrencyRateResponse toResponse(ConversionRate rate) {
        return new CurrencyRateResponse(
                rate.getSourceCode(),
                rate.getDestinationCode(),
                rate.getRate(),
                rate.getRateBeginTime(),
                rate.getRateEndTime(),
                rate.getProviderCode()
        );
    }

    public CurrencyResponse toResponse(Currency currency) {
        return new CurrencyResponse(
                currency.getCode(),
                currency.getIsoCode(),
                currency.getDescription(),
                currency.getSymbol(),
                currency.isActive()
        );
    }

    public RateProviderResponse toResponse(RateProvider provider) {
        return new RateProviderResponse(
                provider.getProviderCode(),
                provider.getProviderName(),
                provider.getPriority(),
                provider.isActive()
        );
    }
}