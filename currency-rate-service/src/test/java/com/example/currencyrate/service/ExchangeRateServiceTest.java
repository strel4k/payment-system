package com.example.currencyrate.service;

import com.example.currencyrate.client.ExternalRateProviderClient;
import com.example.currencyrate.config.ExchangeRateProperties;
import com.example.currencyrate.entity.ConversionRate;
import com.example.currencyrate.entity.Currency;
import com.example.currencyrate.entity.RateProvider;
import com.example.currencyrate.exception.InvalidCurrencyException;
import com.example.currencyrate.exception.RateNotFoundException;
import com.example.currencyrate.repository.ConversionRateRepository;
import com.example.currencyrate.repository.CurrencyRepository;
import com.example.currencyrate.repository.RateCorrectionFactorRepository;
import com.example.currencyrate.repository.RateProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock CurrencyRepository currencyRepository;
    @Mock RateProviderRepository rateProviderRepository;
    @Mock ConversionRateRepository conversionRateRepository;
    @Mock RateCorrectionFactorRepository correctionFactorRepository;
    @Mock ExternalRateProviderClient externalClient;
    @Mock RatePersistenceService ratePersistenceService;
    @Mock ExchangeRateProperties props;

    @InjectMocks
    ExchangeRateService service;

    private RateProvider provider;
    private Currency usd, eur, rub;

    @BeforeEach
    void setUp() {
        provider = new RateProvider("EXR", "ExchangeRate-API", 1, true);
        usd = new Currency(1L, "USD", "USD", "US Dollar", "$", true);
        eur = new Currency(2L, "EUR", "EUR", "Euro", "€", true);
        rub = new Currency(3L, "RUB", "RUB", "Russian Ruble", "₽", true);

        lenient().when(props.getRateTtlHours()).thenReturn(2);
    }

    // ==================== getRate ====================

    @Test
    @DisplayName("getRate returns rate from DB when timestamp provided")
    void getRate_withTimestamp_returnsRateFromDb() {
        LocalDateTime ts = LocalDateTime.of(2025, 6, 1, 10, 0);
        ConversionRate expected = ConversionRate.builder()
                .sourceCode("USD").destinationCode("EUR")
                .rate(new BigDecimal("0.92000000"))
                .rateBeginTime(ts.minusHours(1)).rateEndTime(ts.plusHours(1))
                .providerCode("EXR").build();

        when(currencyRepository.existsByCode("USD")).thenReturn(true);
        when(currencyRepository.existsByCode("EUR")).thenReturn(true);
        when(conversionRateRepository.findRateAtTimestamp("USD", "EUR", ts))
                .thenReturn(Optional.of(expected));

        ConversionRate result = service.getRate("USD", "EUR", ts);

        assertThat(result.getRate()).isEqualByComparingTo("0.92");
        assertThat(result.getSourceCode()).isEqualTo("USD");
        assertThat(result.getDestinationCode()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("getRate returns latest rate when no timestamp provided")
    void getRate_withoutTimestamp_returnsLatestRate() {
        ConversionRate expected = ConversionRate.builder()
                .sourceCode("USD").destinationCode("RUB")
                .rate(new BigDecimal("88.50000000"))
                .rateBeginTime(LocalDateTime.now().minusHours(1))
                .rateEndTime(LocalDateTime.now().plusHours(1))
                .providerCode("EXR").build();

        when(currencyRepository.existsByCode("USD")).thenReturn(true);
        when(currencyRepository.existsByCode("RUB")).thenReturn(true);
        when(conversionRateRepository.findLatestRate("USD", "RUB"))
                .thenReturn(Optional.of(expected));

        ConversionRate result = service.getRate("USD", "RUB", null);

        assertThat(result.getRate()).isEqualByComparingTo("88.50");
    }

    @Test
    @DisplayName("getRate returns identity rate for same currency")
    void getRate_sameCurrency_returnsOne() {
        when(currencyRepository.existsByCode("USD")).thenReturn(true);

        ConversionRate result = service.getRate("USD", "USD", null);

        assertThat(result.getRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.getProviderCode()).isEqualTo("IDENTITY");
    }

    @Test
    @DisplayName("getRate throws InvalidCurrencyException for unknown code")
    void getRate_unknownCurrency_throwsException() {
        when(currencyRepository.existsByCode("XYZ")).thenReturn(false);

        assertThatThrownBy(() -> service.getRate("XYZ", "EUR", null))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    @DisplayName("getRate throws RateNotFoundException when DB returns empty")
    void getRate_notFoundInDb_throwsException() {
        when(currencyRepository.existsByCode("USD")).thenReturn(true);
        when(currencyRepository.existsByCode("EUR")).thenReturn(true);
        when(conversionRateRepository.findLatestRate("USD", "EUR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRate("USD", "EUR", null))
                .isInstanceOf(RateNotFoundException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("EUR");
    }

    // ==================== updateRates ====================

    @Test
    @DisplayName("updateRates correctly calculates cross-rate and saves all pairs")
    void updateRates_savesAllCurrencyPairs() {
        when(currencyRepository.findAllByActiveTrue()).thenReturn(List.of(usd, eur, rub));
        when(rateProviderRepository.findAllByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(provider));
        when(externalClient.fetchBaseRates()).thenReturn(Map.of(
                "USD", new BigDecimal("1.0"),
                "EUR", new BigDecimal("0.92"),
                "RUB", new BigDecimal("88.5")
        ));
        when(correctionFactorRepository.findBySourceCodeAndDestinationCodeAndActiveTrue(anyString(), anyString()))
                .thenReturn(Optional.empty());

        service.updateRates();

        // 3 currencies - 3×2 = 6 pairs
        verify(ratePersistenceService, times(6))
                .savePair(anyString(), anyString(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("updateRates: cross-rate calculation is correct")
    void updateRates_crossRateCalculation_isCorrect() {
        when(currencyRepository.findAllByActiveTrue()).thenReturn(List.of(usd, eur));
        when(rateProviderRepository.findAllByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(provider));
        when(externalClient.fetchBaseRates()).thenReturn(Map.of(
                "USD", new BigDecimal("1.0"),
                "EUR", new BigDecimal("0.9200")
        ));
        when(correctionFactorRepository.findBySourceCodeAndDestinationCodeAndActiveTrue(anyString(), anyString()))
                .thenReturn(Optional.empty());

        service.updateRates();

        var sourceCaptor = ArgumentCaptor.forClass(String.class);
        var destCaptor = ArgumentCaptor.forClass(String.class);
        var rateCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        var beginCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        var endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        var providerCaptor = ArgumentCaptor.forClass(String.class);

        verify(ratePersistenceService, times(2)).savePair(
                sourceCaptor.capture(), destCaptor.capture(),
                rateCaptor.capture(), beginCaptor.capture(),
                endCaptor.capture(), providerCaptor.capture());

        // Find USD→EUR rate
        int usdToEurIdx = destCaptor.getAllValues().indexOf("EUR");
        assertThat(rateCaptor.getAllValues().get(usdToEurIdx)).isEqualByComparingTo("0.92");

        // Find EUR→USD rate = 1.0869
        int eurToUsdIdx = destCaptor.getAllValues().indexOf("USD");
        assertThat(rateCaptor.getAllValues().get(eurToUsdIdx))
                .isGreaterThan(new BigDecimal("1.08"))
                .isLessThan(new BigDecimal("1.09"));
    }

    @Test
    @DisplayName("updateRates throws when external API returns empty")
    void updateRates_emptyApiResponse_throwsException() {
        when(currencyRepository.findAllByActiveTrue()).thenReturn(List.of(usd, eur));
        when(rateProviderRepository.findAllByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(provider));
        when(externalClient.fetchBaseRates()).thenReturn(Map.of());

        assertThatThrownBy(() -> service.updateRates())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("External API returned no rates");
    }

    @Test
    @DisplayName("updateRates skips pair when external API missing currency")
    void updateRates_missingCurrencyInApiResponse_skipsPair() {
        when(currencyRepository.findAllByActiveTrue()).thenReturn(List.of(usd, eur, rub));
        when(rateProviderRepository.findAllByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(provider));
        when(externalClient.fetchBaseRates()).thenReturn(Map.of(
                "USD", new BigDecimal("1.0"),
                "EUR", new BigDecimal("0.92")
        ));
        when(correctionFactorRepository.findBySourceCodeAndDestinationCodeAndActiveTrue(anyString(), anyString()))
                .thenReturn(Optional.empty());

        service.updateRates();

        verify(ratePersistenceService, times(2))
                .savePair(anyString(), anyString(), any(), any(), any(), anyString());
    }
}