package com.example.payment.service;

import com.example.payment.dto.Payout;
import com.example.payment.dto.PayoutRequest;
import com.example.payment.entity.Merchant;
import com.example.payment.entity.OperationStatus;
import com.example.payment.exception.EntityNotFoundException;
import com.example.payment.exception.ValidationException;
import com.example.payment.mapper.PayoutMapper;
import com.example.payment.repository.PayoutRepository;
import com.example.payment.security.MerchantPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock private PayoutRepository payoutRepository;
    @Mock private PayoutMapper payoutMapper;

    @InjectMocks
    private PayoutService payoutService;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = new Merchant();
        merchant.setId(1);
        merchant.setMerchantId("merchant-1");
        merchant.setSecretKey("hashed-secret");

        MerchantPrincipal principal = new MerchantPrincipal(merchant);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("createPayout — успешное создание выплаты")
    void createPayout_success() {
        PayoutRequest request = new PayoutRequest();
        request.setAmount(200.0);
        request.setCurrency("EUR");

        com.example.payment.entity.Payout savedEntity = new com.example.payment.entity.Payout();
        savedEntity.setId(1L);
        savedEntity.setMerchant(merchant);
        savedEntity.setAmount(BigDecimal.valueOf(200.0));
        savedEntity.setCurrency("EUR");
        savedEntity.setStatus(OperationStatus.PENDING);

        Payout expectedDto = new Payout();
        expectedDto.setId(1L);
        expectedDto.setStatus(Payout.StatusEnum.PENDING);

        when(payoutRepository.save(any())).thenReturn(savedEntity);
        when(payoutMapper.toDto(savedEntity)).thenReturn(expectedDto);

        Payout result = payoutService.createPayout(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Payout.StatusEnum.PENDING);
        verify(payoutRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createPayout — невалидный amount бросает ValidationException")
    void createPayout_invalidAmount_throwsValidationException() {
        PayoutRequest request = new PayoutRequest();
        request.setAmount(0.0);
        request.setCurrency("EUR");

        assertThatThrownBy(() -> payoutService.createPayout(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than 0");

        verify(payoutRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPayout — отрицательный amount бросает ValidationException")
    void createPayout_negativeAmount_throwsValidationException() {
        PayoutRequest request = new PayoutRequest();
        request.setAmount(-100.0);
        request.setCurrency("EUR");

        assertThatThrownBy(() -> payoutService.createPayout(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than 0");
    }

    @Test
    @DisplayName("createPayout — невалидная валюта бросает ValidationException")
    void createPayout_invalidCurrency_throwsValidationException() {
        PayoutRequest request = new PayoutRequest();
        request.setAmount(100.0);
        request.setCurrency("EU");

        assertThatThrownBy(() -> payoutService.createPayout(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("currency must be a 3-letter ISO 4217 code");
    }

    @Test
    @DisplayName("getPayout — возвращает выплату по id для текущего мерчанта")
    void getPayout_found_returnsDto() {
        com.example.payment.entity.Payout entity = new com.example.payment.entity.Payout();
        entity.setId(1L);
        entity.setMerchant(merchant);
        entity.setStatus(OperationStatus.PENDING);

        Payout expectedDto = new Payout();
        expectedDto.setId(1L);

        when(payoutRepository.findByIdAndMerchant(1L, merchant)).thenReturn(Optional.of(entity));
        when(payoutMapper.toDto(entity)).thenReturn(expectedDto);

        Payout result = payoutService.getPayout(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getPayout — выплата не найдена бросает EntityNotFoundException")
    void getPayout_notFound_throwsEntityNotFoundException() {
        when(payoutRepository.findByIdAndMerchant(99L, merchant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payoutService.getPayout(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Payout not found: 99");
    }

    @Test
    @DisplayName("listPayouts — без дат возвращает все выплаты мерчанта")
    void listPayouts_noDates_returnsAll() {
        com.example.payment.entity.Payout entity = new com.example.payment.entity.Payout();
        entity.setId(1L);

        Payout dto = new Payout();
        dto.setId(1L);

        when(payoutRepository.findAllByMerchant(merchant)).thenReturn(List.of(entity));
        when(payoutMapper.toDto(entity)).thenReturn(dto);

        List<Payout> result = payoutService.listPayouts(null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("listPayouts — start_date после end_date бросает ValidationException")
    void listPayouts_invalidDateRange_throwsValidationException() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusDays(1);

        assertThatThrownBy(() -> payoutService.listPayouts(start, end))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("start_date must be before end_date");
    }

    @Test
    @DisplayName("listPayouts — возвращает список выплат за период")
    void listPayouts_validRange_returnsList() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        com.example.payment.entity.Payout entity = new com.example.payment.entity.Payout();
        entity.setId(2L);

        Payout dto = new Payout();
        dto.setId(2L);

        when(payoutRepository.findAllByMerchantAndCreatedAtBetween(merchant, start, end))
                .thenReturn(List.of(entity));
        when(payoutMapper.toDto(entity)).thenReturn(dto);

        List<Payout> result = payoutService.listPayouts(start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }
}