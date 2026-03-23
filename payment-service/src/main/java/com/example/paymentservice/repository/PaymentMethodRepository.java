package com.example.paymentservice.repository;

import com.example.paymentservice.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {

    @Query("""
            SELECT DISTINCT pm FROM PaymentMethod pm
            JOIN pm.definitions d
            WHERE pm.isActive = true
              AND pm.profileType = 'INDIVIDUAL'
              AND d.isActive = true
              AND (d.isAllCurrencies = true OR d.currencyCode = :currencyCode)
              AND (d.isAllCountries = true  OR d.countryAlpha3Code = :countryCode)
            """)
    List<PaymentMethod> findActiveByCurrencyAndCountry(
            @Param("currencyCode") String currencyCode,
            @Param("countryCode") String countryCode
    );
}