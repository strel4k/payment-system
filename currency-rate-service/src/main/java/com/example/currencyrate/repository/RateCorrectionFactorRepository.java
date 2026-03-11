package com.example.currencyrate.repository;

import com.example.currencyrate.entity.RateCorrectionFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateCorrectionFactorRepository extends JpaRepository<RateCorrectionFactor, Long> {

    Optional<RateCorrectionFactor> findBySourceCodeAndDestinationCodeAndActiveTrue(
            String sourceCode, String destinationCode);
}