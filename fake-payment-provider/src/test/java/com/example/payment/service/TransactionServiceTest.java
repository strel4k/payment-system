package com.example.payment.service;

import com.example.payment.dto.Transaction;
import com.example.payment.dto.TransactionRequest;
import com.example.payment.entity.Merchant;
import com.example.payment.entity.OperationStatus;
import com.example.payment.exception.EntityNotFoundException;
import com.example.payment.exception.ValidationException;
import com.example.payment.mapper.TransactionMapper;
import com.example.payment.repository.TransactionRepository;
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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

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
    @DisplayName("createTransaction — успешное создание транзакции")
    void createTransaction_success() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(100.0);
        request.setCurrency("USD");
        request.setMethod("CARD");

        com.example.payment.entity.Transaction savedEntity = new com.example.payment.entity.Transaction();
        savedEntity.setId(1L);
        savedEntity.setMerchant(merchant);
        savedEntity.setAmount(BigDecimal.valueOf(100.0));
        savedEntity.setCurrency("USD");
        savedEntity.setMethod("CARD");
        savedEntity.setStatus(OperationStatus.PENDING);

        Transaction expectedDto = new Transaction();
        expectedDto.setId(1L);
        expectedDto.setStatus(Transaction.StatusEnum.PENDING);

        when(transactionRepository.save(any())).thenReturn(savedEntity);
        when(transactionMapper.toDto(savedEntity)).thenReturn(expectedDto);

        Transaction result = transactionService.createTransaction(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Transaction.StatusEnum.PENDING);
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createTransaction — невалидный amount (0) бросает ValidationException")
    void createTransaction_invalidAmount_throwsValidationException() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(0.0);
        request.setCurrency("USD");
        request.setMethod("CARD");

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than 0");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction — отрицательный amount бросает ValidationException")
    void createTransaction_negativeAmount_throwsValidationException() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(-50.0);
        request.setCurrency("USD");
        request.setMethod("CARD");

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be greater than 0");
    }

    @Test
    @DisplayName("createTransaction — невалидная валюта бросает ValidationException")
    void createTransaction_invalidCurrency_throwsValidationException() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(100.0);
        request.setCurrency("US");
        request.setMethod("CARD");

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("currency must be a 3-letter ISO 4217 code");
    }

    @Test
    @DisplayName("createTransaction — неподдерживаемый метод оплаты бросает ValidationException")
    void createTransaction_unsupportedMethod_throwsValidationException() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(100.0);
        request.setCurrency("USD");
        request.setMethod("UNKNOWN_METHOD");

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unsupported payment method");
    }

    @Test
    @DisplayName("getTransaction — возвращает транзакцию по id для текущего мерчанта")
    void getTransaction_found_returnsDto() {
        com.example.payment.entity.Transaction entity = new com.example.payment.entity.Transaction();
        entity.setId(1L);
        entity.setMerchant(merchant);
        entity.setStatus(OperationStatus.PENDING);

        Transaction expectedDto = new Transaction();
        expectedDto.setId(1L);

        when(transactionRepository.findByIdAndMerchant(1L, merchant)).thenReturn(Optional.of(entity));
        when(transactionMapper.toDto(entity)).thenReturn(expectedDto);

        Transaction result = transactionService.getTransaction(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTransaction — транзакция не найдена бросает EntityNotFoundException")
    void getTransaction_notFound_throwsEntityNotFoundException() {
        when(transactionRepository.findByIdAndMerchant(99L, merchant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Transaction not found: 99");
    }

    @Test
    @DisplayName("listTransactions — start_date после end_date бросает ValidationException")
    void listTransactions_invalidDateRange_throwsValidationException() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusDays(1);

        assertThatThrownBy(() -> transactionService.listTransactions(start, end))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("start_date must be before end_date");
    }

    @Test
    @DisplayName("listTransactions — возвращает список транзакций за период")
    void listTransactions_validRange_returnsList() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        com.example.payment.entity.Transaction entity = new com.example.payment.entity.Transaction();
        entity.setId(1L);

        Transaction dto = new Transaction();
        dto.setId(1L);

        when(transactionRepository.findAllByMerchantAndCreatedAtBetween(merchant, start, end))
                .thenReturn(List.of(entity));
        when(transactionMapper.toDto(entity)).thenReturn(dto);

        List<Transaction> result = transactionService.listTransactions(start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }
}