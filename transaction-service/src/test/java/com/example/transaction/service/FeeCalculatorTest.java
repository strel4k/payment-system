package com.example.transaction.service;

import com.example.transaction.config.AppProperties;
import com.example.transaction.entity.enums.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FeeCalculatorTest {

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private FeeCalculator feeCalculator;

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.getDepositFeePercent()).thenReturn(BigDecimal.ZERO);
        lenient().when(appProperties.getWithdrawalFeePercent()).thenReturn(new BigDecimal("0.01"));
        lenient().when(appProperties.getTransferFeePercent()).thenReturn(new BigDecimal("0.005"));
    }

    @Test
    @DisplayName("should calculate zero fee for deposit")
    void shouldCalculateZeroFeeForDeposit() {
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal fee = feeCalculator.calculateFee(amount, PaymentType.DEPOSIT);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should calculate 1% fee for withdrawal")
    void shouldCalculateOnePercentFeeForWithdrawal() {
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal fee = feeCalculator.calculateFee(amount, PaymentType.WITHDRAWAL);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    @Test
    @DisplayName("should calculate 0.5% fee for transfer")
    void shouldCalculateHalfPercentFeeForTransfer() {
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal fee = feeCalculator.calculateFee(amount, PaymentType.TRANSFER);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("0.5000"));
    }

    @Test
    @DisplayName("should return zero fee for null amount")
    void shouldReturnZeroFeeForNullAmount() {
        BigDecimal fee = feeCalculator.calculateFee(null, PaymentType.WITHDRAWAL);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should return zero fee for zero amount")
    void shouldReturnZeroFeeForZeroAmount() {
        BigDecimal fee = feeCalculator.calculateFee(BigDecimal.ZERO, PaymentType.WITHDRAWAL);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should return zero fee for negative amount")
    void shouldReturnZeroFeeForNegativeAmount() {
        BigDecimal fee = feeCalculator.calculateFee(new BigDecimal("-100.00"), PaymentType.WITHDRAWAL);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should calculate total amount including fee")
    void shouldCalculateTotalAmountIncludingFee() {
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal total = feeCalculator.calculateTotalAmount(amount, PaymentType.WITHDRAWAL);
        assertThat(total).isEqualByComparingTo(new BigDecimal("101.00"));
    }

    @Test
    @DisplayName("should handle large amounts correctly")
    void shouldHandleLargeAmountsCorrectly() {
        BigDecimal amount = new BigDecimal("1000000.00");
        BigDecimal fee = feeCalculator.calculateFee(amount, PaymentType.WITHDRAWAL);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("10000.0000"));
    }

    @Test
    @DisplayName("should handle small amounts with precision")
    void shouldHandleSmallAmountsWithPrecision() {
        BigDecimal amount = new BigDecimal("0.01");
        BigDecimal fee = feeCalculator.calculateFee(amount, PaymentType.WITHDRAWAL);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("0.0001"));
    }
}
