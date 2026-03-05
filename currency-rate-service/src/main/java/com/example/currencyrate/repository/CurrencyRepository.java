package com.example.currencyrate.repository;

import com.example.currencyrate.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    List<Currency> findAllByActiveTrue();
    boolean existsByCode(String code);
}
